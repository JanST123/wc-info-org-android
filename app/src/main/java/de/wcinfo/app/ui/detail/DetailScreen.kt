package de.wcinfo.app.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.wcinfo.app.R
import de.wcinfo.app.data.model.GooglePlacesPeriod
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.ui.components.EuroKeyInfoBottomSheet
import de.wcinfo.app.ui.components.NavigationChoiceBottomSheet
import de.wcinfo.app.ui.components.PhotoLightbox
import de.wcinfo.app.ui.results.components.OpeningTimeBadge
import de.wcinfo.app.ui.results.components.QualifiedBadge
import de.wcinfo.app.ui.theme.AccentMagenta
import de.wcinfo.app.ui.theme.BorderSubtle
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.PrimaryPurpleLight
import de.wcinfo.app.ui.theme.StatusClosed
import de.wcinfo.app.ui.theme.StatusOpen
import de.wcinfo.app.ui.theme.StatusUrgent
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    toiletId: Int,
    onBackClick: () -> Unit,
    onNavigateToCompass: (Int) -> Unit,
    onNavigateToUpdate: (Int) -> Unit,
    onNavigateToFeedback: (Int) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEuroKeyDialog by remember { mutableStateOf(false) }
    var showNavigationSheet by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = copyUriToTempFile(context, uri)
            if (file != null) {
                viewModel.uploadPhoto(file)
            }
        }
    }

    LaunchedEffect(toiletId) {
        viewModel.loadToilet(toiletId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.details_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    uiState.toilet?.let { toilet ->
                        IconButton(onClick = { shareToilet(context, toilet) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_toilet_title), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.toilet == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.error_load_toilet), color = Color.Gray)
            }
        } else {
            val toilet = uiState.toilet!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = toilet.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (toilet.isQualified) {
                            Spacer(modifier = Modifier.width(8.dp))
                            QualifiedBadge(size = 22.dp)
                        }
                    }

                    if (toilet.owner.isNotBlank() && toilet.owner != toilet.displayName) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.operator_label, toilet.owner),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            id = if (toilet.publicAccessible) R.string.public_accessible_yes else R.string.public_accessible_no
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (toilet.publicAccessible) StatusOpen else Color(0xFFD97706)
                    )
                }

                // Primary Navigate Button
                Button(
                    onClick = { showNavigationSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.navigate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Feature Checklist Matrix
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCheckRow(
                            title = stringResource(id = R.string.wheelchair_yes),
                            hasFeature = toilet.hasWheelchairAccess
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FeatureCheckRow(
                            title = stringResource(id = R.string.euro_key_yes),
                            hasFeature = toilet.hasEuroKey,
                            onInfoClick = { showEuroKeyDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FeatureCheckRow(
                            title = stringResource(id = R.string.changing_table_yes),
                            hasFeature = toilet.hasChangingTable
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FeatureCheckRow(
                            title = stringResource(id = R.string.gender_separated_yes),
                            hasFeature = toilet.isGenderSeparated
                        )
                        toilet.storageSpace?.let { space ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stringResource(id = R.string.storage_space_label), fontSize = 14.sp)
                                val localizedSpace = when (space.lowercase()) {
                                    "none" -> stringResource(id = R.string.storage_none)
                                    "little" -> stringResource(id = R.string.storage_little)
                                    "much" -> stringResource(id = R.string.storage_much)
                                    else -> space
                                }
                                Text(
                                    text = localizedSpace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Photo Gallery
                Column {
                    Text(
                        text = stringResource(id = R.string.photos),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Add photo button card
                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = AccentMagenta
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isUploadingPhoto) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(id = R.string.add_photo),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.add_photo_button),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Displayed photos
                        toilet.photos.forEachIndexed { index, photo ->
                            AsyncImage(
                                model = photo.url.ifBlank { photo.urlThumb },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                    .clickable { selectedPhotoIndex = index }
                            )
                        }
                    }
                }

                // Opening Hours
                Column {
                    Text(
                        text = stringResource(id = R.string.opening_hours),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OpeningTimeBadge(toilet = toilet, horizontalAlignment = Alignment.Start)

                    Spacer(modifier = Modifier.height(10.dp))

                    toilet.placeOpeningHours?.let { periods ->
                        WeeklyOpeningSchedule(periods = periods)
                    }
                }

                // Address & Website
                toilet.address?.let { addr ->
                    Column {
                        Text(
                            text = stringResource(id = R.string.address),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(text = addr, fontSize = 14.sp, color = Color.Gray)
                    }
                }

                toilet.website?.let { url ->
                    if (url.isNotBlank()) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.website),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = url,
                                    fontSize = 14.sp,
                                    color = PrimaryPurple,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }

                // Notes / Comment
                toilet.comment?.let { comment ->
                    if (comment.isNotBlank()) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.notes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(text = comment, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Actions: Suggest Changes & Report Problem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToUpdate(toilet.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.edit_button), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onNavigateToFeedback(toilet.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.report_button), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showEuroKeyDialog) {
        EuroKeyInfoBottomSheet(
            onDismiss = { showEuroKeyDialog = false }
        )
    }

    if (showNavigationSheet) {
        uiState.toilet?.let { toilet ->
            NavigationChoiceBottomSheet(
                toilet = toilet,
                onDismiss = { showNavigationSheet = false },
                onNavigateCompass = { toiletId ->
                    showNavigationSheet = false
                    onNavigateToCompass(toiletId)
                }
            )
        }
    }

    selectedPhotoIndex?.let { index ->
        uiState.toilet?.let { toilet ->
            if (toilet.photos.isNotEmpty()) {
                PhotoLightbox(
                    toiletId = toilet.id,
                    photos = toilet.photos,
                    initialIndex = index.coerceIn(0, toilet.photos.lastIndex),
                    onDismiss = { selectedPhotoIndex = null },
                    onPhotoDeleted = { deletedFilename ->
                        viewModel.onPhotoDeleted(deletedFilename)
                    }
                )
            } else {
                selectedPhotoIndex = null
            }
        }
    }
}

@Composable
fun FeatureCheckRow(
    title: String,
    hasFeature: Boolean,
    onInfoClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (hasFeature) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (hasFeature) StatusOpen else StatusUrgent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (onInfoClick != null) {
            IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WeeklyOpeningSchedule(periods: List<GooglePlacesPeriod>) {
    val todayDayOfWeek = (LocalDate.now().dayOfWeek.value % 7) // 0 = Sunday in GooglePlacesPoint

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (dayIndex in 1..7) {
            val day = dayIndex % 7
            val periodForDay = periods.find { it.open.day == day }
            val isToday = day == todayDayOfWeek
            val dayOfWeek = DayOfWeek.of(if (day == 0) 7 else day)
            val dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dayName,
                    fontSize = 13.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (periodForDay != null) {
                        val closeTime = periodForDay.close?.formattedTime ?: "24:00"
                        "${periodForDay.open.formattedTime} – $closeTime"
                    } else {
                        stringResource(id = R.string.closed)
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (_: Exception) {
        null
    }
}

private fun shareToilet(context: Context, toilet: Toilet) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_toilet_text, toilet.displayName, toilet.shareUrl))
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_toilet_title)))
}
