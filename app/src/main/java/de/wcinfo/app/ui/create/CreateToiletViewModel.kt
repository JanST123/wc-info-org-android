package de.wcinfo.app.ui.create

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import de.wcinfo.app.data.model.AddToiletPayload
import de.wcinfo.app.data.model.GooglePlacesPeriod
import de.wcinfo.app.data.model.NearbyPlaceOption
import de.wcinfo.app.data.model.PlaceDetails
import de.wcinfo.app.data.model.PlaceResource
import de.wcinfo.app.data.model.UpdateToiletPayload
import de.wcinfo.app.data.repository.ToiletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class QuestionStep(val id: Int) {
    PLACE(1),
    NAME(2),
    GPS_LOCATION(3),
    MAP_PICKER(4),
    GENDER_SEPARATED(5),
    WHEELCHAIR(6),
    EURO_KEY(7),
    ADDRESS(8),
    OPENING_TIMES(9),
    PUBLIC_ACCESS(10),
    OUTSIDE_OPENING_TIMES(11),
    STORAGE_SPACE(12),
    PHOTOS(13),
    COMMENT(14)
}

data class UploadedPhoto(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val isUploading: Boolean = false,
    val isSuccess: Boolean = false,
    val isFailed: Boolean = false,
    val filename: String? = null
)

data class CreateToiletUiState(
    val currentStep: QuestionStep = QuestionStep.PLACE,
    val stepHistory: List<QuestionStep> = emptyList(),
    val activeSteps: List<QuestionStep> = listOf(QuestionStep.PLACE),
    val initialCoordinate: LatLng? = null,
    val currentResultCoordinate: LatLng? = null,
    
    // Step 1: Places
    val nearbyPlaces: List<NearbyPlaceOption> = emptyList(),
    val fetchedPlaceResources: Map<String, PlaceResource> = emptyMap(),
    val isLoadingPlaces: Boolean = false,
    val selectedPlace: NearbyPlaceOption? = null,
    val selectedPlaceDetails: PlaceDetails? = null,
    val isNoneOfThesePlaces: Boolean = false,

    // Step 2: Name
    val toiletName: String = "",

    // Step 3: GPS
    val usedGpsPosition: Boolean = false,
    val gpsCoordinate: LatLng? = null,

    // Step 4: Map Picker
    val mapSelectedCoordinate: LatLng? = null,

    // Step 5: Gender
    val isGenderSeparated: Boolean? = null,
    val isUnisex: Boolean? = null,

    // Step 6: Wheelchair
    val hasWheelchairAccess: Boolean? = null,

    // Step 7: Euro Key
    val euroKey: String? = null,
    val showEuroKeyInfo: Boolean = false,

    // Step 8: Address
    val addressInput: String = "",

    // Step 9: Opening times
    val isEnteringOpeningTimes: Boolean = false,
    val placeOpeningHours: List<GooglePlacesPeriod>? = null,

    // Step 10: Public access
    val publicAccessible: Boolean? = null,

    // Step 11: Outside opening times
    val accessibleOutsideOpeningTimes: Boolean? = null,

    // Step 12: Storage
    val storageSpace: String? = null, // "none", "little", "much", null

    // Step 13: Photos
    val uploadedPhotos: List<UploadedPhoto> = emptyList(),

    // Step 14: Comment
    val commentInput: String = "",

    // Meta & Lifecycle
    val createdToiletId: Int? = null,
    val isSubmitting: Boolean = false,
    val isToiletSaved: Boolean = false,
    val showCelebration: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
    val showCompanyDialog: Boolean = false
)

class CreateToiletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ToiletRepository()

    private val _uiState = MutableStateFlow(CreateToiletUiState())
    val uiState: StateFlow<CreateToiletUiState> = _uiState.asStateFlow()

    fun setInitialData(initialLat: Double?, initialLon: Double?, resultLat: Double? = null, resultLon: Double? = null) {
        val initialCoord = if (initialLat != null && initialLon != null) LatLng(initialLat, initialLon) else null
        val resultCoord = if (resultLat != null && resultLon != null) LatLng(resultLat, resultLon) else initialCoord ?: LatLng(52.5200, 13.4050)

        _uiState.update {
            it.copy(
                initialCoordinate = initialCoord,
                currentResultCoordinate = resultCoord,
                mapSelectedCoordinate = initialCoord ?: resultCoord
            )
        }
        recalculateSteps()
        loadNearbyPlaces()
    }

    fun loadNearbyPlaces() {
        val state = _uiState.value
        val coord = state.initialCoordinate ?: state.currentResultCoordinate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaces = true) }
            repository.fetchNearestPlaces(coord.latitude, coord.longitude, radius = 200.0, limit = 10)
                .onSuccess { places ->
                    val options = mutableListOf<NearbyPlaceOption>()
                    val resourceMap = mutableMapOf<String, PlaceResource>()
                    for (place in places) {
                        val option = place.toNearbyPlaceOption()
                        if (option != null) {
                            options.add(option)
                            resourceMap[option.id] = place
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingPlaces = false,
                            nearbyPlaces = options,
                            fetchedPlaceResources = resourceMap
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingPlaces = false) }
                }
        }
    }

    private fun determineActiveSteps(state: CreateToiletUiState = _uiState.value): List<QuestionStep> {
        val steps = mutableListOf<QuestionStep>()

        // Q1: Place
        steps.add(QuestionStep.PLACE)
        // Q2: Name
        steps.add(QuestionStep.NAME)

        // Q3 & Q4: GPS / Map Picker (Skipped if coordinate already provided at start)
        if (state.initialCoordinate == null) {
            steps.add(QuestionStep.GPS_LOCATION)

            // Q4 Map Picker: Only if no place selected and no GPS position used
            if (state.selectedPlace == null && state.gpsCoordinate == null) {
                steps.add(QuestionStep.MAP_PICKER)
            }
        }

        // Q5: Gender separated
        steps.add(QuestionStep.GENDER_SEPARATED)
        // Q6: Wheelchair
        steps.add(QuestionStep.WHEELCHAIR)

        // Q7: Euro Key (Skipped if wheelchair == false)
        if (state.hasWheelchairAccess != false) {
            steps.add(QuestionStep.EURO_KEY)
        }

        // Q8: Address (Skipped if selected place has formatted address)
        if (state.selectedPlace == null || state.selectedPlaceDetails?.formattedAddress.isNullOrBlank()) {
            steps.add(QuestionStep.ADDRESS)
        }

        // Q9: Opening times (Skipped if selected place has opening hours)
        if (state.selectedPlace == null || state.selectedPlaceDetails?.openingHours.isNullOrEmpty()) {
            steps.add(QuestionStep.OPENING_TIMES)
        }

        // Q10 & Q11: Public access & Outside opening times (Only if opening hours are available)
        val hasOpeningTimes = !state.placeOpeningHours.isNullOrEmpty() ||
                !state.selectedPlaceDetails?.openingHours.isNullOrEmpty()
        if (hasOpeningTimes) {
            steps.add(QuestionStep.PUBLIC_ACCESS)
            steps.add(QuestionStep.OUTSIDE_OPENING_TIMES)
        }

        // Q12: Storage space
        steps.add(QuestionStep.STORAGE_SPACE)
        // Q13: Photos
        steps.add(QuestionStep.PHOTOS)
        // Q14: Comment
        steps.add(QuestionStep.COMMENT)

        return steps
    }

    private fun recalculateSteps() {
        _uiState.update { state ->
            val active = determineActiveSteps(state)
            val current = if (active.contains(state.currentStep)) state.currentStep else active.firstOrNull() ?: QuestionStep.PLACE
            state.copy(activeSteps = active, currentStep = current)
        }
    }

    // Step 1: Place selection
    fun selectPlace(place: NearbyPlaceOption?) {
        val state = _uiState.value
        val details = if (place != null) {
            state.fetchedPlaceResources[place.id]?.toPlaceDetails()
        } else null

        val updatedAddress = if (state.addressInput.isBlank() && details?.formattedAddress != null) {
            details.formattedAddress
        } else state.addressInput

        val updatedHours = if (state.placeOpeningHours == null && !details?.openingHours.isNullOrEmpty()) {
            details.openingHours
        } else state.placeOpeningHours

        _uiState.update {
            it.copy(
                selectedPlace = place,
                selectedPlaceDetails = details,
                isNoneOfThesePlaces = (place == null),
                addressInput = updatedAddress,
                placeOpeningHours = updatedHours
            )
        }
        advanceToNextStep()
    }

    // Step 2: Name
    fun updateToiletName(name: String) {
        _uiState.update { it.copy(toiletName = name) }
    }

    // Step 3: GPS
    fun setGpsLocation(useGps: Boolean, userCoord: LatLng?) {
        _uiState.update {
            it.copy(
                usedGpsPosition = useGps,
                gpsCoordinate = if (useGps) userCoord ?: it.currentResultCoordinate else null
            )
        }
        advanceToNextStep()
    }

    // Step 4: Map Picker
    fun updateMapPickerCoordinate(latLng: LatLng) {
        _uiState.update { it.copy(mapSelectedCoordinate = latLng) }
    }

    fun confirmMapPickerLocation() {
        advanceToNextStep()
    }

    // Step 5: Gender
    fun setGenderSeparated(isSeparated: Boolean) {
        _uiState.update {
            it.copy(
                isGenderSeparated = isSeparated,
                isUnisex = !isSeparated
            )
        }
        advanceToNextStep()
    }

    // Step 6: Wheelchair
    fun setWheelchairAccess(hasAccess: Boolean) {
        _uiState.update {
            it.copy(
                hasWheelchairAccess = hasAccess,
                euroKey = if (!hasAccess) null else it.euroKey
            )
        }
        advanceToNextStep()
    }

    // Step 7: Euro Key
    fun setEuroKey(key: String) {
        _uiState.update { it.copy(euroKey = key) }
        advanceToNextStep()
    }

    fun setShowEuroKeyInfo(show: Boolean) {
        _uiState.update { it.copy(showEuroKeyInfo = show) }
    }

    // Step 8: Address
    fun updateAddressInput(text: String) {
        _uiState.update { it.copy(addressInput = text) }
    }

    fun saveAddressAndContinue() {
        patchCurrentState()
        advanceToNextStep()
    }

    fun skipAddressAndContinue() {
        advanceToNextStep()
    }

    // Step 9: Opening times
    fun setIsEnteringOpeningTimes(entering: Boolean) {
        _uiState.update { it.copy(isEnteringOpeningTimes = entering) }
    }

    fun updateOpeningHours(periods: List<GooglePlacesPeriod>?) {
        _uiState.update { it.copy(placeOpeningHours = periods) }
    }

    fun saveOpeningHoursAndContinue() {
        _uiState.update { it.copy(isEnteringOpeningTimes = false) }
        patchCurrentState()
        advanceToNextStep()
    }

    fun skipOpeningHoursAndContinue() {
        _uiState.update {
            it.copy(
                isEnteringOpeningTimes = false,
                placeOpeningHours = null
            )
        }
        patchCurrentState()
        advanceToNextStep()
    }

    // Step 10: Public Access
    fun setPublicAccessible(isPublic: Boolean) {
        _uiState.update { it.copy(publicAccessible = isPublic) }
        patchCurrentState()
        advanceToNextStep()
    }

    // Step 11: Outside Opening Times
    fun setAccessibleOutsideOpeningTimes(accessible: Boolean) {
        _uiState.update { it.copy(accessibleOutsideOpeningTimes = accessible) }
        patchCurrentState()
        advanceToNextStep()
    }

    // Step 12: Storage
    fun setStorageSpace(space: String?) {
        _uiState.update { it.copy(storageSpace = space) }
    }

    fun saveStorageAndContinue() {
        patchCurrentState()
        advanceToNextStep()
    }

    // Step 13: Photos
    fun uploadPhotoUri(context: Context, uri: Uri) {
        val photoItem = UploadedPhoto(uri = uri, isUploading = true)
        _uiState.update { it.copy(uploadedPhotos = it.uploadedPhotos + photoItem) }

        val toiletId = _uiState.value.createdToiletId
        viewModelScope.launch {
            val file = uriToFile(context, uri)
            if (file != null) {
                repository.uploadPhoto(toiletId, file)
                    .onSuccess { response ->
                        _uiState.update { state ->
                            state.copy(
                                uploadedPhotos = state.uploadedPhotos.map {
                                    if (it.id == photoItem.id) it.copy(isUploading = false, isSuccess = true, filename = response.filename)
                                    else it
                                }
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(
                                uploadedPhotos = state.uploadedPhotos.map {
                                    if (it.id == photoItem.id) it.copy(isUploading = false, isFailed = true)
                                    else it
                                }
                            )
                        }
                    }
            } else {
                _uiState.update { state ->
                    state.copy(
                        uploadedPhotos = state.uploadedPhotos.map {
                            if (it.id == photoItem.id) it.copy(isUploading = false, isFailed = true)
                            else it
                        }
                    )
                }
            }
        }
    }

    fun removePhoto(photo: UploadedPhoto) {
        _uiState.update { state ->
            state.copy(uploadedPhotos = state.uploadedPhotos.filter { it.id != photo.id })
        }
        val toiletId = _uiState.value.createdToiletId
        val filename = photo.filename
        if (toiletId != null && filename != null) {
            viewModelScope.launch {
                repository.deletePhoto(toiletId, filename)
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    // Step 14: Comment
    fun updateCommentInput(comment: String) {
        _uiState.update { it.copy(commentInput = comment) }
    }

    fun saveCommentAndFinish() {
        val trimmed = _uiState.value.commentInput.trim()
        if (trimmed.isNotEmpty()) {
            patchCurrentState()
        }
        finishWithCelebration()
    }

    fun setShowCompanyDialog(show: Boolean) {
        _uiState.update { it.copy(showCompanyDialog = show) }
    }

    // Step Navigation
    fun advanceToNextStep() {
        val state = _uiState.value
        val activeSteps = determineActiveSteps(state)
        val currentIndex = activeSteps.indexOf(state.currentStep)

        // Check if completing mandatory questions (Step 7 EURO_KEY, or Step 6 WHEELCHAIR if hasWheelchairAccess == false)
        val isCompletingMandatory = (state.currentStep == QuestionStep.EURO_KEY) ||
                (state.currentStep == QuestionStep.WHEELCHAIR && state.hasWheelchairAccess == false)

        if (isCompletingMandatory && state.createdToiletId == null) {
            submitMandatoryToilet()
            return
        }

        if (state.createdToiletId != null) {
            patchCurrentState()
        }

        if (currentIndex >= 0 && currentIndex + 1 < activeSteps.size) {
            val next = activeSteps[currentIndex + 1]
            _uiState.update {
                it.copy(
                    currentStep = next,
                    stepHistory = it.stepHistory + state.currentStep,
                    activeSteps = activeSteps,
                    isEnteringOpeningTimes = false
                )
            }
        } else {
            finishWithCelebration()
        }
    }

    fun goBack() {
        val state = _uiState.value
        if (state.currentStep == QuestionStep.OPENING_TIMES && state.isEnteringOpeningTimes) {
            _uiState.update { it.copy(isEnteringOpeningTimes = false) }
            return
        }
        if (state.stepHistory.isNotEmpty()) {
            val prev = state.stepHistory.last()
            val newHistory = state.stepHistory.dropLast(1)
            _uiState.update {
                it.copy(
                    currentStep = prev,
                    stepHistory = newHistory,
                    isEnteringOpeningTimes = false
                )
            }
            recalculateSteps()
        }
    }

    fun finishWithCelebration() {
        _uiState.update { it.copy(showCelebration = true) }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(isCompleted = true, showCelebration = false) }
    }

    private fun resolveCoordinates(): Pair<Double?, Double?> {
        val state = _uiState.value
        if (state.initialCoordinate != null) {
            return state.initialCoordinate.latitude to state.initialCoordinate.longitude
        }
        if (state.gpsCoordinate != null) {
            return state.gpsCoordinate.latitude to state.gpsCoordinate.longitude
        }
        if (state.mapSelectedCoordinate != null) {
            return state.mapSelectedCoordinate.latitude to state.mapSelectedCoordinate.longitude
        }
        if (state.selectedPlaceDetails?.latitude != null && state.selectedPlaceDetails.longitude != null) {
            return state.selectedPlaceDetails.latitude to state.selectedPlaceDetails.longitude
        }
        if (state.currentResultCoordinate != null) {
            return state.currentResultCoordinate.latitude to state.currentResultCoordinate.longitude
        }
        return null to null
    }

    private fun submitMandatoryToilet() {
        val state = _uiState.value
        val (lat, lon) = resolveCoordinates()
        val trimmedName = state.toiletName.trim()
        val trimmedAddress = state.addressInput.trim()

        val payload = AddToiletPayload(
            name = trimmedName.ifBlank { null },
            owner = state.selectedPlace?.name,
            lat = lat ?: 52.5200,
            lon = lon ?: 13.4050,
            placeId = state.selectedPlace?.id,
            isUnisex = state.isUnisex,
            isGenderSeparated = state.isGenderSeparated,
            hasWheelchairAccess = state.hasWheelchairAccess,
            hasChangingTable = null,
            accessibleOutsideOpeningTimes = null,
            publicAccessible = null,
            placeOpeningHours = state.placeOpeningHours ?: state.selectedPlaceDetails?.openingHours,
            address = trimmedAddress.ifBlank { state.selectedPlaceDetails?.formattedAddress },
            website = state.selectedPlaceDetails?.website,
            comment = null,
            euroKey = state.euroKey,
            storageSpace = null,
            status = "active"
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.addToilet(payload)
                .onSuccess { response ->
                    _uiState.update { current ->
                        val active = determineActiveSteps(current.copy(createdToiletId = response.id, isToiletSaved = true))
                        val currentIdx = active.indexOf(current.currentStep)
                        val nextStep = if (currentIdx >= 0 && currentIdx + 1 < active.size) active[currentIdx + 1] else current.currentStep
                        val nextHistory = if (currentIdx >= 0 && currentIdx + 1 < active.size) current.stepHistory + current.currentStep else current.stepHistory
                        current.copy(
                            createdToiletId = response.id,
                            isSubmitting = false,
                            isToiletSaved = true,
                            currentStep = nextStep,
                            stepHistory = nextHistory,
                            activeSteps = active
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.localizedMessage ?: "Fehler beim Speichern"
                        )
                    }
                }
        }
    }

    private fun patchCurrentState() {
        val state = _uiState.value
        val toiletId = state.createdToiletId ?: return
        val (lat, lon) = resolveCoordinates()
        val trimmedName = state.toiletName.trim()
        val trimmedAddress = state.addressInput.trim()
        val trimmedComment = state.commentInput.trim()

        val payload = UpdateToiletPayload(
            name = trimmedName.ifBlank { null },
            owner = state.selectedPlace?.name,
            lat = lat,
            lon = lon,
            placeId = state.selectedPlace?.id,
            isUnisex = state.isUnisex,
            isGenderSeparated = state.isGenderSeparated,
            hasWheelchairAccess = state.hasWheelchairAccess,
            hasChangingTable = null,
            accessibleOutsideOpeningTimes = state.accessibleOutsideOpeningTimes,
            publicAccessible = state.publicAccessible,
            placeOpeningHours = state.placeOpeningHours ?: state.selectedPlaceDetails?.openingHours,
            address = trimmedAddress.ifBlank { null },
            website = state.selectedPlaceDetails?.website,
            comment = trimmedComment.ifBlank { null },
            euroKey = state.euroKey,
            storageSpace = state.storageSpace,
            status = "active"
        )

        viewModelScope.launch {
            repository.updateToilet(toiletId, payload)
        }
    }
}
