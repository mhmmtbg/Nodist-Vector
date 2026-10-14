package io.github.mhmmtbg.nodist.model

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Ağ gradyan ve opaklık maskesi: ikisi de düz vektör olarak, çok sayıda küçük parçayla taklit edilir.
 * (Gerçek ağ gradyan / opaklık maskesi PDF'te ayrı nesnelerdir; burada dosyaya her yerde okunan parçalar yazılır.)
 */

private fun lerp(a: Rgba, b: Rgba, t: Double) = Rgba(
    a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t, a.a + (b.a - a.a) * t,
)

/**
 * Ağ gradyan: kapalı şeklin kutusu [rows]×[cols] hücreye bölünür; her hücre, dört köşe renginden çift doğrusal
 * ara değerle boyanır ve hepsi şekle kırpılır. Konturu varsa kontur üstte ayrıca durur.
 * Köşeler: [topLeft], [topRight], [bottomLeft], [bottomRight].
 */
fun Document.meshGradient(
    nodeId: String, rows: Int, cols: Int,
    topLeft: Rgba, topRight: Rgba, bottomLeft: Rgba, bottomRight: Rgba,
): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    if (node.subpaths.none { it.closed }) return null
    val r = rows.coerceIn(2, 40)
    val c = cols.coerceIn(2, 40)
    val box = node.subpaths.mapNotNull { it.bounds() }.reduceOrNull(Rect::union) ?: return null
    if (box.width <= 1e-9 || box.height <= 1e-9) return null
    val cw = box.width / c
    val ch = box.height / r
    val cells = ArrayList<Node>(r * c)
    // Hücreler birbirine biraz binsin ki aralarında ince çizgiler görünmesin.
    val pad = min(cw, ch) * 0.02
    for (j in 0 until r) for (i in 0 until c) {
        val u = (i + 0.5) / c
        val v = (j + 0.5) / r
        val col = lerp(lerp(topLeft, topRight, u), lerp(bottomLeft, bottomRight, u), v)
        val rect = Rect(box.left + i * cw - pad, box.top + j * ch - pad, box.left + (i + 1) * cw + pad, box.top + (j + 1) * ch + pad)
        cells += PathNode(name = "Hücre", subpaths = listOf(Shapes.rect(rect)), fill = Paint.Solid(col))
    }
    val clipped = GroupNode(name = "Ağ", children = cells, clip = ClipPath(node.subpaths, node.fillRule))
    val children = ArrayList<Node>()
    children += clipped
    if (node.stroke != null) children += node.copy(id = newId(), name = "Kontur", fill = null)
    val group = GroupNode(id = node.id, name = "Ağ gradyan", opacity = node.opacity, blendMode = node.blendMode, children = children)
    return replaceNode(nodeId) { listOf(group) } to group.id
}

/** Rengin görünürlük ağırlığı (parlaklık × saydamlık): opaklık maskesinde beyaz görünür, siyah gizler. */
private fun Rgba.maskValue(): Double = ((0.299 * r + 0.587 * g + 0.114 * b) * a).coerceIn(0.0, 1.0)

private fun gradientColor(stops: List<GradientStop>, t: Double): Rgba {
    val s = stops.sortedBy { it.offset }
    if (s.isEmpty()) return Rgba.Black
    if (t <= s.first().offset) return s.first().color
    if (t >= s.last().offset) return s.last().color
    for (k in 1 until s.size) {
        if (t <= s[k].offset) {
            val a = s[k - 1]
            val b = s[k]
            val span = b.offset - a.offset
            return lerp(a.color, b.color, if (span <= 1e-12) 1.0 else (t - a.offset) / span)
        }
    }
    return s.last().color
}

/**
 * Opaklık maskesi: [maskId] doğrusal gradyanlı şeklin parlaklığı, [targetId] nesnesinin saydamlığını belirler (beyaz görünür,
 * siyah gizler). Nesne, gradyan yönünde [bands] şeride bölünür; her şerit kendi saydamlığıyla kırpılmış bir kopyadır.
 * Maske şekli sonuca dönüşür. @return yeni belge ve sonuç grubunun kimliği; maske doğrusal gradyanlı değilse `null`.
 */
fun Document.opacityMask(maskId: String, targetId: String, bands: Int = 24): Pair<Document, String>? {
    if (maskId == targetId) return null
    val mask = (findNode(maskId) as? PathNode)?.baked() ?: return null
    val grad = mask.fill as? Paint.LinearGradient ?: return null
    val target = findNode(targetId) ?: return null
    val box = boundsOf(targetId) ?: return null
    val a = grad.transform.apply(grad.start)
    val b = grad.transform.apply(grad.end)
    val len = a.distanceTo(b)
    if (len <= 1e-9) return null
    val d = Vec2((b.x - a.x) / len, (b.y - a.y) / len)
    val n = Vec2(-d.y, d.x)
    // Hedef kutusunun eksen üzerindeki izdüşümü
    var lo = Double.MAX_VALUE
    var hi = -Double.MAX_VALUE
    for (p in box.corners) {
        val s = (p.x - a.x) * d.x + (p.y - a.y) * d.y
        lo = min(lo, s)
        hi = max(hi, s)
    }
    val half = hypot(box.width, box.height) + len + 10.0
    fun slab(s0: Double, s1: Double) = SubPath(
        listOf(
            Anchor(Vec2(a.x + d.x * s0 + n.x * half, a.y + d.y * s0 + n.y * half)),
            Anchor(Vec2(a.x + d.x * s1 + n.x * half, a.y + d.y * s1 + n.y * half)),
            Anchor(Vec2(a.x + d.x * s1 - n.x * half, a.y + d.y * s1 - n.y * half)),
            Anchor(Vec2(a.x + d.x * s0 - n.x * half, a.y + d.y * s0 - n.y * half)),
        ),
        closed = true,
    )
    val count = bands.coerceIn(2, 64)
    // Aralıklar: [lo,0] başlangıç rengi, 0..len gradyan, [len,hi] bitiş rengi.
    val slabs = ArrayList<Triple<Double, Double, Double>>() // s0, s1, opaklık
    if (lo < 0.0) slabs += Triple(lo - 1.0, 0.0, gradientColor(grad.stops, 0.0).maskValue())
    for (k in 0 until count) {
        val s0 = len * k / count
        val s1 = len * (k + 1) / count
        if (s1 < lo || s0 > hi) continue
        slabs += Triple(s0, s1, gradientColor(grad.stops, (k + 0.5) / count).maskValue())
    }
    if (hi > len) slabs += Triple(len, hi + 1.0, gradientColor(grad.stops, 1.0).maskValue())
    val children = slabs.filter { it.third > 0.004 }.map { (s0, s1, o) ->
        GroupNode(name = "Şerit", opacity = o, children = listOf(target.duplicated()), clip = ClipPath(listOf(slab(s0, s1))))
    }
    val group = GroupNode(id = target.id, name = "Opaklık maskesi", children = children)
    return removeNode(maskId).replaceNode(targetId) { listOf(group) } to group.id
}
