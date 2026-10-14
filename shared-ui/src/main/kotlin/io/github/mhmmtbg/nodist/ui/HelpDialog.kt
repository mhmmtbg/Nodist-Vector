package io.github.mhmmtbg.nodist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.mhmmtbg.nodist.model.HelpContent
import io.github.mhmmtbg.nodist.model.HelpItem
import io.github.mhmmtbg.nodist.model.L10n
import io.github.mhmmtbg.nodist.model.tr

/** Yardım sayfası: arama kutusu, kullanım kılavuzu ve kısayol tablosu. */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val english = L10n.english
    val results = remember(query, english) { HelpContent.search(query, english) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp).widthIn(max = 900.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Yardım"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(tr("Kapat")) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(tr("Ara…")) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                if (results.isEmpty()) {
                    Text(tr("Sonuç bulunamadı"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(results.size) { i ->
                            val item = results[i]
                            if (i == 0 || results[i - 1].section != item.section) {
                                Text(
                                    item.section,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                                )
                            }
                            HelpRow(item)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpRow(item: HelpItem) {
    if (item.isShortcut) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.keys,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text(item.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(item.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
