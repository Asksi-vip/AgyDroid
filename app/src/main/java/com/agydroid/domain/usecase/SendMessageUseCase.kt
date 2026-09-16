package com.agydroid.domain.usecase

import com.agydroid.data.local.entities.MessageEntity
import com.agydroid.data.remote.bridge.BridgeEvent
import com.agydroid.data.repository.ChatRepository
import com.agydroid.data.repository.ProjectRepository
import com.agydroid.data.local.entities.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val projectRepository: ProjectRepository
) {
    suspend fun sendUserMessage(
        projectId: String,
        workspace: String,
        content: String,
        conversationId: String? = null
    ): Pair<MessageEntity, Flow<BridgeEvent>> {
        // 1. Record user message
        chatRepository.saveUserMessage(projectId, content)

        // 2. Mark project as coding
        projectRepository.updateStatus(projectId, ProjectStatus.CODING)

        // 3. Create placeholder assistant message
        val assistantPlaceholder = chatRepository.createAssistantPlaceholder(projectId)

        val stringBuilder = java.lang.StringBuilder()

        // 4. Stream from AGY via Termux bridge
        val streamFlow = chatRepository.streamAgyPrompt(content, workspace, conversationId)
            .onEach { event ->
                when (event) {
                    is BridgeEvent.Text -> {
                        stringBuilder.append(event.content)
                        chatRepository.updateMessageContent(
                            assistantPlaceholder.id,
                            stringBuilder.toString().trim(),
                            isStreaming = true
                        )
                    }
                    is BridgeEvent.StreamData -> {
                        if (!event.content.isNullOrBlank()) {
                            stringBuilder.append(event.content).append("\n")
                            chatRepository.updateMessageContent(
                                assistantPlaceholder.id,
                                stringBuilder.toString().trim(),
                                isStreaming = true
                            )
                        }
                    }
                    is BridgeEvent.Done -> {
                        chatRepository.updateMessageContent(
                            assistantPlaceholder.id,
                            stringBuilder.toString().trim(),
                            isStreaming = false
                        )
                        val finalStatus = if (event.success) ProjectStatus.READY else ProjectStatus.ERROR
                        projectRepository.updateStatus(projectId, finalStatus)
                    }
                    is BridgeEvent.Error -> {
                        stringBuilder.append("\n⚠️ Error: ").append(event.message)
                        chatRepository.updateMessageContent(
                            assistantPlaceholder.id,
                            stringBuilder.toString().trim(),
                            isStreaming = false
                        )
                        projectRepository.updateStatus(projectId, ProjectStatus.ERROR)
                    }
                    else -> Unit
                }
            }
            .onCompletion {
                chatRepository.updateMessageContent(
                    assistantPlaceholder.id,
                    stringBuilder.toString().trim(),
                    isStreaming = false
                )
            }

        return Pair(assistantPlaceholder, streamFlow)
    }
}
