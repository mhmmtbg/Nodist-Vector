package io.github.mhmmtbg.nodist.model

/** Bir yol işleminin girdisi/sonucu: alt yollar ve dolgu kuralı. */
class Region(val subpaths: List<SubPath>, val rule: FillRule = FillRule.NonZero) {
    val isEmpty: Boolean get() = subpaths.isEmpty()
}

/**
 * Bölgeler üzerinde mantıksal işlemler. Gerçek uygulama platformun yol işlemcisidir (Skia); testlerde basit bir
 * sahtesi kullanılır. Başarısız olan işlem `null` döner.
 */
interface RegionOps {
    fun intersect(a: Region, b: Region): Region?
    fun minus(a: Region, b: Region): Region?
    fun union(a: Region, b: Region): Region?
}

/** Pathfinder "Böl": örtüşen şekilleri, sınırlarına göre örtüşmeyen yüzlere ayırır. */
object Divide {
    /**
     * @param shapes alttan üste doğru sıralı şekiller (aynı koordinat uzayında).
     * @return her yüz ve görünümünü veren şeklin sırası (üstteki şekil kazanır); işlem başarısızsa `null`.
     */
    fun faces(shapes: List<Region>, ops: RegionOps): List<Pair<Region, Int>>? {
        var faces = ArrayList<Pair<Region, Int>>()
        var covered: Region? = null
        for ((i, s) in shapes.withIndex()) {
            val next = ArrayList<Pair<Region, Int>>()
            for ((r, owner) in faces) {
                val inside = ops.intersect(r, s) ?: return null
                val outside = ops.minus(r, s) ?: return null
                if (!inside.isEmpty) next += inside to i
                if (!outside.isEmpty) next += outside to owner
            }
            val rest = if (covered == null) s else ops.minus(s, covered) ?: return null
            if (!rest.isEmpty) next += rest to i
            faces = next
            covered = if (covered == null) s else ops.union(covered, s) ?: return null
        }
        return faces
    }
}

private fun PathNode.region(): Region = Region(subpaths, fillRule)

private fun PathNode.worldBounds(): Rect? = subpaths.mapNotNull { it.bounds() }.reduceOrNull(Rect::union)

/**
 * Böl: seçili yolları (üst düzey, en az iki) sınırlarına göre örtüşmeyen parçalara ayırır; her parça üstteki sahibinin
 * görünümünü alır. @return yeni belge ve oluşan yolların kimlikleri; işlem yapılamazsa `null`.
 */
fun Document.divideShapes(ids: Set<String>, ops: RegionOps): Pair<Document, List<String>>? {
    val paths = topLevelPaths(ids)
    if (paths.size < 2 || paths.size != ids.size) return null
    val baked = paths.map { it.baked() }
    val faces = Divide.faces(baked.map { it.region() }, ops) ?: return null
    val made = faces.map { (r, owner) -> baked[owner].copy(id = newId(), subpaths = r.subpaths, fillRule = r.rule) }
    val top = paths.last()
    var doc = this
    for (n in paths) if (n.id != top.id) doc = doc.removeNode(n.id)
    return doc.replaceNode(top.id) { made } to made.map { it.id }
}

/**
 * Kontur ana hattı: yolun konturunu dolu bir şekle çevirir. Dolgu da varsa dolgu ve kontur bir grupta ayrılır.
 * @return yeni belge ve sonucun kimliği; yolun konturu yoksa ya da işlem başarısızsa `null`.
 */
fun Document.outlineStroke(nodeId: String, ops: RegionOps): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    val st = node.stroke ?: return null
    val pieces = StrokeGeometry.pieces(node.subpaths, st)
    if (pieces.isEmpty()) return null
    val whole = Region(pieces, FillRule.NonZero)
    val outline = ops.union(whole, whole) ?: return null
    if (outline.isEmpty) return null
    val shape = node.copy(
        id = if (node.fill == null) node.id else newId(), name = "Kontur",
        subpaths = outline.subpaths, fillRule = outline.rule, fill = st.paint, stroke = null,
    )
    if (node.fill == null) return replaceNode(nodeId) { listOf(shape) } to shape.id
    val fillPart = node.copy(id = newId(), name = "Dolgu", stroke = null)
    val group = GroupNode(id = node.id, name = node.name, children = listOf(fillPart, shape))
    return replaceNode(nodeId) { listOf(group) } to group.id
}

/**
 * Yolu [distance] kadar öteler: pozitif değer büyütür, negatif küçültür (kapalı şekiller). Yuvarlak köşeler kullanılır.
 * @return yeni belge ve sonucun kimliği; işlem başarısızsa ya da sonuç boşsa `null`.
 */
fun Document.offsetPath(nodeId: String, distance: Double, ops: RegionOps): Pair<Document, String>? {
    val node = (findNode(nodeId) as? PathNode)?.baked() ?: return null
    if (distance == 0.0 || node.subpaths.none { it.closed }) return null
    val closed = node.subpaths.filter { it.closed }
    val band = StrokeGeometry.pieces(closed, Stroke(Paint.Solid(Rgba.Black), 2 * kotlin.math.abs(distance), join = LineJoin.Round))
    if (band.isEmpty()) return null
    val bandRegion = Region(band, FillRule.NonZero).let { ops.union(it, it) } ?: return null
    val shape = Region(closed, node.fillRule)
    val result = (if (distance > 0) ops.union(shape, bandRegion) else ops.minus(shape, bandRegion)) ?: return null
    if (result.isEmpty) return null
    val out = node.copy(subpaths = result.subpaths, fillRule = result.rule)
    return replaceNode(nodeId) { listOf(out) } to out.id
}

/**
 * Bıçak: [layerId] katmanındaki dolgulu, kapalı yolları a→b doğrusu boyunca ikiye böler.
 * @return yeni belge ve oluşan yolların kimlikleri; çizgi hiçbir şekli kesmiyorsa `null`.
 */
fun Document.knifeCut(layerId: String, a: Vec2, b: Vec2, ops: RegionOps): Pair<Document, List<String>>? {
    val layer = findLayer(layerId) ?: return null
    if (a.distanceTo(b) < 1e-9) return null
    var doc = this
    val made = ArrayList<String>()
    for (n in layer.children) {
        val p = n as? PathNode ?: continue
        if (!p.visible || p.locked || p.fill == null || p.subpaths.none { it.closed }) continue
        val baked = p.baked()
        val box = baked.worldBounds() ?: continue
        val extent = StrokeGeometry.extentFor(box, a, b)
        val left = StrokeGeometry.halfPlane(a, b, extent, left = true) ?: continue
        val right = StrokeGeometry.halfPlane(a, b, extent, left = false) ?: continue
        val shape = baked.region()
        val l = ops.intersect(shape, Region(listOf(left), FillRule.NonZero)) ?: continue
        val r = ops.intersect(shape, Region(listOf(right), FillRule.NonZero)) ?: continue
        if (l.isEmpty || r.isEmpty) continue
        val first = baked.copy(subpaths = l.subpaths, fillRule = l.rule)
        val second = baked.copy(id = newId(), subpaths = r.subpaths, fillRule = r.rule)
        doc = doc.replaceNode(p.id) { listOf(first, second) }
        made += first.id
        made += second.id
    }
    return if (made.isEmpty()) null else doc to made
}
