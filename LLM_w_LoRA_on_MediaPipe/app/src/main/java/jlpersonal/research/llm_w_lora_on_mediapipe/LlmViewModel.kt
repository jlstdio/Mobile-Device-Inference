package jlpersonal.research.llm_w_lora_on_mediapipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// LoRA 어댑터 선택을 위한 데이터 클래스
data class LoraAdapter(val name: String, val path: String?)

data class LlmUiState(
    val resultText: String = "Result will appear here...",
    val isLoading: Boolean = false,
    val isModelReady: Boolean = false,
    val availableAdapters: List<LoraAdapter> = emptyList(),
    val selectedAdapter: LoraAdapter? = null
)

class LlmViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LlmUiState())
    val uiState: StateFlow<LlmUiState> = _uiState.asStateFlow()

    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null

    companion object {
        // The model file will be copied to the assets directory.
        // All paths are relative to the assets directory.
        private const val BASE_MODEL_PATH = "gemma3-1b-it-int4.task"


        // 사용 가능한 LoRA 어댑터 목록 정의
        val LORA_ADAPTERS = listOf(
            LoraAdapter(name = "Base Model Only", path = null), // LoRA 미적용 옵션
            LoraAdapter(name = "My LoRA Adapter", path = "my_lora_adapter.tflite")
            // 다른 LoRA 어댑터가 있다면 여기에 추가
        )
    }

    init {
        _uiState.update { it.copy(availableAdapters = LORA_ADAPTERS) }
        initializeBaseModel()
    }

    // 1. 베이스 모델만 먼저 로드하는 함수
    private fun initializeBaseModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, resultText = "Base model is loading...") }
            try {
                // LLM 실행에 필요한 task 초기화, 모델 위치, 모델 max token.. 등등 설정 여기서 설정 가능
                val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(BASE_MODEL_PATH)
                    .build()
                llmInference = LlmInference.createFromOptions(getApplication(), inferenceOptions)

                // 기본적으로 첫 번째 옵션(Base Model Only)으로 세션을 생성
                onLoraAdapterSelected(LORA_ADAPTERS.first())

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isModelReady = false, resultText = "Failed to load base model: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    // 2. 사용자가 LoRA 어댑터를 선택했을 때 호출되는 함수
    fun onLoraAdapterSelected(adapter: LoraAdapter) {
        if (adapter.name == "My LoRA Adapter") {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        resultText = "LoRA is not available, returning to Base Model Only.",
                        selectedAdapter = LORA_ADAPTERS.first()
                    )
                }
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(isLoading = true, resultText = "Applying '${adapter.name}'...", selectedAdapter = adapter, isModelReady = false)
            }
            // 기존 세션이 있다면 닫아줍니다.
            llmSession?.close()
            llmSession = null

            try {
                val sessionOptionsBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(101)

                // 선택된 어댑터의 경로가 null이 아닐 경우에만 LoRA 경로를 설정합니다.
                adapter.path?.let {
                    sessionOptionsBuilder.setLoraPath(it)
                }

                llmSession = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptionsBuilder.build())

                _uiState.update {
                    it.copy(isLoading = false, isModelReady = true, resultText = "'${adapter.name}' is ready. Enter a prompt.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isModelReady = false, resultText = "Failed to create session with '${adapter.name}': ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    fun generateResponse(prompt: String) {
        if (!_uiState.value.isModelReady || llmSession == null) {
            _uiState.value = _uiState.value.copy(resultText = "Model is not ready.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, resultText = "")
            try {
                llmSession?.addQueryChunk(prompt)
                val response = llmSession?.generateResponse()
                _uiState.value = _uiState.value.copy(isLoading = false, resultText = response ?: "No response")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, resultText = "Error generating response: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmSession?.close()
        llmInference?.close()
    }
}