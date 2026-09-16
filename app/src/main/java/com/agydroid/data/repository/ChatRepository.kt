package com.agydroid.data.repository

import com.agydroid.data.local.dao.MessageDao
import com.agydroid.data.local.entities.MessageEntity
import com.agydroid.data.local.entities.MessageRole
import com.agydroid.data.remote.bridge.BridgeEvent
import com.agydroid.data.remote.bridge.BridgeStreamClient
import com.agydroid.data.remote.bridge.ChatPromptRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val bridgeStreamClient: BridgeStreamClient
) {
    fun getMessagesForProject(projectId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForProject(projectId)
    }

    suspend fun saveUserMessage(projectId: String, content: String): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            role = MessageRole.USER,
            content = content,
            isStreaming = false,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
        return message
    }

    suspend fun createAssistantPlaceholder(projectId: String): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
        return message
    }

    suspend fun updateMessageContent(messageId: String, content: String, isStreaming: Boolean) {
        messageDao.updateMessageContent(messageId, content, isStreaming)
    }

    fun streamAgyPrompt(
        prompt: String,
        workspace: String,
        conversationId: String? = null
    ): Flow<BridgeEvent> {
        val request = ChatPromptRequest(
            prompt = prompt,
            workspace = workspace,
            conversationId = conversationId
        )
        return bridgeStreamClient.streamChat(request)
    }
}
