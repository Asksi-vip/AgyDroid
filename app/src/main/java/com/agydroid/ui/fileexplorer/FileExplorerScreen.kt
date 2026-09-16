package com.agydroid.ui.fileexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agydroid.data.remote.bridge.FileItemDto
import com.agydroid.ui.components.CodeBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: FileExplorerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFile by viewModel.selectedFileContent.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadFiles(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFile?.first ?: "Files") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFile != null) {
                            viewModel.closeFileViewer()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (selectedFile != null) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedFile != null) {
                // Code Viewer
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val (fileName, content) = selectedFile!!
                    val ext = fileName.substringAfterLast('.', "txt")
                    CodeBlock(
                        code = content,
                        language = ext,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // File List
                when (val state = uiState) {
                    is FileExplorerUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is FileExplorerUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is FileExplorerUiState.Success -> {
                        if (state.files.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Workspace is empty. Ask Antigravity to write code first!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.files) { item ->
                                    FileRow(item = item, depth = 0, onOpenFile = { viewModel.openFile(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(
    item: FileItemDto,
    depth: Int,
    onOpenFile: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (item.isDir) {
                        isExpanded = !isExpanded
                    } else {
                        onOpenFile(item.path)
                    }
                }
                .padding(horizontal = (16 + depth * 16).dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDir) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (item.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (item.isDir && isExpanded && !item.children.isNullOrEmpty()) {
            item.children.forEach { child ->
                FileRow(item = child, depth = depth + 1, onOpenFile = onOpenFile)
            }
        }
    }
}
