package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransformsTest {
    private val red = Paint.Solid(Rgba.rgb(0xFF0000))
    private val blue = Paint.Solid(Rgba.rgb(0x0000FF))

    private fun box(id: String, r: Rect, fill: Paint = red, stroke: Stroke? = null, opacity: Double = 1.0) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(r)), fill = fill, stroke = stroke, opacity = opacity)

    private fun docOf(vararg nodes: Node) = Document(
        artboards = listOf(Artboard(id = "A1", name = "Çalışma Yüzeyi 1", bounds = Rect(0.0, 0.0, 400.0, 400.0))),
        layers = listOf(Layer(id = "L", name = "L", children = nodes.toList())),
    )

    private fun near(e: Vec2, a: Vec2, tol: Double = 1e-6) {
        assertEquals(e.x, a.x, tol)
        assertEquals(e.y, a.y, tol)
    }

    @Test
    fun rotateAboutCenterIsCounterClockwiseOnScreen() {
        val spec = TransformSpec(TransformKind.Rotate, angle = 90.0)
        val m = spec.matrix(Vec2(0.0, 0.0))
        // Ekranda sağdaki nokta 90° saat yönünün tersine dönünce yukarı (y azalır) çıkar.
        near(Vec2(0.0, -10.0), m.apply(Vec2(10.0, 0.0)))
    }

    @Test
    fun scaleKeepsTheReferencePoint() {
        val doc = docOf(box("a", Rect(100.0, 100.0, 200.0, 200.0)))
        val (out, _) = assertNotNull(doc.transformWith(setOf("a"), TransformSpec(TransformKind.Scale, sx = 2.0, sy = 0.5), RefPoint.TopLeft))
        val b = assertNotNull(out.boundsOf("a"))
        assertEquals(100.0, b.left, 1e-6)
        assertEquals(100.0, b.top, 1e-6)
        assertEquals(200.0, b.width, 1e-6)
        assertEquals(50.0, b.height, 1e-6)
    }

    @Test
    fun reflectOnVerticalAxisFlipsLeftRight() {
        val m = TransformSpec(TransformKind.Reflect, axis = 90.0).matrix(Vec2(50.0, 50.0))
        near(Vec2(90.0, 20.0), m.apply(Vec2(10.0, 20.0)))
        val h = TransformSpec(TransformKind.Reflect, axis = 0.0).matrix(Vec2(50.0, 50.0))
        near(Vec2(10.0, 80.0), h.apply(Vec2(10.0, 20.0)))
    }

    @Test
    fun reflectOnDiagonalSwapsCoordinates() {
        val m = TransformSpec(TransformKind.Reflect, axis = 45.0).matrix(Vec2(0.0, 0.0))
        // 45° (saat yönünün tersine, ekranda) eksen: (1,0) -> (0,-1)
        near(Vec2(0.0, -1.0), m.apply(Vec2(1.0, 0.0)))
    }

    @Test
    fun shearLeansTheTopToTheRight() {
        val m = TransformSpec(TransformKind.Shear, angle = 45.0).matrix(Vec2(0.0, 0.0))
        // Orijinin üstündeki (y<0) nokta sağa kayar; altındaki sola.
        near(Vec2(10.0, -10.0), m.apply(Vec2(0.0, -10.0)))
        near(Vec2(-10.0, 10.0), m.apply(Vec2(0.0, 10.0)))
        near(Vec2(5.0, 0.0), m.apply(Vec2(5.0, 0.0)))
    }

    @Test
    fun copyKeepsTheOriginalAndTransformsTheCopy() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 10.0, 10.0)))
        val (out, ids) = assertNotNull(doc.transformWith(setOf("a"), TransformSpec(TransformKind.Rotate, angle = 90.0), RefPoint.BottomLeft, copy = true))
        assertEquals(2, out.findLayer("L")!!.children.size)
        assertTrue("a" !in ids)
        assertEquals(Rect(0.0, 0.0, 10.0, 10.0), out.boundsOf("a"))
        val b = assertNotNull(out.boundsOf(ids.single()))
        assertEquals(10.0, b.width, 1e-6)
        // Sol alt köşe etrafında saat yönünün tersine 90°: kare sola doğru yatar.
        assertEquals(-10.0, b.left, 1e-6)
        assertEquals(0.0, b.right, 1e-6)
        assertEquals(10.0, b.bottom, 1e-6)
        assertEquals(0.0, b.top, 1e-6)
    }

    @Test
    fun alignToRectAndKeyObject() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 10.0, 10.0)), box("b", Rect(50.0, 30.0, 70.0, 40.0)), box("c", Rect(200.0, 200.0, 220.0, 230.0)))
        val toBoard = doc.alignToRect(setOf("a"), Align.Right, Rect(0.0, 0.0, 400.0, 400.0))
        assertEquals(390.0, toBoard.boundsOf("a")!!.left, 1e-9)
        val key = doc.boundsOf("c")!!
        val aligned = doc.alignToRect(setOf("a", "b", "c"), Align.Left, key, skipId = "c")
        assertEquals(200.0, aligned.boundsOf("a")!!.left, 1e-9)
        assertEquals(200.0, aligned.boundsOf("b")!!.left, 1e-9)
        assertEquals(key, aligned.boundsOf("c"))
        assertEquals("c", doc.frontmost(setOf("a", "c")))
    }

    @Test
    fun distributeWithSpacing() {
        val doc = docOf(box("a", Rect(0.0, 0.0, 10.0, 10.0)), box("b", Rect(50.0, 0.0, 70.0, 10.0)), box("c", Rect(300.0, 0.0, 330.0, 10.0)))
        val out = doc.distributeSpacing(setOf("a", "b", "c"), true, 5.0)
        assertEquals(15.0, out.boundsOf("b")!!.left, 1e-9)
        assertEquals(40.0, out.boundsOf("c")!!.left, 1e-9)
    }

    @Test
    fun artboardLifecycle() {
        val doc = docOf(box("a", Rect(10.0, 10.0, 50.0, 50.0)))
        val (two, id2) = doc.addArtboard()
        assertEquals(2, two.artboards.size)
        val ab2 = two.artboards.first { it.id == id2 }
        assertEquals(460.0, ab2.bounds.left, 1e-9)
        assertEquals(400.0, ab2.bounds.width, 1e-9)
        assertEquals("Çalışma Yüzeyi 2", ab2.name)
        val (three, id3) = assertNotNull(two.duplicateArtboard("A1"))
        // Yüzeydeki nesne de kopyalandı ve yeni yüzeyin içinde.
        assertEquals(2, three.findLayer("L")!!.children.size)
        val copy = three.findLayer("L")!!.children.last()
        assertTrue(three.artboards.first { it.id == id3 }.bounds.contains(copy.bounds()!!.center))
        assertEquals(1, three.removeArtboard(id3).removeArtboard(id2).artboards.size)
        assertEquals(1, three.removeArtboard(id3).removeArtboard(id2).removeArtboard("A1").artboards.size)
        assertEquals(listOf("A1"), three.onlyArtboard("A1")!!.artboards.map { it.id })
        val fit = assertNotNull(doc.fitArtboardToArtwork("A1", 5.0))
        assertEquals(Rect(5.0, 5.0, 55.0, 55.0), fit.artboards.single().bounds)
        assertNull(docOf().fitArtboardToArtwork("A1"))
    }

    @Test
    fun selectSameByFillStrokeAndType() {
        val s = Stroke(blue, 3.0)
        val doc = docOf(
            box("a", Rect(0.0, 0.0, 1.0, 1.0), red, s),
            box("b", Rect(0.0, 0.0, 1.0, 1.0), red, null),
            box("c", Rect(0.0, 0.0, 1.0, 1.0), blue, s, opacity = 0.5),
            GroupNode(id = "g", children = listOf(box("d", Rect(0.0, 0.0, 1.0, 1.0), red, s))),
            box("e", Rect(0.0, 0.0, 1.0, 1.0), red, s).copy(locked = true),
        )
        assertEquals(setOf("a", "b", "d"), doc.selectSame("a", SameKind.Fill))
        assertEquals(setOf("a", "c", "d"), doc.selectSame("a", SameKind.Stroke))
        assertEquals(setOf("a", "d"), doc.selectSame("a", SameKind.FillAndStroke))
        assertEquals(setOf("a", "c", "d"), doc.selectSame("a", SameKind.StrokeWidth))
        assertEquals(setOf("c"), doc.selectSame("c", SameKind.Opacity))
        assertEquals(setOf("a", "b", "c", "d"), doc.selectSame("a", SameKind.Type))
    }
}
