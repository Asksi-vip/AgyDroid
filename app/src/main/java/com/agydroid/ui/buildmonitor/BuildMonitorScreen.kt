package com.agydroid.ui.buildmonitor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agydroid.data.local.entities.BuildStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildMonitorScreen(
    projectId: String,
    runId: Long,
    onBack: () -> Unit,
    viewModel: BuildMonitorViewModel = hiltViewModel()
) {
    val run by viewModel.currentRun.collectAsState()
    val buildEntity by viewModel.getBuildRecord(runId).collectAsState(initial = null)
    val context = LocalContext.current

    LaunchedEffect(projectId, runId) {
        viewModel.startMonitoring(projectId, runId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build #$runId") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Status Graphic
            val isSuccess = buildEntity?.status == BuildStatus.COMPLETED && buildEntity?.conclusion == "success"
            val isFailed = buildEntity?.status == BuildStatus.FAILED || buildEntity?.conclusion == "failure"
            val isInProgress = !isSuccess && !isFailed

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSuccess -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    isFailed -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp
                        )
                    }
                }
            }

            Text(
                text = when {
                    isSuccess -> "🎉 Build Successful!"
                    isFailed -> "Build Failed"
                    else -> "Building on GitHub Actions..."
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = when {
                    isSuccess -> "Your APK artifact is ready for download."
                    isFailed -> "Check GitHub logs for compilation details."
                    else -> "Setting up JDK, resolving dependencies, and compiling APK."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Action Buttons
            if (isSuccess && !buildEntity?.apkUrl.isNullOrBlank()) {
                Button(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(buildEntity!!.apkUrl))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download APK")
                }
            }

            if (run?.htmlUrl != null) {
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(run!!.htmlUrl))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Logs on GitHub")
                }
            }
        }
    }
}
