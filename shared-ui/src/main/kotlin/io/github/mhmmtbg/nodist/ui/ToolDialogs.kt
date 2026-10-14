@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.mhmmtbg.nodist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.mhmmtbg.nodist.editor.EditorViewModel
import io.github.mhmmtbg.nodist.model.Artboard
import io.github.mhmmtbg.nodist.model.Rect
import io.github.mhmmtbg.nodist.model.RefPoint
import io.github.mhmmtbg.nodist.model.TransformKind
import io.github.mhmmtbg.nodist.model.TransformSpec
import io.github.mhmmtbg.nodist.model.trName
import io.github.mhmmtbg.nodist.model.tr

private fun fmtNum(v: Double): String =
    ((v * 100).toLong() / 100.0).let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }

private fun parseNum(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

private val DecimalKeys = KeyboardOptions(keyboardType = KeyboardType.Decimal)

/** Tek sayı soran pencere (boşluk değeri gibi). */
@Composable
fun NumberDialog(title: String, label: String, initial: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var value by remember { mutableStateOf(fmtNum(initial)) }
    val parsed = parseNum(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, singleLine = true, label = { Text(label) }, keyboardOptions = DecimalKeys) },
        confirmButton = { TextButton(onClick = { onConfirm(parsed!!) }, enabled = parsed != null) { Text(tr("Tamam")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

private val RefLabels = listOf(
    RefPoint.TopLeft, RefPoint.Top, RefPoint.TopRight,
    RefPoint.Left, RefPoint.Center, RefPoint.Right,
    RefPoint.BottomLeft, RefPoint.Bottom, RefPoint.BottomRight,
)

/** Döndür, ölçekle, yansıt ve eğ araçları: değerler, sabit nokta ve "kopya" seçeneği. */
@Composable
fun TransformToolsDialog(
    initial: TransformSpec?,
    initialRef: RefPoint,
    onDismiss: () -> Unit,
    onApply: (TransformSpec, RefPoint, Boolean) -> Unit,
) {
    var kind by remember { mutableStateOf(initial?.kind ?: TransformKind.Rotate) }
    var angle by remember { mutableStateOf(fmtNum(initial?.angle ?: 90.0)) }
    var sx by remember { mutableStateOf(fmtNum((initial?.sx ?: 1.0) * 100)) }
    var sy by remember { mutableStateOf(fmtNum((initial?.sy ?: 1.0) * 100)) }
    var uniform by remember { mutableStateOf(true) }
    var axis by remember { mutableStateOf(fmtNum(initial?.axis ?: 90.0)) }
    var ref by remember { mutableStateOf(initialRef) }
    var copy by remember { mutableStateOf(false) }

    fun spec(): TransformSpec? = when (kind) {
        TransformKind.Rotate -> parseNum(angle)?.let { TransformSpec(kind, angle = it) }
        TransformKind.Scale -> {
            val x = parseNum(sx)
            val y = if (uniform) x else parseNum(sy)
            if (x != null && y != null && x > 0 && y > 0) TransformSpec(kind, sx = x / 100, sy = y / 100) else null
        }
        TransformKind.Reflect -> parseNum(axis)?.let { TransformSpec(kind, axis = it) }
        TransformKind.Shear -> {
            val a = parseNum(angle)
            val ax = parseNum(axis)
            if (a != null && ax != null && a > -89 && a < 89) TransformSpec(kind, angle = a, axis = ax) else null
        }
    }

    val kinds = listOf(
        TransformKind.Rotate to tr("Döndür"),
        TransformKind.Scale to tr("Ölçekle"),
        TransformKind.Reflect to tr("Yansıt"),
        TransformKind.Shear to tr("Yatır"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Dönüşüm araçları")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceRow(tr("Araç"), kinds.map { it.second to (it.first == kind) }, icons = listOf(AppIcons.Rotate, AppIcons.Scale, AppIcons.Reflect, AppIcons.Shear)) { kind = kinds[it].first }
                when (kind) {
                    TransformKind.Rotate ->
                        OutlinedTextField(angle, { angle = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Açı (derece, saat yönünün tersine)")) }, keyboardOptions = DecimalKeys)
                    TransformKind.Scale -> {
                        OutlinedTextField(
                            sx, { sx = it; if (uniform) sy = it }, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(if (uniform) tr("Ölçek (%)") else tr("Yatay ölçek (%)")) }, keyboardOptions = DecimalKeys,
                        )
                        if (!uniform) {
                            OutlinedTextField(sy, { sy = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Dikey ölçek (%)")) }, keyboardOptions = DecimalKeys)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(role = Role.Checkbox) { uniform = !uniform; if (uniform) sy = sx }) {
                            Checkbox(uniform, null)
                            Text(tr("Orantılı"))
                        }
                    }
                    TransformKind.Reflect -> {
                        OutlinedTextField(axis, { axis = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Eksen açısı (derece)")) }, keyboardOptions = DecimalKeys)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconTextButton(AppIcons.Reflect, tr("Dikey eksen")) { axis = "90" }
                            IconTextButton(AppIcons.Flat, tr("Yatay eksen")) { axis = "0" }
                        }
                    }
                    TransformKind.Shear -> {
                        OutlinedTextField(angle, { angle = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Eğim açısı (derece)")) }, keyboardOptions = DecimalKeys)
                        OutlinedTextField(axis, { axis = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Eksen açısı (derece)")) }, keyboardOptions = DecimalKeys)
                    }
                }
                Text(tr("Sabit nokta"), style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in RefLabels.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (p in row) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (p == ref) AppColors.Accent else AppColors.PanelRaised)
                                        .clickable(role = Role.RadioButton) { ref = p }
                                        .semantics { contentDescription = tr("Sabit nokta") },
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(role = Role.Checkbox) { copy = !copy }) {
                    Checkbox(copy, null)
                    Text(tr("Kopya oluştur"))
                }
            }
        },
        confirmButton = { TextButton(onClick = { spec()?.let { onApply(it, ref, copy) } }, enabled = spec() != null) { Text(tr("Uygula")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

/** Çalışma yüzeylerinin konumu ve boyutu. */
@Composable
fun ArtboardBoundsDialog(artboard: Artboard, onDismiss: () -> Unit, onApply: (Rect) -> Unit) {
    val b = artboard.bounds
    var x by remember { mutableStateOf(fmtNum(b.left)) }
    var y by remember { mutableStateOf(fmtNum(b.top)) }
    var w by remember { mutableStateOf(fmtNum(b.width)) }
    var h by remember { mutableStateOf(fmtNum(b.height)) }
    val nums = listOf(x, y, w, h).map(::parseNum)
    val valid = nums.all { it != null } && nums[2]!! > 0 && nums[3]!! > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Çalışma yüzeyi boyutu")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(x, { x = it }, Modifier.weight(1f), singleLine = true, label = { Text("X") }, keyboardOptions = DecimalKeys)
                    OutlinedTextField(y, { y = it }, Modifier.weight(1f), singleLine = true, label = { Text("Y") }, keyboardOptions = DecimalKeys)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(w, { w = it }, Modifier.weight(1f), singleLine = true, label = { Text(tr("Genişlik")) }, keyboardOptions = DecimalKeys)
                    OutlinedTextField(h, { h = it }, Modifier.weight(1f), singleLine = true, label = { Text(tr("Yükseklik")) }, keyboardOptions = DecimalKeys)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(Rect(nums[0]!!, nums[1]!!, nums[0]!! + nums[2]!!, nums[1]!! + nums[3]!!)) },
                enabled = valid,
            ) { Text(tr("Uygula")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

/**
 * Çalışma yüzeyleri: listele, yakınlaş, ekle, çoğalt (içindekilerle), yeniden adlandır, boyutlandır, sil ve dışa aktar.
 * [onExport] seçilen yüzey için biçim ve dosya seçimini ana ekrana bırakır.
 */
@Composable
fun ArtboardsDialog(vm: EditorViewModel, onDismiss: () -> Unit, onExport: (String) -> Unit) {
    val boards = vm.state.document.artboards
    var renaming by remember { mutableStateOf<String?>(null) }
    var resizing by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Çalışma yüzeyleri")) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                boards.forEachIndexed { i, ab ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                    ) {
                        Text(
                            "${i + 1}. ${trName(ab.name)}",
                            Modifier.clickable { vm.zoomToArtboard(ab.id); onDismiss() },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "${fmtNum(ab.bounds.width)} × ${fmtNum(ab.bounds.height)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.OnPanelMuted,
                        )
                        FlowRow {
                            IconTextButton(AppIcons.Fit, tr("Göster")) { vm.zoomToArtboard(ab.id); onDismiss() }
                            IconTextButton(AppIcons.Rename, tr("Adlandır")) { renaming = ab.id }
                            IconTextButton(AppIcons.Transform, tr("Boyut…")) { resizing = ab.id }
                            IconTextButton(AppIcons.Duplicate, tr("Çoğalt")) { vm.duplicateArtboardCmd(ab.id) }
                            IconTextButton(AppIcons.Artboards, tr("Çizime sığdır")) { vm.fitArtboardToArtworkCmd(ab.id) }
                            IconTextButton(AppIcons.Export, tr("Dışa aktar…")) { onExport(ab.id) }
                            IconTextButton(AppIcons.Delete, tr("Sil"), enabled = boards.size > 1) { vm.removeArtboardCmd(ab.id) }
                        }
                    }
                }
                HorizontalDivider(color = AppColors.Divider)
                IconTextButton(AppIcons.Add, tr("Yeni çalışma yüzeyi"), onClick = vm::addArtboardCmd)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Tamam")) } },
    )
    renaming?.let { id ->
        val ab = boards.firstOrNull { it.id == id }
        if (ab == null) renaming = null else NameDialog(tr("Yeniden adlandır"), ab.name, onDismiss = { renaming = null }) { renaming = null; vm.renameArtboardCmd(id, it) }
    }
    resizing?.let { id ->
        val ab = boards.firstOrNull { it.id == id }
        if (ab == null) resizing = null else ArtboardBoundsDialog(ab, onDismiss = { resizing = null }) { resizing = null; vm.setArtboardBoundsCmd(id, it) }
    }
}

/** "Aynısını seç" ölçütlerini listeler. */
@Composable
fun SelectSameDialog(onDismiss: () -> Unit, onPick: (io.github.mhmmtbg.nodist.model.SameKind) -> Unit) {
    val items = listOf(
        Triple(io.github.mhmmtbg.nodist.model.SameKind.Fill, AppIcons.FillDot, tr("Aynı dolgu")),
        Triple(io.github.mhmmtbg.nodist.model.SameKind.Stroke, AppIcons.StrokeRing, tr("Aynı kontur rengi")),
        Triple(io.github.mhmmtbg.nodist.model.SameKind.FillAndStroke, AppIcons.FillStroke, tr("Aynı dolgu ve kontur")),
        Triple(io.github.mhmmtbg.nodist.model.SameKind.StrokeWidth, AppIcons.StrokeWidth, tr("Aynı kontur kalınlığı")),
        Triple(io.github.mhmmtbg.nodist.model.SameKind.Opacity, AppIcons.Opacity, tr("Aynı saydamlık")),
        Triple(io.github.mhmmtbg.nodist.model.SameKind.Type, AppIcons.Shapes, tr("Aynı tür")),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Aynısını seç")) },
        text = {
            Column {
                for ((kind, icon, label) in items) MenuRow(icon, label) { onPick(kind) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

/**
 * Renk örnekleri: belgeye kayıtlı renkler. Bir örneğin rengini değiştirmek, belgede o rengi kullanan her yeri de değiştirir.
 */
@Composable
fun SwatchesDialog(vm: EditorViewModel, onDismiss: () -> Unit) {
    val swatches = vm.state.document.swatches
    var selected by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    val sel = selected?.takeIf { it in swatches.indices }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Renk örnekleri")) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (swatches.isEmpty()) {
                    Text(tr("Henüz renk örneği yok. Seçili rengi ya da belgedeki renkleri ekleyin."), style = MaterialTheme.typography.bodyMedium)
                }
                for (row in swatches.withIndex().toList().chunked(6)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((i, sw) in row) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, if (i == sel) AppColors.Accent else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) { selected = i }
                                    .padding(4.dp)
                                    .semantics { contentDescription = sw.name },
                                contentAlignment = Alignment.Center,
                            ) { ColorDot(sw.color, Modifier.size(34.dp)) }
                        }
                    }
                }
                if (sel != null) {
                    Text(swatches[sel].name, style = MaterialTheme.typography.titleSmall)
                    FlowRow {
                        IconTextButton(AppIcons.FillDot, tr("Uygula")) { vm.applySwatch(sel) }
                        IconTextButton(AppIcons.Rename, tr("Adlandır")) { renaming = sel }
                        IconTextButton(AppIcons.Palette, tr("Rengi değiştir")) { editing = sel }
                        IconTextButton(AppIcons.Delete, tr("Sil")) { vm.removeSwatchCmd(sel); selected = null }
                    }
                    Text(
                        tr("Rengi değiştirmek, belgede o rengi kullanan her yeri de değiştirir."),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.OnPanelMuted,
                    )
                }
                HorizontalDivider(color = AppColors.Divider)
                IconTextButton(AppIcons.Add, tr("Seçili rengi ekle"), onClick = vm::addSwatchFromCurrent)
                IconTextButton(AppIcons.Palette, tr("Belgedeki renkleri ekle"), onClick = vm::addDesignColorsAsSwatches)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Tamam")) } },
    )
    renaming?.let { i ->
        val sw = swatches.getOrNull(i)
        if (sw == null) renaming = null else NameDialog(tr("Yeniden adlandır"), sw.name, onDismiss = { renaming = null }) { renaming = null; vm.renameSwatchCmd(i, it) }
    }
    editing?.let { i ->
        val sw = swatches.getOrNull(i)
        if (sw == null) editing = null else ColorPickerDialog(sw.color, onDismiss = { editing = null }) { editing = null; vm.editSwatchColor(i, it) }
    }
}

/** Birkaç sayı isteyen pencere; [labels] ve [initial] aynı uzunluktadır. */
@Composable
fun ParamDialog(title: String, labels: List<String>, initial: List<Double>, onDismiss: () -> Unit, onConfirm: (List<Double>) -> Unit) {
    val values = remember { mutableStateListOf(*initial.map { fmtNum(it) }.toTypedArray()) }
    val parsed = values.map { parseNum(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((i, l) in labels.withIndex()) {
                    OutlinedTextField(values[i], { values[i] = it }, singleLine = true, label = { Text(l) }, keyboardOptions = DecimalKeys)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(parsed.map { it!! }) }, enabled = parsed.all { it != null }) { Text(tr("Tamam")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

/** Seçenek listesi; birine dokunmak [onPick] ile sırasını bildirir. */
@Composable
fun PickDialog(title: String, items: List<Pair<ImageVector, String>>, onDismiss: () -> Unit, selected: Int = -1, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                for ((i, item) in items.withIndex()) MenuRow(item.first, item.second, selected = i == selected) { onPick(i) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}

/** Simgeli liste satırı (pencerelerdeki seçenek listeleri için). */
@Composable
internal fun MenuRow(
    icon: ImageVector,
    label: String,
    detail: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val fg = when {
        !enabled -> AppColors.OnPanelMuted
        selected -> AppColors.Accent
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = fg, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) Text(detail, color = AppColors.OnPanelMuted, style = MaterialTheme.typography.labelMedium)
        }
        trailing?.invoke()
    }
}

/** Simgeli metin düğmesi. */
@Composable
internal fun IconTextButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}
