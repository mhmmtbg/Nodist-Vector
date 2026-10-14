package io.github.mhmmtbg.nodist.model

/** Eksene hizalı dikdörtgenlerle çalışan sahte bölge işlemleri: yalnızca yüz ayırma mantığını sınar. */
internal class BoxOps : RegionOps {
    private fun box(r: Region): Rect = r.subpaths.single().bounds()!!
    private fun region(rects: List<Rect>) = Region(rects.map { Shapes.rect(it) })

    override fun intersect(a: Region, b: Region): Region? {
        // Parçalı bölgeler (birden çok dikdörtgen) için her ikiliyi kesiştirir.
        val out = ArrayList<Rect>()
        for (x in a.subpaths) for (y in b.subpaths) x.bounds()!!.intersect(y.bounds()!!)?.takeIf { it.width > 0 && it.height > 0 }?.let(out::add)
        return region(out)
    }

    override fun minus(a: Region, b: Region): Region? {
        var parts = a.subpaths.map { it.bounds()!! }
        for (cut in b.subpaths.map { it.bounds()!! }) {
            parts = parts.flatMap { p -> subtract(p, cut) }
        }
        return region(parts)
    }

    override fun union(a: Region, b: Region): Region? = Region(a.subpaths + b.subpaths)

    private fun subtract(p: Rect, c: Rect): List<Rect> {
        val i = p.intersect(c)
        if (i == null || i.width <= 0 || i.height <= 0) return listOf(p)
        val out = ArrayList<Rect>()
        if (i.top > p.top) out += Rect(p.left, p.top, p.right, i.top)
        if (i.bottom < p.bottom) out += Rect(p.left, i.bottom, p.right, p.bottom)
        if (i.left > p.left) out += Rect(p.left, i.top, i.left, i.bottom)
        if (i.right < p.right) out += Rect(i.right, i.top, p.right, i.bottom)
        return out
    }
}

