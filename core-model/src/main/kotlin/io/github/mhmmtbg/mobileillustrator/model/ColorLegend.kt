package io.github.mhmmtbg.mobileillustrator.model

import kotlin.math.max
import kotlin.math.roundToInt

/** Renk lejantının durduğu katmanın adı; lejant yeniden eklenince bu katman yenilenir. */
const val LEGEND_LAYER = "Renk lejantı"

/** Lejantta ve paylaşılan listede her rengin hangi kodlarının yazılacağı. */
data class LegendOptions(val hex: Boolean = true, val rgb: Boolean = true, val cmyk: Boolean = true)

/**
 * Rengin CMYK karşılığı. Renk dosyada CMYK olarak tanımlıysa o değerler aynen verilir; değilse sRGB'den
 * hesaplanır (profilsiz, yaklaşık dönüşüm: siyah bileşen en parlak kanaldan çıkarılır).
 */
fun Rgba.toCmyk(): Ink.Cmyk {
    (ink as? Ink.Cmyk)?.let { return it }
    val k = 1.0 - max(r, max(g, b)).coerceIn(0.0, 1.0)
    if (k >= 1.0 - 1e-9) return Ink.Cmyk(0.0, 0.0, 0.0, 1.0)
    fun ch(v: Double) = ((1.0 - v.coerceIn(0.0, 1.0) - k) / (1.0 - k)).coerceIn(0.0, 1.0)
    return Ink.Cmyk(ch(r), ch(g), ch(b), k)
}

/** Rengin kod satırları: "#RRGGBB", "RGB r g b", "CMYK c m y k" (yüzde). */
fun Rgba.codeLines(options: LegendOptions = LegendOptions()): List<String> {
    val argb = toArgb()
    val out = ArrayList<String>(3)
    if (options.hex) out += "#%06X".format(argb and 0xFFFFFF)
    if (options.rgb) out += "RGB %d %d %d".format((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
    if (options.cmyk) {
        val c = toCmyk()
        fun p(v: Double) = (v * 100).roundToInt()
        out += "CMYK %d %d %d %d".format(p(c.c), p(c.m), p(c.y), p(c.k))
    }
    return out
}

private fun Document.withoutLegend(): Document = copy(layers = layers.filter { it.name != LEGEND_LAYER }.ifEmpty { layers })

/** Tasarımda kullanılan renkler (lejantın kendisi hariç), en çok kullanılandan aza. */
fun Document.designColors(): List<Rgba> = withoutLegend().colorGroups().map { it.color }

/** Tasarımdaki renklerin kodları, paylaşmak için düz metin olarak (her renk bir satır). */
fun Document.colorCodesText(options: LegendOptions = LegendOptions(), only: Set<Int>? = null): String =
    designColors().filter { only == null || it.rgbKey() in only }.joinToString("\n") { it.codeLines(options).joinToString("   ") }

/** Belgedeki lejantın gösterdiği renkler (`0xRRGGBB`); lejant yoksa `null`. */
fun Document.legendColors(): Set<Int>? {
    val layer = layers.firstOrNull { it.name == LEGEND_LAYER } ?: return null
    val out = LinkedHashSet<Int>()
    fun walk(nodes: List<Node>) {
        for (n in nodes) {
            if (n is PathNode) (n.fill as? Paint.Solid)?.let { out += it.color.rgbKey() }
            if (n is GroupNode) walk(n.children)
        }
    }
    walk(layer.children)
    return out
}

/** Köşeleri [r] yarıçapıyla yuvarlatılmış kare (saat yönünde, sekiz düğüm). */
private fun roundedSquare(x: Double, y: Double, s: Double, r: Double): SubPath {
    val k = r * 0.5523
    val e = x + s
    val f = y + s
    return SubPath(
        listOf(
            Anchor(Vec2(x + r, y), handleIn = Vec2(x + r - k, y)),
            Anchor(Vec2(e - r, y), handleOut = Vec2(e - r + k, y)),
            Anchor(Vec2(e, y + r), handleIn = Vec2(e, y + r - k)),
            Anchor(Vec2(e, f - r), handleOut = Vec2(e, f - r + k)),
            Anchor(Vec2(e - r, f), handleIn = Vec2(e - r + k, f)),
            Anchor(Vec2(x + r, f), handleOut = Vec2(x + r - k, f)),
            Anchor(Vec2(x, f - r), handleIn = Vec2(x, f - r + k)),
            Anchor(Vec2(x, y + r), handleOut = Vec2(x, y + r - k)),
        ),
        closed = true,
    )
}

/** Belgede lejant varsa hangi kodlarla eklendiği (etiketlerinden okunur); yoksa `null`. */
fun Document.legendOptions(): LegendOptions? {
    val layer = layers.firstOrNull { it.name == LEGEND_LAYER } ?: return null
    fun find(nodes: List<Node>): TextNode? = nodes.firstNotNullOfOrNull { n -> (n as? TextNode) ?: (n as? GroupNode)?.let { find(it.children) } }
    val lines = find(layer.children)?.text?.split('\n') ?: return null
    return LegendOptions(hex = lines.any { it.startsWith("#") }, rgb = lines.any { it.startsWith("RGB") }, cmyk = lines.any { it.startsWith("CMYK") })
}

/** Lejant eklemenin sonucu: yeni belge ve lejant grubunun kimliği. */
class LegendResult(val document: Document, val groupId: String, val colors: Int)

/**
 * Tasarımda kullanılan renklerin lejantını ekler: her renk için köşeleri yuvarlak bir örnek kare ve yanında kodları.
 * Lejant ilk çalışma yüzeyinin alt kenarına, kendi katmanında tek bir grup olarak yerleşir (taşınıp ölçeklenebilir).
 * Önceden eklenmiş lejant varsa yenisiyle değiştirilir; eskisi taşınmış ya da ölçeklenmişse yenisi de aynı yerde durur.
 * [only] verilirse yalnızca o renkler (`0xRRGGBB`) lejanta girer. Eklenecek renk yoksa `null` döner.
 */
fun Document.withColorLegend(options: LegendOptions = LegendOptions(), only: Set<Int>? = null): LegendResult? {
    val base = withoutLegend()
    val colors = base.designColors().filter { only == null || it.rgbKey() in only }.take(60)
    val lines = colors.map { it.codeLines(options) }
    val perItem = lines.firstOrNull()?.size ?: 0
    if (colors.isEmpty() || perItem == 0) return null
    val board = artboards.firstOrNull()?.bounds ?: base.layers.flatMap { it.children }.mapNotNull { it.bounds() }.reduceOrNull(Rect::union) ?: return null

    val s = board.width * 0.045
    val margin = s * 0.8
    val font = minOf(s * 0.42, s / (perItem * 1.25))
    val leading = 1.25
    val textGap = s * 0.3
    val textW = (lines.maxOf { l -> l.maxOf { it.length } }) * font * 0.62
    val itemW = s + textGap + textW + s * 0.9
    val perRow = max(1, ((board.width - 2 * margin + s * 0.9) / itemW).toInt())
    val rows = (colors.size + perRow - 1) / perRow
    val rowH = s * 1.35
    val top = board.bottom - margin - rows * rowH + (rowH - s)
    val dark = Rgba.rgb(0x14182B)

    val children = ArrayList<Node>(colors.size * 2)
    for ((i, color) in colors.withIndex()) {
        val x = board.left + margin + (i % perRow) * itemW
        val y = top + (i / perRow) * rowH
        val label = lines[i]
        children += PathNode(
            name = label.first(), subpaths = listOf(roundedSquare(x, y, s, s * 0.14)),
            fill = Paint.Solid(color), stroke = Stroke(Paint.Solid(dark), s * 0.035, join = LineJoin.Round),
        )
        val block = (perItem - 1) * font * leading + font * 0.72
        children += TextNode(
            name = label.first(), text = label.joinToString("\n"), fontSize = font, lineHeight = leading,
            fill = Paint.Solid(Rgba.Black), transform = Matrix.translate(x + s + textGap, y + (s - block) / 2 + font * 0.72),
        )
    }
    // Kullanıcı eski lejantı taşıdıysa ya da ölçeklediyse yenisi de aynı dönüşümü alır.
    val old = layers.firstOrNull { it.name == LEGEND_LAYER }
    val oldGroup = old?.children?.singleOrNull() as? GroupNode
    var group = GroupNode(name = LEGEND_LAYER, children = children, transform = oldGroup?.transform ?: Matrix.Identity)
    if (old != null) {
        // Yeni lejant eskisinin sol üst köşesine oturtulur: grup çözülmüş ya da parçaları tek tek taşınmış olsa da yerinde kalır.
        val was = old.children.mapNotNull { it.bounds() }.reduceOrNull(Rect::union)
        val now = group.bounds()
        if (was != null && now != null && (was.left != now.left || was.top != now.top)) {
            group = group.copy(transform = Matrix.translate(was.left - now.left, was.top - now.top) * group.transform)
        }
    }
    val layer = Layer(name = LEGEND_LAYER, children = listOf(group))
    return LegendResult(base.copy(layers = base.layers + layer), group.id, colors.size)
}

private val HexToken = Regex("#[0-9A-Fa-f]{6}(?![0-9A-Fa-f])")

/** Etiketleri güncellemenin sonucu: yeni belge ve değişen etiket sayısı. */
class LabelSync(val document: Document, val changed: Int)

/**
 * Tasarımdaki renk kodu yazılarını yanlarındaki renk örneğiyle eşitler. Bir yazı "#RRGGBB" içeriyorsa ve hemen solunda,
 * aynı hizada küçük bir dolu şekil (renk örneği) duruyorsa, yazıdaki kod o şeklin rengine çevrilir; "RGB …" ve "CMYK …"
 * satırları da varsa onlar da yenilenir. [only] verilirse yalnızca örneği o renkte (`0xRRGGBB`) olan etiketler değişir.
 * Kilitli katman ve nesnelere dokunulmaz.
 */
fun Document.syncedColorLabels(only: Int? = null): LabelSync {
    class Swatch(val box: Rect, val color: Rgba)
    class Label(val id: String, val box: Rect, val text: String)
    val swatches = ArrayList<Swatch>()
    val labels = ArrayList<Label>()
    fun walk(nodes: List<Node>, m: Matrix) {
        for (n in nodes) {
            if (!n.visible) continue
            when (n) {
                is PathNode -> (n.fill as? Paint.Solid)?.let { fill -> n.boundsIn(m)?.let { swatches += Swatch(it, fill.color) } }
                is TextNode -> if (!n.locked && n.text.length <= 80 && HexToken.containsMatchIn(n.text)) n.boundsIn(m)?.let { labels += Label(n.id, it, n.text) }
                is GroupNode -> if (!n.locked) walk(n.children, m * n.transform)
                is ImageNode -> {}
            }
        }
    }
    for (l in layers) if (l.visible && !l.locked) walk(l.children, Matrix.Identity)
    if (labels.isEmpty()) return LabelSync(this, 0)

    var doc = this
    var changed = 0
    for (label in labels) {
        val h = label.box.height.coerceAtLeast(1e-6)
        val cy = label.box.center.y
        // Renk örneği: yazıyla aynı hizada, solunda ve yakınında duran, yazıya göre küçük, kareye yakın dolu şekil.
        val swatch = swatches
            .filter { s ->
                val b = s.box
                b.width > 0 && b.height > 0 && b.height <= h * 8 && b.width <= b.height * 3 && b.height <= b.width * 3 &&
                    cy >= b.top && cy <= b.bottom && b.center.x < label.box.left &&
                    label.box.left - b.right <= maxOf(b.width, h) * 2.5 && label.box.left - b.right >= -b.width * 0.5
            }
            .minByOrNull { label.box.left - it.box.right } ?: continue
        if (only != null && swatch.color.rgbKey() != only) continue
        val codes = swatch.color.codeLines()
        val next = label.text.split('\n').joinToString("\n") { line ->
            when {
                line.trimStart().startsWith("RGB ") -> codes[1]
                line.trimStart().startsWith("CMYK ") -> codes[2]
                else -> HexToken.replace(line, codes[0])
            }
        }
        if (next == label.text) continue
        // Yazı değişti: dosyadan gelen harf biçimleri ve ölçülmüş genişlik artık geçerli değil.
        doc = doc.updateNode(label.id) { (it as? TextNode)?.copy(text = next, name = if (it.name == it.text) next else it.name, outline = null, measuredWidth = null) ?: it }
        changed++
    }
    return LabelSync(doc, changed)
}
