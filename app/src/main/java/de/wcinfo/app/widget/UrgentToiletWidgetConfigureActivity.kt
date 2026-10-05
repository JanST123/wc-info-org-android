package de.wcinfo.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import de.wcinfo.app.R
import de.wcinfo.app.ui.theme.WCinfoTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class UrgentToiletWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            WCinfoTheme {
                val coroutineScope = rememberCoroutineScope()

                var showClosed by remember { mutableStateOf(false) }
                var showNonPublic by remember { mutableStateOf(true) }
                var allowNonPublicFallback by remember { mutableStateOf(true) }
                var maxPublicDistanceMeters by remember { mutableIntStateOf(500) }
                var showNonWheelchairAccessible by remember { mutableStateOf(true) }
                var showWithoutChangingTable by remember { mutableStateOf(true) }
                var showWithoutGenderSeparation by remember { mutableStateOf(true) }
                var showWithoutEuroKey by remember { mutableStateOf(true) }

                var previewTabIndex by remember { mutableIntStateOf(0) }

                LaunchedEffect(appWidgetId) {
                    try {
                        val glanceId = GlanceAppWidgetManager(this@UrgentToiletWidgetConfigureActivity)
                            .getGlanceIdBy(appWidgetId)
                        val prefs = getAppWidgetState(
                            this@UrgentToiletWidgetConfigureActivity,
                            PreferencesGlanceStateDefinition,
                            glanceId
                        )
                        showClosed = prefs[UrgentToiletWidgetKeys.SHOW_CLOSED] ?: false
                        showNonPublic = prefs[UrgentToiletWidgetKeys.SHOW_NON_PUBLIC] ?: true
                        allowNonPublicFallback = prefs[UrgentToiletWidgetKeys.ALLOW_NON_PUBLIC_FALLBACK] ?: true
                        maxPublicDistanceMeters = prefs[UrgentToiletWidgetKeys.MAX_PUBLIC_DISTANCE_METERS] ?: 500
                        showNonWheelchairAccessible = prefs[UrgentToiletWidgetKeys.SHOW_NON_WHEELCHAIR_ACCESSIBLE] ?: true
                        showWithoutChangingTable = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_CHANGING_TABLE] ?: true
                        showWithoutGenderSeparation = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_GENDER_SEPARATION] ?: true
                        showWithoutEuroKey = prefs[UrgentToiletWidgetKeys.SHOW_WITHOUT_EURO_KEY] ?: true
                    } catch (_: Exception) {
                        // Keep defaults on first configure
                    }
                }

                val currentConfig = UrgentWidgetConfig(
                    showClosed = showClosed,
                    showNonPublic = showNonPublic,
                    allowNonPublicFallback = allowNonPublicFallback,
                    maxPublicDistanceMeters = maxPublicDistanceMeters,
                    showNonWheelchairAccessible = showNonWheelchairAccessible,
                    showWithoutChangingTable = showWithoutChangingTable,
                    showWithoutGenderSeparation = showWithoutGenderSeparation,
                    showWithoutEuroKey = showWithoutEuroKey
                )

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.widget_config_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val glanceId = GlanceAppWidgetManager(this@UrgentToiletWidgetConfigureActivity)
                                                .getGlanceIdBy(appWidgetId)
                                            updateAppWidgetState(
                                                this@UrgentToiletWidgetConfigureActivity,
                                                PreferencesGlanceStateDefinition,
                                                glanceId
                                            ) { prefs ->
                                                prefs.toMutablePreferences().apply {
                                                    this[UrgentToiletWidgetKeys.SHOW_CLOSED] = showClosed
                                                    this[UrgentToiletWidgetKeys.SHOW_NON_PUBLIC] = showNonPublic
                                                    this[UrgentToiletWidgetKeys.ALLOW_NON_PUBLIC_FALLBACK] = allowNonPublicFallback
                                                    this[UrgentToiletWidgetKeys.MAX_PUBLIC_DISTANCE_METERS] = maxPublicDistanceMeters
                                                    this[UrgentToiletWidgetKeys.SHOW_NON_WHEELCHAIR_ACCESSIBLE] = showNonWheelchairAccessible
                                                    this[UrgentToiletWidgetKeys.SHOW_WITHOUT_CHANGING_TABLE] = showWithoutChangingTable
                                                    this[UrgentToiletWidgetKeys.SHOW_WITHOUT_GENDER_SEPARATION] = showWithoutGenderSeparation
                                                    this[UrgentToiletWidgetKeys.SHOW_WITHOUT_EURO_KEY] = showWithoutEuroKey
                                                }
                                            }
                                            UrgentToiletWidget().update(this@UrgentToiletWidgetConfigureActivity, glanceId)

                                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                            setResult(RESULT_OK, resultValue)
                                            finish()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE01E2E),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.widget_config_save),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.widget_config_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Preview Section
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TabRow(
                                    selectedTabIndex = previewTabIndex,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                ) {
                                    Tab(
                                        selected = previewTabIndex == 0,
                                        onClick = { previewTabIndex = 0 },
                                        text = { Text(stringResource(R.string.widget_config_size_small)) }
                                    )
                                    Tab(
                                        selected = previewTabIndex == 1,
                                        onClick = { previewTabIndex = 1 },
                                        text = { Text(stringResource(R.string.widget_config_size_medium)) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (previewTabIndex == 0) {
                                    SmallWidgetPreview(config = currentConfig)
                                } else {
                                    MediumWidgetPreview(config = currentConfig)
                                }
                            }
                        }

                        // Filter Toggles Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.filter_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_closed),
                                    checked = showClosed,
                                    onCheckedChange = { showClosed = it }
                                )

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_non_public),
                                    checked = showNonPublic,
                                    onCheckedChange = { showNonPublic = it }
                                )

                                if (!showNonPublic) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterToggleItem(
                                            title = stringResource(R.string.widget_config_fallback_desc),
                                            checked = allowNonPublicFallback,
                                            onCheckedChange = { allowNonPublicFallback = it }
                                        )

                                        if (allowNonPublicFallback) {
                                            Text(
                                                text = stringResource(R.string.widget_config_max_dist_title, maxPublicDistanceMeters),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Slider(
                                                value = maxPublicDistanceMeters.toFloat(),
                                                onValueChange = { maxPublicDistanceMeters = (it / 50).roundToInt() * 50 },
                                                valueRange = 200f..2000f,
                                                steps = 35,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFFE01E2E),
                                                    activeTrackColor = Color(0xFFE01E2E)
                                                )
                                            )
                                        }
                                    }
                                }

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_non_wheelchair),
                                    checked = showNonWheelchairAccessible,
                                    onCheckedChange = { showNonWheelchairAccessible = it }
                                )

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_without_changing_table),
                                    checked = showWithoutChangingTable,
                                    onCheckedChange = { showWithoutChangingTable = it }
                                )

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_without_gender_separation),
                                    checked = showWithoutGenderSeparation,
                                    onCheckedChange = { showWithoutGenderSeparation = it }
                                )

                                FilterToggleItem(
                                    title = stringResource(R.string.filter_show_without_euro_key),
                                    checked = showWithoutEuroKey,
                                    onCheckedChange = { showWithoutEuroKey = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFE01E2E)
            )
        )
    }
}

@Composable
private fun SmallWidgetPreview(config: UrgentWidgetConfig) {
    val emergencyGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFE01E2E), Color(0xFFA60D1F))
    )

    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(emergencyGradient)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: NOTFALL pill + WC pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x59000000))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD600))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.widget_notfall),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_wc),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Center: Pictogram
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_urgent_person_pictogram),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(46.dp)
                )
            }

            // Bottom: Title & Compass
            Column {
                Text(
                    text = stringResource(R.string.widget_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_compass_needle),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.widget_subtitle_compass),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumWidgetPreview(config: UrgentWidgetConfig) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val emergencyGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFE01E2E), Color(0xFFA60D1F))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(emergencyGradient)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Pictogram + WC NOTFALL
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_urgent_person_pictogram),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x4D000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_wc_notfall),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD600)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.widget_instant_nav),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = stringResource(R.string.widget_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val tags = config.getActiveFilterTags(context)
                    if (tags.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.take(2).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0x33FFFFFF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (tags.size > 2) {
                                Text(
                                    text = "+${tags.size - 2}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.widget_all_available_toilets),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Callout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x40000000))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_compass_needle),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.widget_tap_to_navigate),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
