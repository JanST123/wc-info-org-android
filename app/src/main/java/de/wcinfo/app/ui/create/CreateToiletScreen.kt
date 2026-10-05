package de.wcinfo.app.ui.create

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import de.wcinfo.app.R
import de.wcinfo.app.data.location.LocationTracker
import de.wcinfo.app.data.model.NearbyPlaceOption
import de.wcinfo.app.ui.components.EuroKeyInfoBottomSheet
import de.wcinfo.app.ui.theme.MapStyles
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.StatusOpen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateToiletScreen(
    initialLat: Double?,
    initialLon: Double?,
    resultLat: Double? = null,
    resultLon: Double? = null,
    onClose: () -> Unit,
    viewModel: CreateToiletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationTracker = remember { LocationTracker(context) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !uiState.showCelebration) {
        if (uiState.stepHistory.isNotEmpty()) {
            viewModel.goBack()
        } else if (uiState.createdToiletId == null) {
            showCancelConfirmDialog = true
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setInitialData(initialLat, initialLon, resultLat, resultLon)
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.wizard_cancel_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.wizard_cancel_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.wizard_cancel_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(text = stringResource(id = R.string.wizard_cancel_dialog_dismiss))
                }
            }
        )
    }

    if (uiState.showEuroKeyInfo) {
        EuroKeyInfoBottomSheet(
            onDismiss = { viewModel.setShowEuroKeyInfo(false) }
        )
    }

    if (uiState.showCompanyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCompanyDialog(false) },
            title = {
                Text(
                    text = stringResource(id = R.string.wizard_company_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.wizard_company_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setShowCompanyDialog(false)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hi@wc-info.org?subject=Toiletten-Import")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(text = stringResource(id = R.string.wizard_company_dialog_email))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowCompanyDialog(false) }) {
                    Text(text = stringResource(id = R.string.close))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.add_toilet_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        if (uiState.stepHistory.isNotEmpty() && !uiState.showCelebration) {
                            IconButton(onClick = { viewModel.goBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.back),
                                    tint = PrimaryPurple
                                )
                            }
                        }
                    },
                    actions = {
                        if (uiState.createdToiletId != null) {
                            Text(
                                text = stringResource(id = R.string.done),
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clickable { viewModel.finishWithCelebration() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.cancel_button),
                                color = PrimaryPurple,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clickable {
                                        showCancelConfirmDialog = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Company / Municipality Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setShowCompanyDialog(true) },
                    color = PrimaryPurple.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.wizard_company_banner),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryPurple,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }

                // 2. Progress Bar
                val currentIndex = uiState.activeSteps.indexOf(uiState.currentStep).coerceAtLeast(0)
                val totalSteps = maxOf(uiState.activeSteps.size, 1)
                val progress = (currentIndex + 1).toFloat() / totalSteps.toFloat()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryPurple,
                    trackColor = PrimaryPurple.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.wizard_step_indicator, currentIndex + 1, totalSteps),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    if (uiState.createdToiletId != null) {
                        Text(
                            text = stringResource(id = R.string.wizard_toilet_saved_badge),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOpen
                        )
                    }
                }

                // 3. Question Scroll Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "wizardStep"
                    ) { step ->
                        when (step) {
                            QuestionStep.PLACE -> Step1PlaceView(
                                uiState = uiState,
                                onSelectPlace = { viewModel.selectPlace(it) }
                            )

                            QuestionStep.NAME -> Step2NameView(
                                toiletName = uiState.toiletName,
                                onNameChange = { viewModel.updateToiletName(it) },
                                onNext = { viewModel.advanceToNextStep() }
                            )

                            QuestionStep.GPS_LOCATION -> Step3GpsLocationView(
                                onUseGps = {
                                    coroutineScope.launch {
                                        val loc = locationTracker.getCurrentLocation()
                                        val latLng = if (loc != null) LatLng(loc.latitude, loc.longitude) else null
                                        viewModel.setGpsLocation(true, latLng)
                                    }
                                },
                                onSkipGps = { viewModel.setGpsLocation(false, null) }
                            )

                            QuestionStep.MAP_PICKER -> Step4MapPickerView(
                                initialCoord = uiState.mapSelectedCoordinate ?: LatLng(52.5200, 13.4050),
                                onCoordinateChanged = { viewModel.updateMapPickerCoordinate(it) },
                                onConfirmLocation = { viewModel.confirmMapPickerLocation() }
                            )

                            QuestionStep.GENDER_SEPARATED -> Step5GenderSeparatedView(
                                onSeparated = { viewModel.setGenderSeparated(true) },
                                onUnisex = { viewModel.setGenderSeparated(false) }
                            )

                            QuestionStep.WHEELCHAIR -> Step6WheelchairView(
                                onYes = { viewModel.setWheelchairAccess(true) },
                                onNo = { viewModel.setWheelchairAccess(false) }
                            )

                            QuestionStep.EURO_KEY -> Step7EuroKeyView(
                                isSubmitting = uiState.isSubmitting,
                                onYes = { viewModel.setEuroKey("yes") },
                                onUnknown = { viewModel.setEuroKey("unknown") },
                                onNo = { viewModel.setEuroKey("no") },
                                onShowInfo = { viewModel.setShowEuroKeyInfo(true) }
                            )

                            QuestionStep.ADDRESS -> Step8AddressView(
                                address = uiState.addressInput,
                                onAddressChange = { viewModel.updateAddressInput(it) },
                                onSave = { viewModel.saveAddressAndContinue() },
                                onSkip = { viewModel.skipAddressAndContinue() }
                            )

                            QuestionStep.OPENING_TIMES -> Step9OpeningTimesView(
                                isEntering = uiState.isEnteringOpeningTimes,
                                periods = uiState.placeOpeningHours,
                                onPeriodsChanged = { viewModel.updateOpeningHours(it) },
                                onStartEntering = { viewModel.setIsEnteringOpeningTimes(true) },
                                onSave = { viewModel.saveOpeningHoursAndContinue() },
                                onCancel = { viewModel.skipOpeningHoursAndContinue() }
                            )

                            QuestionStep.PUBLIC_ACCESS -> Step10PublicAccessView(
                                onPublic = { viewModel.setPublicAccessible(true) },
                                onNonPublic = { viewModel.setPublicAccessible(false) }
                            )

                            QuestionStep.OUTSIDE_OPENING_TIMES -> Step11OutsideOpeningTimesView(
                                onYes = { viewModel.setAccessibleOutsideOpeningTimes(true) },
                                onNo = { viewModel.setAccessibleOutsideOpeningTimes(false) }
                            )

                            QuestionStep.STORAGE_SPACE -> Step12StorageSpaceView(
                                storageSpace = uiState.storageSpace,
                                onSelect = { viewModel.setStorageSpace(it) },
                                onNext = { viewModel.saveStorageAndContinue() }
                            )

                            QuestionStep.PHOTOS -> Step13PhotosView(
                                photos = uiState.uploadedPhotos,
                                onAddPhotoUri = { viewModel.uploadPhotoUri(context, it) },
                                onRemovePhoto = { viewModel.removePhoto(it) },
                                onNext = { viewModel.advanceToNextStep() }
                            )

                            QuestionStep.COMMENT -> Step14CommentView(
                                comment = uiState.commentInput,
                                onCommentChange = { viewModel.updateCommentInput(it) },
                                onFinish = { viewModel.saveCommentAndFinish() }
                            )
                        }
                    }
                }
            }
        }

        // Celebration Overlay
        if (uiState.showCelebration) {
            FloatingHeartsOverlay(
                onDismiss = {
                    viewModel.dismissCelebration()
                    onClose()
                }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP VIEWS
// -------------------------------------------------------------

@Composable
private fun Step1PlaceView(
    uiState: CreateToiletUiState,
    onSelectPlace: (NearbyPlaceOption?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Apartment,
            title = stringResource(id = R.string.wizard_q1_title),
            subtitle = stringResource(id = R.string.wizard_q1_subtitle)
        )

        if (uiState.isLoadingPlaces) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
                Text(
                    text = stringResource(id = R.string.wizard_q1_loading),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.nearbyPlaces.take(3).forEach { place ->
                    PrimaryChoiceButton(
                        title = place.name,
                        subtitle = place.secondaryText,
                        onClick = { onSelectPlace(place) }
                    )
                }

                SecondaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q1_none_of_these),
                    onClick = { onSelectPlace(null) }
                )
            }
        }
    }
}

@Composable
private fun Step2NameView(
    toiletName: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Signpost,
            title = stringResource(id = R.string.wizard_q2_title),
            subtitle = stringResource(id = R.string.wizard_q2_subtitle)
        )

        OutlinedTextField(
            value = toiletName,
            onValueChange = onNameChange,
            placeholder = { Text(text = stringResource(id = R.string.wizard_q2_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        PrimaryChoiceButton(
            title = stringResource(id = R.string.next),
            onClick = onNext
        )
    }
}

@Composable
private fun Step3GpsLocationView(
    onUseGps: () -> Unit,
    onSkipGps: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.MyLocation,
            title = stringResource(id = R.string.wizard_q3_title),
            subtitle = stringResource(id = R.string.wizard_q3_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q3_yes),
                onClick = onUseGps
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q3_no),
                onClick = onSkipGps
            )
        }
    }
}

@Composable
private fun Step4MapPickerView(
    initialCoord: LatLng,
    onCoordinateChanged: (LatLng) -> Unit,
    onConfirmLocation: () -> Unit
) {
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCoord, 16f)
    }
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Map,
            title = stringResource(id = R.string.wizard_q4_title),
            subtitle = stringResource(id = R.string.wizard_q4_subtitle)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(
                    mapStyleOptions = if (isDark) MapStyles.DarkMapStyleOptions else null
                ),
                onMapClick = { latLng ->
                    onCoordinateChanged(latLng)
                }
            ) {
                Marker(
                    state = MarkerState(position = initialCoord),
                    title = "Toiletten-Standort",
                    draggable = true
                )
            }
        }

        PrimaryChoiceButton(
            title = stringResource(id = R.string.wizard_q4_choose_location),
            onClick = onConfirmLocation
        )
    }
}

@Composable
private fun Step5GenderSeparatedView(
    onSeparated: () -> Unit,
    onUnisex: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Wc,
            title = stringResource(id = R.string.wizard_q5_title)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q5_yes),
                onClick = onSeparated
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q5_no_unisex),
                onClick = onUnisex
            )
        }
    }
}

@Composable
private fun Step6WheelchairView(
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Accessible,
            title = stringResource(id = R.string.wizard_q6_title)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q6_yes),
                onClick = onYes
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q6_no),
                onClick = onNo
            )
        }
    }
}

@Composable
private fun Step7EuroKeyView(
    isSubmitting: Boolean,
    onYes: () -> Unit,
    onUnknown: () -> Unit,
    onNo: () -> Unit,
    onShowInfo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(44.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.wizard_q7_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onShowInfo,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Euro Key Info",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.wizard_q7_subtitle),
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        if (isSubmitting) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
                Text(
                    text = stringResource(id = R.string.wizard_saving),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q7_yes),
                    onClick = onYes
                )

                SecondaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q7_unknown),
                    onClick = onUnknown
                )

                SecondaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q7_no),
                    onClick = onNo
                )
            }
        }
    }
}

@Composable
private fun Step8AddressView(
    address: String,
    onAddressChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.Place,
            title = stringResource(id = R.string.wizard_q8_title)
        )

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            placeholder = { Text(text = stringResource(id = R.string.wizard_q8_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 3
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q8_save),
                onClick = onSave
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q8_skip),
                onClick = onSkip
            )
        }
    }
}

@Composable
private fun Step9OpeningTimesView(
    isEntering: Boolean,
    periods: List<de.wcinfo.app.data.model.GooglePlacesPeriod>?,
    onPeriodsChanged: (List<de.wcinfo.app.data.model.GooglePlacesPeriod>?) -> Unit,
    onStartEntering: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (!isEntering) {
            QuestionHeader(
                icon = Icons.Default.Schedule,
                title = stringResource(id = R.string.wizard_q9_title),
                subtitle = stringResource(id = R.string.wizard_q9_subtitle)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q9_yes),
                    onClick = onStartEntering
                )

                SecondaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q9_no),
                    onClick = onCancel
                )
            }
        } else {
            QuestionHeader(
                icon = Icons.Default.Schedule,
                title = stringResource(id = R.string.wizard_q9_edit_title),
                subtitle = stringResource(id = R.string.wizard_q9_edit_subtitle)
            )

            OpeningTimesInput(
                periods = periods,
                onPeriodsChanged = onPeriodsChanged
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryChoiceButton(
                    title = stringResource(id = R.string.wizard_q9_save),
                    onClick = onSave
                )

                SecondaryChoiceButton(
                    title = stringResource(id = R.string.cancel_button),
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun Step10PublicAccessView(
    onPublic: () -> Unit,
    onNonPublic: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.People,
            title = stringResource(id = R.string.wizard_q10_title),
            subtitle = stringResource(id = R.string.wizard_q10_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q10_yes),
                onClick = onPublic
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q10_no),
                onClick = onNonPublic
            )
        }
    }
}

@Composable
private fun Step11OutsideOpeningTimesView(
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.NightsStay,
            title = stringResource(id = R.string.wizard_q11_title),
            subtitle = stringResource(id = R.string.wizard_q11_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryChoiceButton(
                title = stringResource(id = R.string.wizard_q11_yes),
                onClick = onYes
            )

            SecondaryChoiceButton(
                title = stringResource(id = R.string.wizard_q11_no),
                onClick = onNo
            )
        }
    }
}

@Composable
private fun Step12StorageSpaceView(
    storageSpace: String?,
    onSelect: (String?) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.GridView,
            title = stringResource(id = R.string.wizard_q12_title)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StorageRadioButton(
                title = stringResource(id = R.string.wizard_q12_none),
                selected = storageSpace == "none",
                onClick = { onSelect("none") }
            )

            StorageRadioButton(
                title = stringResource(id = R.string.wizard_q12_little),
                selected = storageSpace == "little",
                onClick = { onSelect("little") }
            )

            StorageRadioButton(
                title = stringResource(id = R.string.wizard_q12_much),
                selected = storageSpace == "much",
                onClick = { onSelect("much") }
            )

            StorageRadioButton(
                title = stringResource(id = R.string.wizard_q12_no_info),
                selected = storageSpace == null,
                onClick = { onSelect(null) }
            )
        }

        PrimaryChoiceButton(
            title = stringResource(id = R.string.next),
            onClick = onNext
        )
    }
}

@Composable
private fun StorageRadioButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PrimaryPurple.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun Step13PhotosView(
    photos: List<UploadedPhoto>,
    onAddPhotoUri: (Uri) -> Unit,
    onRemovePhoto: (UploadedPhoto) -> Unit,
    onNext: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { onAddPhotoUri(it) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.Default.CameraAlt,
            title = stringResource(id = R.string.wizard_q13_title)
        )

        // Photo Picker trigger card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            shape = RoundedCornerShape(12.dp),
            color = PrimaryPurple.copy(alpha = 0.08f),
            border = BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.wizard_q13_select_photos),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Text(
                        text = stringResource(id = R.string.wizard_q13_select_photos_subtitle),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Thumbnails grid
        if (photos.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 90.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((photos.size + 2) / 3 * 100).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Status overlay at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photo.isUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.wizard_q13_uploading),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (photo.isSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusOpen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(id = R.string.wizard_q13_uploaded),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Remove button
                        IconButton(
                            onClick = { onRemovePhoto(photo) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        SecondaryChoiceButton(
            title = stringResource(id = R.string.next),
            onClick = onNext
        )
    }
}

@Composable
private fun Step14CommentView(
    comment: String,
    onCommentChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuestionHeader(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(id = R.string.wizard_q14_title)
        )

        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = { Text(text = stringResource(id = R.string.wizard_q14_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = 4
        )

        PrimaryChoiceButton(
            title = stringResource(id = R.string.wizard_q14_save),
            onClick = onFinish
        )
    }
}

// -------------------------------------------------------------
// REUSABLE COMPONENTS
// -------------------------------------------------------------

@Composable
private fun QuestionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier.size(44.dp)
        )

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrimaryChoiceButton(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SecondaryChoiceButton(
    title: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.5f))
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurple,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
