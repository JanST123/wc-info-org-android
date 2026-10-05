package de.wcinfo.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.repository.ToiletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DetailUiState(
    val toilet: Toilet? = null,
    val isLoading: Boolean = true,
    val isUploadingPhoto: Boolean = false,
    val uploadSuccess: Boolean = false,
    val errorMessage: String? = null
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ToiletRepository()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadToilet(toiletId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getToilet(toiletId)
                .onSuccess { toilet ->
                    _uiState.update { it.copy(toilet = toilet, isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Fehler beim Laden"
                        )
                    }
                }
        }
    }

    fun uploadPhoto(file: File) {
        val toiletId = _uiState.value.toilet?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            repository.uploadPhoto(toiletId, file)
                .onSuccess {
                    _uiState.update { it.copy(isUploadingPhoto = false, uploadSuccess = true) }
                    loadToilet(toiletId) // Reload toilet to show uploaded photo
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = err.localizedMessage ?: "Fehler beim Hochladen"
                        )
                    }
                }
        }
    }

    fun clearUploadSuccess() {
        _uiState.update { it.copy(uploadSuccess = false) }
    }

    fun onPhotoDeleted(filename: String) {
        _uiState.update { state ->
            val cur = state.toilet ?: return@update state
            val updated = cur.photos.filter { it.resolvedFilename != filename }
            state.copy(toilet = cur.copy(photos = updated))
        }
    }
}

