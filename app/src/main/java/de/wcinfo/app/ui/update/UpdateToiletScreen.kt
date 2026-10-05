package de.wcinfo.app.ui.update

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import de.wcinfo.app.R
import de.wcinfo.app.data.model.GooglePlacesPeriod
import de.wcinfo.app.data.model.NearbyPlaceOption
import de.wcinfo.app.data.model.PlaceResource
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.model.UpdateToiletPayload
import de.wcinfo.app.data.repository.ToiletRepository
import de.wcinfo.app.ui.components.EuroKeyInfoBottomSheet
import de.wcinfo.app.ui.create.OpeningTimesInput
import de.wcinfo.app.ui.theme.MapStyles
import de.wcinfo.app.ui.theme.PrimaryPurple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateToiletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ToiletRepository()

    data class UiState(
        val toilet: Toilet? = null,
        val nearbyPlaces: List<NearbyPlaceOption> = emptyList(),
        val fetchedPlaceResources: Map<String, PlaceResource> = emptyMap(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getToilet(id).onSuccess { t ->
                _uiState.update { it.copy(toilet = t, isLoading = false) }
                loadNearbyPlaces(t.lat, t.lon)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = getApplication<Application>().getString(R.string.error_loading)
                    )
                }
            }
        }
    }

    private fun loadNearbyPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            repository.fetchNearestPlaces(lat, lon, radius = 300.0, limit = 10)
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
                            nearbyPlaces = options,
                            fetchedPlaceResources = resourceMap
                        )
                    }
                }
        }
    }

    fun save(id: Int, payload: UpdateToiletPayload) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateToilet(id, payload).onSuccess {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.localizedMessage ?: getApplication<Application>().getString(R.string.error_saving)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateToiletScreen(
    toiletId: Int,
    onBackClick: () -> Unit,
    viewModel: UpdateToiletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showEuroKeyInfo by remember { mutableStateOf(false) }

    LaunchedEffect(toiletId) {
        viewModel.load(toiletId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBackClick()
        }
    }

    if (showEuroKeyInfo) {
        EuroKeyInfoBottomSheet(
            onDismiss = { showEuroKeyInfo = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (uiState.toilet != null) {
            val toilet = uiState.toilet!!

            var belongsToPlace by remember { mutableStateOf(!toilet.placeId.isNullOrBlank() || toilet.owner.isNotBlank()) }
            var selectedPlaceId by remember { mutableStateOf(toilet.placeId) }
            var selectedPlaceName by remember { mutableStateOf(toilet.owner.ifBlank { toilet.name }) }
            var showPlaceDropdown by remember { mutableStateOf(false) }

            var name by remember { mutableStateOf(toilet.name) }
            var isGenderSeparated by remember { mutableStateOf(toilet.isGenderSeparated) }
            var hasChangingTable by remember { mutableStateOf(toilet.hasChangingTable) }
            var hasWheelchair by remember { mutableStateOf(toilet.hasWheelchairAccess) }
            var hasEuroKey by remember { mutableStateOf(toilet.hasEuroKey) }
            var publicAccessible by remember { mutableStateOf(toilet.publicAccessible) }
            var accessibleOutsideOpeningTimes by remember { mutableStateOf(toilet.accessibleOutsideOpeningTimes) }
            var storageSpace by remember { mutableStateOf(toilet.storageSpace) }

            var showOpeningTimes by remember { mutableStateOf(!toilet.placeOpeningHours.isNullOrEmpty()) }
            var placeOpeningHours by remember { mutableStateOf<List<GooglePlacesPeriod>?>(toilet.placeOpeningHours) }

            var selectedCoordinate by remember { mutableStateOf(LatLng(toilet.lat, toilet.lon)) }
            var confirmedCoordinate by remember { mutableStateOf(LatLng(toilet.lat, toilet.lon)) }
            var isSatellite by remember { mutableStateOf(false) }

            var address by remember { mutableStateOf(toilet.address ?: "") }
            var website by remember { mutableStateOf(toilet.website ?: "") }
            var comment by remember { mutableStateOf(toilet.comment ?: "") }

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(selectedCoordinate, 16f)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Drag handle indicator
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                // Modal Header: Cancel button + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onBackClick() }
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = stringResource(id = R.string.suggest_changes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row 1: Place Association
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { belongsToPlace = !belongsToPlace }
                    ) {
                        Checkbox(
                            checked = belongsToPlace,
                            onCheckedChange = { belongsToPlace = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.toilet_belongs_to_place),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (belongsToPlace) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, PrimaryPurple),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPlaceDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedPlaceName.ifBlank { stringResource(id = R.string.select_place) },
                                        color = PrimaryPurple,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showPlaceDropdown,
                                onDismissRequest = { showPlaceDropdown = false }
                            ) {
                                if (toilet.owner.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text(toilet.owner, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedPlaceId = toilet.placeId
                                            selectedPlaceName = toilet.owner
                                            showPlaceDropdown = false
                                        }
                                    )
                                }

                                uiState.nearbyPlaces.forEach { place ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(place.name, fontWeight = FontWeight.Medium)
                                                if (!place.secondaryText.isNullOrBlank()) {
                                                    Text(
                                                        place.secondaryText,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedPlaceId = place.id
                                            selectedPlaceName = place.name
                                            val resource = uiState.fetchedPlaceResources[place.id]
                                            if (resource?.resolvedAddress != null && address.isBlank()) {
                                                address = resource.resolvedAddress ?: ""
                                            }
                                            if (resource?.resolvedWebsite != null && website.isBlank()) {
                                                website = resource.resolvedWebsite ?: ""
                                            }
                                            if (!resource?.resolvedOpeningHours.isNullOrEmpty() && placeOpeningHours.isNullOrEmpty()) {
                                                placeOpeningHours = resource?.resolvedOpeningHours
                                                showOpeningTimes = true
                                            }
                                            showPlaceDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Name der Toilette (optional)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.toilet_name_optional),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text(
                                text = "WC #$toiletId",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Eigenschaften (Features)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.features_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 3 Feature Cards in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Gender separated
                        FeatureCard(
                            icon = Icons.Default.People,
                            title = stringResource(id = R.string.gender_separated_title),
                            checked = isGenderSeparated,
                            onCheckedChange = { isGenderSeparated = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Changing table
                        FeatureCard(
                            icon = Icons.Default.ChildCare,
                            title = stringResource(id = R.string.changing_table_title),
                            checked = hasChangingTable,
                            onCheckedChange = { hasChangingTable = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Card 3: Wheelchair
                        FeatureCard(
                            icon = Icons.AutoMirrored.Filled.Accessible,
                            title = stringResource(id = R.string.wheelchair_access_title),
                            checked = hasWheelchair,
                            onCheckedChange = {
                                hasWheelchair = it
                                if (!it) hasEuroKey = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Euro-Key Row (Dependent on wheelchair access)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (hasWheelchair) 1f else 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = hasWheelchair) {
                                        hasEuroKey = !hasEuroKey
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (hasWheelchair && hasEuroKey) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.euro_key_title),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { showEuroKeyInfo = true }
                                )
                            }

                            Checkbox(
                                checked = hasEuroKey,
                                onCheckedChange = { if (hasWheelchair) hasEuroKey = it },
                                enabled = hasWheelchair,
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                            )
                        }
                    }
                }

                // Checkboxes: Public & Outside opening times
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { publicAccessible = !publicAccessible }
                    ) {
                        Checkbox(
                            checked = publicAccessible,
                            onCheckedChange = { publicAccessible = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.public_accessible_yes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accessibleOutsideOpeningTimes = !accessibleOutsideOpeningTimes }
                    ) {
                        Checkbox(
                            checked = accessibleOutsideOpeningTimes,
                            onCheckedChange = { accessibleOutsideOpeningTimes = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.open_status_accessible_outside),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Ablagefläche (Storage Space)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.storage_space_label),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val options = listOf(
                            "none" to stringResource(id = R.string.storage_none),
                            "little" to stringResource(id = R.string.storage_little),
                            "much" to stringResource(id = R.string.storage_much)
                        )

                        options.forEach { (key, label) ->
                            val isSelected = storageSpace == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        storageSpace = if (storageSpace == key) null else key
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Öffnungszeiten angeben Checkbox & Editor
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOpeningTimes = !showOpeningTimes }
                    ) {
                        Checkbox(
                            checked = showOpeningTimes,
                            onCheckedChange = { showOpeningTimes = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.provide_opening_hours),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = showOpeningTimes) {
                        OpeningTimesInput(
                            periods = placeOpeningHours,
                            onPeriodsChanged = { placeOpeningHours = it }
                        )
                    }
                }

                // Standort auf der Karte (Location on Map)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.location_on_map_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                mapType = if (isSatellite) MapType.HYBRID else MapType.NORMAL,
                                mapStyleOptions = if (!isSatellite && isDark) MapStyles.DarkMapStyleOptions else null
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = false
                            ),
                            onMapClick = { latLng ->
                                selectedCoordinate = latLng
                            }
                        ) {
                            Marker(
                                state = MarkerState(position = selectedCoordinate),
                                title = name.ifBlank { "Toilette" }
                            )
                        }

                        // Map Layer toggle button top right
                        IconButton(
                            onClick = { isSatellite = !isSatellite },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Layer",
                                tint = if (isSatellite) PrimaryPurple else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Floating position overlay card at bottom
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.selected_position),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Lat: %.5f, Lon: %.5f".format(selectedCoordinate.latitude, selectedCoordinate.longitude),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        confirmedCoordinate = selectedCoordinate
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.choose_this_position),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Adresse (Address)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.address_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Webseite (Website)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.website_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextField(
                        value = website,
                        onValueChange = { website = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Bemerkung (Comment / Notes)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.notes_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        minLines = 3
                    )
                }

                // Error message (if any)
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Button: Save changes
                Button(
                    onClick = {
                        val payload = UpdateToiletPayload(
                            name = name.trim().ifBlank { null },
                            owner = if (belongsToPlace) selectedPlaceName.trim().ifBlank { null } else null,
                            lat = confirmedCoordinate.latitude,
                            lon = confirmedCoordinate.longitude,
                            placeId = if (belongsToPlace) selectedPlaceId else null,
                            isUnisex = !isGenderSeparated,
                            isGenderSeparated = isGenderSeparated,
                            hasWheelchairAccess = hasWheelchair,
                            hasChangingTable = hasChangingTable,
                            accessibleOutsideOpeningTimes = accessibleOutsideOpeningTimes,
                            publicAccessible = publicAccessible,
                            placeOpeningHours = if (showOpeningTimes) placeOpeningHours else null,
                            address = address.trim().ifBlank { null },
                            website = website.trim().ifBlank { null },
                            comment = comment.trim().ifBlank { null },
                            euroKey = if (hasWheelchair) (if (hasEuroKey) "yes" else "no") else null,
                            storageSpace = storageSpace,
                            status = "active"
                        )
                        viewModel.save(toiletId, payload)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.save_changes_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (checked) PrimaryPurple.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .height(145.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                )
            }
        }
    }
}
