package jlpersonal.research.llm_w_lora_on_mediapipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jlpersonal.research.llm_w_lora_on_mediapipe.ui.theme.LLM_w_LoRA_on_MediaPipeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LlmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LLM_w_LoRA_on_MediaPipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Stateful Composable 호출 (실제 앱에서 사용)
                    LlmInferenceScreen(viewModel)
                }
            }
        }
    }
}

/**
 * ViewModel과 연결되어 실제 데이터를 관리하는 Stateful Composable
 */
@Composable
fun LlmInferenceScreen(viewModel: LlmViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ViewModel의 데이터를 Stateless Composable에 전달
    LlmInferenceScreenContent(
        uiState = uiState,
        onAdapterSelected = { adapter -> viewModel.onLoraAdapterSelected(adapter) },
        onGenerateClicked = { prompt -> viewModel.generateResponse(prompt) }
    )
}

/**
 * UI의 모양과 레이아웃만 담당하는 Stateless Composable
 * 이 함수는 ViewModel에 대한 의존성이 없어 Preview가 가능합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmInferenceScreenContent(
    uiState: LlmUiState,
    onAdapterSelected: (LoraAdapter) -> Unit,
    onGenerateClicked: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
                Text(
            text = "Gemini 3 1B with LoRA",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
        ) {
            OutlinedTextField(
                value = uiState.selectedAdapter?.name ?: "Select Adapter...",
                onValueChange = {},
                readOnly = true,
                label = { Text("LoRA Adapter") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                uiState.availableAdapters.forEach { adapter ->
                    DropdownMenuItem(
                        text = { Text(adapter.name) },
                        onClick = {
                            onAdapterSelected(adapter)
                            isDropdownExpanded = false
                        },
                        enabled = !uiState.isLoading
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter your prompt here...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (prompt.isNotBlank()) {
                    onGenerateClicked(prompt)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isModelReady && !uiState.isLoading
        ) {
            Text("Generate")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F1F1))
                .padding(8.dp)
        ) {
            Text(
                text = uiState.resultText,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * UI 미리보기를 위한 Composable
 */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // 1. 미리보기에 사용할 가짜(fake) 데이터 생성
    val previewUiState = LlmUiState(
        resultText = "This is a preview result. The model is ready to generate a response based on your prompt.",
        isLoading = false,
        isModelReady = true,
        availableAdapters = listOf(
            LoraAdapter("Base Model Only", null),
            LoraAdapter("My LoRA Adapter", "path/to/lora"),
            LoraAdapter("Summarization LoRA", "path/to/another/lora")
        ),
        selectedAdapter = LoraAdapter("Base Model Only", null)
    )

    // 2. 앱 테마를 적용하고 Stateless Composable을 호출
    LLM_w_LoRA_on_MediaPipeTheme {
        LlmInferenceScreenContent(
            uiState = previewUiState,
            onAdapterSelected = {}, // 프리뷰에서는 아무 동작도 하지 않음
            onGenerateClicked = {}  // 프리뷰에서는 아무 동작도 하지 않음
        )
    }
}