package io.github.mhmmtbg.nodist.model

import kotlin.math.tan

/** Döndür, ölçekle, yansıt, eğ: Illustrator'daki dönüşüm araçları. */
enum class TransformKind { Rotate, Scale, Reflect, Shear }

/** Dönüşümün sabit kaldığı nokta: seçim kutusunun dokuz noktasından biri. */
enum class RefPoint(val fx: Double, val fy: Double) {
    TopLeft(0.0, 0.0), Top(0.5, 0.0), TopRight(1.0, 0.0),
    Left(0.0, 0.5), Center(0.5, 0.5), Right(1.0, 0.5),
    BottomLeft(0.0, 1.0), Bottom(0.5, 1.0), BottomRight(1.0, 1.0);

    fun on(r: Rect) = Vec2(r.left + r.width * fx, r.top + r.height * fy)
}

/**
 * Bir dönüşümün tarifi. Açılar derecedir ve Illustrator'daki gibi saat yönünün tersine artar.
 * - [Rotate]: [angle] kadar döndürür.
 * - [Scale]: [sx] ve [sy] çarpanlarıyla ölçekler (1 = aynı boy).
 * - [Reflect]: [axis] açısındaki eksene göre yansıtır (0 = yatay eksen, 90 = dikey eksen).
 * - [Shear]: [axis] eksenine göre [angle] kadar eğer.
 */
data class TransformSpec(
    val kind: TransformKind,
    val angle: Double = 0.0,
    val sx: Double = 1.0,
    val sy: Double = 1.0,
    val axis: Double = 0.0,
) {
    /** [origin] noktası sabit kalacak şekilde dönüşüm matrisi (belge uzayında; y aşağı doğrudur). */
    fun matrix(origin: Vec2): Matrix {
        val core = when (kind) {
            TransformKind.Rotate -> Matrix.rotate(-Math.toRadians(angle))
            TransformKind.Scale -> Matrix.scale(sx, sy)
            TransformKind.Reflect -> aboutAxis(Matrix.scale(1.0, -1.0))
            TransformKind.Shear -> aboutAxis(Matrix(c = -tan(Math.toRadians(angle))))
        }
        return Matrix.translate(origin.x, origin.y) * core * Matrix.translate(-origin.x, -origin.y)
    }

    /** [m] dönüşümünü, eksen açısı kadar döndürülmüş bir eksen takımında uygular. */
    private fun aboutAxis(m: Matrix): Matrix {
        val phi = -Math.toRadians(axis)
        return Matrix.rotate(phi) * m * Matrix.rotate(-phi)
    }
}

/**
 * [ids] düğümlerine [spec] dönüşümünü seçimin [ref] noktasına göre uygular. [copy] doğruysa özgün nesneler yerinde kalır,
 * dönüştürülen kopyalar eklenir (Illustrator'daki "Kopya").
 * @return yeni belge ve dönüştürülen düğümlerin kimlikleri (kopyada yeni kimlikler).
 */
fun Document.transformWith(ids: Set<String>, spec: TransformSpec, ref: RefPoint = RefPoint.Center, copy: Boolean = false): Pair<Document, Set<String>>? {
    val box = boundsOf(ids) ?: return null
    val m = spec.matrix(ref.on(box))
    if (!copy) return transformNodes(ids, m) to ids
    val (withCopies, created) = duplicate(ids, Vec2.Zero)
    val newIds = created.toSet()
    return withCopies.transformNodes(newIds, m) to newIds
}

// ---- Hizalama hedefleri -------------------------------------------------------

/**
 * [ids] düğümlerini [target] kutusuna göre hizalar ([skipId] yerinde kalır; anahtar nesne için).
 */
fun Document.alignToRect(ids: Set<String>, mode: Align, target: Rect, skipId: String? = null): Document {
    var doc = this
    for (id in ids) {
        if (id == skipId) continue
        val b = boundsOf(id) ?: continue
        val dx = when (mode) {
            Align.Left -> target.left - b.left
            Align.CenterH -> target.center.x - b.center.x
            Align.Right -> target.right - b.right
            else -> 0.0
        }
        val dy = when (mode) {
            Align.Top -> target.top - b.top
            Align.CenterV -> target.center.y - b.center.y
            Align.Bottom -> target.bottom - b.bottom
            else -> 0.0
        }
        if (dx != 0.0 || dy != 0.0) doc = doc.transformNode(id, Matrix.translate(dx, dy))
    }
    return doc
}

/** Seçimdeki, çizimde en üstte olan düğüm (anahtar nesne olarak kullanılır). */
fun Document.frontmost(ids: Set<String>): String? {
    var found: String? = null
    fun walk(nodes: List<Node>) {
        for (n in nodes) {
            if (n.id in ids) found = n.id
            if (n is GroupNode) walk(n.children)
        }
    }
    for (l in layers) walk(l.children)
    return found
}

/**
 * Düğümleri aralarındaki boşluk [gap] olacak şekilde dizer; ilk nesne yerinde kalır.
 */
fun Document.distributeSpacing(ids: Set<String>, horizontal: Boolean, gap: Double): Document {
    val boxes = ids.mapNotNull { id -> boundsOf(id)?.let { id to it } }
        .sortedBy { if (horizontal) it.second.left else it.second.top }
    if (boxes.size < 2) return this
    var cursor = if (horizontal) boxes[0].second.right + gap else boxes[0].second.bottom + gap
    var doc = this
    for ((id, b) in boxes.drop(1)) {
        val d = cursor - if (horizontal) b.left else b.top
        if (kotlin.math.abs(d) > 1e-9) doc = doc.transformNode(id, if (horizontal) Matrix.translate(d, 0.0) else Matrix.translate(0.0, d))
        cursor += (if (horizontal) b.width else b.height) + gap
    }
    return doc
}
