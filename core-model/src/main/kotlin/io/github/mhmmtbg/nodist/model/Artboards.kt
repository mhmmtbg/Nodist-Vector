package io.github.mhmmtbg.nodist.model

/** Çalışma yüzeyi yönetimi. */

private val artboardName = Regex("^Çalışma Yüzeyi (\\d+)$")

/** Henüz kullanılmayan "Çalışma Yüzeyi N" adı. */
fun Document.nextArtboardName(): String {
    val used = artboards.mapNotNull { artboardName.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }.toSet()
    var n = artboards.size + 1
    while (n in used) n++
    return "Çalışma Yüzeyi $n"
}

/**
 * Tüm çalışma yüzeylerinin sağına, [like] ile aynı boyda (yoksa sonuncunun boyunda) yeni bir yüzey ekler.
 * @return yeni belge ve yüzeyin kimliği.
 */
fun Document.addArtboard(like: Rect? = null, gap: Double = 60.0): Pair<Document, String> {
    val all = artboardBounds()
    val size = like ?: artboards.lastOrNull()?.bounds ?: Rect(0.0, 0.0, 1080.0, 1080.0)
    val left = (all?.right ?: 0.0) + (if (all == null) 0.0 else gap)
    val top = all?.top ?: 0.0
    val ab = Artboard(name = nextArtboardName(), bounds = Rect(left, top, left + size.width, top + size.height))
    return copy(artboards = artboards + ab) to ab.id
}

/** Yüzeyi siler; son yüzey silinmez. İçindeki nesneler yerinde kalır. */
fun Document.removeArtboard(id: String): Document {
    if (artboards.size <= 1 || artboards.none { it.id == id }) return this
    return copy(artboards = artboards.filter { it.id != id })
}

fun Document.renameArtboard(id: String, name: String): Document =
    copy(artboards = artboards.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it })

/** Yüzeyin kutusunu değiştirir (boyut ve konum); nesnelere dokunulmaz. */
fun Document.setArtboardBounds(id: String, bounds: Rect): Document {
    if (bounds.width <= 0.0 || bounds.height <= 0.0) return this
    return copy(artboards = artboards.map { if (it.id == id) it.copy(bounds = bounds) else it })
}

/** Yüzeyi içindeki üst düzey nesnelerle birlikte, tüm yüzeylerin sağına çoğaltır. @return yeni belge ve yeni yüzeyin kimliği. */
fun Document.duplicateArtboard(id: String, gap: Double = 60.0): Pair<Document, String>? {
    val src = artboards.firstOrNull { it.id == id } ?: return null
    val (withBoard, newId) = addArtboard(src.bounds, gap)
    val dest = withBoard.artboards.first { it.id == newId }
    val inside = layers.filter { it.visible && !it.locked }.flatMap { l ->
        l.children.filter { n -> n.boundsIn(Matrix.Identity)?.center?.let(src.bounds::contains) == true }
    }.map { it.id }.toSet()
    val offset = Vec2(dest.bounds.left - src.bounds.left, dest.bounds.top - src.bounds.top)
    val doc = if (inside.isEmpty()) withBoard else withBoard.duplicate(inside, offset).first
    return doc.renameArtboard(newId, "${src.name} kopya") to newId
}

/** Yüzeyi tüm çizimin sınır kutusuna sığdırır (Illustrator'daki "Çizim sınırlarına sığdır"). */
fun Document.fitArtboardToArtwork(id: String, margin: Double = 0.0): Document? {
    val boxes = layers.filter { it.visible }.flatMap { l -> l.children.filter { it.visible }.mapNotNull { it.bounds() } }
    val all = boxes.reduceOrNull(Rect::union) ?: return null
    return setArtboardBounds(id, all.inflate(margin))
}

/** Yalnızca [id] yüzeyini içeren kopya belge (yüzey başına dışa aktarmak için). */
fun Document.onlyArtboard(id: String): Document? {
    val ab = artboards.firstOrNull { it.id == id } ?: return null
    return copy(artboards = listOf(ab))
}
