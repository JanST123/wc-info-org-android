package de.wcinfo.app.ui.compass

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.wcinfo.app.data.compass.CompassData
import de.wcinfo.app.data.compass.CompassSensorManager
import de.wcinfo.app.data.location.LocationTracker
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.repository.ToiletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompassUiState(
    val toilet: Toilet? = null,
    val compassData: CompassData = CompassData(0f, 0f, 0f, 0f),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ToiletRepository()
    private val compassSensor = CompassSensorManager(application)
    private val locationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private var currentHeading: Float = 0f
    private var currentUserLocation: Location? = null

    fun initCompass(toiletId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getToilet(toiletId)
                .onSuccess { toilet ->
                    _uiState.update { it.copy(toilet = toilet, isLoading = false) }
                    startTracking(toilet)
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

    private fun startTracking(toilet: Toilet) {
        // Collect heading updates
        viewModelScope.launch {
            compassSensor.getHeadingUpdates().collect { heading ->
                currentHeading = heading
                updateCompassState(toilet)
            }
        }

        // Collect location updates
        viewModelScope.launch {
            locationTracker.getLocationUpdates(2000L).collect { location ->
                currentUserLocation = location
                updateCompassState(toilet)
            }
        }
    }

    private fun updateCompassState(toilet: Toilet) {
        val userLoc = currentUserLocation
        val distanceMeters = if (userLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(userLoc.latitude, userLoc.longitude, toilet.lat, toilet.lon, results)
            results[0]
        } else {
            ((toilet.distance ?: 0.0) * 1000.0).toFloat()
        }

        val bearing = if (userLoc != null) {
            CompassSensorManager.calculateBearing(userLoc.latitude, userLoc.longitude, toilet.lat, toilet.lon)
        } else {
            0f
        }

        val relativeAngle = (bearing - currentHeading + 360f) % 360f

        _uiState.update {
            it.copy(
                compassData = CompassData(
                    deviceHeading = currentHeading,
                    targetBearing = bearing,
                    relativeAngle = relativeAngle,
                    distanceMeters = distanceMeters
                )
            )
        }
    }
}
