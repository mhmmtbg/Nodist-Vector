package io.github.mhmmtbg.nodist.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompoundTest {
    private val red = Paint.Solid(Rgba.rgb(0xFF0000))
    private val blue = Paint.Solid(Rgba.rgb(0x0000FF))

    private fun docOf(vararg nodes: Node) = Document(
        artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 400.0, 400.0))),
        layers = listOf(Layer(id = "L", name = "L", children = nodes.toList())),
    )

    private fun box(id: String, r: Rect, fill: Paint = red) = PathNode(id = id, subpaths = listOf(Shapes.rect(r)), fill = fill)
    private fun open(id: String, vararg pts: Vec2) =
        PathNode(id = id, subpaths = listOf(SubPath(pts.map { Anchor(it) })), stroke = Stroke(red, 2.0))

    @Test
    fun compoundMergesSubpathsWithEvenOddAndTopStyle() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 100.0, 100.0), red), box("b", Rect(25.0, 25.0, 75.0, 75.0), blue))
        val (out, id) = assertNotNull(doc.makeCompound(setOf("a", "b")))
        val kids = out.findLayer("L")!!.children
        assertEquals(1, kids.size)
        val p = kids[0] as PathNode
        assertEquals("b", id)
        assertEquals(2, p.subpaths.size)
        assertEquals(FillRule.EvenOdd, p.fillRule)
        assertEquals(blue, p.fill)
    }

    @Test
    fun compoundBakesTransforms() {
        val a = box("a", Rect(0.0, 0.0, 10.0, 10.0)).copy(transform = Matrix.translate(100.0, 0.0))
        val b = box("b", Rect(0.0, 0.0, 10.0, 10.0))
        val (out, _) = assertNotNull(docOf(a, b).makeCompound(setOf("a", "b")))
        val p = out.findNode("b") as PathNode
        val xs = p.subpaths.flatMap { it.anchors }.map { it.point.x }
        assertEquals(110.0, xs.max())
        assertEquals(0.0, xs.min())
    }

    @Test
    fun compoundNeedsTwoPaths() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 10.0, 10.0)))
        assertNull(doc.makeCompound(setOf("a")))
    }

    @Test
    fun releaseSplitsIntoSeparatePaths() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 100.0, 100.0)), box("b", Rect(25.0, 25.0, 75.0, 75.0), blue))
        val (c, id) = assertNotNull(doc.makeCompound(setOf("a", "b")))
        val (out, ids) = assertNotNull(c.releaseCompound(id))
        assertEquals(2, ids.size)
        val kids = out.findLayer("L")!!.children
        assertEquals(2, kids.size)
        assertTrue(kids.all { it is PathNode && it.subpaths.size == 1 && it.fillRule == FillRule.NonZero })
        assertEquals(ids, kids.map { it.id })
    }

    @Test
    fun joinClosesSingleOpenPath() {
        val doc = docOf(open("a", Vec2(0.0, 0.0), Vec2(100.0, 0.0), Vec2(100.0, 100.0)))
        val (out, id) = assertNotNull(doc.joinPaths(setOf("a")))
        val p = out.findNode(id) as PathNode
        assertTrue(p.subpaths.single().closed)
    }

    @Test
    fun joinChainsOpenPathsByNearestEnds() {
        val doc = docOf(
            open("a", Vec2(0.0, 0.0), Vec2(50.0, 0.0)),
            open("b", Vec2(100.0, 0.0), Vec2(60.0, 0.0)), // ters yönde
        )
        val (out, id) = assertNotNull(doc.joinPaths(setOf("a", "b")))
        val sp = (out.findNode(id) as PathNode).subpaths.single()
        assertFalse(sp.closed)
        assertEquals(4, sp.anchors.size)
        assertEquals(Vec2(0.0, 0.0), sp.anchors.first().point)
        assertEquals(Vec2(100.0, 0.0), sp.anchors.last().point)
        assertEquals(Vec2(60.0, 0.0), sp.anchors[2].point)
    }

    @Test
    fun joinMergesCoincidentEnds() {
        val doc = docOf(
            open("a", Vec2(0.0, 0.0), Vec2(50.0, 0.0)),
            open("b", Vec2(50.0, 0.0), Vec2(50.0, 50.0)),
        )
        val (out, id) = assertNotNull(doc.joinPaths(setOf("a", "b")))
        assertEquals(3, (out.findNode(id) as PathNode).subpaths.single().anchors.size)
    }

    @Test
    fun joinClosesWhenLoopTouches() {
        val doc = docOf(
            open("a", Vec2(0.0, 0.0), Vec2(100.0, 0.0), Vec2(100.0, 100.0)),
            open("b", Vec2(100.0, 100.0), Vec2(0.0, 100.0), Vec2(0.0, 0.0)),
        )
        val (out, id) = assertNotNull(doc.joinPaths(setOf("a", "b")))
        val sp = (out.findNode(id) as PathNode).subpaths.single()
        assertTrue(sp.closed)
        assertEquals(4, sp.anchors.size)
    }

    @Test
    fun scissorsSplitsOpenPathInTwo() {
        val doc = docOf(open("a", Vec2(0.0, 0.0), Vec2(100.0, 0.0)))
        val (out, ids) = assertNotNull(doc.scissorsCut("a", Vec2(40.0, 1.0), 6.0))
        assertEquals(2, ids.size)
        val kids = out.findLayer("L")!!.children.map { it as PathNode }
        assertEquals(2, kids.size)
        val first = kids[0].subpaths.single()
        val second = kids[1].subpaths.single()
        assertEquals(40.0, first.anchors.last().point.x, 1e-6)
        assertEquals(40.0, second.anchors.first().point.x, 1e-6)
        assertEquals(Vec2(100.0, 0.0), second.anchors.last().point)
    }

    @Test
    fun scissorsOpensClosedPath() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 100.0, 100.0)))
        val (out, ids) = assertNotNull(doc.scissorsCut("a", Vec2(50.0, 0.0), 6.0))
        assertEquals(1, ids.size)
        val sp = (out.findNode("a") as PathNode).subpaths.single()
        assertFalse(sp.closed)
        assertEquals(6, sp.anchors.size)
        assertEquals(50.0, sp.anchors.first().point.x, 1e-6)
        assertEquals(50.0, sp.anchors.last().point.x, 1e-6)
    }

    @Test
    fun scissorsMissesFarAwayPoints() {
        val doc = docOf(open("a", Vec2(0.0, 0.0), Vec2(100.0, 0.0)))
        assertNull(doc.scissorsCut("a", Vec2(40.0, 50.0), 6.0))
    }

    @Test
    fun shapesAreClosedAndSized() {
        val r = Shapes.roundedRect(Rect(0.0, 0.0, 100.0, 50.0), 80.0) // yarıçap kısa kenarın yarısına iner
        assertTrue(r.closed)
        val b = assertNotNull(r.bounds())
        assertEquals(100.0, b.width, 1e-6)
        assertEquals(50.0, b.height, 1e-6)
        val poly = Shapes.polygon(Vec2(0.0, 0.0), 10.0, 6)
        assertEquals(6, poly.anchors.size)
        assertTrue(poly.anchors.all { abs(it.point.distanceTo(Vec2.Zero) - 10.0) < 1e-9 })
        val star = Shapes.star(Vec2(0.0, 0.0), 10.0, 4.0, 5)
        assertEquals(10, star.anchors.size)
        assertEquals(4.0, star.anchors[1].point.distanceTo(Vec2.Zero), 1e-9)
        assertEquals(Vec2(0.0, -10.0).y, star.anchors[0].point.y, 1e-9)
    }
}

class PathNearTest {
    @Test
    fun findsClosestVisiblePathWithinTolerance() {
        val a = PathNode(id = "a", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 2.0))
        val b = PathNode(id = "b", subpaths = listOf(Shapes.line(Vec2(0.0, 10.0), Vec2(100.0, 10.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 2.0))
        val locked = b.copy(id = "c", subpaths = listOf(Shapes.line(Vec2(0.0, 6.0), Vec2(100.0, 6.0))), locked = true)
        val doc = Document(artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 200.0, 200.0))), layers = listOf(Layer(id = "L", name = "L", children = listOf(a, b, locked))))
        assertEquals("a", doc.pathNear("L", Vec2(50.0, 2.0), 5.0))
        assertEquals("b", doc.pathNear("L", Vec2(50.0, 9.0), 5.0))
        assertNull(doc.pathNear("L", Vec2(50.0, 50.0), 5.0))
    }

    @Test
    fun looksInsideTransformedGroups() {
        val inner = PathNode(id = "p", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(10.0, 0.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 1.0))
        val g = GroupNode(id = "g", transform = Matrix.translate(100.0, 100.0) * Matrix.scale(2.0), children = listOf(inner))
        val doc = Document(artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 200.0, 200.0))), layers = listOf(Layer(id = "L", name = "L", children = listOf(g))))
        assertEquals("p", doc.pathNear("L", Vec2(110.0, 101.0), 3.0))
        assertNull(doc.pathNear("L", Vec2(5.0, 5.0), 3.0))
    }
}
