package org.easy.ai.plugins.multimodal

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.easy.ai.domain.TextAndImageGeneratingUseCase
import javax.inject.Inject

@OptIn(ExperimentalFoundationApi::class)
@HiltViewModel
internal class MultiModalViewModel @Inject constructor(
    private val multiModalGeneratingUseCase: TextAndImageGeneratingUseCase
) : ViewModel() {
    companion object {
        private const val CONTENT_LIMIT_SIZE = 4 * 1024 * 1024
    }

    val promptTextField = TextFieldState("")
    private val _uiState = MutableStateFlow(MultiModalUiState())

    val uiState = _uiState.asStateFlow()

    fun onImageChanged(imagesByte: List<ByteArray>) {
        _uiState.update {
            it.copy(images = imagesByte)
        }
    }

    private fun validatePrompt(): String? {
        if (promptTextField.text.toString().isBlank()) {
            return "prompt can not be empty..."
        }
        with(_uiState.value) {
            val promptSize = promptTextField.text.toString().encodeToByteArray().size
            val totalSize = (images?.sumOf { image -> image.size } ?: 0) + promptSize
            if (totalSize > CONTENT_LIMIT_SIZE) {
                return "the entire prompt is too large, 4MB limited"
            }
        }
        return null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun submitPrompt() {
        val error = validatePrompt()
        if (error != null) {
            // error handling
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(inProgress = true, promptResult = null, error = null) }
        val images = _uiState.value.images
        val promptResult = StringBuilder()
        val bitmaps = images?.map {
            BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap()
                .asAndroidBitmap()
        }
        multiModalGeneratingUseCase(promptTextField.text.toString(), bitmaps)
            .onEach { result ->
                promptResult.append(result)
                _uiState.update {
                    it.copy(
                        promptResult = promptResult.toString(),
                        error = null
                    )
                }
            }.catch { cause ->
                _uiState.update {
                    it.copy(
                        inProgress = false,
                        error = cause.message ?: "unknown generating error..."
                    )
                }
            }.onCompletion {
                _uiState.update {
                    it.copy(inProgress = false)
                }
            }.launchIn(viewModelScope)
    }
}