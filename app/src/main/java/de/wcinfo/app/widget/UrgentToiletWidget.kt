package de.wcinfo.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.wcinfo.app.R

class UrgentToiletWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    companion object {
        val SMALL_SIZE = DpSize(100.dp, 100.dp)
        val MEDIUM_SIZE = DpSize(220.dp, 100.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, MEDIUM_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val config = UrgentWidgetConfig.fromPreferences(prefs)
            val size = LocalSize.current

            if (size.width >= 210.dp) {
                MediumWidgetContent(config)
            } else {
                SmallWidgetContent(config)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(config: UrgentWidgetConfig) {
        val context = LocalContext.current
        val intent = Intent(Intent.ACTION_VIEW, config.toDeepLinkUri()).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.bg_widget_emergency))
                .clickable(actionStartActivity(intent))
                .padding(12.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Top Row: Emergency Pill + WC Pill
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.bg_pill_emergency))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_yellow_dot),
                            contentDescription = null,
                            modifier = GlanceModifier.size(6.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = context.getString(R.string.widget_notfall),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.bg_pill_wc))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.widget_wc),
                            style = TextStyle(
                                color = ColorProvider(Color(0xE6FFFFFF)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Center: Urgent Person Pictogram
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_urgent_person_pictogram),
                        contentDescription = "Urgent Pictogram",
                        modifier = GlanceModifier.size(46.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Bottom: Title & 1-Tap Compass Subtitle
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Text(
                        text = context.getString(R.string.widget_title),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_compass_needle),
                            contentDescription = null,
                            modifier = GlanceModifier.size(10.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Text(
                            text = context.getString(R.string.widget_subtitle_compass),
                            style = TextStyle(
                                color = ColorProvider(Color(0xE6FFFFFF)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MediumWidgetContent(config: UrgentWidgetConfig) {
        val context = LocalContext.current
        val intent = Intent(Intent.ACTION_VIEW, config.toDeepLinkUri()).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.bg_widget_emergency))
                .clickable(actionStartActivity(intent))
                .padding(14.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Left Column: Pictogram + WC NOTFALL
                Column(
                    modifier = GlanceModifier.width(80.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(R.drawable.ic_urgent_person_pictogram),
                        contentDescription = "Urgent Pictogram",
                        modifier = GlanceModifier.size(52.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.bg_pill_emergency))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.widget_wc_notfall),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFFD600)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Right Column: SOFORT-NAVIGATION, Title, Filter tags, Action Callout
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.Vertical.Top
                ) {
                    // Header: Bolt + SOFORT-NAVIGATION
                    Row(
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_bolt),
                            contentDescription = null,
                            modifier = GlanceModifier.size(11.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = context.getString(R.string.widget_instant_nav),
                            style = TextStyle(
                                color = ColorProvider(Color(0xE6FFFFFF)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = context.getString(R.string.widget_title),
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Active Filter Chips
                    val tags = config.getActiveFilterTags(context)
                    if (tags.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            tags.take(2).forEachIndexed { index, tag ->
                                if (index > 0) {
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                }
                                Box(
                                    modifier = GlanceModifier
                                        .background(ImageProvider(R.drawable.bg_pill_filter_chip))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                 ) {
                                    Text(
                                        text = tag,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(Color.White),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                            if (tags.size > 2) {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = "+${tags.size - 2}",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xCCFFFFFF)),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            text = context.getString(R.string.widget_all_available_toilets),
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(Color(0xCCFFFFFF)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Action Callout Button
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ImageProvider(R.drawable.bg_card_action))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_compass_needle),
                            contentDescription = null,
                            modifier = GlanceModifier.size(12.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = context.getString(R.string.widget_tap_to_navigate),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Image(
                            provider = ImageProvider(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = GlanceModifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
