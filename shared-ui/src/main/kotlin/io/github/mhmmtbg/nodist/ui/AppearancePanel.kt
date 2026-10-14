package io.github.mhmmtbg.nodist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mhmmtbg.nodist.editor.EditorViewModel
import io.github.mhmmtbg.nodist.model.BlendMode
import io.github.mhmmtbg.nodist.model.Effect
import io.github.mhmmtbg.nodist.model.GroupNode
import io.github.mhmmtbg.nodist.model.ImageNode
import io.github.mhmmtbg.nodist.model.Node
import io.github.mhmmtbg.nodist.model.PathNode
import io.github.mhmmtbg.nodist.model.Rgba
import io.github.mhmmtbg.nodist.model.StrokeAlign
import io.github.mhmmtbg.nodist.model.TextNode
import io.github.mhmmtbg.nodist.model.findNode
import io.github.mhmmtbg.nodist.model.tr

internal val BlendLabels = listOf(
    BlendMode.Normal to "Normal", BlendMode.Multiply to "Çarpma", BlendMode.Screen to "Ekran", BlendMode.Overlay to "Bindirme",
    BlendMode.Darken to "Koyulaştır", BlendMode.Lighten to "Açıklaştır", BlendMode.ColorDodge to "Renk soldur", BlendMode.ColorBurn to "Renk yak",
    BlendMode.HardLight to "Sert ışık", BlendMode.SoftLight to "Yumuşak ışık", BlendMode.Difference to "Fark", BlendMode.Exclusion to "Dışlama",
    BlendMode.Hue to "Ton", BlendMode.Saturation to "Doygunluk", BlendMode.Color to "Renk tonu", BlendMode.Luminosity to "Aydınlık",
)

/** Efekt türleri: panelde eklenebilecekler, sırayla. Anahtar efektin sınıfının adıdır. */
private enum class EffectKind(val label: String, val hint: String) {
    RoundCorners("Köşe yuvarlama", "Düz köşeleri yuvarlar"),
    OffsetPath("Yolu ötele", "Kapalı yolu büyütür ya da küçültür"),
    Taper("Değişken genişlik", "Açık yolun kalınlığı baştan sona değişir"),
    Arrowheads("Ok uçları", "Konturlu açık yolun uçlarına"),
    AlignStroke("Kontur hizası", "Konturu içeri ya da dışarı alır"),
    OuterStroke("Dış kontur", "Arkaya ikinci bir kontur"),
    Shadow("Gölge", "Yumuşak gölge"),
    Mesh("Ağ gradyan", "Dört köşe renginden boyama"),
    Pattern("Desen", "En üstteki kapalı yol, diğer seçilenlerle döşenir"),
    ArtBrush("Fırça", "Kapalı şekil, seçili açık yolun boyunca esner"),
    TextPath("Yol üzerine yazı", "Seçili metin, seçili yolun boyunca dizilir"),
    OpacityMask("Opaklık maskesi", "Üstteki doğrusal gradyanlı şekil saydamlığı belirler"),
    ;

    val icon: ImageVector
        get() = when (this) {
            RoundCorners -> AppIcons.RoundCorner
            OffsetPath -> AppIcons.Offset
            Taper -> AppIcons.Taper
            Arrowheads -> AppIcons.Arrow
            AlignStroke -> AppIcons.StrokeInside
            OuterStroke -> AppIcons.OuterStroke
            Shadow -> AppIcons.Shadow
            Mesh -> AppIcons.Mesh
            Pattern -> AppIcons.Pattern
            ArtBrush -> AppIcons.Brush
            TextPath -> AppIcons.TextPath
            OpacityMask -> AppIcons.OpacityMask
        }

    companion object {
        fun of(e: Effect): EffectKind = when (e) {
            is Effect.RoundCorners -> RoundCorners
            is Effect.OffsetPath -> OffsetPath
            is Effect.Taper -> Taper
            is Effect.Arrowheads -> Arrowheads
            is Effect.AlignStroke -> AlignStroke
            is Effect.OuterStroke -> OuterStroke
            is Effect.Shadow -> Shadow
            is Effect.Mesh -> Mesh
            is Effect.Pattern -> Pattern
            is Effect.ArtBrush -> ArtBrush
            is Effect.TextPath -> TextPath
            is Effect.OpacityMask -> OpacityMask
        }
    }
}

private fun n(v: Double): String {
    val r = Math.round(v * 100) / 100.0
    return if (r == Math.rint(r)) r.toLong().toString() else r.toString()
}

/** Efektin kısa özeti (panelde adının altında). */
private fun summary(e: Effect): String = when (e) {
    is Effect.RoundCorners -> tr("Yarıçap: %s", n(e.radius))
    is Effect.OffsetPath -> tr("Mesafe: %s", n(e.distance))
    is Effect.Taper -> "${n(e.startWidth)} → ${n(e.endWidth)}"
    is Effect.Arrowheads -> (if (e.atStart && e.atEnd) tr("İki uca") else if (e.atStart) tr("Başa") else tr("Sona")) + " · ×" + n(e.scale)
    is Effect.AlignStroke -> if (e.align == StrokeAlign.Inside) tr("İçeri") else tr("Dışarı")
    is Effect.OuterStroke -> tr("Kalınlık: %s", n(e.width))
    is Effect.Shadow -> "${n(e.dx)}, ${n(e.dy)} · " + tr("bulanıklık %s", n(e.blur)) + " · %" + n(e.opacity * 100)
    is Effect.Mesh -> "${e.rows} × ${e.cols}"
    is Effect.Pattern -> tr("Boşluk: %s", "${n(e.gapX)} × ${n(e.gapY)}")
    is Effect.ArtBrush -> tr("Kalınlık: %s", "×" + n(e.thickness))
    is Effect.TextPath -> tr("Başlangıç: %s", n(e.offset))
    is Effect.OpacityMask -> tr("%s şerit", e.bands)
}

/** Efekt [kind] bu nesneye eklenebilir mi (iki nesne isteyenler seçimin bütününe bakar). */
private fun applicable(kind: EffectKind, nodes: List<Node>): Boolean {
    val single = nodes.singleOrNull()
    val paths = nodes.filterIsInstance<PathNode>()
    return when (kind) {
        EffectKind.RoundCorners, EffectKind.OffsetPath -> paths.isNotEmpty() || nodes.any { it is GroupNode }
        EffectKind.Taper, EffectKind.Arrowheads -> paths.any { p -> p.subpaths.any { !it.closed } }
        EffectKind.AlignStroke -> paths.any { p -> p.stroke != null && p.subpaths.any { it.closed } }
        EffectKind.OuterStroke, EffectKind.Mesh -> paths.isNotEmpty()
        EffectKind.Shadow -> nodes.any { it !is ImageNode }
        EffectKind.Pattern -> nodes.size >= 2
        EffectKind.ArtBrush -> nodes.size == 2 && paths.size == 2
        EffectKind.TextPath -> nodes.size == 2 && nodes.any { it is TextNode } && paths.size == 1
        EffectKind.OpacityMask -> nodes.size == 2
    } && (single !is ImageNode || kind == EffectKind.Shadow || kind == EffectKind.OpacityMask)
}

/**
 * Görünüm paneli: seçili nesnenin karışım modu, canlı efektlerinin listesi (dokununca düzenlenir, çöp kutusuyla silinir),
 * yeni efekt ekleme ve görünümü genişletme (efektleri kalıcı vektöre çevirme).
 */
@Composable
fun AppearanceDialog(vm: EditorViewModel, onDismiss: () -> Unit) {
    // Düzenlenen efekt: (sıra, efekt) — sıra -1 ise yeni ekleniyor demektir.
    var editing by remember { mutableStateOf<Pair<Int, Any>?>(null) }
    var blendPicker by remember { mutableStateOf(false) }
    val state = vm.state
    val nodes = state.selection.mapNotNull { state.document.findNode(it) }
    val single = nodes.singleOrNull()

    if (blendPicker) {
        val current = single?.blendMode ?: BlendMode.Normal
        PickDialog(
            tr("Karışım modu"),
            BlendLabels.map { AppIcons.Blend to tr(it.second) },
            onDismiss = { blendPicker = false },
            selected = BlendLabels.indexOfFirst { it.first == current },
        ) { i ->
            vm.setBlendMode(BlendLabels[i].first)
            blendPicker = false
        }
        return
    }
    editing?.let { (index, item) ->
        val nodeId = single?.id
        EffectEditor(
            item = item,
            onDismiss = { editing = null },
        ) { result ->
            editing = null
            if (index >= 0 && nodeId != null && result is Effect) vm.updateEffect(nodeId, index, result) else addNew(vm, result)
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Görünüm")) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                if (single != null) {
                    val label = BlendLabels.firstOrNull { it.first == single.blendMode }?.second ?: "Normal"
                    MenuRow(AppIcons.Blend, tr("Karışım modu"), detail = tr(label)) { blendPicker = true }
                    SectionTitle(tr("Efektler"))
                    if (single.effects.isEmpty()) {
                        Text(tr("Bu nesnede canlı efekt yok"), Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium, color = AppColors.OnPanelMuted)
                    }
                    single.effects.forEachIndexed { i, e ->
                        val kind = EffectKind.of(e)
                        MenuRow(
                            kind.icon, tr(kind.label), detail = summary(e),
                            trailing = {
                                IconButton(onClick = { vm.removeEffect(single.id, i) }, modifier = Modifier.semantics { contentDescription = tr("Efekti kaldır") }) {
                                    Icon(AppIcons.Delete, contentDescription = null, tint = AppColors.OnPanelMuted)
                                }
                            },
                        ) { editing = i to e }
                    }
                } else if (nodes.size > 1) {
                    MenuRow(AppIcons.Blend, tr("Karışım modu"), detail = tr("%s nesne", nodes.size)) { blendPicker = true }
                }
                SectionTitle(tr("Efekt ekle"))
                for (kind in EffectKind.entries) {
                    val ok = applicable(kind, nodes)
                    MenuRow(kind.icon, tr(kind.label), detail = tr(kind.hint), enabled = ok) { editing = -1 to kind }
                }
                if (nodes.any { io.github.mhmmtbg.nodist.model.LiveEffects.hasEffects(it) }) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = AppColors.Divider)
                    MenuRow(AppIcons.Bake, tr("Görünümü genişlet"), detail = tr("Efektleri kalıcı vektöre çevirir; artık düzenlenemez")) {
                        vm.expandAppearance()
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Kapat")) } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = AppColors.Accent)
}

/** Yeni efekt: penceredeki değerlerle ilgili komut çağrılır (iki nesne isteyenler seçimden kurulur). */
private fun addNew(vm: EditorViewModel, result: Any) {
    when (result) {
        is Effect.RoundCorners -> vm.roundCornersCmd(result.radius)
        is Effect.OffsetPath -> vm.offsetEffectCmd(result.distance)
        is Effect.Taper -> vm.taperStrokeCmd(result.startWidth, result.endWidth)
        is Effect.Arrowheads -> vm.arrowheadsCmd(result.atStart, result.atEnd, result.scale)
        is Effect.AlignStroke -> vm.alignStrokeCmd(result.align)
        is Effect.OuterStroke -> vm.addOuterStrokeCmd(result.width)
        is Effect.Shadow -> vm.addShadowCmd(result.dx, result.dy, result.blur, result.opacity * 100)
        is Effect.Mesh -> vm.meshGradientCmd(result.rows, result.cols)
        is Effect.Pattern -> vm.tilePatternCmd(result.gapX, result.gapY)
        is Effect.ArtBrush -> vm.artBrushCmd(result.thickness)
        is Effect.TextPath -> vm.textOnPathCmd()
        is Effect.OpacityMask -> vm.opacityMaskCmd()
    }
}

private val DecimalKeys = KeyboardOptions(keyboardType = KeyboardType.Decimal)

private fun parse(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

/**
 * Efekt parametre penceresi. [item] düzenlenen efekt ya da (yeni eklemede) [EffectKind]. Sonuç bir [Effect]tir;
 * yeni eklemede iki nesne isteyen efektlerin düğüm alanları boştur, komut onları seçimden kurar.
 */
@Composable
private fun EffectEditor(item: Any, onDismiss: () -> Unit, onDone: (Effect) -> Unit) {
    val existing = item as? Effect
    val kind = existing?.let { EffectKind.of(it) } ?: item as EffectKind
    // Sayısal alanlar: (etiket, başlangıç değeri)
    val fields: List<Pair<String, Double>> = when (kind) {
        EffectKind.RoundCorners -> listOf(tr("Yarıçap") to ((existing as? Effect.RoundCorners)?.radius ?: 10.0))
        EffectKind.OffsetPath -> listOf(tr("Mesafe (negatif küçültür)") to ((existing as? Effect.OffsetPath)?.distance ?: 10.0))
        EffectKind.Taper -> (existing as? Effect.Taper).let { listOf(tr("Baştaki kalınlık") to (it?.startWidth ?: 12.0), tr("Sondaki kalınlık") to (it?.endWidth ?: 0.0)) }
        EffectKind.Arrowheads -> listOf(tr("Boy (kontur kalınlığının katı)") to ((existing as? Effect.Arrowheads)?.scale ?: 4.0))
        EffectKind.AlignStroke -> emptyList()
        EffectKind.OuterStroke -> listOf(tr("Kalınlık") to ((existing as? Effect.OuterStroke)?.width ?: 4.0))
        EffectKind.Shadow -> (existing as? Effect.Shadow).let {
            listOf(
                tr("Yatay kayma") to (it?.dx ?: 6.0), tr("Dikey kayma") to (it?.dy ?: 6.0),
                tr("Bulanıklık") to (it?.blur ?: 8.0), tr("Saydamlık (%)") to ((it?.opacity ?: 0.5) * 100),
            )
        }
        EffectKind.Mesh -> (existing as? Effect.Mesh).let { listOf(tr("Satır sayısı") to (it?.rows ?: 12).toDouble(), tr("Sütun sayısı") to (it?.cols ?: 12).toDouble()) }
        EffectKind.Pattern -> (existing as? Effect.Pattern).let { listOf(tr("Yatay boşluk") to (it?.gapX ?: 0.0), tr("Dikey boşluk") to (it?.gapY ?: 0.0)) }
        EffectKind.ArtBrush -> listOf(tr("Kalınlık çarpanı") to ((existing as? Effect.ArtBrush)?.thickness ?: 1.0))
        EffectKind.TextPath -> listOf(tr("Başlangıç uzaklığı") to ((existing as? Effect.TextPath)?.offset ?: 0.0))
        EffectKind.OpacityMask -> listOf(tr("Şerit sayısı") to ((existing as? Effect.OpacityMask)?.bands ?: 24).toDouble())
    }
    val values = remember { mutableStateListOf(*fields.map { n(it.second) }.toTypedArray()) }
    val parsed = values.map(::parse)
    // Ok uçları ve kontur hizası için seçimler; gölge ve dış kontur için renk.
    var atStart by remember { mutableStateOf((existing as? Effect.Arrowheads)?.atStart ?: false) }
    var atEnd by remember { mutableStateOf((existing as? Effect.Arrowheads)?.atEnd ?: true) }
    var inside by remember { mutableStateOf((existing as? Effect.AlignStroke)?.align != StrokeAlign.Outside) }
    var color by remember {
        mutableStateOf(
            when (existing) {
                is Effect.Shadow -> existing.color
                is Effect.OuterStroke -> existing.color
                else -> null
            },
        )
    }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        ColorPickerDialog(color ?: Rgba.Black, onDismiss = { picking = false }) { color = it; picking = false }
        return
    }

    fun build(): Effect? {
        if (parsed.any { it == null }) return null
        val v = parsed.map { it!! }
        return when (kind) {
            EffectKind.RoundCorners -> Effect.RoundCorners(v[0].coerceAtLeast(0.0))
            EffectKind.OffsetPath -> Effect.OffsetPath(v[0])
            EffectKind.Taper -> Effect.Taper(v[0].coerceAtLeast(0.0), v[1].coerceAtLeast(0.0))
            EffectKind.Arrowheads -> if (!atStart && !atEnd) null else Effect.Arrowheads(atStart, atEnd, v[0].coerceIn(0.5, 40.0))
            EffectKind.AlignStroke -> Effect.AlignStroke(if (inside) StrokeAlign.Inside else StrokeAlign.Outside)
            EffectKind.OuterStroke -> if (v[0] <= 0.0) null else Effect.OuterStroke(v[0], color ?: Rgba.Black)
            EffectKind.Shadow -> Effect.Shadow(v[0], v[1], v[2].coerceAtLeast(0.0), (v[3] / 100).coerceIn(0.0, 1.0), color ?: Rgba.Black)
            EffectKind.Mesh -> (existing as? Effect.Mesh)?.copy(rows = v[0].toInt().coerceIn(2, 40), cols = v[1].toInt().coerceIn(2, 40))
                ?: Effect.Mesh(v[0].toInt().coerceIn(2, 40), v[1].toInt().coerceIn(2, 40), Rgba.Black, Rgba.Black, Rgba.Black, Rgba.Black)
            EffectKind.Pattern -> (existing as? Effect.Pattern)?.copy(gapX = v[0], gapY = v[1])
                ?: Effect.Pattern(io.github.mhmmtbg.nodist.model.GroupNode(children = emptyList()), v[0], v[1])
            EffectKind.ArtBrush -> (existing as? Effect.ArtBrush)?.copy(thickness = v[0].coerceAtLeast(0.05))
                ?: Effect.ArtBrush(PathNode(subpaths = emptyList()), v[0].coerceAtLeast(0.05))
            EffectKind.TextPath -> (existing as? Effect.TextPath)?.copy(offset = v[0]) ?: Effect.TextPath(emptyList(), v[0])
            EffectKind.OpacityMask -> (existing as? Effect.OpacityMask)?.copy(bands = v[0].toInt().coerceIn(2, 64))
                ?: Effect.OpacityMask(io.github.mhmmtbg.nodist.model.Paint.LinearGradient(io.github.mhmmtbg.nodist.model.Vec2(0.0, 0.0), io.github.mhmmtbg.nodist.model.Vec2(1.0, 0.0), emptyList()), v[0].toInt().coerceIn(2, 64))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(kind.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr(kind.label))
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing == null) Text(tr(kind.hint), style = MaterialTheme.typography.bodyMedium, color = AppColors.OnPanelMuted)
                for ((i, f) in fields.withIndex()) {
                    OutlinedTextField(values[i], { values[i] = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(f.first) }, keyboardOptions = DecimalKeys)
                }
                when (kind) {
                    EffectKind.Arrowheads -> {
                        CheckRow(tr("Başta"), atStart) { atStart = it }
                        CheckRow(tr("Sonda"), atEnd) { atEnd = it }
                    }
                    EffectKind.AlignStroke -> ChoiceRow(
                        tr("Kontur"), listOf(tr("İçeri") to inside, tr("Dışarı") to !inside),
                        icons = listOf(AppIcons.StrokeInside, AppIcons.StrokeOutside),
                    ) { inside = it == 0 }
                    EffectKind.Shadow, EffectKind.OuterStroke -> {
                        if (existing != null) {
                            Row(
                                Modifier.fillMaxWidth().clickable(role = Role.Button) { picking = true }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ColorDot(color ?: Rgba.Black, Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(tr("Renk…"))
                            }
                        } else if (kind == EffectKind.OuterStroke) {
                            Text(tr("Renk: seçili kontur rengi (sonradan değiştirilebilir)"), style = MaterialTheme.typography.labelMedium, color = AppColors.OnPanelMuted)
                        }
                    }
                    EffectKind.Mesh -> Text(tr("Köşe renkleri: belgedeki ilk dört renk örneği; yoksa dolgu ve kontur rengi."), style = MaterialTheme.typography.labelMedium, color = AppColors.OnPanelMuted)
                    else -> {}
                }
            }
        },
        confirmButton = { TextButton(onClick = { build()?.let(onDone) }, enabled = build() != null) { Text(if (existing == null) tr("Ekle") else tr("Uygula")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, null)
        Text(label)
    }
}
