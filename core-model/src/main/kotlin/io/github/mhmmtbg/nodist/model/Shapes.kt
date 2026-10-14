package io.github.mhmmtbg.nodist.model

/** Temel şekilleri Bezier yoluna çeviren yardımcılar. */
object Shapes {
    /** Çeyrek daireyi tek kübik Bezier ile yaklaştıran katsayı. */
    const val KAPPA = 0.5522847498307936

    fun rect(r: Rect) = SubPath(r.corners.map { Anchor(it) }, closed = true)

    fun ellipse(r: Rect): SubPath {
        val c = r.center
        val rx = r.width / 2
        val ry = r.height / 2
        val kx = rx * KAPPA
        val ky = ry * KAPPA
        return SubPath(
            listOf(
                Anchor(Vec2(c.x, r.top), Vec2(c.x - kx, r.top), Vec2(c.x + kx, r.top)),
                Anchor(Vec2(r.right, c.y), Vec2(r.right, c.y - ky), Vec2(r.right, c.y + ky)),
                Anchor(Vec2(c.x, r.bottom), Vec2(c.x + kx, r.bottom), Vec2(c.x - kx, r.bottom)),
                Anchor(Vec2(r.left, c.y), Vec2(r.left, c.y + ky), Vec2(r.left, c.y - ky)),
            ),
            closed = true,
        )
    }

    /** Köşeleri yuvarlatılmış dikdörtgen; [radius] kısa kenarın yarısıyla sınırlanır. */
    fun roundedRect(r: Rect, radius: Double): SubPath {
        val rad = radius.coerceIn(0.0, minOf(r.width, r.height) / 2)
        if (rad <= 1e-9) return rect(r)
        val k = rad * (1 - KAPPA)
        val l = r.left
        val t = r.top
        val rt = r.right
        val b = r.bottom
        return SubPath(
            listOf(
                Anchor(Vec2(l + rad, t), handleIn = Vec2(l + k, t)),
                Anchor(Vec2(rt - rad, t), handleOut = Vec2(rt - k, t)),
                Anchor(Vec2(rt, t + rad), handleIn = Vec2(rt, t + k)),
                Anchor(Vec2(rt, b - rad), handleOut = Vec2(rt, b - k)),
                Anchor(Vec2(rt - rad, b), handleIn = Vec2(rt - k, b)),
                Anchor(Vec2(l + rad, b), handleOut = Vec2(l + k, b)),
                Anchor(Vec2(l, b - rad), handleIn = Vec2(l, b - k)),
                Anchor(Vec2(l, t + rad), handleOut = Vec2(l, t + k)),
            ),
            closed = true,
        )
    }

    /** Düzgün çokgen: ilk köşe [startAngle] (radyan, 0 = sağ) yönündedir. */
    fun polygon(center: Vec2, radius: Double, sides: Int, startAngle: Double = -Math.PI / 2): SubPath {
        val n = sides.coerceAtLeast(3)
        return SubPath(
            List(n) { i ->
                val a = startAngle + 2 * Math.PI * i / n
                Anchor(Vec2(center.x + radius * Math.cos(a), center.y + radius * Math.sin(a)))
            },
            closed = true,
        )
    }

    /** Yıldız: [points] uç, dış yarıçap [outer], iç yarıçap [inner]. İlk uç [startAngle] yönündedir. */
    fun star(center: Vec2, outer: Double, inner: Double, points: Int, startAngle: Double = -Math.PI / 2): SubPath {
        val n = points.coerceAtLeast(3)
        return SubPath(
            List(2 * n) { i ->
                val a = startAngle + Math.PI * i / n
                val r = if (i % 2 == 0) outer else inner
                Anchor(Vec2(center.x + r * Math.cos(a), center.y + r * Math.sin(a)))
            },
            closed = true,
        )
    }

    fun line(a: Vec2, b: Vec2) = SubPath(listOf(Anchor(a), Anchor(b)), closed = false)
}
