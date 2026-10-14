package io.github.mhmmtbg.nodist.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Silginin bir adımının sonucu. [changed] yanlışsa [document] girişle aynı nesnedir.
 * [skippedUnsupported]: silginin değdiği metin ya da görsel var; bunlar parça parça silinemez.
 */
class EraseResult(val document: Document, val changed: Boolean, val skippedUnsupported: Boolean)

/**
 * Silgi: [from]'dan [to]'ya sürüklenen, yarıçapı [radius] (belge birimi) olan yuvarlak uçlu fırçanın değdiği yeri siler.
 * Yalnızca [layerId] katmanındaki (ve gruplar içindeki) görünür, kilitsiz nesneler etkilenir; başka katmanlara dokunulmaz.
 *
 * - Konturlu yollar değdiği yerden bölünür: eğriler Bezier olarak kesilir, kalan parçalar ayrı açık yollar olur.
 * - Dolgular değdiği yerden kesilir (kesilen kısım çokgen olur; dokunulmayan alt yollar olduğu gibi kalır).
 * - Hem dolgusu hem konturu olan yol, dolgu ve kontur olarak ikiye ayrılır; böylece silinen yerde kenar çizgisi kalmaz.
 * - Metin ve görseller parça parça silinemez; [EraseResult.skippedUnsupported] ile bildirilir.
 */
fun Document.erase(layerId: String, from: Vec2, to: Vec2, radius: Double): EraseResult {
    val layer = findLayer(layerId) ?: return EraseResult(this, false, false)
    if (!layer.visible || layer.locked || radius <= 0.0) return EraseResult(this, false, false)
    val eraser = Eraser(from, to, radius)
    val ctx = EraseContext()
    var changed = false
    val out = ArrayList<Node>(layer.children.size)
    for (node in layer.children) {
        val r = eraseNode(node, Matrix.Identity, eraser, ctx)
        if (r == null) {
            out += node
        } else {
            changed = true
            out += r
        }
    }
    if (!changed) return EraseResult(this, false, ctx.skipped)
    return EraseResult(updateLayer(layerId) { it.copy(children = out) }, true, ctx.skipped)
}

private class Eraser(val a: Vec2, val b: Vec2, val r: Double) {
    val box: Rect = Rect.of(a, b).inflate(r)
}

private class EraseContext {
    var skipped = false
}

/** `null`: değişmedi. Boş liste: nesne tamamen silindi. */
private fun eraseNode(node: Node, parent: Matrix, e: Eraser, ctx: EraseContext): List<Node>? {
    if (!node.visible || node.locked) return null
    return when (node) {
        is TextNode, is ImageNode -> {
            val box = node.boundsIn(parent)
            if (box != null && box.intersects(e.box)) ctx.skipped = true
            null
        }
        is GroupNode -> {
            val m = parent * node.transform
            var changed = false
            val kids = ArrayList<Node>(node.children.size)
            for (c in node.children) {
                val r = eraseNode(c, m, e, ctx)
                if (r == null) {
                    kids += c
                } else {
                    changed = true
                    kids += r
                }
            }
            when {
                !changed -> null
                kids.isEmpty() -> emptyList()
                else -> listOf(node.copy(children = kids))
            }
        }
        is PathNode -> erasePath(node, parent, e)
    }
}

private fun erasePath(node: PathNode, parent: Matrix, e: Eraser): List<Node>? {
    val m = parent * node.transform
    val scale = m.meanScale
    if (scale <= 1e-12) return null
    val inv = m.inverse() ?: return null
    val strokeWidth = node.stroke?.width ?: 0.0
    val box = node.boundsIn(parent) ?: return null
    if (!box.inflate(strokeWidth * scale / 2).intersects(e.box)) return null

    // Silgi, yolun yerel uzayında düşünülür.
    val a = inv.apply(e.a)
    val b = inv.apply(e.b)
    val r = e.r / scale

    var strokeChanged = false
    var strokeSubs: List<SubPath> = emptyList()
    if (node.stroke != null) {
        // Kontur kalınlığının bir kısmı kadar pay: kalın bir çizgi silginin kenarında asılı kalmasın.
        val thr = r + strokeWidth / 4
        val list = ArrayList<SubPath>()
        for (sp in node.subpaths) {
            val pieces = splitStroke(sp, a, b, thr)
            if (pieces == null) list += sp else { strokeChanged = true; list += pieces }
        }
        strokeSubs = list
    }
    var fillChanged = false
    var fillSubs: List<SubPath> = emptyList()
    if (node.fill != null) {
        val capsule = capsulePolygon(a, b, r)
        val list = ArrayList<SubPath>()
        for (sp in node.subpaths) {
            val pieces = cutFill(sp, capsule, r)
            if (pieces == null) list += sp else { fillChanged = true; list += pieces }
        }
        fillSubs = list
    }
    if (!strokeChanged && !fillChanged) return null

    val hasFill = node.fill != null
    val hasStroke = node.stroke != null
    return when {
        hasFill && hasStroke -> buildList {
            if (fillSubs.isNotEmpty()) add(node.copy(stroke = null, subpaths = fillSubs))
            if (strokeSubs.isNotEmpty()) add(node.copy(id = if (fillSubs.isEmpty()) node.id else newId(), fill = null, subpaths = strokeSubs))
        }
        hasFill -> if (fillSubs.isEmpty()) emptyList() else listOf(node.copy(subpaths = fillSubs))
        else -> if (strokeSubs.isEmpty()) emptyList() else listOf(node.copy(subpaths = strokeSubs))
    }
}

// ---- Kontur: yolu silginin değdiği yerden böl --------------------------------

private fun distToSegment(p: Vec2, a: Vec2, b: Vec2): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len2 = dx * dx + dy * dy
    if (len2 == 0.0) return p.distanceTo(a)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0.0, 1.0)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

/** `null`: bu alt yola değilmedi. Aksi halde geriye kalan açık parçalar (hepsi silindiyse boş). */
private fun splitStroke(sp: SubPath, a: Vec2, b: Vec2, thr: Double): List<SubPath>? {
    val bounds = sp.bounds() ?: return null
    if (!bounds.inflate(thr).intersects(Rect.of(a, b).inflate(thr))) return null
    val segs = sp.segments()
    if (segs.isEmpty()) {
        // Tek düğümlü yol.
        val p = sp.anchors.firstOrNull()?.point ?: return null
        return if (distToSegment(p, a, b) <= thr) emptyList() else null
    }

    val runs = ArrayList<MutableList<Anchor>>()
    var cur: MutableList<Anchor>? = null
    var cut = false
    var startsAtBegin = false
    var endsAtEnd = false
    for ((index, seg) in segs.withIndex()) {
        val spans = outsideSpans(seg, a, b, thr)
        if (!(spans.size == 1 && spans[0][0] == 0.0 && spans[0][1] == 1.0)) cut = true
        if (index == 0) startsAtBegin = spans.isNotEmpty() && spans[0][0] == 0.0
        if (spans.isEmpty()) {
            cur?.let { runs += it }
            cur = null
        }
        for ((k, span) in spans.withIndex()) {
            val u0 = span[0]
            val u1 = span[1]
            val piece = seg.sub(u0, u1)
            val open = cur
            if (open == null || u0 > 0.0) {
                if (open != null) runs += open
                cur = startRun(piece)
            } else {
                appendPiece(open, piece)
            }
            if (u1 < 1.0) {
                runs += cur!!
                cur = null
            }
            if (index == segs.lastIndex && k == spans.lastIndex) endsAtEnd = u1 == 1.0
        }
    }
    cur?.let { runs += it }
    if (!cut) return null

    // Kapalı yolda başlangıç ve bitiş silinmediyse ilk ve son parça tek parça olur.
    if (sp.closed && startsAtBegin && endsAtEnd && runs.size >= 2) {
        val tail = runs.removeAt(runs.lastIndex)
        val head = runs.removeAt(0)
        val joint = Anchor(head[0].point, handleIn = tail.last().handleIn, handleOut = head[0].handleOut)
        runs += (tail.dropLast(1) + joint + head.drop(1)).toMutableList()
    }
    return runs
        .filter { run -> run.size >= 2 && (run.size > 2 || run[0].point.distanceTo(run[1].point) > 1e-9) }
        .map { SubPath(it, closed = false) }
}

/** [seg] üzerinde silgiden uzakta kalan parametre aralıkları: `[t0, t1]` çiftleri. */
private fun outsideSpans(seg: Segment, a: Vec2, b: Vec2, thr: Double): List<DoubleArray> {
    fun inside(t: Double) = distToSegment(seg.pointAt(t), a, b) <= thr
    val ctrlLen = seg.p0.distanceTo(seg.p1) + seg.p1.distanceTo(seg.p2) + seg.p2.distanceTo(seg.p3)
    // Örnekleme adımı eşiğin yarısını geçmez; silginin altından atlanmasın.
    val n = ceil(ctrlLen / (thr * 0.5)).toInt().coerceIn(16, 400)
    val flags = BooleanArray(n + 1) { inside(it.toDouble() / n) }
    if (flags.none { it }) return listOf(doubleArrayOf(0.0, 1.0))

    fun boundary(t0: Double, t1: Double, insideAtT0: Boolean): Double {
        var lo = t0
        var hi = t1
        repeat(30) {
            val mid = (lo + hi) / 2
            if (inside(mid) == insideAtT0) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    val out = ArrayList<DoubleArray>()
    var start: Double? = if (!flags[0]) 0.0 else null
    for (i in 1..n) {
        if (flags[i - 1] == flags[i]) continue
        val t = boundary((i - 1).toDouble() / n, i.toDouble() / n, flags[i - 1])
        if (flags[i - 1]) {
            start = t
        } else {
            val s = start
            if (s != null && t - s > 1e-9) out += doubleArrayOf(s, t)
            start = null
        }
    }
    start?.let { if (1.0 - it > 1e-9) out += doubleArrayOf(it, 1.0) }
    return out
}

private fun Segment.splitAt(t: Double): Pair<Segment, Segment> {
    fun lerp(p: Vec2, q: Vec2) = Vec2(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
    val p01 = lerp(p0, p1)
    val p12 = lerp(p1, p2)
    val p23 = lerp(p2, p3)
    val p012 = lerp(p01, p12)
    val p123 = lerp(p12, p23)
    val mid = lerp(p012, p123)
    return Segment(p0, p01, p012, mid, false) to Segment(mid, p123, p23, p3, false)
}

/** Parametre aralığı [u0, u1] arasındaki parça. */
private fun Segment.sub(u0: Double, u1: Double): Segment {
    if (isLine) {
        val s = pointAt(u0)
        val e = pointAt(u1)
        return Segment(s, s, e, e, true)
    }
    var s = this
    if (u1 < 1.0) s = s.splitAt(u1).first
    if (u0 > 0.0) s = s.splitAt(u0 / u1).second
    return s
}

private fun startRun(piece: Segment): MutableList<Anchor> = mutableListOf(
    Anchor(piece.p0, handleOut = if (piece.isLine) null else piece.p1),
    Anchor(piece.p3, handleIn = if (piece.isLine) null else piece.p2),
)

private fun appendPiece(run: MutableList<Anchor>, piece: Segment) {
    run[run.lastIndex] = run.last().copy(handleOut = if (piece.isLine) null else piece.p1)
    run += Anchor(piece.p3, handleIn = if (piece.isLine) null else piece.p2)
}

// ---- Dolgu: çokgenden silgi şeklini çıkar ---------------------------------

/** Silgi fırçasının (iki daire ve aradaki bant) dışbükey çokgeni. */
private fun capsulePolygon(a: Vec2, b: Vec2, r: Double): List<Vec2> {
    val steps = 16
    val angle = if (a.distanceTo(b) < 1e-12) 0.0 else atan2(b.y - a.y, b.x - a.x)
    val pts = ArrayList<Vec2>(steps * 2 + 2)
    // b ucundaki yarım daire: angle-90° → angle+90°
    for (i in 0..steps) {
        val t = angle - PI / 2 + PI * i / steps
        pts += Vec2(b.x + r * cos(t), b.y + r * sin(t))
    }
    // a ucundaki yarım daire: angle+90° → angle+270°
    for (i in 0..steps) {
        val t = angle + PI / 2 + PI * i / steps
        pts += Vec2(a.x + r * cos(t), a.y + r * sin(t))
    }
    return pts
}

private fun polygonArea(p: List<Vec2>): Double {
    var s = 0.0
    for (i in p.indices) {
        val q = p[(i + 1) % p.size]
        s += p[i].x * q.y - q.x * p[i].y
    }
    return s / 2
}

/** `null`: bu alt yolun dolgusuna değilmedi. Aksi halde kalan kapalı parçalar (hepsi silindiyse boş). */
private fun cutFill(sp: SubPath, capsule: List<Vec2>, r: Double): List<SubPath>? {
    val bounds = sp.bounds() ?: return null
    val capBox = Rect.bounding(capsule) ?: return null
    if (!bounds.intersects(capBox)) return null
    var poly = sp.flatten(32)
    if (poly.size > 1 && poly.first() == poly.last()) poly = poly.dropLast(1)
    if (poly.size < 3) return null
    val pieces = subtractConvex(poly, capsule, r * r * 1e-6) ?: return null
    return pieces.map { pts -> SubPath(pts.map { Anchor(it) }, closed = true) }
}

/**
 * [subject] çokgeninden dışbükey [clip] çokgenini çıkarır (Sutherland-Hodgman, kenar kenar).
 * Kesişim yoksa `null`; aksi halde birbirine değmeyen kalan parçalar.
 */
private fun subtractConvex(subject: List<Vec2>, clip: List<Vec2>, minArea: Double): List<List<Vec2>>? {
    val o = if (polygonArea(clip) >= 0) 1.0 else -1.0
    var rest = subject
    val pieces = ArrayList<List<Vec2>>()
    for (i in clip.indices) {
        val p = clip[i]
        val q = clip[(i + 1) % clip.size]
        val outside = clipHalf(rest, p, q, o, keepInside = false)
        if (outside.size >= 3 && abs(polygonArea(outside)) > minArea) pieces += outside
        rest = clipHalf(rest, p, q, o, keepInside = true)
        if (rest.size < 3) return null
    }
    return if (abs(polygonArea(rest)) > minArea) pieces else null
}

private fun clipHalf(poly: List<Vec2>, p: Vec2, q: Vec2, o: Double, keepInside: Boolean): List<Vec2> {
    if (poly.isEmpty()) return poly
    val sign = if (keepInside) 1.0 else -1.0
    fun side(x: Vec2) = sign * o * ((q.x - p.x) * (x.y - p.y) - (q.y - p.y) * (x.x - p.x))
    fun cross(from: Vec2, to: Vec2, fs: Double, ts: Double): Vec2 {
        val t = fs / (fs - ts)
        return Vec2(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
    }
    val out = ArrayList<Vec2>()
    var prev = poly.last()
    var ps = side(prev)
    for (cur in poly) {
        val cs = side(cur)
        if (cs >= 0) {
            if (ps < 0) out += cross(prev, cur, ps, cs)
            out += cur
        } else if (ps >= 0) {
            out += cross(prev, cur, ps, cs)
        }
        prev = cur
        ps = cs
    }
    return out
}
