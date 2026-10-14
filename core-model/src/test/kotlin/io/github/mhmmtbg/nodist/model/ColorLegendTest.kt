package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorLegendTest {
    private fun box(color: Rgba, x: Double) =
        PathNode(subpaths = listOf(Shapes.rect(Rect(x, 0.0, x + 50, 50.0))), fill = Paint.Solid(color))

    private fun doc(vararg colors: Rgba) = Document(
        artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 1000.0, 600.0))),
        layers = listOf(Layer(name = "Katman 1", children = colors.mapIndexed { i, c -> box(c, i * 60.0) })),
    )

    @Test
    fun codesGiveHexRgbAndCmyk() {
        assertEquals(listOf("#FF0000", "RGB 255 0 0", "CMYK 0 100 100 0"), Rgba.rgb(0xFF0000).codeLines())
        assertEquals(listOf("#000000", "RGB 0 0 0", "CMYK 0 0 0 100"), Rgba.Black.codeLines())
        assertEquals(listOf("RGB 128 128 128"), Rgba.rgb(0x808080).codeLines(LegendOptions(hex = false, cmyk = false)))
        assertEquals("CMYK 0 0 0 50", Rgba.rgb(0x808080).codeLines().last())
        // Dosyada CMYK olarak tanımlı renkte özgün değerler yazılır, RGB'den yeniden hesaplanmaz.
        val print = Ink.Cmyk(0.62, 0.0, 0.38, 0.05).toRgb()
        assertEquals("CMYK 62 0 38 5", print.codeLines().last())
    }

    @Test
    fun legendListsEveryDesignColourInsideTheArtboard() {
        val d = doc(Rgba.rgb(0x6B6A42), Rgba.rgb(0xD8CBA0), Rgba.rgb(0x6B6A42), Rgba.rgb(0x2A2B18))
        val r = assertNotNull(d.withColorLegend())
        assertEquals(3, r.colors)
        val layer = r.document.layers.last()
        assertEquals(LEGEND_LAYER, layer.name)
        val group = layer.children.single() as GroupNode
        assertEquals(r.groupId, group.id)
        // Her renk için bir örnek kare ve bir etiket; en çok kullanılan renk başta.
        assertEquals(6, group.children.size)
        val swatch = group.children[0] as PathNode
        assertEquals(0x6B6A42, (swatch.fill as Paint.Solid).color.toArgb() and 0xFFFFFF)
        assertEquals("#6B6A42\nRGB 107 106 66\nCMYK 0 1 38 58", (group.children[1] as TextNode).text)
        val box = assertNotNull(group.bounds())
        val board = d.artboards.first().bounds
        assertTrue(box.left >= board.left && box.right <= board.right && box.bottom <= board.bottom && box.top > board.center.y, "lejant yüzeyin altında durmalı: $box")
        assertEquals(d.colorCodesText(), r.document.colorCodesText(), "lejantın kendi renkleri listeye karışmamalı")
        assertEquals(3, d.colorCodesText().lines().size)
        assertEquals(d.colorGroups().map { it.rgb }, r.document.colorGroups().map { it.rgb })
        assertEquals(LegendOptions(), r.document.legendOptions())
        assertNull(d.legendOptions())
    }

    @Test
    fun addingAgainReplacesTheOldLegendAndManyColoursWrap() {
        val many = (0 until 14).map { Rgba.rgb(0x101010 * it + 0x0000FF) }.toTypedArray()
        val first = assertNotNull(doc(*many).withColorLegend(LegendOptions(rgb = false, cmyk = false)))
        val again = assertNotNull(first.document.withColorLegend())
        assertEquals(2, again.document.layers.size)
        assertEquals(14, again.colors)
        val group = again.document.layers.last().children.single() as GroupNode
        val rows = group.children.filterIsInstance<PathNode>().mapNotNull { it.bounds()?.top }.distinct()
        assertTrue(rows.size > 1, "çok renk birden çok satıra bölünmeli")
        val board = again.document.artboards.first().bounds
        assertTrue(group.bounds()!!.right <= board.right + 1.0)
        assertNull(Document.blank().withColorLegend())
    }

    /** Tasarımın kendi lejantı: küçük renk kareleri ve yanlarında elle yazılmış (eskimiş) kodlar. */
    private fun handMadeLegend(): Document {
        fun swatch(id: String, x: Double, c: Int) = PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(x, 20.0, x + 18, 38.0))), fill = Paint.Solid(Rgba.rgb(c)))
        fun label(id: String, x: Double, t: String) = TextNode(id = id, text = t, fontSize = 8.0, transform = Matrix.translate(x, 32.0), measuredWidth = 34.0)
        return Document(
            artboards = listOf(Artboard(bounds = Rect(0.0, 0.0, 1000.0, 600.0))),
            layers = listOf(
                Layer(name = "Gövde", children = listOf(PathNode(id = "body", subpaths = listOf(Shapes.rect(Rect(100.0, 100.0, 500.0, 500.0))), fill = Paint.Solid(Rgba.rgb(0x080052))))),
                Layer(
                    name = "Bilgi",
                    children = listOf(
                        GroupNode(id = "g", transform = Matrix.translate(600.0, 0.0), children = listOf(swatch("s1", 0.0, 0x0B2654), label("t1", 24.0, "#6B6A42"))),
                        swatch("s2", 700.0, 0x080052), label("t2", 724.0, "#2A2B18"),
                        TextNode(id = "title", text = "ALTERNATİF #11", fontSize = 12.0, transform = Matrix.translate(50.0, 34.0)),
                    ),
                ),
            ),
        )
    }

    @Test
    fun recolouringUpdatesTheCodeWrittenNextToTheSwatch() {
        val d = handMadeLegend().recolored(0x080052, Rgba.rgb(0x700022))
        val r = d.syncedColorLabels(only = 0x700022)
        assertEquals(1, r.changed)
        assertEquals("#700022", (r.document.findNode("t2") as TextNode).text)
        // Değişmeyen rengin etiketi ve başlık yerinde kalır.
        assertEquals("#6B6A42", (r.document.findNode("t1") as TextNode).text)
        assertEquals("ALTERNATİF #11", (r.document.findNode("title") as TextNode).text)
        assertNull((r.document.findNode("t2") as TextNode).measuredWidth)
    }

    @Test
    fun allStaleCodesCanBeCorrectedAtOnce() {
        val r = handMadeLegend().syncedColorLabels()
        assertEquals(2, r.changed)
        assertEquals("#0B2654", (r.document.findNode("t1") as TextNode).text)
        assertEquals("#080052", (r.document.findNode("t2") as TextNode).text)
        assertEquals(0, r.document.syncedColorLabels().changed)
        // Uygulamanın eklediği lejantın etiketleri zaten doğrudur.
        assertEquals(0, doc(Rgba.rgb(0x6B6A42), Rgba.rgb(0xD8CBA0)).withColorLegend()!!.document.syncedColorLabels().changed)
    }

    @Test
    fun onlyTheChosenColoursEnterTheLegendAndAMovedLegendStaysInPlace() {
        val d = doc(Rgba.rgb(0x6B6A42), Rgba.rgb(0xD8CBA0), Rgba.rgb(0x2A2B18))
        val r = assertNotNull(d.withColorLegend(LegendOptions(rgb = false, cmyk = false), only = setOf(0x6B6A42, 0x2A2B18)))
        assertEquals(2, r.colors)
        assertEquals(setOf(0x6B6A42, 0x2A2B18), r.document.legendColors())
        assertEquals(2, d.colorCodesText(only = setOf(0xD8CBA0, 0x2A2B18)).lines().size)
        assertNull(d.withColorLegend(only = setOf(0x123456)))
        assertNull(d.legendColors())
        // Lejant taşındıktan sonra yenilenirse aynı yerde kalır.
        val moved = r.document.transformNodes(setOf(r.groupId), Matrix.translate(300.0, -400.0))
        val before = assertNotNull(moved.boundsOf(r.groupId))
        val again = assertNotNull(moved.withColorLegend(LegendOptions(rgb = false, cmyk = false), only = moved.legendColors()))
        val after = assertNotNull(again.document.boundsOf(again.groupId))
        assertEquals(before.left, after.left, 1e-6)
        assertEquals(before.top, after.top, 1e-6)
        // Grubu çözülmüş ve taşınmış lejant da yenilenince aynı köşede kalır.
        val loose = moved.ungroup(r.groupId)!!.first
        val was = loose.layers.last().children.mapNotNull { it.bounds() }.reduce(Rect::union)
        val redo = assertNotNull(loose.withColorLegend(LegendOptions(rgb = false, cmyk = false)))
        val box = assertNotNull(redo.document.boundsOf(redo.groupId))
        assertEquals(was.left, box.left, 1e-6)
        assertEquals(was.top, box.top, 1e-6)
    }
}
