package de.wcinfo.app.ui.results

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import de.wcinfo.app.R
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.model.ToiletPhoto
import de.wcinfo.app.ui.components.NavigationChoiceBottomSheet
import de.wcinfo.app.ui.components.PhotoLightbox
import de.wcinfo.app.ui.results.components.FilterBanner
import de.wcinfo.app.ui.results.components.QualifiedBadge
import de.wcinfo.app.ui.results.components.ToiletCard
import de.wcinfo.app.ui.theme.AccentMagenta
import de.wcinfo.app.ui.theme.MapStyles
import de.wcinfo.app.ui.theme.PrimaryPurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    latitude: Double,
    longitude: Double,
    locationName: String,
    initialToiletId: Int? = null,
    onBackClick: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToCompass: (Int) -> Unit,
    onNavigateToCreate: (Double?, Double?, Double?, Double?) -> Unit,
    viewModel: ResultsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val searchLatLng = remember { LatLng(latitude, longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(searchLatLng, 15f)
    }

    var navigationTargetToilet by remember { mutableStateOf<Toilet?>(null) }

    var portraitRatio by remember { mutableFloatStateOf(0.62f) }
    var landscapeRatio by remember { mutableFloatStateOf(0.48f) }
    var isDraggingSplit by remember { mutableStateOf(false) }

    var lightboxToilet by remember { mutableStateOf<Toilet?>(null) }
    var lightboxPhotoIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.initLocation(latitude, longitude, initialToiletId)
    }

    // Scroll to selected toilet in the list
    LaunchedEffect(uiState.selectedToilet) {
        uiState.selectedToilet?.let { selected ->
            val index = uiState.toilets.indexOfFirst { it.id == selected.id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Camera idle bounds fetching
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            if (bounds != null) {
                viewModel.onCameraIdle(
                    south = bounds.southwest.latitude,
                    west = bounds.southwest.longitude,
                    north = bounds.northeast.latitude,
                    east = bounds.northeast.longitude
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = locationName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight
            val density = LocalDensity.current
            val totalHeightPx = with(density) { maxHeight.toPx() }
            val totalWidthPx = with(density) { maxWidth.toPx() }

            if (isLandscape) {
                // Tablet / Landscape Split View (List Left, Map Right with Draggable Handle)
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(landscapeRatio)
                    ) {
                        ToiletListPane(
                            uiState = uiState,
                            listState = listState,
                            onToiletSelect = { toilet ->
                                viewModel.selectToilet(toilet)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(toilet.lat, toilet.lon), 16f
                                        )
                                    )
                                }
                            },
                            onNavigateClick = { navigationTargetToilet = it },
                            onDetailClick = { onNavigateToDetail(it.id) },
                            onFilterChange = { viewModel.updateFilters(it) },
                            onAddToiletClick = { onNavigateToCreate(null, null, latitude, longitude) },
                            onPhotoClick = { toilet, photoIndex ->
                                lightboxToilet = toilet
                                lightboxPhotoIndex = photoIndex
                            }
                        )
                    }

                    // Draggable Vertical Split Handle
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { deltaPx ->
                                    if (totalWidthPx > 0) {
                                        landscapeRatio = (landscapeRatio + deltaPx / totalWidthPx).coerceIn(0.20f, 0.80f)
                                    }
                                },
                                onDragStarted = { isDraggingSplit = true },
                                onDragStopped = { isDraggingSplit = false }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(
                                    color = if (isDraggingSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f - landscapeRatio)
                    ) {
                        ToiletMapPane(
                            uiState = uiState,
                            cameraPositionState = cameraPositionState,
                            searchLocation = searchLatLng,
                            searchLocationName = locationName,
                            onMarkerClick = { toilet ->
                                viewModel.selectToilet(toilet)
                            },
                            onToiletDetailClick = { toiletId ->
                                onNavigateToDetail(toiletId)
                            },
                            onCreateToiletClick = { lat, lon ->
                                onNavigateToCreate(lat, lon, latitude, longitude)
                            },
                            onDismissSelection = {
                                viewModel.selectToilet(null)
                            },
                            onSatelliteToggle = { viewModel.toggleSatellite() },
                            onMyLocationClick = {
                                uiState.userLocation?.let { userLoc ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(userLoc, 16f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // Mobile Portrait Split View (List Top, Map Bottom with Draggable Handle)
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(portraitRatio)
                    ) {
                        ToiletListPane(
                            uiState = uiState,
                            listState = listState,
                            onToiletSelect = { toilet ->
                                viewModel.selectToilet(toilet)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(toilet.lat, toilet.lon), 16f
                                        )
                                    )
                                }
                            },
                            onNavigateClick = { navigationTargetToilet = it },
                            onDetailClick = { onNavigateToDetail(it.id) },
                            onFilterChange = { viewModel.updateFilters(it) },
                            onAddToiletClick = { onNavigateToCreate(null, null, latitude, longitude) },
                            onPhotoClick = { toilet, photoIndex ->
                                lightboxToilet = toilet
                                lightboxPhotoIndex = photoIndex
                            }
                        )
                    }

                    // Draggable Horizontal Split Handle (matching iOS screenshot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { deltaPx ->
                                    if (totalHeightPx > 0) {
                                        portraitRatio = (portraitRatio + deltaPx / totalHeightPx).coerceIn(0.18f, 0.82f)
                                    }
                                },
                                onDragStarted = { isDraggingSplit = true },
                                onDragStopped = { isDraggingSplit = false }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    color = if (isDraggingSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f - portraitRatio)
                    ) {
                        ToiletMapPane(
                            uiState = uiState,
                            cameraPositionState = cameraPositionState,
                            searchLocation = searchLatLng,
                            searchLocationName = locationName,
                            onMarkerClick = { toilet ->
                                viewModel.selectToilet(toilet)
                            },
                            onToiletDetailClick = { toiletId ->
                                onNavigateToDetail(toiletId)
                            },
                            onCreateToiletClick = { lat, lon ->
                                onNavigateToCreate(lat, lon, latitude, longitude)
                            },
                            onDismissSelection = {
                                viewModel.selectToilet(null)
                            },
                            onSatelliteToggle = { viewModel.toggleSatellite() },
                            onMyLocationClick = {
                                uiState.userLocation?.let { userLoc ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(userLoc, 16f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Navigation Choice Sheet: Compass vs External Maps
    navigationTargetToilet?.let { toilet ->
        NavigationChoiceBottomSheet(
            toilet = toilet,
            onDismiss = { navigationTargetToilet = null },
            onNavigateCompass = { toiletId ->
                navigationTargetToilet = null
                onNavigateToCompass(toiletId)
            }
        )
    }

    // Photo Lightbox Dialog
    lightboxToilet?.let { toilet ->
        PhotoLightbox(
            toiletId = toilet.id,
            photos = toilet.photos,
            initialIndex = lightboxPhotoIndex,
            onDismiss = { lightboxToilet = null },
            onPhotoDeleted = { deletedFilename ->
                viewModel.onPhotoDeleted(toilet.id, deletedFilename)
                val remaining = toilet.photos.filter { it.resolvedFilename != deletedFilename }
                if (remaining.isEmpty()) {
                    lightboxToilet = null
                } else {
                    lightboxToilet = toilet.copy(photos = remaining)
                }
            }
        )
    }
}

@Composable
private fun ToiletListPane(
    uiState: ResultsUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToiletSelect: (Toilet) -> Unit,
    onNavigateClick: (Toilet) -> Unit,
    onDetailClick: (Toilet) -> Unit,
    onFilterChange: (de.wcinfo.app.data.model.ToiletFilterSettings) -> Unit,
    onAddToiletClick: () -> Unit,
    onPhotoClick: (Toilet, Int) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterBanner(
            filterSettings = uiState.filterSettings,
            onFilterChange = onFilterChange,
            onAddToiletClick = onAddToiletClick
        )

        if (uiState.isLoading && uiState.toilets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (uiState.toilets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.no_toilets_found),
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.toilets, key = { it.id }) { toilet ->
                    ToiletCard(
                        toilet = toilet,
                        isSelected = uiState.selectedToilet?.id == toilet.id,
                        onCardClick = { onToiletSelect(toilet) },
                        onNavigateClick = { onNavigateClick(toilet) },
                        onDetailsClick = { onDetailClick(toilet) },
                        onPhotoClick = { photoIndex -> onPhotoClick(toilet, photoIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToiletMapPane(
    uiState: ResultsUiState,
    cameraPositionState: CameraPositionState,
    searchLocation: LatLng? = null,
    searchLocationName: String? = null,
    onMarkerClick: (Toilet) -> Unit,
    onToiletDetailClick: (Int) -> Unit,
    onCreateToiletClick: (Double, Double) -> Unit,
    onDismissSelection: () -> Unit,
    onSatelliteToggle: () -> Unit,
    onMyLocationClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var tappedEmptyCoordinate by remember { mutableStateOf<LatLng?>(null) }

    // When a toilet is selected from the list, clear tappedEmptyCoordinate
    LaunchedEffect(uiState.selectedToilet) {
        if (uiState.selectedToilet != null) {
            tappedEmptyCoordinate = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = uiState.userLocation != null,
                mapType = if (uiState.isSatellite) MapType.SATELLITE else MapType.NORMAL,
                mapStyleOptions = if (isDark && !uiState.isSatellite) MapStyles.DarkMapStyleOptions else null
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = { latLng ->
                onDismissSelection()
                tappedEmptyCoordinate = latLng
            },
            onMapLongClick = { latLng ->
                onDismissSelection()
                tappedEmptyCoordinate = latLng
            }
        ) {
            val context = LocalContext.current

            // Default Maps marker at the search location / place the resultview is for
            searchLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = searchLocationName ?: stringResource(id = R.string.current_location),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

            uiState.toilets.forEach { toilet ->
                val markerIcon = ToiletMarkerIcons.getMarkerIcon(context, toilet)

                Marker(
                    state = MarkerState(position = LatLng(toilet.lat, toilet.lon)),
                    title = toilet.displayName,
                    snippet = toilet.address,
                    icon = markerIcon,
                    onClick = {
                        tappedEmptyCoordinate = null
                        onMarkerClick(toilet)
                        true // Suppress default native Google Maps info bubble
                    }
                )
            }

            // Marker for tapped empty location
            tappedEmptyCoordinate?.let { coord ->
                Marker(
                    state = MarkerState(position = coord),
                    title = stringResource(id = R.string.map_tooltip_add_toilet_title),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                )
            }
        }

        // Selected Toilet Callout / Tooltip Card (Matching iOS style)
        AnimatedVisibility(
            visible = uiState.selectedToilet != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp, end = 72.dp)
        ) {
            uiState.selectedToilet?.let { toilet ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToiletDetailClick(toilet.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = toilet.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (toilet.isQualified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    QualifiedBadge(size = 16.dp)
                                }
                            }

                            val subtitleText = listOfNotNull(
                                if (toilet.isUnisex) "Unisex" else if (toilet.isGenderSeparated) "Getrennt" else null,
                                toilet.address?.ifBlank { null }
                            ).joinToString(", ")

                            if (subtitleText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(id = R.string.map_tooltip_view_details),
                                color = AccentMagenta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = onDismissSelection,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tapped Empty Coordinate Tooltip Card (Matching iOS style)
        AnimatedVisibility(
            visible = tappedEmptyCoordinate != null && uiState.selectedToilet == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp, end = 72.dp)
        ) {
            tappedEmptyCoordinate?.let { coord ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateToiletClick(coord.latitude, coord.longitude) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.map_tooltip_add_toilet_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.map_tooltip_add_toilet_subtitle,
                                    coord.latitude,
                                    coord.longitude
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(id = R.string.map_tooltip_add_toilet_action),
                                color = AccentMagenta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = { tappedEmptyCoordinate = null },
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Map Control Floating Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onSatelliteToggle,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Satellitenansicht")
            }

            FloatingActionButton(
                onClick = onMyLocationClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Meinen Standort zentrieren")
            }
        }
    }
}
