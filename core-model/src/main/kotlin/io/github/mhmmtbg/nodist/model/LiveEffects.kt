package io.github.mhmmtbg.nodist.model

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.min
import kotlin.math.tan

/**
 * Canlı efektler: nesneye parametreleriyle bağlanır, nesnenin kendisini değiştirmez. Görünüm, çizilirken ve dışa
 * aktarılırken [LiveEffects.expand] ile üretilir; böylece parametreler sonradan düzenlenebilir, efekt kaldırılabilir.
 * Uzaysal veriler (opaklık maskesinin gradyanı, yazı yolunun yolu) nesnenin yerel uzayındadır.
 */
sealed interface Effect {
    /** Köşe yuvarlama: düz kenarların birleştiği köşeler [radius] yarıçaplı yaylarla yuvarlanır. */
    data class RoundCorners(val radius: Double) : Effect

    /** Yolu öteleme: pozitif büyütür, negatif küçültür (kapalı yollar). */
    data class OffsetPath(val distance: Double) : Effect

    /** Değişken genişlikli kontur: açık yol, uçlarda verilen kalınlıklarda dolu şekle dönüşür. */
    data class Taper(val startWidth: Double, val endWidth: Double) : Effect

    /** Ok uçları; uzunluk kontur kalınlığının [scale] katıdır. */
    data class Arrowheads(val atStart: Boolean, val atEnd: Boolean, val scale: Double) : Effect

    /** Kontur hizası (içeri ya da dışarı). */
    data class AlignStroke(val align: StrokeAlign) : Effect

    /** Nesnenin arkasında ikinci, daha kalın bir kontur. */
    data class OuterStroke(val width: Double, val color: Rgba) : Effect

    /** Yumuşak gölge. [opacity] 0..1. */
    data class Shadow(val dx: Double, val dy: Double, val blur: Double, val opacity: Double, val color: Rgba = Rgba.Black) : Effect

    /** Ağ gradyan: dört köşe renginden çift doğrusal boyama. */
    data class Mesh(val rows: Int, val cols: Int, val topLeft: Rgba, val topRight: Rgba, val bottomLeft: Rgba, val bottomRight: Rgba) : Effect

    /** Opaklık maskesi: doğrusal gradyanın parlaklığı saydamlığı belirler (gradyan nesnenin yerel uzayında). */
    data class OpacityMask(val gradient: Paint.LinearGradient, val bands: Int = 24) : Effect

    /** Desen dolgusu: [tile] karosu şeklin içini döşer. */
    data class Pattern(val tile: Node, val gapX: Double, val gapY: Double) : Effect

    /** Sanat fırçası: [art] şekli bu yolun boyunca esner; yolun kendisi görünmez. */
    data class ArtBrush(val art: PathNode, val thickness: Double) : Effect

    /** Yol üzerine yazı: metin [guide] yolunun (metnin yerel uzayında) boyunca dizilir; [offset] başlangıç uzaklığı. */
    data class TextPath(val guide: List<SubPath>, val offset: Double = 0.0) : Effect
}

/** Efektlerin ihtiyaç duyduğu platform hizmetleri: yol işlemleri (Skia) ve metnin harf biçimleri. */
class EffectContext(
    val ops: RegionOps? = null,
    val outlineText: ((TextNode) -> List<SubPath>)? = null,
)

object LiveEffects {
    /** Uygulama sırası: önce şekli değiştirenler, sonra boyamayı, en son saydamlık ve gölge. */
    private fun stage(e: Effect): Int = when (e) {
        is Effect.TextPath -> 0
        is Effect.RoundCorners, is Effect.OffsetPath, is Effect.ArtBrush -> 1
        is Effect.Taper, is Effect.AlignStroke, is Effect.Mesh, is Effect.Pattern -> 2
        is Effect.Arrowheads -> 3
        is Effect.OuterStroke -> 4
        is Effect.OpacityMask -> 5
        is Effect.Shadow -> 6
    }

    /** Efekt listesini sıraya koyar (aynı aşamadakiler eklendikleri sırada kalır). */
    fun ordered(effects: List<Effect>): List<Effect> = effects.sortedBy(::stage)

    /**
     * Nesnenin efektlerle görünümü: efektsiz düğümler (yol, grup…). Çocukların kendi efektleri genişletilmez;
     * çizici onları çizerken ayrıca genişletir. Bütün ağaç için [expandDeep].
     */
    fun expand(node: Node, ctx: EffectContext): Node {
        if (node.effects.isEmpty()) return node
        val original = node
        var cur: Node = node.withEffects(emptyList())
        for (e in ordered(node.effects)) {
            cur = try {
                apply(e, cur, original, ctx) ?: cur
            } catch (t: Throwable) {
                cur
            }
        }
        return cur
    }

    /** Ağacın tamamında efektleri genişletir (dışa aktarma için). */
    fun expandDeep(node: Node, ctx: EffectContext): Node {
        val inner = if (node is GroupNode) node.copy(children = node.children.map { expandDeep(it, ctx) }) else node
        val out = expand(inner, ctx)
        // Efektlerin ürettiği kopyalar (gölge, desen karosu) da efekt taşıyabilir.
        return if (out !== inner && out is GroupNode && hasEffects(out)) out.copy(children = out.children.map { expandDeep(it, ctx) }) else out
    }

    fun expandDocument(doc: Document, ctx: EffectContext): Document {
        if (!doc.layers.any { l -> l.children.any(::hasEffects) }) return doc
        return doc.copy(layers = doc.layers.map { l -> l.copy(children = l.children.map { expandDeep(it, ctx) }) })
    }

    /**
     * Efektlerin nesnenin sınır kutusunun dışına taşabileceği en büyük uzaklık (görünmeyen alan elemesi için).
     * Yol boyunca dizilen yazı ve fırça her yere uzanabildiğinden çok büyük döner.
     */
    fun pad(n: Node): Double {
        var p = 0.0
        val strokeW = (n as? PathNode)?.stroke?.width ?: 0.0
        for (e in n.effects) {
            p += when (e) {
                is Effect.Shadow -> kotlin.math.abs(e.dx) + kotlin.math.abs(e.dy) + e.blur + strokeW
                is Effect.OuterStroke -> e.width + strokeW
                is Effect.OffsetPath -> kotlin.math.max(0.0, e.distance)
                is Effect.Arrowheads -> e.scale * strokeW + strokeW
                is Effect.Taper -> kotlin.math.max(e.startWidth, e.endWidth)
                is Effect.AlignStroke -> strokeW
                is Effect.TextPath, is Effect.ArtBrush -> 1e7
                else -> 0.0
            }
        }
        if (n is GroupNode && n.clip == null) p += n.children.maxOfOrNull { pad(it) } ?: 0.0
        return p
    }

    fun hasEffects(n: Node): Boolean = n.effects.isNotEmpty() || (n is GroupNode && n.children.any(::hasEffects))

    // ---- Uygulama ------------------------------------------------------------------

    private fun soloDoc(vararg nodes: Node) = Document(name = "", artboards = emptyList(), layers = listOf(Layer(id = "solo", name = "", children = nodes.toList())))

    private fun solo(n: Node, f: Document.(String) -> Pair<Document, *>?): Node? =
        soloDoc(n).f(n.id)?.first?.layers?.get(0)?.children?.singleOrNull()

    /** Yaprak yollara [f] uygular (gruplarda içeri inilir); `null` dönen yol olduğu gibi kalır. */
    private fun leaves(n: Node, f: (PathNode) -> Node?): Node = when (n) {
        is PathNode -> f(n) ?: n
        is GroupNode -> n.copy(children = n.children.map { leaves(it, f) })
        else -> n
    }

    private fun apply(e: Effect, cur: Node, original: Node, ctx: EffectContext): Node? = when (e) {
        is Effect.RoundCorners -> if (e.radius <= 0.0) null else leaves(cur) { p ->
            val b = p.baked()
            b.copy(subpaths = b.subpaths.map { it.roundedCorners(e.radius) })
        }
        is Effect.OffsetPath -> ctx.ops?.let { ops -> leaves(cur) { p -> solo(p) { offsetPath(it, e.distance, ops) } } }
        is Effect.Taper -> leaves(cur) { p -> solo(p) { taperStroke(it, e.startWidth, e.endWidth) } }
        is Effect.Arrowheads -> leaves(cur) { p -> solo(p) { addArrowheads(it, e.atStart, e.atEnd, e.scale) } }
        is Effect.AlignStroke -> ctx.ops?.let { ops -> leaves(cur) { p -> solo(p) { alignStroke(it, e.align, ops) } } }
        is Effect.OuterStroke -> leaves(cur) { p -> solo(p) { addOuterStroke(setOf(it), e.width, e.color) } }
        is Effect.Mesh -> leaves(cur) { p ->
            solo(p) { meshGradient(it, e.rows, e.cols, e.topLeft, e.topRight, e.bottomLeft, e.bottomRight) }
        }
        is Effect.Pattern -> leaves(cur) { p ->
            val tile = e.tile.duplicated()
            soloDoc(p, tile).tilePattern(p.id, setOf(tile.id), e.gapX, e.gapY)?.first?.layers?.get(0)?.children?.singleOrNull()
        }
        is Effect.ArtBrush -> (cur as? PathNode)?.let { guide ->
            val art = e.art.copy(id = newId(), effects = emptyList())
            soloDoc(art, guide).artBrush(art.id, guide.id, e.thickness)?.first?.layers?.get(0)?.children?.singleOrNull()
                ?.withName(guide.name)?.withOpacity(guide.opacity * art.opacity)
        }
        is Effect.TextPath -> (cur as? TextNode)?.let { t -> textOnPath(t, e, ctx) }
        is Effect.OpacityMask -> {
            val mask = PathNode(subpaths = listOf(Shapes.rect(Rect(0.0, 0.0, 1.0, 1.0))), fill = e.gradient.transformedBy(original.transform))
            soloDoc(cur, mask).opacityMask(mask.id, cur.id, e.bands)?.first?.layers?.get(0)?.children?.singleOrNull()
        }
        is Effect.Shadow -> solo(cur) { dropShadow(setOf(it), e.dx, e.dy, e.blur, e.color, e.opacity) }
    }

    private fun textOnPath(t: TextNode, e: Effect.TextPath, ctx: EffectContext): Node? {
        val outline = ctx.outlineText ?: return null
        val guide = e.guide.firstOrNull { it.anchors.size >= 2 } ?: return null
        val shapes = outline(t)
        val box = shapes.mapNotNull { it.bounds() }.reduceOrNull(Rect::union) ?: return null
        val bent = alongPath(shapes, Ruler(guide), startOffset = e.offset, originX = box.left)
        if (bent.isEmpty()) return null
        return PathNode(
            id = t.id, name = t.name, visible = t.visible, locked = t.locked, opacity = t.opacity, transform = t.transform,
            blendMode = t.blendMode, subpaths = bent, fill = t.fill, stroke = t.stroke,
        )
    }
}

// ---- Köşe yuvarlama ---------------------------------------------------------------

/**
 * İki düz kenarın birleştiği köşeleri [radius] yarıçaplı yaylarla yuvarlar. Yarıçap, kenarın yarısını aşmayacak
 * biçimde küçültülür. Eğri kenarlı ya da yumuşak düğümlere dokunulmaz.
 */
fun SubPath.roundedCorners(radius: Double): SubPath {
    val a = anchors
    val n = a.size
    if (radius <= 0.0 || n < 3) return this
    val out = ArrayList<Anchor>(n * 2)
    for (i in 0 until n) {
        val cur = a[i]
        val hasPrev = closed || i > 0
        val hasNext = closed || i < n - 1
        if (!hasPrev || !hasNext) { out += cur; continue }
        val prev = a[(i - 1 + n) % n]
        val next = a[(i + 1) % n]
        val straight = cur.handleIn == null && cur.handleOut == null && prev.handleOut == null && next.handleIn == null
        if (!straight) { out += cur; continue }
        val p = cur.point
        val lp = p.distanceTo(prev.point)
        val ln = p.distanceTo(next.point)
        if (lp < 1e-9 || ln < 1e-9) { out += cur; continue }
        val u = Vec2((prev.point.x - p.x) / lp, (prev.point.y - p.y) / lp)
        val v = Vec2((next.point.x - p.x) / ln, (next.point.y - p.y) / ln)
        val cos = (u.x * v.x + u.y * v.y).coerceIn(-1.0, 1.0)
        val theta = acos(cos)
        // Neredeyse düz (180°) ya da tamamen geri dönen köşe yuvarlanmaz.
        if (theta > PI - 1e-3 || theta < 1e-3) { out += cur; continue }
        var t = radius / tan(theta / 2)
        val maxT = min(lp, ln) / 2
        if (t > maxT) t = maxT
        val r = t * tan(theta / 2)
        val turn = PI - theta
        val h = 4.0 / 3.0 * tan(turn / 4) * r
        val t1 = Vec2(p.x + u.x * t, p.y + u.y * t)
        val t2 = Vec2(p.x + v.x * t, p.y + v.y * t)
        out += Anchor(t1, handleIn = null, handleOut = Vec2(t1.x - u.x * h, t1.y - u.y * h))
        out += Anchor(t2, handleIn = Vec2(t2.x - v.x * h, t2.y - v.y * h), handleOut = null)
    }
    return copy(anchors = out)
}

/** Düğümün (yolun) düz köşeleri var mı: köşe yuvarlama yalnızca bunlarda bir şey yapar. */
fun PathNode.hasStraightCorners(): Boolean = subpaths.any { sp ->
    val a = sp.anchors
    val n = a.size
    n >= 3 && (0 until n).any { i ->
        val ok = sp.closed || (i > 0 && i < n - 1)
        ok && a[i].handleIn == null && a[i].handleOut == null && a[(i - 1 + n) % n].handleOut == null && a[(i + 1) % n].handleIn == null
    }
}

// ---- Kodlama (dosyada saklama) ------------------------------------------------

/**
 * Efektleri metne çevirir: `Ad(k=v,k=v);Ad(...)`. Düğüm taşıyan efektlerin (desen karosu, fırça şekli) düğümleri
 * ayrıca, sırayla yazılır ("yuva"); metinde sıraları geçer.
 */
object EffectCodec {
    private fun num(v: Double): String = if (v == Math.rint(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()

    private fun color(c: Rgba) = listOf(c.r, c.g, c.b, c.a).joinToString(" ") { num(it) }

    private fun parseColor(s: String?): Rgba? {
        val p = s?.trim()?.split(' ')?.mapNotNull { it.toDoubleOrNull() } ?: return null
        if (p.size < 3) return null
        return Rgba(p[0], p[1], p[2], p.getOrElse(3) { 1.0 })
    }

    fun pathData(subpaths: List<SubPath>): String {
        val sb = StringBuilder()
        fun pt(v: Vec2) { sb.append(num(v.x)).append(' ').append(num(v.y)).append(' ') }
        fun seg(a: Anchor, b: Anchor) {
            if (a.handleOut == null && b.handleIn == null) {
                sb.append("L "); pt(b.point)
            } else {
                sb.append("C "); pt(a.handleOut ?: a.point); pt(b.handleIn ?: b.point); pt(b.point)
            }
        }
        for (sp in subpaths) {
            val a = sp.anchors
            if (a.isEmpty()) continue
            sb.append("M "); pt(a[0].point)
            for (i in 1 until a.size) seg(a[i - 1], a[i])
            if (sp.closed) { if (a.size > 1) seg(a.last(), a[0]); sb.append("Z ") }
        }
        return sb.toString().trim()
    }

    fun parsePath(s: String): List<SubPath> {
        val tok = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = ArrayList<SubPath>()
        var cur: ArrayList<Anchor>? = null
        var i = 0
        fun d(): Double = tok.getOrNull(i++)?.toDoubleOrNull() ?: 0.0
        fun flush(closed: Boolean) {
            val c = cur ?: return
            if (closed && c.size > 1 && c.first().point.distanceTo(c.last().point) < 1e-9) {
                val last = c.removeAt(c.size - 1)
                c[0] = c[0].copy(handleIn = last.handleIn)
            }
            if (c.isNotEmpty()) out += SubPath(c, closed)
            cur = null
        }
        while (i < tok.size) {
            when (tok[i++]) {
                "M" -> { flush(false); cur = arrayListOf(Anchor(Vec2(d(), d()))) }
                "L" -> cur?.add(Anchor(Vec2(d(), d())))
                "C" -> {
                    val c1 = Vec2(d(), d()); val c2 = Vec2(d(), d()); val p = Vec2(d(), d())
                    val c = cur ?: continue
                    val last = c[c.size - 1]
                    c[c.size - 1] = last.copy(handleOut = c1.takeIf { it.distanceTo(last.point) > 1e-12 })
                    c += Anchor(p, handleIn = c2.takeIf { it.distanceTo(p) > 1e-12 })
                }
                "Z" -> flush(true)
            }
        }
        flush(false)
        return out
    }

    private fun gradient(g: Paint.LinearGradient): String {
        val m = g.transform
        val head = listOf(g.start.x, g.start.y, g.end.x, g.end.y, m.a, m.b, m.c, m.d, m.e, m.f).joinToString(" ") { num(it) }
        return head + g.stops.joinToString("") { " | " + num(it.offset) + " " + color(it.color) }
    }

    private fun parseGradient(s: String?): Paint.LinearGradient? {
        val parts = s?.split('|') ?: return null
        val h = parts[0].trim().split(' ').mapNotNull { it.toDoubleOrNull() }
        if (h.size < 10) return null
        val stops = parts.drop(1).mapNotNull { st ->
            val v = st.trim().split(' ').mapNotNull { it.toDoubleOrNull() }
            if (v.size < 4) null else GradientStop(v[0], Rgba(v[1], v[2], v[3], v.getOrElse(4) { 1.0 }))
        }
        if (stops.isEmpty()) return null
        return Paint.LinearGradient(Vec2(h[0], h[1]), Vec2(h[2], h[3]), stops, Matrix(h[4], h[5], h[6], h[7], h[8], h[9]))
    }

    /** @return metin ve yuvaya yazılacak düğümler. */
    fun encode(effects: List<Effect>): Pair<String, List<Node>> {
        val slots = ArrayList<Node>()
        val parts = effects.map { e ->
            when (e) {
                is Effect.RoundCorners -> "RoundCorners(r=${num(e.radius)})"
                is Effect.OffsetPath -> "OffsetPath(d=${num(e.distance)})"
                is Effect.Taper -> "Taper(a=${num(e.startWidth)},b=${num(e.endWidth)})"
                is Effect.Arrowheads -> "Arrowheads(s=${if (e.atStart) 1 else 0},e=${if (e.atEnd) 1 else 0},k=${num(e.scale)})"
                is Effect.AlignStroke -> "AlignStroke(a=${e.align.name})"
                is Effect.OuterStroke -> "OuterStroke(w=${num(e.width)},c=${color(e.color)})"
                is Effect.Shadow -> "Shadow(x=${num(e.dx)},y=${num(e.dy)},b=${num(e.blur)},o=${num(e.opacity)},c=${color(e.color)})"
                is Effect.Mesh -> "Mesh(r=${e.rows},n=${e.cols},a=${color(e.topLeft)},b=${color(e.topRight)},c=${color(e.bottomLeft)},d=${color(e.bottomRight)})"
                is Effect.OpacityMask -> "OpacityMask(n=${e.bands},g=${gradient(e.gradient)})"
                is Effect.Pattern -> { slots += e.tile; "Pattern(s=${slots.size - 1},x=${num(e.gapX)},y=${num(e.gapY)})" }
                is Effect.ArtBrush -> { slots += e.art; "ArtBrush(s=${slots.size - 1},t=${num(e.thickness)})" }
                is Effect.TextPath -> "TextPath(o=${num(e.offset)},p=${pathData(e.guide)})"
            }
        }
        return parts.joinToString(";") to slots
    }

    /** Okunamayan efektler atlanır. */
    fun decode(text: String, slots: List<Node>): List<Effect> {
        val out = ArrayList<Effect>()
        for (part in text.split(';')) {
            val open = part.indexOf('(')
            val close = part.lastIndexOf(')')
            if (open <= 0 || close < open) continue
            val name = part.substring(0, open).trim()
            val args = HashMap<String, String>()
            for (kv in part.substring(open + 1, close).split(',')) {
                val eq = kv.indexOf('=')
                if (eq > 0) args[kv.substring(0, eq).trim()] = kv.substring(eq + 1)
            }
            fun d(k: String, def: Double = 0.0) = args[k]?.trim()?.toDoubleOrNull() ?: def
            fun slot(): Node? = args["s"]?.trim()?.toIntOrNull()?.let { slots.getOrNull(it) }
            val e: Effect? = when (name) {
                "RoundCorners" -> Effect.RoundCorners(d("r"))
                "OffsetPath" -> Effect.OffsetPath(d("d"))
                "Taper" -> Effect.Taper(d("a"), d("b"))
                "Arrowheads" -> Effect.Arrowheads(d("s") != 0.0, d("e") != 0.0, d("k", 4.0))
                "AlignStroke" -> StrokeAlign.entries.firstOrNull { it.name == args["a"]?.trim() }?.let { Effect.AlignStroke(it) }
                "OuterStroke" -> parseColor(args["c"])?.let { Effect.OuterStroke(d("w"), it) }
                "Shadow" -> Effect.Shadow(d("x"), d("y"), d("b"), d("o", 0.5), parseColor(args["c"]) ?: Rgba.Black)
                "Mesh" -> {
                    val c = listOf("a", "b", "c", "d").map { parseColor(args[it]) }
                    if (c.any { it == null }) null else Effect.Mesh(d("r", 8.0).toInt(), d("n", 8.0).toInt(), c[0]!!, c[1]!!, c[2]!!, c[3]!!)
                }
                "OpacityMask" -> parseGradient(args["g"])?.let { Effect.OpacityMask(it, d("n", 24.0).toInt()) }
                "Pattern" -> slot()?.let { Effect.Pattern(it, d("x"), d("y")) }
                "ArtBrush" -> (slot() as? PathNode)?.let { Effect.ArtBrush(it, d("t", 1.0)) }
                "TextPath" -> args["p"]?.let { p -> parsePath(p).takeIf { it.isNotEmpty() }?.let { Effect.TextPath(it, d("o")) } }
                else -> null
            }
            if (e != null) out += e
        }
        return out
    }

    /** Efektin yerel uzaydaki verisini [m] ile taşır (yol ve grupların dönüşümü dosyada koordinatlara işlenir). */
    fun transformed(e: Effect, m: Matrix): Effect = when (e) {
        is Effect.OpacityMask -> e.copy(gradient = e.gradient.transformedBy(m) as Paint.LinearGradient)
        else -> e
    }
}
