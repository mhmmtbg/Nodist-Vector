package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DivideTest {
    private fun r(l: Double, t: Double, rr: Double, b: Double) = Region(listOf(Shapes.rect(Rect(l, t, rr, b))))
    private fun area(reg: Region) = reg.subpaths.sumOf { it.bounds()!!.let { b -> b.width * b.height } }

    @Test
    fun twoOverlappingBoxesGiveThreeFaces() {
        val faces = assertNotNull(Divide.faces(listOf(r(0.0, 0.0, 10.0, 10.0), r(5.0, 5.0, 15.0, 15.0)), BoxOps()))
        // alt-sadece: 100-25 = 75; kesişim 25 (üstteki şeklin görünümü); üst-sadece 100-25 = 75
        assertEquals(3, faces.size)
        val byOwner = faces.groupBy({ it.second }, { area(it.first) })
        assertEquals(75.0, byOwner[0]!!.sum(), 1e-9)
        assertEquals(100.0, byOwner[1]!!.sum(), 1e-9) // kesişim + üst-sadece
        assertEquals(175.0, faces.sumOf { area(it.first) }, 1e-9)
    }

    @Test
    fun separateBoxesStayWhole() {
        val faces = assertNotNull(Divide.faces(listOf(r(0.0, 0.0, 10.0, 10.0), r(20.0, 0.0, 30.0, 10.0)), BoxOps()))
        assertEquals(2, faces.size)
        assertEquals(listOf(0, 1), faces.map { it.second }.sorted())
    }

    @Test
    fun threeBoxesCoverTheUnionWithoutOverlap() {
        val shapes = listOf(r(0.0, 0.0, 10.0, 10.0), r(5.0, 0.0, 15.0, 10.0), r(8.0, 0.0, 12.0, 10.0))
        val faces = assertNotNull(Divide.faces(shapes, BoxOps()))
        assertEquals(150.0, faces.sumOf { area(it.first) }, 1e-9)
        // En üstteki şekil (3.) kendi alanının tamamını alır.
        assertEquals(40.0, faces.filter { it.second == 2 }.sumOf { area(it.first) }, 1e-9)
    }

    private fun doc(vararg nodes: Node): Document = Document(name = "t", artboards = emptyList(), layers = listOf(Layer(id = "L", name = "k", children = nodes.toList())))
    private fun box(id: String, l: Double, t: Double, r: Double, b: Double, fill: Rgba) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(l, t, r, b))), fill = Paint.Solid(fill))

    @Test
    fun knifeSplitsAFilledShapeAlongTheLine() {
        val d = doc(box("a", 0.0, 0.0, 10.0, 10.0, Rgba.Black), box("b", 50.0, 0.0, 60.0, 10.0, Rgba.Black))
        val (out, ids) = assertNotNull(d.knifeCut("L", Vec2(4.0, -100.0), Vec2(4.0, 100.0), BoxOps()))
        assertEquals(2, ids.size)
        assertEquals(3, out.layers[0].children.size)
        assertEquals(100.0, ids.sumOf { area(Region(listOf((out.findNode(it) as PathNode).subpaths.single()))) }, 1e-9)
    }

    @Test
    fun knifeMissingEverythingReturnsNull() {
        val d = doc(box("a", 0.0, 0.0, 10.0, 10.0, Rgba.Black))
        assertEquals(null, d.knifeCut("L", Vec2(40.0, -100.0), Vec2(40.0, 100.0), BoxOps()))
    }

    @Test
    fun divideShapesReplacesTheSelectionWithFaces() {
        val d = doc(box("a", 0.0, 0.0, 10.0, 10.0, Rgba.rgb(0xFF0000)), box("b", 5.0, 5.0, 15.0, 15.0, Rgba.rgb(0x0000FF)))
        val (out, ids) = assertNotNull(d.divideShapes(setOf("a", "b"), BoxOps()))
        assertEquals(ids.size, out.layers[0].children.size)
        assertEquals(3, ids.size)
    }

    @Test
    fun outlineStrokeNeedsAStrokeAndGroupsWithFill() {
        val plain = doc(box("a", 0.0, 0.0, 10.0, 10.0, Rgba.Black))
        assertEquals(null, plain.outlineStroke("a", BoxOps()))
        val st = doc(PathNode(id = "s", subpaths = listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(50.0, 0.0))), stroke = Stroke(Paint.Solid(Rgba.Black), 4.0)))
        val (o1, id1) = assertNotNull(st.outlineStroke("s", BoxOps()))
        val n = o1.findNode(id1) as PathNode
        assertEquals(null, n.stroke)
        assertNotNull(n.fill)
        val both = doc(box("f", 0.0, 0.0, 10.0, 10.0, Rgba.Black).copy(stroke = Stroke(Paint.Solid(Rgba.rgb(0xFF0000)), 2.0)))
        val (o2, id2) = assertNotNull(both.outlineStroke("f", BoxOps()))
        assertEquals(2, (o2.findNode(id2) as GroupNode).children.size)
    }
}
