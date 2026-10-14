package io.github.mhmmtbg.nodist.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Konturu dolu şekillere çevirmek için geometri ("Kontur ana hatlarını çıkar", "Yolu kaydır").
 *
 * [pieces] konturun kapladığı alanı, üst üste binen basit çokgenlerle verir: her doğru parçası için bir dörtgen,
 * köşeler için birleşim şekilleri, uçlar için kapaklar. Hepsi aynı yönde sarılır; bu yüzden "sıfır olmayan sarım"
 * kuralıyla doldurulduklarında birleşimleri konturun alanıdır. Gerçek bir birleşim için bir yol işlemcisi
 * (Skia) kullanılır.
 */
object StrokeGeometry {
    private const val CIRCLE_SIDES = 32

    /** Yolun düzleştirilmiş noktaları; yinelenen ardışık noktalar atılır, kapalı yolun tekrarlanan son noktası düşer. */
    private fun polyline(sp: SubPath, steps: Int): List<Vec2> {
        val raw = sp.flatten(steps)
        val out = ArrayList<Vec2>(raw.size)
        for (p in raw) if (out.isEmpty() || out.last().distanceTo(p) > 1e-9) out += p
        if (sp.closed && out.size > 1 && out.first().distanceTo(out.last()) <= 1e-9) out.removeAt(out.size - 1)
        return out
    }

    private fun area(p: List<Vec2>): Double {
        var s = 0.0
        for (i in p.indices) {
            val q = p[(i + 1) % p.size]
            s += p[i].x * q.y - q.x * p[i].y
        }
        return s / 2
    }

    /** Çokgeni her zaman aynı yönde (alanı pozitif) döndürür. */
    private fun piece(points: List<Vec2>): SubPath? {
        if (points.size < 3) return null
        val a = area(points)
        if (kotlin.math.abs(a) < 1e-12) return null
        val ordered = if (a > 0) points else points.reversed()
        return SubPath(ordered.map { Anchor(it) }, closed = true)
    }

    private fun circle(c: Vec2, r: Double): SubPath? =
        piece(List(CIRCLE_SIDES) { i ->
            val a = 2 * PI * i / CIRCLE_SIDES
            Vec2(c.x + r * cos(a), c.y + r * sin(a))
        })

    /** Kesikli çizgi: açık çoklu çizgi parçalarına ayrılır. [dash] boş ya da hepsi sıfırsa yol olduğu gibi döner. */
    internal fun dashed(pts: List<Vec2>, closed: Boolean, dash: List<Double>, offset: Double): List<List<Vec2>> {
        val pattern = if (dash.size % 2 == 1) dash + dash else dash
        val period = pattern.sum()
        if (pattern.isEmpty() || period <= 1e-9 || pattern.any { it < 0 }) return listOf(if (closed) pts + pts.first() else pts)
        val line = if (closed) pts + pts.first() else pts
        val out = ArrayList<List<Vec2>>()
        var idx = 0
        var remain = pattern[0]
        var o = ((offset % period) + period) % period
        while (o > 0) {
            if (o >= remain) { o -= remain; idx = (idx + 1) % pattern.size; remain = pattern[idx] } else { remain -= o; o = 0.0 }
        }
        var current: ArrayList<Vec2>? = if (idx % 2 == 0) arrayListOf(line[0]) else null
        var guard = 0
        for (i in 0 until line.size - 1) {
            val a = line[i]
            val b = line[i + 1]
            val len = a.distanceTo(b)
            if (len <= 1e-12) continue
            val d = Vec2((b.x - a.x) / len, (b.y - a.y) / len)
            var pos = 0.0
            while (len - pos > remain && guard++ < 200_000) {
                pos += remain
                val p = Vec2(a.x + d.x * pos, a.y + d.y * pos)
                if (idx % 2 == 0) {
                    // Açık dilimin sonu: sıfır uzunluklu (nokta) dilimler çok kısa bir çizgi olur.
                    val c = current ?: arrayListOf(p)
                    if (c.size == 1 && c[0].distanceTo(p) <= 1e-9) c += Vec2(p.x + d.x * 1e-4, p.y + d.y * 1e-4) else c += p
                    out += c
                    current = null
                } else {
                    current = arrayListOf(p)
                }
                idx = (idx + 1) % pattern.size
                remain = pattern[idx]
            }
            remain -= (len - pos)
            if (current != null) current += b
        }
        current?.let { if (it.size >= 2) out += it }
        return out
    }

    /**
     * [subpaths] yollarının [stroke] konturunu oluşturan örtüşen parçalar (hepsi aynı yönde sarılı, kapalı çokgenler).
     * Eğriler düzleştirilir; [curveSteps] her eğri parçasının bölüm sayısıdır.
     */
    fun pieces(subpaths: List<SubPath>, stroke: Stroke, curveSteps: Int = 24): List<SubPath> {
        val h = stroke.width / 2
        if (h <= 0.0) return emptyList()
        val out = ArrayList<SubPath>()
        fun add(p: SubPath?) { if (p != null) out += p }
        for (sp in subpaths) {
            val base = polyline(sp, curveSteps)
            if (base.isEmpty()) continue
            if (base.size == 1) {
                // Tek noktalı yol: yuvarlak kapakta nokta, kare kapakta kare.
                when (stroke.cap) {
                    LineCap.Round -> add(circle(base[0], h))
                    LineCap.Square -> add(piece(listOf(Vec2(base[0].x - h, base[0].y - h), Vec2(base[0].x + h, base[0].y - h), Vec2(base[0].x + h, base[0].y + h), Vec2(base[0].x - h, base[0].y + h))))
                    LineCap.Butt -> {}
                }
                continue
            }
            val runs = if (stroke.dash.isEmpty()) listOf(base to sp.closed) else dashed(base, sp.closed, stroke.dash, stroke.dashOffset).map { it to false }
            for ((pts, closed) in runs) addRun(pts, closed, stroke, h, out)
        }
        return out
    }

    private fun addRun(pts: List<Vec2>, closed: Boolean, stroke: Stroke, h: Double, out: MutableList<SubPath>) {
        val n = pts.size
        if (n < 2) return
        fun add(p: SubPath?) { if (p != null) out += p }
        val count = if (closed) n else n - 1
        val dirs = ArrayList<Vec2>(count)
        for (i in 0 until count) {
            val a = pts[i]
            val b = pts[(i + 1) % n]
            val len = hypot(b.x - a.x, b.y - a.y)
            dirs += if (len <= 1e-12) Vec2(1.0, 0.0) else Vec2((b.x - a.x) / len, (b.y - a.y) / len)
        }
        for (i in 0 until count) {
            val a = pts[i]
            val b = pts[(i + 1) % n]
            if (a.distanceTo(b) <= 1e-12) continue
            val d = dirs[i]
            val nx = -d.y * h
            val ny = d.x * h
            add(piece(listOf(Vec2(a.x + nx, a.y + ny), Vec2(b.x + nx, b.y + ny), Vec2(b.x - nx, b.y - ny), Vec2(a.x - nx, a.y - ny))))
        }
        // Köşeler
        val joinRange = if (closed) 0 until n else 1 until n - 1
        for (i in joinRange) {
            val d1 = dirs[(i - 1 + count) % count]
            val d2 = dirs[i % count]
            joinAt(pts[i], d1, d2, stroke, h, out)
        }
        // Uç kapakları
        if (!closed) {
            val dStart = dirs.first()
            val dEnd = dirs.last()
            capAt(pts.first(), Vec2(-dStart.x, -dStart.y), stroke.cap, h, out)
            capAt(pts.last(), dEnd, stroke.cap, h, out)
        }
    }

    private fun capAt(p: Vec2, outward: Vec2, cap: LineCap, h: Double, out: MutableList<SubPath>) {
        when (cap) {
            LineCap.Butt -> {}
            LineCap.Round -> circle(p, h)?.let(out::add)
            LineCap.Square -> {
                val nx = -outward.y * h
                val ny = outward.x * h
                val ex = outward.x * h
                val ey = outward.y * h
                piece(listOf(Vec2(p.x + nx, p.y + ny), Vec2(p.x + nx + ex, p.y + ny + ey), Vec2(p.x - nx + ex, p.y - ny + ey), Vec2(p.x - nx, p.y - ny)))?.let(out::add)
            }
        }
    }

    private fun joinAt(v: Vec2, d1: Vec2, d2: Vec2, stroke: Stroke, h: Double, out: MutableList<SubPath>) {
        val cross = d1.x * d2.y - d1.y * d2.x
        val dot = d1.x * d2.x + d1.y * d2.y
        if (kotlin.math.abs(cross) < 1e-9 && dot > 0) return // doğrusal devam
        if (stroke.join == LineJoin.Round || (kotlin.math.abs(cross) < 1e-9 && dot < 0)) {
            circle(v, h)?.let(out::add)
            return
        }
        // Dış taraf, dönüşün tersi yöndedir.
        val s = if (cross > 0) -1.0 else 1.0
        val p1 = Vec2(v.x + s * -d1.y * h, v.y + s * d1.x * h)
        val p2 = Vec2(v.x + s * -d2.y * h, v.y + s * d2.x * h)
        if (stroke.join == LineJoin.Miter) {
            // Sivri uç uzunluğu / kalınlık = 1 / cos(dönüş açısı / 2)
            val cosHalf = sqrt(max(0.0, (1 + dot) / 2))
            val ratio = if (cosHalf < 1e-9) Double.MAX_VALUE else 1 / cosHalf
            if (ratio <= stroke.miterLimit) {
                val k = h / (1 + dot)
                val tip = Vec2(v.x + s * (-d1.y - d2.y) * k, v.y + s * (d1.x + d2.x) * k)
                piece(listOf(v, p1, tip, p2))?.let(out::add)
                return
            }
        }
        piece(listOf(v, p1, p2))?.let(out::add)
    }

    /** Parçaların sarım sayısına göre birleşim içinde olup olmadığı (testler ve önizleme için). */
    fun covers(pieces: List<SubPath>, p: Vec2): Boolean {
        for (sp in pieces) {
            val pts = sp.anchors.map { it.point }
            var inside = false
            var j = pts.size - 1
            for (i in pts.indices) {
                val a = pts[i]
                val b = pts[j]
                if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
                j = i
            }
            if (inside) return true
        }
        return false
    }

    /** Çizginin bir tarafındaki yarım düzlemi kaplayan büyük dörtgen ([left] doğruysa yönün solu). */
    fun halfPlane(a: Vec2, b: Vec2, extent: Double, left: Boolean): SubPath? {
        val len = a.distanceTo(b)
        if (len <= 1e-9) return null
        val d = Vec2((b.x - a.x) / len, (b.y - a.y) / len)
        val side = if (left) 1.0 else -1.0
        val n = Vec2(-d.y * side, d.x * side)
        val far = extent
        val a2 = Vec2(a.x - d.x * far, a.y - d.y * far)
        val b2 = Vec2(b.x + d.x * far, b.y + d.y * far)
        return SubPath(
            listOf(Anchor(a2), Anchor(b2), Anchor(Vec2(b2.x + n.x * far, b2.y + n.y * far)), Anchor(Vec2(a2.x + n.x * far, a2.y + n.y * far))),
            closed = true,
        )
    }

    /** Yarım düzlem dörtgenleri için, [box] kutusunu ve çizgiyi rahatça aşan bir uzunluk. */
    fun extentFor(box: Rect, a: Vec2, b: Vec2): Double = max(hypot(box.width, box.height), a.distanceTo(b)) * 4 + 1.0
}
