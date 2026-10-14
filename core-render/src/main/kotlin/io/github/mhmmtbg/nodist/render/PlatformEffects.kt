package io.github.mhmmtbg.nodist.render

import io.github.mhmmtbg.nodist.model.EffectContext
import io.github.mhmmtbg.nodist.model.LiveEffects
import io.github.mhmmtbg.nodist.model.Node

/**
 * Canlı efektlerin platformdaki karşılığı: yol işlemleri Skia ile, metnin harf biçimleri cihaz fontuyla çıkarılır.
 * Genişletilmiş görünüm, düğüm nesnesi değişmedikçe yeniden üretilmez (düğümler değişmezdir; kimlik karşılaştırılır).
 */
object PlatformEffects {
    val context = EffectContext(ops = SkiaRegionOps, outlineText = { TextOutliner.outline(it) })

    private class Key(val node: Node) {
        override fun equals(other: Any?) = other is Key && other.node === node
        override fun hashCode() = System.identityHashCode(node)
    }

    private val cache = object : LinkedHashMap<Key, Node>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Node>?) = size > 512
    }

    /** Efektli düğümün görünümü (efektsiz düğümler). */
    @Synchronized
    fun expanded(node: Node): Node = cache.getOrPut(Key(node)) { LiveEffects.expand(node, context) }
}
