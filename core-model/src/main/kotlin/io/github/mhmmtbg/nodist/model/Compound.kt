package io.github.mhmmtbg.nodist.model

/**
 * Bileşik yol, yol birleştirme ve makas: yollar üzerinde Illustrator'daki "Bileşik yol", "Birleştir"
 * (Join) ve "Makas" işlemleri.
 */

/** [nodeId] düğümünü [f]'nin döndürdüğü düğümlerle değiştirir (boş liste silmek demektir); yerleri korunur. */
fun Document.replaceNode(nodeId: String, f: (Node) -> List<Node>): Document {
    fun walk(nodes: List<Node>): List<Node> {
        var changed = false
        val out = ArrayList<Node>(nodes.size)
        for (n in nodes) {
            if (n.id == nodeId) {
                changed = true
                out += f(n)
            } else if (n is GroupNode) {
                val kids = walk(n.children)
                if (kids !== n.children) {
                    changed = true
                    out += n.copy(children = kids)
                } else {
                    out += n
                }
            } else {
                out += n
            }
        }
        return if (changed) out else nodes
    }
    var changed = false
    val newLayers = layers.map { layer ->
        val kids = walk(layer.children)
        if (kids !== layer.children) {
            changed = true
            layer.copy(children = kids)
        } else {
            layer
        }
    }
    return if (changed) copy(layers = newLayers) else this
}

/** Seçilen, katmanların doğrudan altındaki yollar; alttan üste (çizim) sırasıyla. */
internal fun Document.topLevelPaths(ids: Set<String>): List<PathNode> =
    layers.flatMap { it.children }.filter { it.id in ids }.filterIsInstance<PathNode>()

/** Yolu kendi dönüşümüyle belge uzayına taşır; kontur kalınlığı ve gradyanlar da eşlenir. */
fun PathNode.baked(): PathNode {
    val m = transform
    if (m.isIdentity) return this
    return copy(
        transform = Matrix.Identity,
        subpaths = subpaths.map { it.transformed(m) },
        fill = fill?.transformedBy(m),
        stroke = stroke?.let { st -> st.copy(paint = st.paint.transformedBy(m), width = st.width * m.meanScale, dash = st.dash.map { it * m.meanScale }) },
        effects = if (effects.isEmpty()) effects else effects.map { EffectCodec.transformed(it, m) },
    )
}

/**
 * Bileşik yol yap: seçili yollar tek bir yola birleşir, her biri bir alt yol olur. İçteki şekiller delik açar
 * (çift-tek dolgu kuralı). Görünüm en üstteki yoldan gelir.
 * @return yeni belge ve bileşik yolun kimliği; en az iki yol seçili değilse `null`.
 */
fun Document.makeCompound(ids: Set<String>): Pair<Document, String>? {
    val paths = topLevelPaths(ids)
    if (paths.size < 2 || paths.size != ids.size) return null
    val baked = paths.map { it.baked() }
    val top = baked.last()
    val merged = top.copy(
        name = "Bileşik yol",
        subpaths = baked.flatMap { it.subpaths },
        fillRule = FillRule.EvenOdd,
    )
    var doc = this
    for (n in paths) if (n.id != top.id) doc = doc.removeNode(n.id)
    return doc.updateNode(top.id) { merged } to top.id
}

/**
 * Bileşik yolu çözer: her alt yol kendi yolu olur ve görünümü korur. Delikler artık delik olmaz (Illustrator'daki gibi).
 * @return yeni belge ve oluşan yolların kimlikleri; yolda en az iki alt yol yoksa `null`.
 */
fun Document.releaseCompound(nodeId: String): Pair<Document, List<String>>? {
    val node = findNode(nodeId) as? PathNode ?: return null
    if (node.subpaths.size < 2) return null
    val pieces = node.subpaths.mapIndexed { i, sp ->
        node.copy(id = if (i == 0) node.id else newId(), name = if (i == 0) node.name else node.name, subpaths = listOf(sp), fillRule = FillRule.NonZero)
    }
    return replaceNode(nodeId) { pieces } to pieces.map { it.id }
}

// ---- Yol birleştirme (Join) ----------------------------------------------

private fun SubPath.reversedPath(): SubPath =
    copy(anchors = anchors.reversed().map { Anchor(it.point, handleIn = it.handleOut, handleOut = it.handleIn) })

/** Zincirin ucuna [next]'i ekler; uçlar aynı noktadaysa tek düğüme birleştirir. */
private fun appendJoined(chain: SubPath, next: SubPath, eps: Double): SubPath {
    val last = chain.anchors.last()
    val first = next.anchors.first()
    return if (last.point.distanceTo(first.point) <= eps) {
        val merged = Anchor(last.point, handleIn = last.handleIn, handleOut = first.handleOut)
        chain.copy(anchors = chain.anchors.dropLast(1) + merged + next.anchors.drop(1))
    } else {
        chain.copy(anchors = chain.anchors + next.anchors)
    }
}

/**
 * Yolları birleştirir. Tek bir açık yol seçiliyse kapatılır; birden çok açık yol seçiliyse uçları en yakın uca bağlanarak
 * tek yola dönüşür (uçlar çakışıyorsa tek düğüm olur, değilse aralarına düz çizgi gelir). Görünüm en üstteki yoldan gelir.
 * @return yeni belge ve sonuç yolun kimliği; birleştirilecek açık yol yoksa `null`.
 */
fun Document.joinPaths(ids: Set<String>): Pair<Document, String>? {
    val paths = topLevelPaths(ids)
    if (paths.isEmpty() || paths.size != ids.size) return null
    val baked = paths.map { it.baked() }
    val open = baked.flatMap { p -> p.subpaths.filter { !it.closed && it.anchors.size >= 2 } }
    if (open.isEmpty()) return null
    val top = baked.last()
    val closedOnes = baked.flatMap { p -> p.subpaths.filter { it.closed || it.anchors.size < 2 } }
    val eps = 1e-6
    val result: List<SubPath>
    if (open.size == 1) {
        result = closedOnes + open[0].copy(closed = true)
    } else {
        val rest = ArrayList(open.drop(1))
        var chain = open[0]
        while (rest.isNotEmpty()) {
            val end = chain.anchors.last().point
            // Zincirin sonuna en yakın uç; gerekirse yol ters çevrilir.
            var best = -1
            var bestRev = false
            var bestD = Double.MAX_VALUE
            for ((i, sp) in rest.withIndex()) {
                val dFirst = end.distanceTo(sp.anchors.first().point)
                val dLast = end.distanceTo(sp.anchors.last().point)
                if (dFirst < bestD) { bestD = dFirst; best = i; bestRev = false }
                if (dLast < bestD) { bestD = dLast; best = i; bestRev = true }
            }
            val pick = rest.removeAt(best)
            chain = appendJoined(chain, if (bestRev) pick.reversedPath() else pick, eps)
        }
        // Son uç ilk uca değiyorsa yol kapanır.
        val a = chain.anchors.first()
        val b = chain.anchors.last()
        chain = if (chain.anchors.size > 2 && a.point.distanceTo(b.point) <= eps) {
            chain.copy(anchors = listOf(a.copy(handleIn = b.handleIn)) + chain.anchors.drop(1).dropLast(1), closed = true)
        } else chain
        result = closedOnes + chain + rest
    }
    val merged = top.copy(subpaths = result)
    var doc = this
    for (n in paths) if (n.id != top.id) doc = doc.removeNode(n.id)
    return doc.updateNode(top.id) { merged } to top.id
}

// ---- Makas -----------------------------------------------------------------

/**
 * Yolu [loc] noktasından keser. Açık alt yol ikiye bölünür; kapalı alt yol o noktada açılır (tek açık yol olur).
 * @return alt yolların yeni listesi ve kesilen alt yoldan çıkan parçaların listedeki sıraları.
 */
fun List<SubPath>.cutAt(loc: PathLocation): Pair<List<SubPath>, List<Int>>? {
    val (inserted, ref) = insertAnchor(loc) ?: return null
    val sp = inserted[ref.subpath]
    val k = ref.index
    val a = sp.anchors
    val pieces: List<SubPath> = if (sp.closed) {
        val cut = a[k]
        val start = cut.copy(handleIn = null)
        val end = cut.copy(handleOut = null)
        listOf(SubPath(listOf(start) + a.drop(k + 1) + a.take(k) + end, closed = false))
    } else {
        val cut = a[k]
        listOf(
            SubPath(a.take(k) + cut.copy(handleOut = null), closed = false),
            SubPath(listOf(cut.copy(handleIn = null)) + a.drop(k + 1), closed = false),
        )
    }
    val out = ArrayList<SubPath>()
    val order = ArrayList<Int>()
    for ((i, s) in inserted.withIndex()) {
        if (i == ref.subpath) {
            for (p in pieces) { order += out.size; out += p }
        } else {
            out += s
        }
    }
    return out to order
}

/**
 * Makas: [nodeId] yolunu belge uzayındaki [point] noktasına en yakın yerden keser. [tolerance] belge biriminde isabet payıdır.
 * Kesilen parçalar ayrı yollar olur (yerleri ve görünümleri korunur).
 * @return yeni belge ve parçaların kimlikleri; yol yakında değilse `null`.
 */
fun Document.scissorsCut(nodeId: String, point: Vec2, tolerance: Double): Pair<Document, List<String>>? {
    val node = findNode(nodeId) as? PathNode ?: return null
    val world = parentMatrix(nodeId) * node.transform
    val inv = world.inverse() ?: return null
    val loc = node.subpaths.nearest(inv.apply(point)) ?: return null
    if (loc.distance * world.meanScale > tolerance) return null
    val (subs, order) = node.subpaths.cutAt(loc) ?: return null
    // Kesilen alt yolun parçaları ayrı düğümler olur; diğer alt yollar ilk düğümde kalır.
    val cutPieces = order.map { subs[it] }
    val others = subs.filterIndexed { i, _ -> i !in order }
    val nodes = ArrayList<PathNode>()
    nodes += node.copy(subpaths = others + cutPieces.first())
    for (p in cutPieces.drop(1)) nodes += node.copy(id = newId(), subpaths = listOf(p))
    return replaceNode(nodeId) { nodes } to nodes.map { it.id }
}

/**
 * [layerId] katmanında [point] noktasına [tolerance] (belge birimi) içinde en yakın yol (görünür ve kilitsiz olanlar).
 * Gruplar içindeki yollar da aranır.
 */
fun Document.pathNear(layerId: String, point: Vec2, tolerance: Double): String? {
    val layer = findLayer(layerId) ?: return null
    var best: String? = null
    var bestD = tolerance
    fun walk(nodes: List<Node>, parent: Matrix) {
        for (n in nodes) {
            if (!n.visible || n.locked) continue
            val world = parent * n.transform
            when (n) {
                is PathNode -> {
                    val inv = world.inverse() ?: continue
                    val loc = n.subpaths.nearest(inv.apply(point)) ?: continue
                    val d = loc.distance * world.meanScale
                    if (d <= bestD) { bestD = d; best = n.id }
                }
                is GroupNode -> walk(n.children, world)
                else -> {}
            }
        }
    }
    walk(layer.children, Matrix.Identity)
    return best
}
