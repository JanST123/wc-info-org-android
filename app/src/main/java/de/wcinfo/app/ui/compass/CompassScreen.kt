package de.wcinfo.app.ui.compass

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.wcinfo.app.R
import de.wcinfo.app.data.compass.CompassSensorManager
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.ui.results.components.OpeningTimeBadge
import de.wcinfo.app.ui.results.components.QualifiedBadge
import de.wcinfo.app.ui.theme.AccentMagenta
import de.wcinfo.app.ui.theme.BorderSubtle
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.PrimaryPurpleLight
import de.wcinfo.app.ui.theme.StatusOpen
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    toiletId: Int,
    onBackClick: () -> Unit,
    viewModel: CompassViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toiletId) {
        viewModel.initCompass(toiletId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.compass_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.close))
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
            val compassData = uiState.compassData

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = toilet.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (toilet.isQualified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        QualifiedBadge(size = 18.dp)
                                    }
                                }

                                if (toilet.address != null) {
                                    Text(
                                        text = toilet.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            OpeningTimeBadge(toilet = toilet)
                        }
                    }
                }

                // Compass Dial
                Box(
                    modifier = Modifier
                        .size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating dial
                    val animatedDialAngle by animateFloatAsState(
                        targetValue = -compassData.deviceHeading,
                        animationSpec = spring(stiffness = 300f),
                        label = "DialAngle"
                    )

                    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val outlineColor = MaterialTheme.colorScheme.outline

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedDialAngle)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 8.dp.toPx()

                        // Outer circle border
                        drawCircle(
                            color = outlineVariantColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Draw 24 tick marks
                        for (i in 0 until 24) {
                            val angleRad = Math.toRadians((i * 15).toDouble())
                            val isMajor = i % 6 == 0
                            val tickLength = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                            val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                            val startX = (center.x + (radius - tickLength) * sin(angleRad)).toFloat()
                            val startY = (center.y - (radius - tickLength) * cos(angleRad)).toFloat()
                            val endX = (center.x + radius * sin(angleRad)).toFloat()
                            val endY = (center.y - radius * cos(angleRad)).toFloat()

                            drawLine(
                                color = if (isMajor) primaryColor else outlineColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth
                            )
                        }
                    }

                    // Destination Pointer / Needle
                    val animatedNeedleAngle by animateFloatAsState(
                        targetValue = compassData.relativeAngle,
                        animationSpec = spring(stiffness = 300f),
                        label = "NeedleAngle"
                    )

                    // Target pulse glow if facing destination
                    if (compassData.isFacingTarget) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.12f))
                        )
                    }

                    // Rotating Needle
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .rotate(animatedNeedleAngle)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val needleLength = size.height * 0.42f
                        val needleWidth = 14.dp.toPx()

                        val path = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x + needleWidth, center.y)
                            lineTo(center.x, center.y + needleLength * 0.3f)
                            lineTo(center.x - needleWidth, center.y)
                            close()
                        }

                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryPurple, AccentMagenta)
                            )
                        )

                        // Center circle
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = PrimaryPurple,
                            radius = 5.dp.toPx(),
                            center = center
                        )
                    }
                }

                // Distance & Direction Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (compassData.hasArrived) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOpen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.arrived),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusOpen
                            )
                        }
                    } else {
                        val directionText = stringResource(
                            id = CompassSensorManager.getDirectionResId(
                                compassData.relativeAngle,
                                compassData.distanceMeters
                            )
                        )
                        Text(
                            text = directionText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )

                        val distText = if (compassData.distanceMeters < 1000f) {
                            "${compassData.distanceMeters.toInt()} m"
                        } else {
                            "%.1f km".format(compassData.distanceMeters / 1000f)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = distText,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Fallback to External Maps Button
                OutlinedButton(
                    onClick = { openWalkingRoute(context, toilet) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.open_in_maps), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun openWalkingRoute(context: Context, toilet: Toilet) {
    val navUri = Uri.parse("google.navigation:q=${toilet.lat},${toilet.lon}&mode=w")
    val intent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${toilet.lat},${toilet.lon}&travelmode=walking")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
