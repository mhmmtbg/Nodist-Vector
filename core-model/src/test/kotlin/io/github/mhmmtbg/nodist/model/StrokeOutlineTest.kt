package io.github.mhmmtbg.nodist.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StrokeOutlineTest {
    private val black = Paint.Solid(Rgba.Black)

    /** Örnekleme ile birleşim alanı. */
    private fun coveredArea(pieces: List<SubPath>, box: Rect, step: Double = 0.25): Double {
        var n = 0
        var y = box.top + step / 2
        while (y < box.bottom) {
            var x = box.left + step / 2
            while (x < box.right) {
                if (StrokeGeometry.covers(pieces, Vec2(x, y))) n++
                x += step
            }
            y += step
        }
        return n * step * step
    }

    private fun near(e: Double, a: Double, tol: Double) = assertTrue(abs(e - a) <= tol, "beklenen $e, gelen $a")

    @Test
    fun buttLineIsARectangle() {
        val pieces = StrokeGeometry.pieces(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), Stroke(black, 10.0))
        near(1000.0, coveredArea(pieces, Rect(-10.0, -10.0, 110.0, 10.0)), 20.0)
    }

    @Test
    fun roundCapsAddTwoHalfDiscs() {
        val pieces = StrokeGeometry.pieces(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), Stroke(black, 10.0, cap = LineCap.Round))
        near(1000.0 + PI * 25, coveredArea(pieces, Rect(-10.0, -10.0, 110.0, 10.0)), 25.0)
    }

    @Test
    fun squareCapsExtendByHalfTheWidth() {
        val pieces = StrokeGeometry.pieces(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), Stroke(black, 10.0, cap = LineCap.Square))
        near(1100.0, coveredArea(pieces, Rect(-10.0, -10.0, 110.0, 10.0)), 25.0)
    }

    @Test
    fun closedSquareWithMiterJoinsMatchesOuterMinusInner() {
        val sq = Shapes.rect(Rect(0.0, 0.0, 100.0, 100.0))
        val pieces = StrokeGeometry.pieces(listOf(sq), Stroke(black, 10.0, join = LineJoin.Miter))
        // dış 110x110, iç 90x90
        near(110.0 * 110 - 90.0 * 90, coveredArea(pieces, Rect(-10.0, -10.0, 110.0, 110.0), 0.5), 40.0)
        assertTrue(!StrokeGeometry.covers(pieces, Vec2(50.0, 50.0)))
        assertTrue(StrokeGeometry.covers(pieces, Vec2(-4.0, -4.0))) // sivri köşe
    }

    @Test
    fun bevelAndRoundJoinsClipTheCorner() {
        val sq = Shapes.rect(Rect(0.0, 0.0, 100.0, 100.0))
        val bevel = StrokeGeometry.pieces(listOf(sq), Stroke(black, 10.0, join = LineJoin.Bevel))
        assertTrue(!StrokeGeometry.covers(bevel, Vec2(-4.5, -4.5)))
        assertTrue(StrokeGeometry.covers(bevel, Vec2(-2.0, -2.0)))
        val round = StrokeGeometry.pieces(listOf(sq), Stroke(black, 10.0, join = LineJoin.Round))
        assertTrue(StrokeGeometry.covers(round, Vec2(-3.0, -3.0)))
        assertTrue(!StrokeGeometry.covers(round, Vec2(-4.9, -4.9)))
    }

    @Test
    fun miterLimitFallsBackToBevel() {
        val sharp = SubPath(listOf(Anchor(Vec2(0.0, 0.0)), Anchor(Vec2(100.0, 5.0)), Anchor(Vec2(0.0, 10.0))))
        val limited = StrokeGeometry.pieces(listOf(sharp), Stroke(black, 6.0, join = LineJoin.Miter, miterLimit = 2.0))
        val unlimited = StrokeGeometry.pieces(listOf(sharp), Stroke(black, 6.0, join = LineJoin.Miter, miterLimit = 100.0))
        assertTrue(!StrokeGeometry.covers(limited, Vec2(140.0, 5.0)))
        assertTrue(StrokeGeometry.covers(unlimited, Vec2(140.0, 5.0)))
    }

    @Test
    fun dashesLeaveGaps() {
        val st = Stroke(black, 4.0, dash = listOf(10.0, 10.0))
        val pieces = StrokeGeometry.pieces(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(100.0, 0.0))), st)
        assertTrue(StrokeGeometry.covers(pieces, Vec2(5.0, 0.0)))
        assertTrue(!StrokeGeometry.covers(pieces, Vec2(15.0, 0.0)))
        assertTrue(StrokeGeometry.covers(pieces, Vec2(25.0, 0.0)))
        near(5 * 10.0 * 4.0, coveredArea(pieces, Rect(-1.0, -3.0, 101.0, 3.0)), 20.0)
    }

    @Test
    fun dottedRoundStrokeMakesDots() {
        val st = Stroke(black, 6.0, cap = LineCap.Round, dash = listOf(0.0, 12.0))
        val pieces = StrokeGeometry.pieces(listOf(Shapes.line(Vec2(0.0, 0.0), Vec2(60.0, 0.0))), st)
        assertTrue(StrokeGeometry.covers(pieces, Vec2(12.0, 0.0)))
        assertTrue(!StrokeGeometry.covers(pieces, Vec2(18.0, 0.0)))
    }

    @Test
    fun curvesAreCovered() {
        val c = Shapes.ellipse(Rect(0.0, 0.0, 100.0, 100.0))
        val pieces = StrokeGeometry.pieces(listOf(c), Stroke(black, 8.0))
        assertTrue(StrokeGeometry.covers(pieces, Vec2(50.0, 0.0)))
        assertTrue(StrokeGeometry.covers(pieces, Vec2(0.0, 50.0)))
        assertTrue(!StrokeGeometry.covers(pieces, Vec2(50.0, 50.0)))
        near(PI * (54.0 * 54 - 46.0 * 46), coveredArea(pieces, Rect(-6.0, -6.0, 106.0, 106.0), 0.5), 120.0)
    }

    @Test
    fun zeroWidthGivesNothingAndHalfPlaneSplitsTheBox() {
        assertTrue(StrokeGeometry.pieces(listOf(Shapes.rect(Rect(0.0, 0.0, 1.0, 1.0))), Stroke(black, 0.0)).isEmpty())
        val left = assertNotNull(StrokeGeometry.halfPlane(Vec2(0.0, 50.0), Vec2(100.0, 50.0), 1000.0, left = true))
        val right = assertNotNull(StrokeGeometry.halfPlane(Vec2(0.0, 50.0), Vec2(100.0, 50.0), 1000.0, left = false))
        // Yönün solu ekranda (y aşağı) çizginin altıdır: (-d.y, d.x) = (0, 1)
        assertTrue(StrokeGeometry.covers(listOf(left), Vec2(50.0, 80.0)))
        assertTrue(!StrokeGeometry.covers(listOf(left), Vec2(50.0, 20.0)))
        assertTrue(StrokeGeometry.covers(listOf(right), Vec2(50.0, 20.0)))
    }
}
