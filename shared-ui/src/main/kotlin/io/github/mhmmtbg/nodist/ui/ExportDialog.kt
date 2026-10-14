package io.github.mhmmtbg.nodist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.mhmmtbg.nodist.editor.ExportFormat
import io.github.mhmmtbg.nodist.model.tr

/** Dışa aktarma biçimi seçilir; ardından çağıran kaydedilecek yeri sorar. Katmanlar ve seçili nesneler aynı pencereyi kullanır. */
@Composable
internal fun ExportFormatDialog(title: String, hint: String, onDismiss: () -> Unit, onPick: (ExportFormat) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                val formats = listOf(
                    ExportFormat.Nodist to tr("Nodist (.nodist) — yeniden düzenlemek için"),
                    ExportFormat.Ai to "Illustrator (.ai)",
                    ExportFormat.Pdf to "PDF (.pdf)",
                    ExportFormat.Svg to "SVG (.svg)",
                    ExportFormat.Png to "PNG (.png)",
                )
                for ((format, label) in formats) {
                    Text(
                        label,
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.PanelRaised)
                            .clickable(role = Role.Button) { onPick(format) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        color = AppColors.OnPanel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Vazgeç")) } },
    )
}
