// Modify ChatViewModel.kt
package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

class ChatViewModel(
    private var inferenceModel: InferenceModel,
    private val chatDao: ChatDao // Inject ChatDao
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> =_uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    init {
        // Load existing messages from DB when ViewModel is created
        viewModelScope.launch(Dispatchers.IO) { // <--- This is crucial
            chatDao.getAllChatMessages().collect { entities ->
                val chatMessages = entities.map {
                    ChatMessage(
                        id = it.id.toString(),
                        rawMessage = it.rawMessage,
                        author = it.author,
                        isLoading = false,
                        isThinking = false
                    )
                }
                // Corrected update to _uiState's value
                // Use .value directly if you're replacing the whole UiState object
                _uiState.value = UiState(inferenceModel.uiState.supportsThinking, chatMessages.asReversed())
                recomputeSizeInTokens("")
            }
        }
    }

    fun resetInferenceModel(newModel: InferenceModel) {
        inferenceModel = newModel
        _uiState.value = inferenceModel.uiState
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save user message to DB
            chatDao.insertChatMessage(ChatMessageEntity(rawMessage = userMessage, author = USER_PREFIX))
            
            // Update UI on Main thread
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.value.addMessage(userMessage, USER_PREFIX)
                _uiState.value.createLoadingMessage()
                setInputEnabled(false)
            }

            val promptForModel = if (InferenceModel.model == Model.DB_AWARE_LLM) {
                // Approach 1: Fine-tuned LLM
                userMessage // The model is assumed to be fine-tuned to handle DB queries from the raw prompt
            } else {
                // Approach 2: Prompt-based DB access (for non-DB-aware models)
                generateDbAccessPrompt(userMessage)
            }

            try {
                // In ChatViewModel.kt, inside sendMessage function
                val asyncInference =  inferenceModel.generateResponseAsync(promptForModel) { partialResult, done ->
                    // Ensure UI updates happen on Main thread
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.value.appendMessage(partialResult)
                        if (done) {
                            setInputEnabled(true)
                            val lastMessage = _uiState.value.messages.firstOrNull()
                            if (lastMessage?.author == MODEL_PREFIX && !lastMessage.isLoading && !lastMessage.isThinking) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    chatDao.insertChatMessage(ChatMessageEntity(rawMessage = lastMessage.rawMessage, author = MODEL_PREFIX))
                                }
                            }
                        } else {
                            _tokensRemaining.update { max(0, it - 1) }
                        }
                    }
                }
                // Once the inference is done, recompute the remaining size in tokens
                asyncInference.addListener({
                    viewModelScope.launch(Dispatchers.IO) {
                        recomputeSizeInTokens(userMessage)
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                    setInputEnabled(true)
                }
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteAllMessages()
            _uiState.value.clearMessages()
            recomputeSizeInTokens("")
        }
    }

    // New function for prompt-based DB access
    private suspend fun generateDbAccessPrompt(userQuery: String): String {
        val allMessages = chatDao.getAllChatMessages().first()
        val formattedHistory = allMessages.joinToString("\n") { message ->
            "${message.author}: ${message.rawMessage}"
        }

        // This is a simplified example. A real implementation might parse the user query
        // to decide which DB queries to perform and inject the results.
        return """
            You are a helpful assistant. Here is the past chat history:
            $formattedHistory

            Based on the user's query, if it relates to past chat messages, retrieve them from the database.
            Example of database query results (if applicable):
            ---DB_QUERY_RESULTS_START---
            [Here you would insert results of actual DB queries based on the user's prompt]
            ---DB_QUERY_RESULTS_END---

            User's Query: $userQuery
            Your response:
        """.trimIndent()
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                val database = AppDatabase.getDatabase(context)
                val chatDao = database.chatDao()
                return ChatViewModel(inferenceModel, chatDao) as T
            }
        }
    }
}