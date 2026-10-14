package io.github.mhmmtbg.nodist.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MeshMaskTest {
    private fun doc(vararg nodes: Node) = Document(name = "t", artboards = emptyList(), layers = listOf(Layer(id = "L", name = "k", children = nodes.toList())))
    private fun box(id: String, l: Double, t: Double, r: Double, b: Double, fill: Paint? = Paint.Solid(Rgba.Black)) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(l, t, r, b))), fill = fill)

    private fun colorOf(n: Node): Rgba = ((n as PathNode).fill as Paint.Solid).color

    @Test
    fun meshHasOneCellPerGridSlotAndCornersMatch() {
        val red = Rgba.rgb(0xFF0000)
        val blue = Rgba.rgb(0x0000FF)
        val (out, id) = assertNotNull(doc(box("a", 0.0, 0.0, 100.0, 100.0)).meshGradient("a", 4, 5, red, blue, blue, red))
        val g = out.findNode(id) as GroupNode
        val mesh = g.children[0] as GroupNode
        assertNotNull(mesh.clip)
        assertEquals(20, mesh.children.size)
        // sol üst hücre kırmızıya, sağ üst maviye yakın
        assertTrue(colorOf(mesh.children[0]).r > 0.7)
        assertTrue(colorOf(mesh.children[4]).b > 0.7)
        // tek çocuk: kontur yok
        assertEquals(1, g.children.size)
    }

    @Test
    fun meshKeepsTheStrokeOnTop() {
        val n = box("a", 0.0, 0.0, 50.0, 50.0).copy(stroke = Stroke(Paint.Solid(Rgba.Black), 2.0))
        val (out, id) = assertNotNull(doc(n).meshGradient("a", 3, 3, Rgba.Black, Rgba.Black, Rgba.Black, Rgba.Black))
        val g = out.findNode(id) as GroupNode
        assertEquals(2, g.children.size)
        assertNull((g.children[1] as PathNode).fill)
        assertNull(doc(PathNode(id = "o", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(5.0, 5.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 1.0))).meshGradient("o", 3, 3, Rgba.Black, Rgba.Black, Rgba.Black, Rgba.Black))
    }

    private fun gradientMask(id: String, from: Double, to: Double) = box(
        id, 0.0, 0.0, 100.0, 100.0,
        Paint.LinearGradient(Vec2(from, 0.0), Vec2(to, 0.0), listOf(GradientStop(0.0, Rgba.Black), GradientStop(1.0, Rgba(1.0, 1.0, 1.0, 1.0)))),
    )

    @Test
    fun opacityMaskFadesAlongTheGradient() {
        val d = doc(box("t", 0.0, 0.0, 100.0, 100.0, Paint.Solid(Rgba.rgb(0x2288FF))), gradientMask("m", 0.0, 100.0))
        val (out, id) = assertNotNull(d.opacityMask("m", "t", 10))
        assertNull(out.findNode("m"))
        val g = out.findNode(id) as GroupNode
        assertTrue(g.children.size in 9..10)
        val ops = g.children.map { it.opacity }
        assertEquals(ops.sorted(), ops)
        assertTrue(ops.last() > 0.9)
        assertTrue(ops.first() < 0.2)
        assertTrue(g.children.all { (it as GroupNode).clip != null })
    }

    @Test
    fun opacityMaskNeedsALinearGradientAndExtendsPastTheEnds() {
        val d = doc(box("t", 0.0, 0.0, 100.0, 100.0), box("m", 0.0, 0.0, 10.0, 10.0))
        assertNull(d.opacityMask("m", "t"))
        // Gradyan hedefin yalnızca ortasını kaplıyor: uçlar bitiş renklerini sürdürür; siyah uç gizlenir.
        val d2 = doc(box("t", 0.0, 0.0, 100.0, 100.0), gradientMask("m", 40.0, 60.0))
        val (out, id) = assertNotNull(d2.opacityMask("m", "t", 4))
        val g = out.findNode(id) as GroupNode
        assertTrue(g.children.any { abs(it.opacity - 1.0) < 1e-9 })
        assertTrue(g.children.none { it.opacity < 0.004 })
    }
}
