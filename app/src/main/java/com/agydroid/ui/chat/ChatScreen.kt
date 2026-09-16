package com.agydroid.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agydroid.data.local.entities.MessageEntity
import com.agydroid.data.local.entities.MessageRole
import com.agydroid.ui.components.CodeBlock
import com.agydroid.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    projectId: String,
    onNavigateToFiles: () -> Unit,
    onNavigateToBuild: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val project by viewModel.getProject(projectId).collectAsState(initial = null)
    val messages by viewModel.getMessages(projectId).collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val suggestions = listOf(
        "أريد تطبيق يعرض أسعار العملات والذهب مباشر",
        "تطبيق قرآن كريم مع أوقات الصلاة والقبلة",
        "تطبيق إدارة المهام والملاحظات الذكي",
        "تطبيق متجر إلكتروني مع سلة مشتريات ودفع"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                project?.name ?: "AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Antigravity CLI (agy v1.2.3)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFiles) {
                        Icon(Icons.Default.Folder, contentDescription = "Files", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            project?.let { p ->
                                viewModel.triggerBuild(p) { runId ->
                                    onNavigateToBuild(runId)
                                }
                            }
                        },
                        enabled = !uiState.isBuilding && project != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (uiState.isBuilding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Build APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Agent Live Status Indicator
            AnimatedVisibility(visible = uiState.isStreaming) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري كتابة الكود وبناء ملفات المشروع...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyChatState(
                            onSelectSuggestion = { suggestion ->
                                inputPrompt = suggestion
                            }
                        )
                    }
                }

                items(messages, key = { it.id }) { message ->
                    ChatMessageRow(message = message)
                }
            }

            // Quick suggestion chips above input
            if (messages.isEmpty() || !uiState.isStreaming) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { inputPrompt = suggestion },
                            label = { Text(suggestion, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // Bottom Input Bar (ChatGPT Style)
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = { Text("اطلب تطبيقك أو اسأل عن الكود...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        enabled = !uiState.isStreaming
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (uiState.isStreaming) {
                        IconButton(
                            onClick = { viewModel.cancelGeneration() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val text = inputPrompt.trim()
                                if (text.isNotBlank() && project != null) {
                                    viewModel.sendMessage(project!!, text)
                                    inputPrompt = ""
                                }
                            },
                            enabled = inputPrompt.isNotBlank() && project != null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputPrompt.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputPrompt.isNotBlank()) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(onSelectSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "مساعدك البرمجي الذكي لبناء تطبيقات الأندرويد",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "اكتب فكرة تطبيقك وسيقوم المهندس الذكي بإنشاء الملفات وكتابة الأكواد المصدرية وتجهيز البناء السحابي للـ APK.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ChatMessageRow(message: MessageEntity) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(14.dp)
            ) {
                val content = message.content
                if (content.contains("```")) {
                    FormattedRichContent(
                        raw = content,
                        textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = content.ifEmpty { if (message.isStreaming) "● ● ●" else "" },
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            // Copy action button
            if (!message.isStreaming && message.content.isNotBlank()) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "تم نسخ الرسالة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp).padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "نسخ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FormattedRichContent(raw: String, textColor: Color) {
    val parts = raw.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                val lines = part.trim().lines()
                val lang = if (lines.isNotEmpty() && lines.first().matches("^[a-zA-Z]+$".toRegex())) lines.first() else "kotlin"
                val code = if (lines.size > 1 && lines.first().matches("^[a-zA-Z]+$".toRegex())) {
                    lines.drop(1).joinToString("\n")
                } else part.trim()
                CodeBlock(code = code, language = lang)
            } else {
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
