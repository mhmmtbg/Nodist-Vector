package io.github.mhmmtbg.nodist.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectsTest {
    private fun doc(vararg nodes: Node) = Document(name = "t", artboards = emptyList(), layers = listOf(Layer(id = "L", name = "k", children = nodes.toList())))
    private fun box(id: String, l: Double, t: Double, r: Double, b: Double, fill: Paint? = Paint.Solid(Rgba.Black)) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(l, t, r, b))), fill = fill)
    private fun near(e: Double, a: Double, tol: Double) = assertTrue(abs(e - a) <= tol, "beklenen $e, gelen $a")
    private fun area(sp: SubPath): Double {
        val p = sp.anchors.map { it.point }
        var s = 0.0
        for (i in p.indices) { val q = p[(i + 1) % p.size]; s += p[i].x * q.y - q.x * p[i].y }
        return abs(s / 2)
    }

    @Test
    fun dropShadowWrapsTheNodeWithSoftLayers() {
        val (out, ids) = assertNotNull(doc(box("a", 0.0, 0.0, 10.0, 10.0)).dropShadow(setOf("a"), 3.0, 4.0, 6.0, Rgba.Black, 0.5))
        val g = out.findNode(ids.single()) as GroupNode
        val layers = g.children[0] as GroupNode
        assertEquals(8, layers.children.size)
        assertEquals("a", (g.children[1] as PathNode).id)
        val total = 1 - layers.children.fold(1.0) { acc, n -> acc * (1 - (n as PathNode).opacity) }
        near(0.5, total, 1e-9)
        // gölge kaymış
        assertEquals(3.0, layers.children[0].transform.apply(Vec2(0.0, 0.0)).x, 1e-9)
        assertEquals(4.0, layers.children[0].transform.apply(Vec2(0.0, 0.0)).y, 1e-9)
    }

    @Test
    fun hardShadowIsASingleCopyAndImagesAreSkipped() {
        val (out, ids) = assertNotNull(doc(box("a", 0.0, 0.0, 10.0, 10.0)).dropShadow(setOf("a"), 2.0, 2.0, 0.0, Rgba.Black, 0.7))
        assertEquals(1, ((out.findNode(ids.single()) as GroupNode).children[0] as GroupNode).children.size)
        val img = ImageNode(id = "i", image = ImageData(ByteArray(1), "image/png", 1, 1))
        assertNull(doc(img).dropShadow(setOf("i"), 1.0, 1.0, 1.0, Rgba.Black, 0.5))
    }

    @Test
    fun outerStrokeSitsBehindAndIsWider() {
        val n = box("a", 0.0, 0.0, 10.0, 10.0).copy(stroke = Stroke(Paint.Solid(Rgba.Black), 2.0))
        val (out, id) = assertNotNull(doc(n).addOuterStroke(setOf("a"), 3.0, Rgba.rgb(0xFF0000)))
        val g = out.findNode(id.single()) as GroupNode
        assertEquals(8.0, (g.children[0] as PathNode).stroke!!.width, 1e-9)
        assertEquals("a", g.children[1].id)
        assertNull(doc(n).addOuterStroke(setOf("a"), 0.0, Rgba.Black))
    }

    @Test
    fun taperedStrokeAreaIsTheAverageWidthTimesLength() {
        val line = PathNode(id = "l", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 2.0))
        val (out, id) = assertNotNull(doc(line).taperStroke("l", 10.0, 0.0))
        val n = out.findNode(id) as PathNode
        assertNull(n.stroke)
        assertNotNull(n.fill)
        near(500.0, area(n.subpaths.single()), 1.0)
        val closed = PathNode(id = "c", subpaths = listOf(Shapes.rect(Rect(0.0, 0.0, 5.0, 5.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 1.0))
        assertNull(doc(closed).taperStroke("c", 3.0, 1.0))
    }

    @Test
    fun arrowheadsAppendTrianglesBeyondTheEnds() {
        val line = PathNode(id = "l", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 4.0))
        val (out, id) = assertNotNull(doc(line).addArrowheads("l", atStart = true, atEnd = true, scale = 3.0))
        val g = out.findNode(id) as GroupNode
        val heads = g.children[1] as PathNode
        assertEquals(2, heads.subpaths.size)
        val maxX = heads.subpaths.flatMap { it.anchors }.maxOf { it.point.x }
        val minX = heads.subpaths.flatMap { it.anchors }.minOf { it.point.x }
        near(106.0, maxX, 1e-9)
        near(-6.0, minX, 1e-9)
        assertNull(doc(line).addArrowheads("l", atStart = false, atEnd = false, scale = 3.0))
        assertNull(doc(box("b", 0.0, 0.0, 5.0, 5.0)).addArrowheads("b", atStart = false, atEnd = true, scale = 3.0))
    }

    @Test
    fun alongPathBendsOntoTheGuide() {
        val vertical = Ruler(Shapes.line(Vec2(0.0, 0.0), Vec2(0.0, 100.0)))
        near(100.0, vertical.length, 1e-9)
        val out = alongPath(listOf(SubPath(listOf(Anchor(Vec2(10.0, 0.0)), Anchor(Vec2(20.0, 5.0))))), vertical)
        val a = out.single().anchors
        assertEquals(0.0, a[0].point.x, 1e-9)
        assertEquals(10.0, a[0].point.y, 1e-9)
        assertEquals(-5.0, a[1].point.x, 1e-9)
        assertEquals(20.0, a[1].point.y, 1e-9)
        // yolun ötesi teğet yönünde uzar
        val beyond = alongPath(listOf(SubPath(listOf(Anchor(Vec2(0.0, 0.0)), Anchor(Vec2(130.0, 0.0))))), vertical).single().anchors
        assertEquals(130.0, beyond[1].point.y, 1e-6)
    }

    @Test
    fun artBrushStretchesTheShapeAlongTheGuide() {
        val shape = box("s", 0.0, 0.0, 20.0, 10.0)
        val guide = PathNode(id = "g", subpaths = listOf(Shapes.line(Vec2(0.0, 50.0), Vec2(100.0, 50.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 1.0))
        val (out, id) = assertNotNull(doc(shape, guide).artBrush("s", "g", 1.0))
        assertNull(out.findNode("g"))
        val n = out.findNode(id) as PathNode
        val b = n.subpaths.mapNotNull { it.bounds() }.reduce(Rect::union)
        near(100.0, b.width, 1e-6)
        near(10.0, b.height, 1e-6)
        near(50.0, b.center.y, 1e-6)
    }

    @Test
    fun tilePatternFillsAndClipsAndRemovesTheTile() {
        val d = doc(box("t", 0.0, 0.0, 100.0, 100.0), box("tile", 200.0, 200.0, 210.0, 210.0))
        val (out, id) = assertNotNull(d.tilePattern("t", setOf("tile"), 0.0, 0.0))
        assertNull(out.findNode("tile"))
        val g = out.findNode(id) as GroupNode
        assertNotNull(g.clip)
        assertEquals(121, g.children.size)
        // Çok sayıda karo reddedilir.
        assertNull(d.tilePattern("t", setOf("tile"), 0.0, 0.0, maxTiles = 50))
        assertNull(d.tilePattern("t", setOf("t"), 0.0, 0.0))
    }

    @Test
    fun alignStrokeProducesAFilledRing() {
        val n = box("a", 0.0, 0.0, 10.0, 10.0).copy(stroke = Stroke(Paint.Solid(Rgba.rgb(0xFF0000)), 2.0))
        val (out, id) = assertNotNull(doc(n).alignStroke("a", StrokeAlign.Inside, BoxOps()))
        val g = out.findNode(id) as GroupNode
        assertEquals(2, g.children.size)
        assertNull((g.children[1] as PathNode).stroke)
        assertNull(doc(box("b", 0.0, 0.0, 5.0, 5.0)).alignStroke("b", StrokeAlign.Outside, BoxOps()))
    }

    @Test
    fun outlineViewStripsFillsEverywhere() {
        val g = GroupNode(children = listOf(box("a", 0.0, 0.0, 5.0, 5.0).copy(opacity = 0.3, blendMode = BlendMode.Multiply)))
        val o = doc(g).outlineView()
        val p = (o.layers[0].children[0] as GroupNode).children[0] as PathNode
        assertNull(p.fill)
        assertNotNull(p.stroke)
        assertEquals(1.0, p.opacity, 0.0)
        assertEquals(BlendMode.Normal, p.blendMode)
    }
}
