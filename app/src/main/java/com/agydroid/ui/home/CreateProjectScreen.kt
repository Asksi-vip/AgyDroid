package com.agydroid.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    onProjectCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.example.") }

    LaunchedEffect(uiState) {
        if (uiState is CreateProjectUiState.Success) {
            onProjectCreated((uiState as CreateProjectUiState.Success).projectId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Android Project") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Project Details",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (packageName.startsWith("com.example.")) {
                        val cleanName = it.lowercase().replace(" ", "").filter { c -> c.isLetterOrDigit() }
                        packageName = "com.example.$cleanName"
                    }
                },
                label = { Text("Project Name") },
                placeholder = { Text("e.g. CryptoTracker") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it.lowercase().trim() },
                label = { Text("Package Name") },
                placeholder = { Text("e.g. com.example.cryptotracker") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState is CreateProjectUiState.Error) {
                Text(
                    text = (uiState as CreateProjectUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.createProject(name, packageName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = uiState !is CreateProjectUiState.Loading && name.isNotBlank() && packageName.isNotBlank()
            ) {
                if (uiState is CreateProjectUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Project")
                }
            }
        }
    }
}
