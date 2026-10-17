@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package io.github.mhmmtbg.nodist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.mhmmtbg.nodist.editor.AlignTarget
import io.github.mhmmtbg.nodist.editor.EditorViewModel
import io.github.mhmmtbg.nodist.editor.Tool
import io.github.mhmmtbg.nodist.model.Align
import io.github.mhmmtbg.nodist.model.GroupNode
import io.github.mhmmtbg.nodist.model.ImageNode
import io.github.mhmmtbg.nodist.model.PathNode
import io.github.mhmmtbg.nodist.model.TextNode
import io.github.mhmmtbg.nodist.model.ZMove
import io.github.mhmmtbg.nodist.model.anchorAt
import io.github.mhmmtbg.nodist.model.findNode
import io.github.mhmmtbg.nodist.model.hasStraightCorners
import io.github.mhmmtbg.nodist.model.tr
import io.github.mhmmtbg.nodist.render.BooleanOp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Seçim eylemi: [key] kaynak dildeki adıdır (çeviri, simge ve erişilebilirlik adı bundan gelir). */
private class Act(val key: String, val section: String, val active: Boolean = false, val onClick: () -> Unit)

/** Eksi/artı düğmeli ayar (köşe yarıçapı, kenar sayısı…); etiketler "Köşe −" ve "Köşe +" gibidir. */
private class Stepper(val base: String, val icon: ImageVector, val minus: () -> Unit, val plus: () -> Unit)

/** Hızlı çubukta öne çıkan eylemler, öncelik sırasıyla; sığmayanlar yalnızca palette görünür. */
private val QuickOrder = listOf(
    "Metni düzenle", "Vektöre çevir", "Maskeyi bırak", "Grupla", "Grubu çöz", "Çoğalt", "Görünüm…", "Dönüştür",
    "Öne", "Arkaya", "Birleştir", "Kopyala", "Yapıştır", "Dönüşüm araçları…", "Eğme yok",
)

/**
 * Seçim eylemleri: kaydırılmayan kısa bir simge çubuğu ve dokununca açılan, bütün eylemleri bölümler halinde gösteren
 * bir palet. Simgelerin adı fareyle üzerine gelince ya da uzun basınca görünür; palette her simgenin altında yazar.
 */
@Composable
internal fun ContextBar(
    vm: EditorViewModel,
    modifier: Modifier,
    onRename: (String) -> Unit,
    onTransform: () -> Unit,
    onExport: () -> Unit,
    onTransformTools: () -> Unit,
    onSpacing: (Boolean) -> Unit,
    onSelectSame: () -> Unit,
    onAppearance: () -> Unit,
) {
    val state = vm.state
    var paletteOpen by remember { mutableStateOf(false) }
    var offsetDialog by remember { mutableStateOf(false) }
    if (offsetDialog) {
        NumberDialog(tr("Yolu ötele"), tr("Mesafe (negatif küçültür)"), 10.0, onDismiss = { offsetDialog = false }) { v ->
            offsetDialog = false
            vm.offsetPathCmd(v)
        }
    }
    val doc = state.history.present
    val edit = state.nodeEdit
    val acts = ArrayList<Act>()
    val steppers = ArrayList<Stepper>()
    // Kalem ve düğüm düzenlemede az sayıda eylem olur; bunlar adlarıyla birlikte gösterilir.
    var labeled = false
    var warpNode: TextNode? = null

    when {
        state.pen != null -> {
            labeled = true
            acts += Act("Bitir", "") { vm.finishPen(close = false) }
            if (state.pen.anchors.size >= 3) acts += Act("Kapat ve bitir", "") { vm.finishPen(close = true) }
            acts += Act("İptal", "") { vm.cancelPen() }
        }
        state.tool == Tool.Direct && edit?.anchor != null -> {
            labeled = true
            val anchor = (doc.findNode(edit.nodeId) as? PathNode)?.subpaths?.anchorAt(edit.anchor)
            val curved = anchor != null && (anchor.handleIn != null || anchor.handleOut != null)
            acts += Act(if (curved) "Köşeye çevir" else "Yumuşat", "") { vm.toggleSmoothAnchor() }
            acts += Act("Düğümü sil", "") { vm.deleteSelection() }
        }
        state.selection.isNotEmpty() -> {
            val nodes = state.selection.mapNotNull { doc.findNode(it) }
            val single = nodes.singleOrNull()
            val paths = nodes.filterIsInstance<PathNode>()
            warpNode = (state.selection.singleOrNull()?.let { state.document.findNode(it) }) as? TextNode

            val edit1 = "Düzen"
            if (single is TextNode) {
                acts += Act("Metni düzenle", edit1) { vm.editSelectedText() }
                acts += Act("Yola çevir", edit1) { vm.outlineSelectedText() }
            }
            if (single is ImageNode) acts += Act("Vektöre çevir", edit1) { vm.startTrace() }
            if (single is GroupNode && single.clip != null) acts += Act("Maskeyi bırak", edit1) { vm.releaseClippingMask() }
            acts += Act("Kopyala", edit1) { vm.copySelection() }
            acts += Act("Kes", edit1) { vm.cutSelection() }
            if (vm.canPaste) acts += Act("Yapıştır", edit1) { vm.paste() }
            acts += Act("Çoğalt", edit1) { vm.duplicateSelection() }
            if (nodes.size >= 2) acts += Act("Grupla", edit1) { vm.groupSelection() }
            if (nodes.any { it is GroupNode }) acts += Act("Grubu çöz", edit1) { vm.ungroupSelection() }
            if (single != null) acts += Act("Adlandır", edit1) { onRename(single.id) }
            acts += Act("Katmanda göster", edit1) { vm.revealSelectionInLayers() }
            acts += Act("Dışa aktar…", edit1) { onExport() }

            val order = "Sıra"
            acts += Act("Öne", order) { vm.reorderSelection(ZMove.Forward) }
            acts += Act("Arkaya", order) { vm.reorderSelection(ZMove.Backward) }
            acts += Act("En öne", order) { vm.reorderSelection(ZMove.Front) }
            acts += Act("En arkaya", order) { vm.reorderSelection(ZMove.Back) }

            val tf = "Dönüşüm"
            acts += Act("Dönüştür", tf) { onTransform() }
            acts += Act("Dönüşüm araçları…", tf) { onTransformTools() }
            if (vm.lastTransform != null) acts += Act("Tekrar dönüştür", tf) { vm.repeatTransform() }
            acts += Act("Aynısını seç…", tf) { onSelectSame() }
            if (single != null) acts += Act("Kenarlara kılavuz", tf) { vm.guidesFromSelection() }

            val al = "Hizala"
            acts += Act("Sola", al) { vm.alignSelection(Align.Left) }
            acts += Act("Yatay ortala", al) { vm.alignSelection(Align.CenterH) }
            acts += Act("Sağa", al) { vm.alignSelection(Align.Right) }
            acts += Act("Üste", al) { vm.alignSelection(Align.Top) }
            acts += Act("Dikey ortala", al) { vm.alignSelection(Align.CenterV) }
            acts += Act("Alta", al) { vm.alignSelection(Align.Bottom) }
            if (nodes.size >= 3) {
                acts += Act("Yatay dağıt", al) { vm.distributeSelection(true) }
                acts += Act("Dikey dağıt", al) { vm.distributeSelection(false) }
            }
            if (nodes.size >= 2) {
                acts += Act("Yatay boşluk…", al) { onSpacing(true) }
                acts += Act("Dikey boşluk…", al) { onSpacing(false) }
            }
            acts += Act(
                when (state.alignTarget) {
                    AlignTarget.Selection -> "Hedef: seçim"
                    AlignTarget.Artboard -> "Hedef: çalışma yüzeyi"
                    AlignTarget.Key -> "Hedef: anahtar nesne"
                },
                al,
            ) { vm.cycleAlignTarget() }

            if (nodes.size >= 2) {
                val sh = "Şekil işlemleri"
                acts += Act("Birleştir", sh) { vm.pathfinder(BooleanOp.Unite) }
                acts += Act("Öndekini çıkar", sh) { vm.pathfinder(BooleanOp.MinusFront) }
                acts += Act("Kesiştir", sh) { vm.pathfinder(BooleanOp.Intersect) }
                acts += Act("Dışla", sh) { vm.pathfinder(BooleanOp.Exclude) }
                if (paths.size >= 2 && paths.size == nodes.size) acts += Act("Böl", sh) { vm.divideCmd() }
                acts += Act("Maske yap", sh) { vm.makeClippingMask() }
            }

            if (paths.isNotEmpty()) {
                val ps = "Yol"
                if (paths.size >= 2) acts += Act("Bileşik yap", ps) { vm.makeCompoundPath() }
                if (paths.size == 1 && nodes.size == 1 && paths[0].subpaths.size >= 2) acts += Act("Bileşiği çöz", ps) { vm.releaseCompoundPath() }
                if (paths.any { p -> p.subpaths.any { !it.closed } }) acts += Act("Yolları birleştir", ps) { vm.joinSelection() }
                if (paths.any { it.stroke != null }) acts += Act("Kontur ana hattı", ps) { vm.outlineStrokeCmd() }
                if (paths.any { p -> p.subpaths.any { it.closed } }) acts += Act("Yolu ötele…", ps) { offsetDialog = true }
            }

            if (single is TextNode) {
                val tx = "Metin eğme"
                val style = single.warp?.style
                acts += Act("Eğme yok", tx, active = style == null) { vm.setTextWarp(null, 0.0, done = true) }
                for ((s, label) in WarpLabels) {
                    acts += Act(label, tx, active = style == s) { vm.setTextWarp(s, single.warp?.bend?.takeIf { it != 0.0 } ?: 0.5, done = true) }
                }
            }

            acts += Act("Görünüm…", "Görünüm") { onAppearance() }
        }
    }

    if (state.pen == null) {
        // Seçili düz köşeli yollarda köşe yarıçapı (canlı köşe yuvarlama): çizildikten sonra da ayarlanır.
        val cornerPaths = state.selection.any { (doc.findNode(it) as? PathNode)?.hasStraightCorners() == true }
        if (cornerPaths && state.tool != Tool.Direct) {
            steppers += Stepper("Köşe", AppIcons.RoundCorner, { vm.adjustSelectionCorners(-1) }, { vm.adjustSelectionCorners(1) })
        }
        // Şekil araçlarının ayarları (bir sonraki şekil için).
        when (state.tool) {
            Tool.RoundedRect -> if (!cornerPaths) steppers += Stepper("Köşe", AppIcons.RoundCorner, { vm.adjustCornerRatio(-0.05) }, { vm.adjustCornerRatio(0.05) })
            Tool.Polygon -> steppers += Stepper("Kenar", AppIcons.Polygon, { vm.adjustPolygonSides(-1) }, { vm.adjustPolygonSides(1) })
            Tool.Star -> {
                steppers += Stepper("Uç", AppIcons.Star, { vm.adjustStarPoints(-1) }, { vm.adjustStarPoints(1) })
                steppers += Stepper("İç", AppIcons.Star, { vm.adjustStarInner(-0.1) }, { vm.adjustStarInner(0.1) })
            }
            else -> {}
        }
    }
    if (acts.isEmpty() && steppers.isEmpty()) return
    val open = paletteOpen && !labeled && acts.isNotEmpty()
    val canDelete = state.selection.isNotEmpty() && state.pen == null && !(state.tool == Tool.Direct && edit?.anchor != null)

    Column(
        modifier
            .fillMaxWidth()
            .background(AppColors.Panel.copy(alpha = 0.96f))
            // Çubuğa dokunuş alttaki tuvale geçmesin.
            .clickable(interactionSource = null, indication = null) {}
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        val warp = warpNode?.warp
        if (warp != null && state.selection.size == 1) {
            // Eğim sürgüsü: oynarken tuvalde anında görülür, bırakınca geçmişe işlenir.
            var live by remember(warp.style) { mutableStateOf(warp.bend.toFloat()) }
            Row(Modifier.padding(horizontal = 16.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("%${(live * 100).roundToInt()}", Modifier.width(52.dp), style = MaterialTheme.typography.labelMedium, color = AppColors.OnPanelMuted)
                Slider(
                    value = live,
                    onValueChange = { live = it; vm.setTextWarp(warp.style, it.toDouble(), done = false) },
                    onValueChangeFinished = { vm.setTextWarp(warp.style, live.toDouble(), done = true) },
                    valueRange = -1f..1f,
                    modifier = Modifier.weight(1f).semantics { contentDescription = tr("Eğim") },
                )
            }
        }
        if (open) {
            ActionPalette(
                title = selectionTitle(vm),
                acts = acts,
                onPick = { a -> paletteOpen = false; a.onClick() },
                onClose = { paletteOpen = false },
            )
        }
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (labeled) {
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()).semantics { contentDescription = tr("Seçim eylemleri") },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (a in acts) LabeledAction(a)
                }
            } else {
                BoxWithConstraints(Modifier.weight(1f).semantics { contentDescription = tr("Seçim eylemleri") }) {
                    val slot = 46.dp
                    val stepperWidth = 128.dp
                    val paletteSlot = if (acts.isNotEmpty()) slot else 0.dp
                    val free = maxWidth - stepperWidth * steppers.size - paletteSlot
                    val fit = if (free > 0.dp) (free / slot).toInt() else 0
                    // Önce öne çıkan eylemler; geniş ekranda yer kaldıkça diğerleri de palet sırasıyla eklenir.
                    val first = QuickOrder.mapNotNull { k -> acts.firstOrNull { it.key == k } }
                    val quick = (first + acts.filter { it !in first }).take(fit)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (st in steppers) StepperControl(st)
                        for (a in quick) HintIcon(ActionIcons[a.key] ?: AppIcons.More, tr(a.key), active = a.active, onClick = a.onClick)
                        if (acts.isNotEmpty()) {
                            HintIcon(AppIcons.Apps, tr("Tüm eylemler"), active = open, onClick = { paletteOpen = !paletteOpen })
                        }
                    }
                }
            }
            if (canDelete) {
                HintIcon(AppIcons.Delete, tr("Sil"), tint = Color(0xFFFF8A8A), onClick = vm::deleteSelection)
            }
        }
    }
}

/** Paletin başlığı: seçimin ne olduğu ("Yol", "3 nesne"…). */
@Composable
private fun selectionTitle(vm: EditorViewModel): String {
    val nodes = vm.state.selection.mapNotNull { vm.state.history.present.findNode(it) }
    val single = nodes.singleOrNull() ?: return tr("%s nesne", nodes.size)
    return when (single) {
        is PathNode -> tr("Yol")
        is GroupNode -> tr("Grup")
        is ImageNode -> tr("Görsel")
        is TextNode -> tr("Metin")
    }
}

/**
 * Bütün eylemler: bölüm bölüm, simgeli ve altında adı yazan küçük kutucuklar. Dokunulan eylem uygulanır ve palet kapanır.
 */
@Composable
private fun ActionPalette(title: String, acts: List<Act>, onPick: (Act) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp)
            .semantics { contentDescription = tr("Eylem paleti") },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = AppColors.OnPanel)
            IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = tr("Paleti kapat") }) {
                Icon(AppIcons.Close, contentDescription = null, tint = AppColors.OnPanelMuted)
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
            for ((section, items) in acts.groupBy { it.section }) {
                Text(
                    tr(section),
                    Modifier.padding(top = 6.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Accent,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (a in items) PaletteTile(a) { onPick(a) }
                }
            }
        }
    }
}

@Composable
private fun PaletteTile(a: Act, onClick: () -> Unit) {
    val fg = if (a.active) Color(0xFF2B1A0E) else AppColors.OnPanel
    Column(
        Modifier
            .width(70.dp)
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (a.active) AppColors.Accent else AppColors.PanelRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(ActionIcons[a.key] ?: AppIcons.More, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            tr(a.key),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LabeledAction(a: Act) {
    val tint = if (a.key == "Düğümü sil") Color(0xFFFF8A8A) else AppColors.OnPanel
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.PanelRaised)
            .clickable(role = Role.Button, onClick = a.onClick)
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcons[a.key]?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
            Spacer(Modifier.width(7.dp))
        }
        Text(tr(a.key), color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Eksi | simge | artı: tek parça ayar düğmesi. */
@Composable
private fun StepperControl(st: Stepper) {
    Row(
        Modifier
            .padding(end = 6.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.PanelRaised),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HintIcon(AppIcons.Minus, tr(st.base + " −"), background = false, onClick = st.minus)
        Icon(st.icon, contentDescription = null, tint = AppColors.OnPanelMuted, modifier = Modifier.size(18.dp))
        HintIcon(AppIcons.Plus, tr(st.base + " +"), background = false, onClick = st.plus)
    }
}

/**
 * Yalnızca simgeli düğme. Adı erişilebilirlik adı olarak verilir; fareyle üzerine gelince ya da uzun basınca
 * düğmenin üstünde küçük bir etiket olarak görünür.
 */
@Composable
internal fun HintIcon(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    background: Boolean = true,
    tint: Color = AppColors.OnPanel,
    onClick: () -> Unit,
) {
    var hover by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val show = hover || pressed
    if (pressed) {
        LaunchedEffect(Unit) {
            delay(1600)
            pressed = false
        }
    }
    val fg = if (active) Color(0xFF2B1A0E) else tint
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AppColors.Accent else if (background) AppColors.PanelRaised else Color.Transparent)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        when (e.type) {
                            PointerEventType.Enter -> hover = true
                            PointerEventType.Exit -> hover = false
                            else -> {}
                        }
                    }
                }
            }
            .combinedClickable(onClick = { hover = false; onClick() }, onLongClick = { pressed = true })
            .semantics {
                contentDescription = label
                role = Role.Button
                selected = active
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        if (show) {
            val lift = with(LocalDensity.current) { 50.dp.roundToPx() }
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -lift)) {
                Text(
                    label,
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xF0111113))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}
