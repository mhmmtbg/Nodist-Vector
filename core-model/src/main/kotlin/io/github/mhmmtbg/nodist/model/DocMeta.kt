package io.github.mhmmtbg.nodist.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Belgeye bağlı, katmanlarda ve çizimde yer almayan bilgiler (kılavuzlar, renk örnekleri, çalışma yüzeyi adları).
 * `.nodist` dosyasında tek bir ASCII metin olarak saklanır; başka programlar bu alanı yok sayar.
 *
 * Biçim: `NODIST1;g=v:100.5:0|h:20:1;s=FF0000:Ad|...;a=Ad1|Ad2`. Metinler URL kodlamalıdır.
 */
object DocMeta {
    private const val HEADER = "NODIST1"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = try { URLDecoder.decode(s, "UTF-8") } catch (e: IllegalArgumentException) { s }

    private fun hex(c: Rgba): String {
        val v = c.toArgb() and 0xFFFFFF
        val base = "%06X".format(v)
        val ink = c.ink as? Ink.Cmyk
        return if (ink != null) base + "~" + listOf(ink.c, ink.m, ink.y, ink.k).joinToString(",") { "%.4f".format(java.util.Locale.ROOT, it) } else base
    }

    private fun parseColor(s: String): Rgba? {
        val parts = s.split('~')
        val rgb = parts[0].toIntOrNull(16) ?: return null
        val ink = parts.getOrNull(1)?.split(',')?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 4 }
        return if (ink != null) Ink.Cmyk(ink[0], ink[1], ink[2], ink[3]).toRgb() else Rgba.rgb(rgb)
    }

    /** Kaydedilecek bir şey yoksa `null`. */
    fun encode(doc: Document): String? {
        val customNames = doc.artboards.any { !Regex("^Çalışma Yüzeyi \\d+$").matches(it.name) }
        if (doc.guides.isEmpty() && doc.swatches.isEmpty() && !customNames) return null
        val sb = StringBuilder(HEADER)
        if (doc.guides.isNotEmpty()) {
            sb.append(";g=").append(doc.guides.joinToString("|") { (if (it.vertical) "v" else "h") + ":" + "%.4f".format(java.util.Locale.ROOT, it.position) + ":" + (if (it.locked) "1" else "0") })
        }
        if (doc.swatches.isNotEmpty()) {
            sb.append(";s=").append(doc.swatches.joinToString("|") { hex(it.color) + ":" + enc(it.name) })
        }
        if (customNames) sb.append(";a=").append(doc.artboards.joinToString("|") { enc(it.name) })
        return sb.toString()
    }

    /** [encode]'un tersi; tanınmayan ya da bozuk metin belgeyi olduğu gibi bırakır. */
    fun apply(doc: Document, text: String?): Document {
        if (text == null || !text.startsWith(HEADER)) return doc
        var out = doc
        for (part in text.split(';').drop(1)) {
            val key = part.substringBefore('=', "")
            val value = part.substringAfter('=', "")
            when (key) {
                "g" -> out = out.copy(
                    guides = value.split('|').mapNotNull { g ->
                        val f = g.split(':')
                        val pos = f.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
                        if (f[0] != "v" && f[0] != "h") return@mapNotNull null
                        Guide(vertical = f[0] == "v", position = pos, locked = f.getOrNull(2) == "1")
                    },
                )
                "s" -> out = out.copy(
                    swatches = value.split('|').mapNotNull { sw ->
                        val color = parseColor(sw.substringBefore(':')) ?: return@mapNotNull null
                        Swatch(dec(sw.substringAfter(':', "")), color)
                    },
                )
                "a" -> {
                    val names = value.split('|').map(::dec)
                    if (names.size == out.artboards.size) {
                        out = out.copy(artboards = out.artboards.mapIndexed { i, ab -> ab.copy(name = names[i].ifBlank { ab.name }) })
                    }
                }
            }
        }
        return out
    }
}

// ---- Kılavuzlar ve renk örnekleri ---------------------------------------------------------

fun Document.addGuide(vertical: Boolean, position: Double): Pair<Document, String> {
    val g = Guide(vertical = vertical, position = position)
    return copy(guides = guides + g) to g.id
}

fun Document.moveGuide(id: String, position: Double): Document =
    copy(guides = guides.map { if (it.id == id && !it.locked) it.copy(position = position) else it })

fun Document.removeGuide(id: String): Document = copy(guides = guides.filter { it.id != id })

fun Document.clearGuides(): Document = if (guides.isEmpty()) this else copy(guides = emptyList())

fun Document.setGuidesLocked(locked: Boolean): Document = copy(guides = guides.map { it.copy(locked = locked) })

/** Seçili nesnelerin sınır kutusunun kenarlarına ve ortasına kılavuz koymak için yardımcı: kutudan kılavuzlar üretir. */
fun Document.guidesFromBox(box: Rect): Document {
    val v = listOf(box.left, box.right).map { Guide(vertical = true, position = it) }
    val h = listOf(box.top, box.bottom).map { Guide(vertical = false, position = it) }
    return copy(guides = guides + v + h)
}

/** Dolu bir nesneyi kılavuza çevirir (Illustrator'daki "Kılavuz yap"): yolun sınır kutusu kenarları kılavuz olur. */
fun Document.makeGuidesFrom(nodeId: String): Document {
    val box = boundsOf(nodeId) ?: return this
    return guidesFromBox(box)
}

/** [color] örneği ekler; aynı renkte örnek varsa eklenmez. */
fun Document.addSwatch(color: Rgba, name: String? = null): Document {
    val argb = color.toArgb() and 0xFFFFFF
    if (swatches.any { (it.color.toArgb() and 0xFFFFFF) == argb }) return this
    val label = name ?: "#%06X".format(argb)
    return copy(swatches = swatches + Swatch(label, color))
}

fun Document.removeSwatch(index: Int): Document =
    if (index !in swatches.indices) this else copy(swatches = swatches.filterIndexed { i, _ -> i != index })

fun Document.renameSwatch(index: Int, name: String): Document =
    if (index !in swatches.indices) this else copy(swatches = swatches.mapIndexed { i, s -> if (i == index) s.copy(name = name.ifBlank { s.name }) else s })

/**
 * Belgedeki [from] renkli her şeyi (dolgu, kontur, gradyan durağı) [to] ile değiştirir ve örneği de günceller
 * ("global renk" davranışı: örneği değiştirmek onu kullanan her yeri değiştirir).
 */
fun Document.replaceSwatchColor(index: Int, to: Rgba): Document {
    val old = swatches.getOrNull(index) ?: return this
    val recolored = recolored(old.color.toArgb() and 0xFFFFFF, to)
    return recolored.copy(swatches = recolored.swatches.mapIndexed { i, s -> if (i == index) s.copy(color = to) else s })
}
