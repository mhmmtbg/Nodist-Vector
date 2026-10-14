package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LiveEffectsTest {
    private val ctx = EffectContext(ops = BoxOps(), outlineText = { t -> listOf(Shapes.rect(Rect(0.0, -10.0, 10.0 * t.text.length, 0.0))) })
    private fun square(id: String = "a", size: Double = 100.0) =
        PathNode(id = id, subpaths = listOf(Shapes.rect(Rect(0.0, 0.0, size, size))), fill = Paint.Solid(Rgba.Black))

    @Test
    fun roundedCornersReplaceEachCornerWithAnArc() {
        val sp = Shapes.rect(Rect(0.0, 0.0, 100.0, 50.0)).roundedCorners(10.0)
        assertEquals(8, sp.anchors.size)
        val b = assertNotNull(sp.bounds())
        assertEquals(0.0, b.left, 1e-9); assertEquals(100.0, b.right, 1e-9); assertEquals(50.0, b.bottom, 1e-9)
        assertTrue(sp.anchors.any { it.point.distanceTo(Vec2(10.0, 0.0)) < 1e-9 })
        assertTrue(sp.anchors.any { it.point.distanceTo(Vec2(0.0, 10.0)) < 1e-9 })
        // Kenarın yarısından büyük yarıçap kısaltılır
        val big = Shapes.rect(Rect(0.0, 0.0, 20.0, 20.0)).roundedCorners(100.0)
        assertTrue(big.anchors.all { a -> a.point.x in -1e-9..20.0 + 1e-9 })
        // Eğrili yol değişmez
        val e = Shapes.ellipse(Rect(0.0, 0.0, 10.0, 10.0))
        assertEquals(e, e.roundedCorners(3.0))
        assertTrue(square().hasStraightCorners())
        assertTrue(!PathNode(subpaths = listOf(e)).hasStraightCorners())
    }

    @Test
    fun expandAppliesEffectsInStageOrderAndKeepsTheSource() {
        val n = square().copy(effects = listOf(Effect.Shadow(5.0, 5.0, 0.0, 0.5), Effect.RoundCorners(10.0)))
        val out = LiveEffects.expand(n, ctx) as GroupNode
        val shape = out.children[1] as PathNode
        assertEquals(8, shape.subpaths.single().anchors.size)
        assertTrue(shape.effects.isEmpty())
        assertEquals(4, n.subpaths.single().anchors.size)
        // Efektsiz düğüm aynen döner
        val plain = square()
        assertSame(plain, LiveEffects.expand(plain, ctx))
    }

    @Test
    fun effectsNeedingPathOpsAreSkippedWithoutThem() {
        val n = square().copy(effects = listOf(Effect.OffsetPath(5.0)))
        val out = LiveEffects.expand(n, EffectContext())
        assertEquals(square(), out)
    }

    @Test
    fun textPathBendsTheTextAndFollowsTheTextTransform() {
        val t = TextNode(id = "t", text = "ab", transform = Matrix.translate(100.0, 0.0),
            effects = listOf(Effect.TextPath(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(0.0, 100.0))))))
        val p = LiveEffects.expand(t, ctx) as PathNode
        assertEquals(Matrix.translate(100.0, 0.0), p.transform)
        val b = p.subpaths.mapNotNull { it.bounds() }.reduce(Rect::union)
        // Dikey yol: metin yukarıdan aşağı dizilir, genişliği 20 birim uzunluk olur.
        assertEquals(20.0, b.height, 1e-6)
    }

    @Test
    fun expandDeepReachesChildrenAndDocument() {
        val g = GroupNode(id = "g", children = listOf(square("c").copy(effects = listOf(Effect.RoundCorners(5.0)))))
        val doc = Document(name = "d", artboards = emptyList(), layers = listOf(Layer(name = "k", children = listOf(g))))
        val out = LiveEffects.expandDocument(doc, ctx)
        val c = ((out.layers[0].children[0] as GroupNode).children[0]) as PathNode
        assertEquals(8, c.subpaths.single().anchors.size)
        assertTrue(!LiveEffects.hasEffects(out.layers[0].children[0]))
        val clean = Document(name = "d", artboards = emptyList(), layers = listOf(Layer(name = "k", children = listOf(square()))))
        assertSame(clean, LiveEffects.expandDocument(clean, ctx))
    }

    @Test
    fun opacityMaskFollowsTheNodeTransform() {
        val grad = Paint.LinearGradient(Vec2(0.0, 0.0), Vec2(100.0, 0.0), listOf(GradientStop(0.0, Rgba.Black), GradientStop(1.0, Rgba(1.0, 1.0, 1.0))))
        val n = square().copy(transform = Matrix.translate(500.0, 0.0), effects = listOf(Effect.OpacityMask(grad, 4)))
        val g = LiveEffects.expand(n, ctx) as GroupNode
        val ops = g.children.map { it.opacity }
        assertEquals(ops.sorted(), ops)
        assertTrue(ops.last() > 0.8)
    }

    @Test
    fun codecRoundTripsEveryEffect() {
        val tile = square("tile", 10.0)
        val art = square("art", 20.0).copy(fill = Paint.Solid(Rgba.rgb(0x336699)))
        val grad = Paint.LinearGradient(Vec2(1.5, 2.0), Vec2(30.0, -4.25), listOf(GradientStop(0.0, Rgba.Black), GradientStop(0.5, Rgba(0.2, 0.4, 0.6, 0.5))), Matrix(2.0, 0.0, 0.0, 2.0, 3.0, 4.0))
        val effects = listOf(
            Effect.RoundCorners(12.5), Effect.OffsetPath(-3.0), Effect.Taper(10.0, 0.0),
            Effect.Arrowheads(atStart = true, atEnd = false, scale = 4.0), Effect.AlignStroke(StrokeAlign.Outside),
            Effect.OuterStroke(3.0, Rgba(1.0, 0.0, 0.0)), Effect.Shadow(6.0, -6.0, 8.0, 0.35, Rgba(0.1, 0.2, 0.3)),
            Effect.Mesh(6, 7, Rgba.Black, Rgba(1.0, 1.0, 1.0), Rgba(1.0, 0.0, 0.0), Rgba(0.0, 0.0, 1.0)),
            Effect.OpacityMask(grad, 16), Effect.Pattern(tile, 2.0, 3.0), Effect.ArtBrush(art, 1.5),
            Effect.TextPath(listOf(Shapes.ellipse(Rect(0.0, 0.0, 40.0, 20.0)), Shapes.line(Vec2(1.0, 2.0), Vec2(3.0, 4.0))), 7.0),
        )
        val (text, slots) = EffectCodec.encode(effects)
        assertEquals(listOf<Node>(tile, art), slots)
        assertEquals(effects, EffectCodec.decode(text, slots))
        // Bozuk ya da bilinmeyen parçalar atlanır
        assertEquals(listOf<Effect>(Effect.RoundCorners(2.0)), EffectCodec.decode("Bilinmez(a=1);RoundCorners(r=2);Pattern(s=9)", emptyList()))
    }

    @Test
    fun movingAPathCarriesTheMaskGradient() {
        val grad = Paint.LinearGradient(Vec2(0.0, 0.0), Vec2(100.0, 0.0), listOf(GradientStop(0.0, Rgba.Black), GradientStop(1.0, Rgba(1.0, 1.0, 1.0))))
        val n = square().copy(effects = listOf(Effect.OpacityMask(grad, 4)))
        val moved = n.applied(Matrix.translate(300.0, 0.0)) as PathNode
        val g = (moved.effects.single() as Effect.OpacityMask).gradient
        assertEquals(300.0, g.transform.apply(g.start).x, 1e-9)
        // Görünüm de aynı kalır (şeritler nesneyle birlikte gider).
        val before = (LiveEffects.expand(n, ctx) as GroupNode).children.map { it.opacity }
        val after = (LiveEffects.expand(moved, ctx) as GroupNode).children.map { it.opacity }
        assertEquals(before, after)
    }
}
