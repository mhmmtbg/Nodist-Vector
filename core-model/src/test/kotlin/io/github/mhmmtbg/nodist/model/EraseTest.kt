package io.github.mhmmtbg.nodist.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EraseTest {
    private val red = Paint.Solid(Rgba.rgb(0xFF0000))

    private fun line(id: String, x0: Double = 0.0, x1: Double = 100.0, y: Double = 0.0) =
        PathNode(id = id, subpaths = listOf(Shapes.line(Vec2(x0, y), Vec2(x1, y))), stroke = Stroke(red, 2.0))

    private fun square(id: String) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(0.0, 0.0, 100.0, 100.0))), fill = red)

    private fun docOf(vararg layers: Layer) = Document(artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 200.0, 200.0))), layers = layers.toList())

    private fun area(sp: SubPath): Double {
        val p = sp.flatten()
        var s = 0.0
        for (i in p.indices) {
            val q = p[(i + 1) % p.size]
            s += p[i].x * q.y - q.x * p[i].y
        }
        return abs(s / 2)
    }

    @Test
    fun strokeIsSplitAtTheTouchedPlace() {
        val layer = Layer(id = "L", name = "L", children = listOf(line("a")))
        val r = docOf(layer).erase("L", Vec2(50.0, 0.0), Vec2(50.0, 0.0), 10.0)
        assertTrue(r.changed)
        val kids = r.document.findLayer("L")!!.children
        assertEquals(1, kids.size)
        val subs = (kids[0] as PathNode).subpaths
        assertEquals(2, subs.size)
        // Eşik = yarıçap + kalınlık/4 = 10,5: kalanlar yaklaşık [0, 39,5] ve [60,5, 100].
        val left = subs.first { it.anchors.first().point.x < 1.0 }
        val right = subs.first { it.anchors.last().point.x > 99.0 }
        assertTrue(abs(left.anchors.last().point.x - 39.5) < 0.05, "sol parça ${left.anchors.last().point.x}")
        assertTrue(abs(right.anchors.first().point.x - 60.5) < 0.05, "sağ parça ${right.anchors.first().point.x}")
    }

    @Test
    fun otherLayersAreNeverTouched() {
        val a = Layer(id = "A", name = "A", children = listOf(line("a")))
        val b = Layer(id = "B", name = "B", children = listOf(line("b")))
        val doc = docOf(a, b)
        val r = doc.erase("A", Vec2(50.0, 0.0), Vec2(50.0, 0.0), 10.0)
        assertTrue(r.changed)
        assertSame(b, r.document.findLayer("B"))
        // Aynı yerde B etkinken yalnızca B değişir.
        val r2 = doc.erase("B", Vec2(50.0, 0.0), Vec2(50.0, 0.0), 10.0)
        assertSame(a, r2.document.findLayer("A"))
    }

    @Test
    fun nothingHappensWhenBrushMisses() {
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(line("a"))))
        val r = doc.erase("L", Vec2(50.0, 40.0), Vec2(60.0, 40.0), 10.0)
        assertFalse(r.changed)
        assertSame(doc, r.document)
    }

    @Test
    fun lockedOrHiddenLayerIsLeftAlone() {
        val doc = docOf(Layer(id = "L", name = "L", locked = true, children = listOf(line("a"))))
        assertFalse(doc.erase("L", Vec2(50.0, 0.0), Vec2(50.0, 0.0), 10.0).changed)
        val hidden = docOf(Layer(id = "L", name = "L", visible = false, children = listOf(line("a"))))
        assertFalse(hidden.erase("L", Vec2(50.0, 0.0), Vec2(50.0, 0.0), 10.0).changed)
    }

    @Test
    fun dragAcrossCutsTheWholeLineInPieces() {
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(line("a"))))
        // Fırça çizginin ortasından dikey geçmez; boyunca sürülür: [20, 80] silinir.
        val r = doc.erase("L", Vec2(20.0, 0.0), Vec2(80.0, 0.0), 5.0)
        val subs = (r.document.findLayer("L")!!.children.single() as PathNode).subpaths
        assertEquals(2, subs.size)
        val ends = subs.map { it.anchors.last().point.x to it.anchors.first().point.x }
        assertTrue(subs.any { abs(it.anchors.last().point.x - 14.5) < 0.05 })
        assertTrue(subs.any { abs(it.anchors.first().point.x - 85.5) < 0.05 }, "uçlar $ends")
    }

    @Test
    fun fullyCoveredPathIsRemoved() {
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(line("a", 0.0, 10.0), line("b", 0.0, 100.0, 50.0))))
        val r = doc.erase("L", Vec2(5.0, 0.0), Vec2(5.0, 0.0), 30.0)
        assertEquals(listOf("b"), r.document.findLayer("L")!!.children.map { it.id })
    }

    @Test
    fun filledShapeLosesOnlyTheTouchedPart() {
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(square("s"))))
        val r = doc.erase("L", Vec2(100.0, 50.0), Vec2(100.0, 50.0), 10.0)
        assertTrue(r.changed)
        val node = r.document.findLayer("L")!!.children.single() as PathNode
        assertTrue(node.stroke == null)
        // Kalan şekil tek parça: kenarları ortak parçalar (aralarında ince çizgi bırakan) olmamalı.
        assertEquals(1, node.subpaths.size)
        val total = node.subpaths.sumOf { area(it) }
        // Karenin 10000 alanından kenardaki yarım dairenin (≈157) çıkması beklenir.
        assertTrue(total > 9800 && total < 9900, "alan $total")
    }

    @Test
    fun filledAndStrokedShapeSplitsIntoFillAndOutline() {
        val shape = square("s").copy(stroke = Stroke(red, 2.0))
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(shape)))
        val r = doc.erase("L", Vec2(100.0, 50.0), Vec2(100.0, 50.0), 10.0)
        val kids = r.document.findLayer("L")!!.children.filterIsInstance<PathNode>()
        assertEquals(2, kids.size)
        val fill = kids.single { it.fill != null }
        val outline = kids.single { it.stroke != null }
        assertTrue(fill.stroke == null)
        assertTrue(outline.fill == null)
        // Kenar çizgisi silinen yerde kopuk: kapalı değil, açık parça(lar).
        assertTrue(outline.subpaths.none { it.closed })
    }

    @Test
    fun erasingInsideTransformedGroupUsesDocumentSpace() {
        val group = GroupNode(id = "g", transform = Matrix.translate(100.0, 0.0), children = listOf(line("a")))
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(group)))
        // Çizgi belgede x = 100..200; ortası 150.
        val r = doc.erase("L", Vec2(150.0, 0.0), Vec2(150.0, 0.0), 10.0)
        assertTrue(r.changed)
        val subs = ((r.document.findLayer("L")!!.children.single() as GroupNode).children.single() as PathNode).subpaths
        assertEquals(2, subs.size)
        // Yerel uzayda 50 civarı silinmeli.
        assertTrue(subs.any { abs(it.anchors.last().point.x - 39.5) < 0.05 })
    }

    @Test
    fun textAndImagesAreReportedNotErased() {
        val text = TextNode(id = "t", text = "Merhaba", transform = Matrix.translate(10.0, 30.0))
        val doc = docOf(Layer(id = "L", name = "L", children = listOf(text)))
        val r = doc.erase("L", Vec2(20.0, 25.0), Vec2(20.0, 25.0), 10.0)
        assertFalse(r.changed)
        assertTrue(r.skippedUnsupported)
    }
}
