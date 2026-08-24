package com.scoop.app.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.extractor.MediaExtractor
import kotlinx.coroutines.launch

sealed interface AnalyzeUiState {
    data object Idle : AnalyzeUiState

    data object Analyzing : AnalyzeUiState

    data class Success(val info: MediaInfo) : AnalyzeUiState

    data class Error(val message: String) : AnalyzeUiState
}

class HomeViewModel(private val extractor: MediaExtractor, private val downloadManager: DownloadManager) :
    ViewModel() {

    var url by mutableStateOf("")
        private set

    var uiState by mutableStateOf<AnalyzeUiState>(AnalyzeUiState.Idle)
        private set

    fun onUrlChange(value: String) {
        url = value
        if (uiState !is AnalyzeUiState.Idle) uiState = AnalyzeUiState.Idle
    }

    fun analyze() {
        val target = url.trim()
        if (target.isEmpty()) return
        uiState = AnalyzeUiState.Analyzing
        viewModelScope.launch {
            extractor
                .analyze(target)
                .onSuccess { uiState = AnalyzeUiState.Success(it) }
                .onFailure { uiState = AnalyzeUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun download(kind: DownloadKind) {
        val state = uiState as? AnalyzeUiState.Success ?: return
        downloadManager.enqueue(
            request = DownloadRequest(url = state.info.sourceUrl, kind = kind),
            title = state.info.title,
            thumbnailUrl = state.info.thumbnailUrl,
        )
    }
}
