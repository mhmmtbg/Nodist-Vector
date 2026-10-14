package io.github.mhmmtbg.nodist.model

/** "Aynısını seç": ölçüt. */
enum class SameKind { Fill, Stroke, FillAndStroke, StrokeWidth, Opacity, Type }

private fun Node.typeKey(): String = when (this) {
    is PathNode -> "path"
    is GroupNode -> "group"
    is ImageNode -> "image"
    is TextNode -> "text"
}

private fun Node.sample(): Pair<Paint?, Stroke?>? = when (this) {
    is PathNode -> fill to stroke
    is TextNode -> fill to stroke
    else -> null
}

/**
 * [nodeId] ile aynı özelliği taşıyan, görünür ve kilitsiz tüm düğümler (kendisi dahil). Gruplar içindeki düğümler de
 * aranır; grubun kendisi yalnızca [SameKind.Type] ile aranır.
 */
fun Document.selectSame(nodeId: String, kind: SameKind): Set<String> {
    val ref = findNode(nodeId) ?: return emptySet()
    val refSample = ref.sample()
    val out = LinkedHashSet<String>()
    fun matches(n: Node): Boolean {
        if (kind == SameKind.Type) return n.typeKey() == ref.typeKey()
        if (kind == SameKind.Opacity) return n !is GroupNode && n.opacity == ref.opacity
        val s = n.sample() ?: return false
        val r = refSample ?: return false
        return when (kind) {
            SameKind.Fill -> s.first == r.first
            SameKind.Stroke -> s.second?.paint == r.second?.paint
            SameKind.FillAndStroke -> s.first == r.first && s.second?.paint == r.second?.paint
            SameKind.StrokeWidth -> s.second?.width == r.second?.width
            else -> false
        }
    }
    fun walk(nodes: List<Node>) {
        for (n in nodes) {
            if (!n.visible || n.locked) continue
            if (matches(n)) out += n.id
            if (n is GroupNode && kind != SameKind.Type) walk(n.children)
            else if (n is GroupNode && n.typeKey() != ref.typeKey()) walk(n.children)
        }
    }
    for (l in layers) if (l.visible && !l.locked) walk(l.children)
    out += nodeId
    return out
}
