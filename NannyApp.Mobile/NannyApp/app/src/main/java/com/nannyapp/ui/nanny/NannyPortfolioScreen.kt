package com.nannyapp.ui.nanny

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.PortfolioType
import com.nannyapp.ui.components.*

/** Document/portfolio management for request #19: upload ID/certs/references/photos,
 * show pending/verified/rejected status set by admin. */
@Composable
fun NannyPortfolioScreen(onBack: () -> Unit, viewModel: NannyPortfolioViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showUploadDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = "Portfolio & Documents", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showUploadDialog = true }) { Icon(Icons.Filled.UploadFile, contentDescription = "Upload") }
        },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.items.isEmpty() -> EmptyState(
                title = "No documents yet", message = "Upload your ID, certificates, and references to get verified.",
                icon = Icons.Filled.Folder, actionLabel = "Upload a document", onAction = { showUploadDialog = true }, modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(if (item.adminVerified) Icons.Filled.CheckCircle else Icons.Filled.HourglassEmpty, contentDescription = null, tint = if (item.adminVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text("${item.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${if (item.adminVerified) "Verified" else "Pending review"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.delete(item.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                        }
                    }
                }
            }
        }

        if (showUploadDialog) {
            UploadDialog(
                uploading = state.uploading,
                onDismiss = { showUploadDialog = false },
                onUpload = { type, title, bytes, fileName ->
                    viewModel.upload(type, title, bytes, fileName)
                    showUploadDialog = false
                },
            )
        }
    }
}

@Composable
private fun UploadDialog(
    uploading: Boolean,
    onDismiss: () -> Unit,
    onUpload: (PortfolioType, String, ByteArray, String) -> Unit,
) {
    var type by remember { mutableStateOf(PortfolioType.CERTIFICATE) }
    var title by remember { mutableStateOf("") }
    var pickedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }

    val pickFile = rememberFilePickerLauncher { bytes, fileName ->
        pickedBytes = bytes
        pickedName = fileName
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload document") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    PortfolioType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = pickedName ?: "Choose a photo of the document", onClick = pickFile, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { val bytes = pickedBytes; if (title.isNotBlank() && bytes != null) onUpload(type, title, bytes, pickedName ?: "$title.jpg") },
                enabled = !uploading && title.isNotBlank() && pickedBytes != null,
            ) { Text("Upload") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

