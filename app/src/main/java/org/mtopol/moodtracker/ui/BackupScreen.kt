package org.mtopol.moodtracker.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtopol.moodtracker.ImportOutcome
import org.mtopol.moodtracker.R
import java.io.File
import java.time.LocalDate

/**
 * "Backup" tab. Two user-driven actions plus an explainer for the automatic
 * cloud backup that runs without any UI. Export hands a JSON file to the system
 * share sheet (the reliable route to Drive/email/etc.); import reads a file
 * picked through the Storage Access Framework. The file is validated on import,
 * so the picker deliberately accepts any type — Drive often mislabels JSON.
 */
@Composable
fun BackupScreen(
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> ImportOutcome,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            if (text == null) {
                context.toast(context.getString(R.string.import_failed))
                return@launch
            }
            when (val result = onImportJson(text)) {
                is ImportOutcome.Success ->
                    context.toast(context.getString(R.string.import_success, result.count))
                ImportOutcome.Empty ->
                    context.toast(context.getString(R.string.import_empty))
                is ImportOutcome.Failure ->
                    context.toast(context.getString(R.string.import_failed))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                stringResource(R.string.tab_backup),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.backup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ActionCard(
            title = stringResource(R.string.export_title),
            body = stringResource(R.string.export_desc),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val json = runCatching { onExportJson() }.getOrNull()
                        if (json == null) {
                            context.toast(context.getString(R.string.export_failed))
                            return@launch
                        }
                        val uri = withContext(Dispatchers.IO) {
                            runCatching { writeExportFile(context, json) }.getOrNull()
                        }
                        if (uri == null) {
                            context.toast(context.getString(R.string.export_failed))
                            return@launch
                        }
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.export_share_title),
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    share,
                                    context.getString(R.string.export_share_title),
                                ),
                            )
                        }.onFailure {
                            context.toast(context.getString(R.string.export_failed))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Text(
                    stringResource(R.string.export_button),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        ActionCard(
            title = stringResource(R.string.import_title),
            body = stringResource(R.string.import_desc),
        ) {
            OutlinedButton(
                onClick = { importPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Text(
                    stringResource(R.string.import_button),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        ActionCard(
            title = stringResource(R.string.backup_auto_title),
            body = stringResource(R.string.backup_auto_desc),
            content = null,
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    body: String,
    content: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content?.invoke()
        }
    }
}

/**
 * Writes the export to a private cache file and returns a `content://` URI the
 * share target can read (via [FileProvider], authority `<pkg>.fileprovider`).
 * One fixed filename per day keeps the cache from accumulating copies.
 */
private fun writeExportFile(context: Context, json: String): android.net.Uri {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "cloudy-backup-${LocalDate.now()}.json")
    file.writeText(json)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
