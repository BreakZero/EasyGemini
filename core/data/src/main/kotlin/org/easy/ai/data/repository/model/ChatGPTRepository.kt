package org.easy.ai.data.repository.model

import kotlinx.coroutines.flow.Flow
import org.easy.ai.model.ChatMessage
import javax.inject.Inject

class ChatGPTRepository @Inject constructor() : ModelChatRepository {
    override fun initial(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun switchChat(history: List<ChatMessage>) {
        TODO("Not yet implemented")
    }

    override suspend fun sendMessage(userMessage: String): ChatMessage {
        TODO("Not yet implemented")
    }
}