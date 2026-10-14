package io.github.mhmmtbg.nodist.model

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Görünüm ve efekt komutları. Hepsi sonucu düz vektör olarak üretir (gölge, ok ucu, desen…), bu yüzden .ai, PDF ve SVG'ye
 * kayıpsız yazılır; canlı (sonradan düzenlenen) efekt değildir.
 */

private fun Node.at(dx: Double, dy: Double): Node = withTransform(Matrix.translate(dx, dy) * transform)

// ---- Gölge -------------------------------------------------------------------

/**
 * Seçili yol, metin ve grupların arkasına yumuşak bir gölge koyar. Bulanıklık, kademeli genişleyen yarı saydam kopyalarla
 * taklit edilir. @return yeni belge ve gölge+nesne gruplarının kimlikleri; uygun nesne yoksa `null`.
 */
fun Document.dropShadow(ids: Set<String>, dx: Double, dy: Double, blur: Double, color: Rgba, opacity: Double): Pair<Document, List<String>>? {
    var doc = this
    val made = ArrayList<String>()
    for (id in ids) {
        val node = doc.findNode(id) ?: continue
        val shadow = node.shadowLayers(dx, dy, blur, color, opacity.coerceIn(0.0, 1.0)) ?: continue
        val group = GroupNode(name = "Gölge", children = listOf(GroupNode(name = "Gölge katmanı", children = shadow), node))
        doc = doc.replaceNode(id) { listOf(group) }
        made += group.id
    }
    return if (made.isEmpty()) null else doc to made
}

private fun Node.shadowLayers(dx: Double, dy: Double, blur: Double, color: Rgba, opacity: Double): List<Node>? {
    val paint = Paint.Solid(color.copy(a = 1.0))
    val steps = if (blur <= 0.01) 1 else 8
    // k kat üst üste binince toplam saydamlık [opacity] olsun.
    val each = 1 - (1 - opacity).pow(1.0 / steps)
    fun tint(n: Node, w: Double): Node? = when (n) {
        is PathNode -> n.copy(
            id = newId(), name = "Gölge",
            fill = if (n.fill != null) paint else null,
            stroke = if (w > 0.0 || n.stroke != null) Stroke(paint, max(w, 0.0) + (n.stroke?.width ?: 0.0), LineCap.Round, LineJoin.Round) else null,
            opacity = each * n.opacity,
            blendMode = BlendMode.Normal,
        ).let { p -> if (p.fill == null && p.stroke == null) p.copy(fill = paint) else p }
        is TextNode -> n.copy(
            id = newId(), name = "Gölge", fill = paint,
            stroke = if (w > 0.0) Stroke(paint, w, LineCap.Round, LineJoin.Round) else null,
            opacity = each * n.opacity, blendMode = BlendMode.Normal,
        )
        is GroupNode -> n.copy(id = newId(), name = "Gölge", opacity = n.opacity, blendMode = BlendMode.Normal, children = n.children.mapNotNull { tint(it, w) })
        is ImageNode -> null
    }
    val layers = ArrayList<Node>()
    for (i in 0 until steps) {
        val w = if (steps == 1) 0.0 else blur * (i + 1) / steps
        val t = tint(this, w) ?: return null
        layers += t.at(dx, dy)
    }
    return layers
}

// ---- Dış kontur (ek kontur) ---------------------------------------------------

/** Her yolun arkasına [width] kalınlığında, [color] renginde bir dış kontur ekler (Illustrator'daki ikinci kontur). */
fun Document.addOuterStroke(ids: Set<String>, width: Double, color: Rgba): Pair<Document, List<String>>? {
    if (width <= 0.0) return null
    var doc = this
    val made = ArrayList<String>()
    val paint = Paint.Solid(color)
    for (id in ids) {
        val node = doc.findNode(id) as? PathNode ?: continue
        val under = node.copy(
            id = newId(), name = "Dış kontur", fill = paint,
            stroke = Stroke(paint, (node.stroke?.width ?: 0.0) + 2 * width, LineCap.Round, LineJoin.Round),
            opacity = 1.0, blendMode = BlendMode.Normal,
        )
        val group = GroupNode(name = node.name, children = listOf(under, node.copy(name = node.name)))
        doc = doc.replaceNode(id) { listOf(group) }
        made += group.id
    }
    return if (made.isEmpty()) null else doc to made
}

// ---- Değişken genişlikli kontur ------------------------------------------------

/**
 * Açık yolu uçları [startWidth] ve [endWidth] kalınlığında, aradaki kalınlık doğrusal değişen dolu bir şekle çevirir
 * (Illustrator'daki genişlik profili). Dolgu, yolun kontur boyasıdır.
 */
fun Document.taperStroke(nodeId: String, startWidth: Double, endWidth: Double): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    val paint = node.stroke?.paint ?: node.fill ?: Paint.Solid(Rgba.Black)
    val shapes = node.subpaths.filter { !it.closed && it.anchors.size >= 2 }.mapNotNull { taperedOutline(it, startWidth, endWidth) }
    if (shapes.isEmpty()) return null
    val out = node.copy(subpaths = shapes, fill = paint, stroke = null, fillRule = FillRule.NonZero)
    return replaceNode(nodeId) { listOf(out) } to out.id
}

internal fun taperedOutline(sp: SubPath, w0: Double, w1: Double): SubPath? {
    val raw = sp.flatten(24)
    val pts = ArrayList<Vec2>()
    for (p in raw) if (pts.isEmpty() || pts.last().distanceTo(p) > 1e-9) pts += p
    if (pts.size < 2) return null
    val cum = DoubleArray(pts.size)
    for (i in 1 until pts.size) cum[i] = cum[i - 1] + pts[i - 1].distanceTo(pts[i])
    val total = cum.last()
    if (total <= 1e-9) return null
    // Köşe başına ortalama normal (köşelerde bükülme için ters yöne dönmesin diye iki komşu doğrultusunun ortalaması).
    fun dir(i: Int): Vec2 {
        val a = pts[max(i - 1, 0)]
        val b = pts[min(i + 1, pts.size - 1)]
        val l = a.distanceTo(b)
        return Vec2((b.x - a.x) / l, (b.y - a.y) / l)
    }
    val left = ArrayList<Vec2>()
    val right = ArrayList<Vec2>()
    for (i in pts.indices) {
        val d = dir(i)
        val half = (w0 + (w1 - w0) * cum[i] / total) / 2
        val n = Vec2(-d.y, d.x)
        left += Vec2(pts[i].x + n.x * half, pts[i].y + n.y * half)
        right += Vec2(pts[i].x - n.x * half, pts[i].y - n.y * half)
    }
    return SubPath((left + right.reversed()).map { Anchor(it) }, closed = true)
}

// ---- Ok uçları ------------------------------------------------------------------

/**
 * Açık yolun ucuna (ve/veya başına) ok başı ekler. Ok başının uzunluğu kontur kalınlığının [scale] katıdır.
 * @return yeni belge ve yol+oklardan oluşan grubun kimliği; yolda açık uç yoksa `null`.
 */
fun Document.addArrowheads(nodeId: String, atStart: Boolean, atEnd: Boolean, scale: Double): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    val st = node.stroke ?: return null
    val open = node.subpaths.filter { !it.closed && it.anchors.size >= 2 }
    if (open.isEmpty() || (!atStart && !atEnd)) return null
    val len = max(st.width * scale, 1.0)
    val heads = ArrayList<SubPath>()
    for (sp in open) {
        val pts = ArrayList<Vec2>()
        for (p in sp.flatten(24)) if (pts.isEmpty() || pts.last().distanceTo(p) > 1e-9) pts += p
        if (pts.size < 2) continue
        if (atEnd) arrowAt(pts[pts.size - 1], pts[max(pts.size - 2, 0)], len)?.let(heads::add)
        if (atStart) arrowAt(pts[0], pts[1], len)?.let(heads::add)
    }
    if (heads.isEmpty()) return null
    val paint = st.paint
    val headNode = PathNode(name = "Ok başı", subpaths = heads, fill = paint)
    // Ok başı çizgiyi uzatmasın: yol kontur ucu yuvarlak değilse de okun altında kalır.
    val group = GroupNode(id = node.id, name = node.name, children = listOf(node.copy(id = newId()), headNode))
    return replaceNode(nodeId) { listOf(group) } to group.id
}

/** [tip] ucunda, [from] noktasından gelen yönde, uzunluğu [len] olan bir üçgen. */
private fun arrowAt(tip: Vec2, from: Vec2, len: Double): SubPath? {
    val l = tip.distanceTo(from)
    if (l < 1e-9) return null
    val d = Vec2((tip.x - from.x) / l, (tip.y - from.y) / l)
    val n = Vec2(-d.y, d.x)
    val baseC = Vec2(tip.x - d.x * len, tip.y - d.y * len)
    val half = len * 0.45
    // Uç, çizgi ucunun biraz ötesine uzanır ki çizgi başın içinde kalsın.
    return SubPath(
        listOf(
            Anchor(Vec2(tip.x + d.x * len * 0.5, tip.y + d.y * len * 0.5)),
            Anchor(Vec2(baseC.x + n.x * half, baseC.y + n.y * half)),
            Anchor(Vec2(baseC.x - n.x * half, baseC.y - n.y * half)),
        ),
        closed = true,
    )
}

// ---- Kontur hizası --------------------------------------------------------------

enum class StrokeAlign { Inside, Outside }

/**
 * Kapalı yolun konturunu şeklin içine ya da dışına hizalar. Sonuç, dolgu ve dolu bir kontur şeklinden oluşan gruptur.
 * @return yeni belge ve grubun kimliği; yolun konturu ya da kapalı alt yolu yoksa `null`.
 */
fun Document.alignStroke(nodeId: String, align: StrokeAlign, ops: RegionOps): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    val st = node.stroke ?: return null
    val closed = node.subpaths.filter { it.closed }
    if (closed.isEmpty()) return null
    val pieces = StrokeGeometry.pieces(closed, st.copy(width = st.width * 2, dash = st.dash.map { it * 2 }))
    if (pieces.isEmpty()) return null
    val band = Region(pieces, FillRule.NonZero).let { ops.union(it, it) } ?: return null
    val shape = Region(closed, node.fillRule)
    val ring = (if (align == StrokeAlign.Inside) ops.intersect(shape, band) else ops.minus(band, shape)) ?: return null
    if (ring.isEmpty) return null
    val ringNode = node.copy(id = newId(), name = "Kontur", subpaths = ring.subpaths, fillRule = ring.rule, fill = st.paint, stroke = null)
    val fillNode = node.copy(id = newId(), name = "Dolgu", stroke = null, fill = node.fill)
    val children = if (node.fill != null) listOf(fillNode, ringNode) else listOf(ringNode)
    val group = GroupNode(id = node.id, name = node.name, children = children)
    return replaceNode(nodeId) { listOf(group) } to group.id
}

// ---- Yol boyunca dizme (fırça, yazı) ---------------------------------------------

/** Bir yolun düzleştirilmiş hali; yay uzunluğuyla konum ve yön verir. */
class Ruler(sp: SubPath) {
    private val pts: List<Vec2>
    private val cum: DoubleArray
    val length: Double

    init {
        val raw = sp.flatten(24)
        val p = ArrayList<Vec2>()
        for (q in raw) if (p.isEmpty() || p.last().distanceTo(q) > 1e-9) p += q
        pts = p
        cum = DoubleArray(p.size)
        for (i in 1 until p.size) cum[i] = cum[i - 1] + p[i - 1].distanceTo(p[i])
        length = if (p.size < 2) 0.0 else cum.last()
    }

    val valid: Boolean get() = pts.size >= 2 && length > 1e-9

    /** [s] yay uzunluğundaki nokta ve birim teğet; [s] uçlarda kırpılır (yolun uzantısında kalan harfler uca yapışır). */
    fun frame(s: Double): Pair<Vec2, Vec2> {
        val c = s.coerceIn(0.0, length)
        var lo = 0
        var hi = pts.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (cum[mid] <= c) lo = mid else hi = mid
        }
        val a = pts[lo]
        val b = pts[hi]
        val seg = cum[hi] - cum[lo]
        val t = if (seg <= 1e-12) 0.0 else (c - cum[lo]) / seg
        val l = a.distanceTo(b)
        val d = if (l <= 1e-12) Vec2(1.0, 0.0) else Vec2((b.x - a.x) / l, (b.y - a.y) / l)
        return Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) to d
    }
}

/**
 * [shapes] şekillerini [guide] yolunun boyunca bükerek yerleştirir. Şekillerin x ekseni yol boyunca ilerler ve
 * [startOffset] yay uzunluğundan başlar; y ekseni yola dik (aşağı) yöndedir. Eğriler küçük parçalara bölünür.
 */
fun alongPath(shapes: List<SubPath>, guide: Ruler, startOffset: Double = 0.0, originX: Double = 0.0): List<SubPath> {
    if (!guide.valid) return emptyList()
    fun map(p: Vec2): Vec2 {
        val (pos, d) = guide.frame(startOffset + p.x - originX)
        // Yolun ötesine taşan kısım teğet yönünde uzar.
        val over = (startOffset + p.x - originX) - (startOffset + p.x - originX).coerceIn(0.0, guide.length)
        val n = Vec2(-d.y, d.x)
        return Vec2(pos.x + d.x * over + n.x * p.y, pos.y + d.y * over + n.y * p.y)
    }
    val out = ArrayList<SubPath>()
    for (sp in shapes) {
        val pts = ArrayList<Vec2>()
        for (q in sp.flatten(10)) if (pts.isEmpty() || pts.last().distanceTo(q) > 1e-9) pts += q
        if (sp.closed && pts.size > 1 && pts.first().distanceTo(pts.last()) <= 1e-9) pts.removeAt(pts.size - 1)
        if (pts.size < 2) continue
        out += SubPath(pts.map { Anchor(map(it)) }, sp.closed)
    }
    return out
}

/**
 * Sanat fırçası: [shapeId] şeklini [guideId] yolunun boyuna esneterek yola oturtur. Şekil, yolun uzunluğu kadar gerilir;
 * kalınlık [thickness] ile ölçeklenir. Yol ve şekil yerine sonuç tek bir şekil olarak gelir.
 */
fun Document.artBrush(shapeId: String, guideId: String, thickness: Double = 1.0): Pair<Document, String>? {
    val shape = (findNode(shapeId) as? PathNode)?.baked() ?: return null
    val guideNode = (findNode(guideId) as? PathNode)?.baked() ?: return null
    val g = guideNode.subpaths.firstOrNull { it.anchors.size >= 2 } ?: return null
    val ruler = Ruler(g)
    val box = shape.subpaths.mapNotNull { it.bounds() }.reduceOrNull(Rect::union) ?: return null
    if (!ruler.valid || box.width <= 1e-9) return null
    val k = ruler.length / box.width
    val cy = box.center.y
    val prepared = shape.subpaths.map { sp ->
        sp.transformed(Matrix.scale(k, thickness) * Matrix.translate(-box.left, -cy)).let { it }
    }
    // Ölçeklemeden sonra x 0..uzunluk aralığındadır.
    val bent = alongPath(prepared, ruler)
    if (bent.isEmpty()) return null
    val out = shape.copy(subpaths = bent, stroke = shape.stroke?.copy(width = shape.stroke.width * thickness))
    return removeNode(guideId).replaceNode(shapeId) { listOf(out) } to out.id
}

// ---- Desenle doldurma -------------------------------------------------------------

/**
 * Desen: [tileIds] nesnelerinin oluşturduğu karo, [targetId] kapalı şeklinin içini döşer. Karolar şekle kırpılır.
 * Karo ve hedef birbirinden farklı olmalıdır; karolar sonuca dönüştüğünden belgeden kaybolur.
 * @return yeni belge ve kırpma grubunun kimliği; sınır aşılırsa ([maxTiles]) ya da girdiler uygun değilse `null`.
 */
fun Document.tilePattern(targetId: String, tileIds: Set<String>, gapX: Double, gapY: Double, maxTiles: Int = 2500): Pair<Document, String>? {
    if (targetId in tileIds || tileIds.isEmpty()) return null
    val target = (findNode(targetId) as? PathNode)?.baked() ?: return null
    if (target.subpaths.none { it.closed }) return null
    val tiles = tileIds.mapNotNull { findNode(it) }
    if (tiles.size != tileIds.size) return null
    val tileBox = boundsOf(tileIds) ?: return null
    val area = target.subpaths.mapNotNull { it.bounds() }.reduceOrNull(Rect::union) ?: return null
    val stepX = tileBox.width + gapX
    val stepY = tileBox.height + gapY
    if (stepX <= 1e-6 || stepY <= 1e-6) return null
    val cols = ceil(area.width / stepX).toInt() + 1
    val rows = ceil(area.height / stepY).toInt() + 1
    if (cols.toLong() * rows > maxTiles) return null
    // Karoların sol üst köşesi alanın sol üstüne denk gelir.
    val ox = area.left - tileBox.left
    val oy = area.top - tileBox.top
    val copies = ArrayList<Node>(cols * rows)
    for (r in 0 until rows) for (c in 0 until cols) {
        val dx = ox + c * stepX
        val dy = oy + r * stepY
        val tile = GroupNode(name = "Karo", children = tiles.map { it.duplicated() }).at(dx, dy)
        copies += tile
    }
    val clip = ClipPath(target.subpaths, target.fillRule)
    val group = GroupNode(id = target.id, name = "Desen", children = copies, clip = clip)
    var doc = this
    for (t in tileIds) doc = doc.removeNode(t)
    return doc.replaceNode(targetId) { listOf(group) } to group.id
}

// ---- Ana hat görünümü -----------------------------------------------------------

/** Ana hat görünümü için belgenin yalnızca ince çizgilerden oluşan hali (dolgu, gradyan ve saydamlık yok). */
fun Document.outlineView(lineColor: Rgba = Rgba.Black, lineWidth: Double = 1.0): Document {
    val paint = Paint.Solid(lineColor)
    fun conv(n: Node): Node = when (n) {
        is PathNode -> n.copy(fill = null, stroke = Stroke(paint, lineWidth), opacity = 1.0, blendMode = BlendMode.Normal, effects = emptyList())
        is GroupNode -> n.copy(children = n.children.map(::conv), opacity = 1.0, blendMode = BlendMode.Normal, effects = emptyList())
        is TextNode -> n.copy(fill = null, stroke = Stroke(paint, lineWidth), opacity = 1.0, blendMode = BlendMode.Normal, effects = emptyList())
        is ImageNode -> n
    }
    return copy(layers = layers.map { l -> l.copy(children = l.children.map(::conv)) })
}
