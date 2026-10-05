package de.wcinfo.app.ui.results

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import de.wcinfo.app.data.location.LocationTracker
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.model.ToiletFilterSettings
import de.wcinfo.app.data.model.ToiletPhoto
import de.wcinfo.app.data.repository.FilterPreferencesRepository
import de.wcinfo.app.data.repository.ToiletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultsUiState(
    val toilets: List<Toilet> = emptyList(),
    val selectedToilet: Toilet? = null,
    val isLoading: Boolean = true,
    val isFetchingBounds: Boolean = false,
    val filterSettings: ToiletFilterSettings = ToiletFilterSettings(),
    val isSatellite: Boolean = false,
    val userLocation: LatLng? = null,
    val errorMessage: String? = null
)

class ResultsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ToiletRepository = ToiletRepository(),
    private val filterPreferencesRepository: FilterPreferencesRepository = FilterPreferencesRepository(application)
) : AndroidViewModel(application) {

    private val locationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private var boundsJob: Job? = null
    private var searchCoordinate: LatLng? = null

    init {
        viewModelScope.launch {
            val savedFilters = filterPreferencesRepository.getFilterSettings()
            _uiState.update { it.copy(filterSettings = savedFilters) }
        }
        viewModelScope.launch {
            locationTracker.getCurrentLocation()?.let { loc ->
                _uiState.update { it.copy(userLocation = LatLng(loc.latitude, loc.longitude)) }
            }
        }
    }

    fun initLocation(latitude: Double, longitude: Double, initialToiletId: Int? = null) {
        searchCoordinate = LatLng(latitude, longitude)
        viewModelScope.launch {
            val savedFilters = filterPreferencesRepository.getFilterSettings()
            _uiState.update { it.copy(filterSettings = savedFilters) }
            loadToiletsInternal(latitude, longitude, initialToiletId, savedFilters)
        }
    }

    fun loadToilets(latitude: Double, longitude: Double, initialToiletId: Int? = null) {
        loadToiletsInternal(latitude, longitude, initialToiletId, _uiState.value.filterSettings)
    }

    private fun loadToiletsInternal(
        latitude: Double,
        longitude: Double,
        initialToiletId: Int?,
        filters: ToiletFilterSettings
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getToiletsNearby(
                latitude = latitude,
                longitude = longitude,
                distanceKm = 10,
                filters = filters
            )

            result.onSuccess { list ->
                val selected = if (initialToiletId != null) {
                    list.find { it.id == initialToiletId } ?: list.firstOrNull()
                } else {
                    list.firstOrNull()
                }
                _uiState.update {
                    it.copy(
                        toilets = list,
                        selectedToilet = selected,
                        isLoading = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Fehler beim Laden der Toiletten"
                    )
                }
            }
        }
    }

    fun onCameraIdle(south: Double, west: Double, north: Double, east: Double) {
        boundsJob?.cancel()
        boundsJob = viewModelScope.launch {
            delay(500) // Debounce map camera movement
            _uiState.update { it.copy(isFetchingBounds = true) }
            val result = repository.getToiletsInBounds(
                south = south,
                west = west,
                north = north,
                east = east,
                filters = _uiState.value.filterSettings
            )
            result.onSuccess { newToilets ->
                // Merge without duplicates
                val existingMap = _uiState.value.toilets.associateBy { it.id }.toMutableMap()
                newToilets.forEach { existingMap[it.id] = it }
                val merged = existingMap.values.toList()
                _uiState.update {
                    it.copy(
                        toilets = merged,
                        isFetchingBounds = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isFetchingBounds = false) }
            }
        }
    }

    fun updateFilters(newFilters: ToiletFilterSettings) {
        _uiState.update { it.copy(filterSettings = newFilters) }
        viewModelScope.launch {
            filterPreferencesRepository.saveFilterSettings(newFilters)
        }
        searchCoordinate?.let {
            loadToiletsInternal(it.latitude, it.longitude, null, newFilters)
        }
    }

    fun selectToilet(toilet: Toilet?) {
        _uiState.update { it.copy(selectedToilet = toilet) }
    }

    fun toggleSatellite() {
        _uiState.update { it.copy(isSatellite = !it.isSatellite) }
    }

    fun refresh() {
        searchCoordinate?.let {
            loadToilets(it.latitude, it.longitude)
        }
    }

    fun onPhotoDeleted(toiletId: Int, filename: String) {
        _uiState.update { state ->
            val updatedToilets = state.toilets.map { t ->
                if (t.id == toiletId) {
                    t.copy(photos = t.photos.filter { it.resolvedFilename != filename })
                } else t
            }
            val updatedSelected = if (state.selectedToilet?.id == toiletId) {
                state.selectedToilet.copy(photos = state.selectedToilet.photos.filter { it.resolvedFilename != filename })
            } else state.selectedToilet
            state.copy(toilets = updatedToilets, selectedToilet = updatedSelected)
        }
    }
}
