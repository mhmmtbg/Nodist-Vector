package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocMetaTest {
    private fun doc() = Document(
        artboards = listOf(Artboard(name = "Çalışma Yüzeyi 1", bounds = Rect(0.0, 0.0, 100.0, 100.0)), Artboard(name = "Kapak | ön; yüz", bounds = Rect(160.0, 0.0, 260.0, 100.0))),
        layers = listOf(
            Layer(
                id = "L", name = "L",
                children = listOf(PathNode(id = "a", subpaths = listOf(Shapes.rect(Rect(10.0, 10.0, 50.0, 40.0))), fill = Paint.Solid(Rgba.rgb(0xFF0000)))),
            ),
        ),
    )

    @Test
    fun nothingToSaveGivesNull() {
        assertNull(DocMeta.encode(doc().copy(artboards = listOf(Artboard(name = "Çalışma Yüzeyi 1", bounds = Rect(0.0, 0.0, 1.0, 1.0))))))
    }

    @Test
    fun roundTripsGuidesSwatchesAndArtboardNames() {
        var d = doc()
        d = d.addGuide(true, 123.5).first.addGuide(false, 20.25).first
        d = d.copy(guides = d.guides.mapIndexed { i, g -> if (i == 1) g.copy(locked = true) else g })
        d = d.addSwatch(Rgba.rgb(0x3B9BFF), "Mavi; açık").addSwatch(Ink.Cmyk(1.0, 0.0, 0.0, 0.0).toRgb(), "Camgöbeği")
        val text = assertNotNull(DocMeta.encode(d))
        assertTrue(text.all { it.code in 32..126 })
        val plain = doc()
        val back = DocMeta.apply(plain, text)
        assertEquals(d.guides.map { Triple(it.vertical, it.position, it.locked) }, back.guides.map { Triple(it.vertical, it.position, it.locked) })
        assertEquals(listOf("Mavi; açık", "Camgöbeği"), back.swatches.map { it.name })
        assertEquals(0x3B9BFF, back.swatches[0].color.toArgb() and 0xFFFFFF)
        assertTrue(back.swatches[1].color.ink is Ink.Cmyk)
        assertEquals(listOf("Çalışma Yüzeyi 1", "Kapak | ön; yüz"), back.artboards.map { it.name })
    }

    @Test
    fun garbageIsIgnored() {
        val d = doc()
        assertEquals(d, DocMeta.apply(d, null))
        assertEquals(d, DocMeta.apply(d, "başka bir şey"))
        val broken = DocMeta.apply(d, "NODIST1;g=x:y:z|v:abc;s=ZZZ:Ad;a=Tek")
        assertEquals(d, broken)
    }

    @Test
    fun guidesCanBeMovedLockedAndRemoved() {
        val (d1, id) = doc().addGuide(true, 10.0)
        assertEquals(20.0, d1.moveGuide(id, 20.0).guides.single().position)
        val locked = d1.setGuidesLocked(true)
        assertEquals(10.0, locked.moveGuide(id, 20.0).guides.single().position)
        assertTrue(d1.removeGuide(id).guides.isEmpty())
        assertTrue(d1.addGuide(false, 5.0).first.clearGuides().guides.isEmpty())
        assertEquals(4, doc().makeGuidesFrom("a").guides.size)
    }

    @Test
    fun swatchesAreDeduplicatedAndGlobal() {
        val d = doc().addSwatch(Rgba.rgb(0xFF0000)).addSwatch(Rgba.rgb(0xFF0000), "Yine kırmızı")
        assertEquals(1, d.swatches.size)
        assertEquals("#FF0000", d.swatches[0].name)
        val changed = d.replaceSwatchColor(0, Rgba.rgb(0x00FF00))
        assertEquals(0x00FF00, changed.swatches[0].color.toArgb() and 0xFFFFFF)
        val fill = (changed.findNode("a") as PathNode).fill as Paint.Solid
        assertEquals(0x00FF00, fill.color.toArgb() and 0xFFFFFF)
        assertEquals(0, changed.removeSwatch(0).swatches.size)
        assertEquals("Mor", changed.renameSwatch(0, "Mor").swatches[0].name)
    }

    @Test
    fun snapUsesExtraLinesAndGrid() {
        val box = Rect(103.0, 52.0, 153.0, 82.0)
        val r = Snap.box(box, emptyList(), 5.0, linesX = listOf(100.0), linesY = listOf(50.0))
        assertEquals(-3.0, r.dx, 1e-9)
        assertEquals(-2.0, r.dy, 1e-9)
        assertEquals(listOf(100.0), r.guidesX)
        val far = Snap.box(box, emptyList(), 5.0, linesX = listOf(0.0))
        assertEquals(0.0, far.dx, 1e-9)
        val grid = Snap.gridLines(doubleArrayOf(103.0, 128.0, 153.0), 25.0)
        assertEquals(listOf(100.0, 125.0, 150.0), grid)
        val onGrid = Snap.box(box, emptyList(), 5.0, linesX = grid)
        assertEquals(-3.0, onGrid.dx, 1e-9)
        assertTrue(Snap.gridLines(doubleArrayOf(1.0), 0.0).isEmpty())
    }
}
