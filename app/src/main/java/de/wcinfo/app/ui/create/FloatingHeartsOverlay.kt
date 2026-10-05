package de.wcinfo.app.ui.create

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wcinfo.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class HeartParticle(
    val id: Int,
    val xOffset: Float,
    val sizeSp: Float,
    val delayMs: Long,
    val durationMs: Int
)

private val particles = listOf(
    HeartParticle(0, -110f, 40f, 0L, 1800),
    HeartParticle(1, -40f, 54f, 120L, 2000),
    HeartParticle(2, 25f, 44f, 50L, 1900),
    HeartParticle(3, 100f, 42f, 220L, 2100),
    HeartParticle(4, -70f, 36f, 280L, 1700),
    HeartParticle(5, 65f, 60f, 160L, 2000),
    HeartParticle(6, 0f, 68f, 80L, 1900),
    HeartParticle(7, -130f, 38f, 320L, 2100),
    HeartParticle(8, 130f, 40f, 260L, 1800)
)

@Composable
fun FloatingHeartsOverlay(
    onDismiss: () -> Unit
) {
    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isStarted = true
        delay(2200L)
        onDismiss()
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0.7f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(300),
        label = "cardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isStarted) 0.45f else 0f)),
        contentAlignment = Alignment.Center
    ) {
        // Floating Heart Particles
        particles.forEach { particle ->
            SingleFloatingHeart(particle = particle, isStarted = isStarted)
        }

        // Center card
        Card(
            modifier = Modifier
                .scale(cardScale)
                .alpha(cardAlpha)
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF242424).copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❤️",
                    fontSize = 56.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.wizard_celebration_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.wizard_celebration_subtitle),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SingleFloatingHeart(
    particle: HeartParticle,
    isStarted: Boolean
) {
    val yOffset = remember { Animatable(350f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(isStarted) {
        if (isStarted) {
            delay(particle.delayMs)
            launch {
                yOffset.animateTo(
                    targetValue = -450f,
                    animationSpec = tween(particle.durationMs, easing = FastOutSlowInEasing)
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = tween(particle.durationMs, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay((particle.durationMs * 0.6).toLong())
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween((particle.durationMs * 0.4).toInt(), easing = LinearEasing)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .offset(x = particle.xOffset.dp, y = yOffset.value.dp)
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        Text(
            text = "❤️",
            fontSize = particle.sizeSp.sp
        )
    }
}
