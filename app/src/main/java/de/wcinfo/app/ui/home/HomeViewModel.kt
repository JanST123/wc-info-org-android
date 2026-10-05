package de.wcinfo.app.ui.home

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import de.wcinfo.app.BuildConfig
import de.wcinfo.app.data.location.LocationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class HomeUiState(
    val searchQuery: String = "",
    val predictions: List<PlaceSuggestion> = emptyList(),
    val isLoadingPredictions: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val selectedLocation: Pair<Double, Double>? = null,
    val selectedLocationName: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val locationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private fun getPlacesClient(): PlacesClient? {
        val app = getApplication<Application>()
        val apiKey = BuildConfig.GOOGLE_API_KEY
        if (apiKey.isBlank()) {
            Log.w("HomeViewModel", "GOOGLE_API_KEY is not configured in local.properties. Falling back to built-in Geocoder.")
            return null
        }
        if (!Places.isInitialized()) {
            try {
                Places.initialize(app.applicationContext, apiKey)
                Log.i("HomeViewModel", "Google Places initialized successfully.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to initialize Google Places: ${e.message}", e)
                return null
            }
        }
        return try {
            Places.createClient(app)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to create PlacesClient: ${e.message}", e)
            null
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _uiState.update { it.copy(predictions = emptyList(), isLoadingPredictions = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            fetchPredictions(query.trim())
        }
    }

    private fun fetchPredictions(query: String) {
        val client = getPlacesClient()
        if (client == null) {
            fetchGeocoderPredictions(query)
            return
        }

        _uiState.update { it.copy(isLoadingPredictions = true) }

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val list = response.autocompletePredictions.map {
                    PlaceSuggestion(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString()
                    )
                }
                if (list.isNotEmpty()) {
                    _uiState.update {
                        it.copy(predictions = list, isLoadingPredictions = false)
                    }
                } else {
                    fetchGeocoderPredictions(query)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeViewModel", "Google Places findAutocompletePredictions failed: ${exception.message}", exception)
                // Fallback to Android Geocoder so user still gets suggestions
                fetchGeocoderPredictions(query)
            }
    }

    private fun fetchGeocoderPredictions(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 5) ?: emptyList()
                val list = addresses.mapNotNull { address ->
                    val primary = address.locality
                        ?: address.subLocality
                        ?: address.featureName
                        ?: query
                    val secondaryParts = mutableListOf<String>()
                    address.postalCode?.let { secondaryParts.add(it) }
                    if (address.locality != null && address.locality != primary) {
                        secondaryParts.add(address.locality)
                    }
                    if (address.adminArea != null && address.adminArea != primary) {
                        secondaryParts.add(address.adminArea)
                    }
                    address.countryName?.let { secondaryParts.add(it) }
                    val secondary = secondaryParts.joinToString(", ")

                    PlaceSuggestion(
                        placeId = "geo:${address.latitude},${address.longitude}",
                        primaryText = primary,
                        secondaryText = secondary,
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(predictions = list, isLoadingPredictions = false) }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Geocoder fallback error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoadingPredictions = false) }
                }
            }
        }
    }

    fun selectPrediction(suggestion: PlaceSuggestion, onNavigate: (Double, Double, String) -> Unit) {
        // If already resolved with coordinates (e.g. Geocoder)
        if (suggestion.latitude != null && suggestion.longitude != null) {
            _uiState.update { it.copy(searchQuery = suggestion.primaryText, predictions = emptyList()) }
            onNavigate(suggestion.latitude, suggestion.longitude, suggestion.primaryText)
            return
        }

        val client = getPlacesClient()
        if (client == null) {
            performSearch(suggestion.primaryText, onNavigate)
            return
        }

        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(suggestion.placeId, fields)

        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng
                if (latLng != null) {
                    _uiState.update { it.copy(searchQuery = suggestion.primaryText, predictions = emptyList()) }
                    onNavigate(latLng.latitude, latLng.longitude, suggestion.primaryText)
                } else {
                    performSearch(suggestion.primaryText, onNavigate)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeViewModel", "Google Places fetchPlace failed: ${exception.message}", exception)
                performSearch(suggestion.primaryText, onNavigate)
            }
    }

    fun performSearch(query: String, onNavigate: (Double, Double, String) -> Unit) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Bitte gib einen Ort ein, um danach zu suchen.") }
            return
        }

        val firstMatch = _uiState.value.predictions.firstOrNull()
        if (firstMatch != null) {
            selectPrediction(firstMatch, onNavigate)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(trimmed, 1)
                val first = results?.firstOrNull()
                withContext(Dispatchers.Main) {
                    if (first != null) {
                        val name = first.locality ?: first.featureName ?: trimmed
                        _uiState.update { it.copy(searchQuery = name, predictions = emptyList()) }
                        onNavigate(first.latitude, first.longitude, name)
                    } else {
                        _uiState.update { it.copy(errorMessage = "Ort \"$trimmed\" konnte nicht gefunden werden.") }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "performSearch geocoding error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "Suche fehlgeschlagen: ${e.message}") }
                }
            }
        }
    }

    fun searchCurrentLocation(onNavigate: (Double, Double, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, errorMessage = null) }
            val location = locationTracker.getCurrentLocation()
            _uiState.update { it.copy(isLoadingLocation = false) }

            if (location != null) {
                onNavigate(location.latitude, location.longitude, "Aktueller Standort")
            } else {
                _uiState.update { it.copy(errorMessage = "Standort konnte nicht ermittelt werden.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
