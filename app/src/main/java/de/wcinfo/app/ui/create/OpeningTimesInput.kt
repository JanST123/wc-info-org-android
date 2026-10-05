package de.wcinfo.app.ui.create

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wcinfo.app.R
import de.wcinfo.app.data.model.GooglePlacesPeriod
import de.wcinfo.app.data.model.GooglePlacesPoint
import de.wcinfo.app.ui.theme.PrimaryPurple
import de.wcinfo.app.ui.theme.PrimaryPurpleLight
import java.util.UUID

data class TimeRangeDraft(
    val id: String = UUID.randomUUID().toString(),
    val selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val openHour: Int = 8,
    val openMinute: Int = 0,
    val closeHour: Int = 18,
    val closeMinute: Int = 0,
    val is24Hours: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpeningTimesInput(
    periods: List<GooglePlacesPeriod>?,
    onPeriodsChanged: (List<GooglePlacesPeriod>?) -> Unit
) {
    var drafts by remember {
        mutableStateOf(parsePeriodsToDrafts(periods))
    }

    fun syncPeriods(currentDrafts: List<TimeRangeDraft>) {
        val result = mutableListOf<GooglePlacesPeriod>()
        for (draft in currentDrafts) {
            if (draft.selectedDays.isEmpty()) continue
            for (day in draft.selectedDays.sorted()) {
                if (draft.is24Hours) {
                    result.add(
                        GooglePlacesPeriod(
                            open = GooglePlacesPoint(day = day, hour = 0, minute = 0),
                            close = null
                        )
                    )
                } else {
                    result.add(
                        GooglePlacesPeriod(
                            open = GooglePlacesPoint(day = day, hour = draft.openHour, minute = draft.openMinute),
                            close = GooglePlacesPoint(day = day, hour = draft.closeHour, minute = draft.closeMinute)
                        )
                    )
                }
            }
        }
        onPeriodsChanged(if (result.isEmpty()) null else result)
    }

    LaunchedEffect(drafts) {
        syncPeriods(drafts)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        drafts.forEachIndexed { index, draft ->
            TimeRangeCard(
                index = index,
                draft = draft,
                canDelete = drafts.size > 1,
                onUpdate = { updated ->
                    drafts = drafts.map { if (it.id == updated.id) updated else it }
                },
                onDelete = {
                    drafts = drafts.filter { it.id != draft.id }
                }
            )
        }

        OutlinedButton(
            onClick = {
                drafts = drafts + TimeRangeDraft(
                    selectedDays = emptySet(),
                    openHour = 8,
                    openMinute = 0,
                    closeHour = 18,
                    closeMinute = 0,
                    is24Hours = false
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.wizard_q9_add_period),
                color = PrimaryPurple,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimeRangeCard(
    index: Int,
    draft: TimeRangeDraft,
    canDelete: Boolean,
    onUpdate: (TimeRangeDraft) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val weekdays = listOf(
        1 to "Mo",
        2 to "Di",
        3 to "Mi",
        4 to "Do",
        5 to "Fr",
        6 to "Sa",
        0 to "So"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.wizard_q9_period_n, index + 1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Presets
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    title = stringResource(id = R.string.wizard_q9_preset_mo_fr),
                    isSelected = draft.selectedDays == setOf(1, 2, 3, 4, 5),
                    onClick = { onUpdate(draft.copy(selectedDays = setOf(1, 2, 3, 4, 5))) }
                )
                PresetChip(
                    title = stringResource(id = R.string.wizard_q9_preset_sa_so),
                    isSelected = draft.selectedDays == setOf(6, 0),
                    onClick = { onUpdate(draft.copy(selectedDays = setOf(6, 0))) }
                )
                PresetChip(
                    title = stringResource(id = R.string.wizard_q9_preset_daily),
                    isSelected = draft.selectedDays == setOf(0, 1, 2, 3, 4, 5, 6),
                    onClick = { onUpdate(draft.copy(selectedDays = setOf(0, 1, 2, 3, 4, 5, 6))) }
                )
            }

            // Weekday Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekdays.forEach { (day, label) ->
                    val isSelected = draft.selectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val nextDays = if (isSelected) draft.selectedDays - day else draft.selectedDays + day
                                onUpdate(draft.copy(selectedDays = nextDays))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider()

            // 24 Hours Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.wizard_q9_open_24h),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = draft.is24Hours,
                    onCheckedChange = { onUpdate(draft.copy(is24Hours = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryPurple
                    )
                )
            }

            // Time Pickers
            AnimatedVisibility(visible = !draft.is24Hours) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Opens at
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.wizard_q9_opens_at),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TimeBox(
                            timeString = "%02d:%02d".format(draft.openHour, draft.openMinute),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        onUpdate(draft.copy(openHour = hourOfDay, openMinute = minute))
                                    },
                                    draft.openHour,
                                    draft.openMinute,
                                    true
                                ).show()
                            }
                        )
                    }

                    // Closes at
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.wizard_q9_closes_at),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TimeBox(
                            timeString = "%02d:%02d".format(draft.closeHour, draft.closeMinute),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        onUpdate(draft.copy(closeHour = hourOfDay, closeMinute = minute))
                                    },
                                    draft.closeHour,
                                    draft.closeMinute,
                                    true
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBox(
    timeString: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeString,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) PrimaryPurple else PrimaryPurple.copy(alpha = 0.12f)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else PrimaryPurple,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun parsePeriodsToDrafts(periods: List<GooglePlacesPeriod>?): List<TimeRangeDraft> {
    if (periods.isNullOrEmpty()) {
        return listOf(
            TimeRangeDraft(
                selectedDays = setOf(1, 2, 3, 4, 5),
                openHour = 8,
                openMinute = 0,
                closeHour = 18,
                closeMinute = 0,
                is24Hours = false
            )
        )
    }

    data class DraftKey(
        val is24Hours: Boolean,
        val openHour: Int,
        val openMinute: Int,
        val closeHour: Int,
        val closeMinute: Int
    )

    val grouped = mutableMapOf<DraftKey, MutableSet<Int>>()
    val keyOrder = mutableListOf<DraftKey>()

    for (period in periods) {
        val is24 = (period.open.hour == 0 && period.open.minute == 0 && (period.close == null || period.close.hour == 23 || period.close.hour == 0))
        val openH = period.open.hour
        val openM = period.open.minute
        val closeH = period.close?.hour ?: if (is24) 0 else 18
        val closeM = period.close?.minute ?: 0

        val key = DraftKey(
            is24Hours = is24,
            openHour = openH,
            openMinute = openM,
            closeHour = closeH,
            closeMinute = closeM
        )
        if (!grouped.containsKey(key)) {
            keyOrder.add(key)
            grouped[key] = mutableSetOf()
        }
        grouped[key]?.add(period.open.day)
    }

    val result = mutableListOf<TimeRangeDraft>()
    for (key in keyOrder) {
        val days = grouped[key] ?: emptySet()
        result.add(
            TimeRangeDraft(
                selectedDays = days,
                openHour = key.openHour,
                openMinute = key.openMinute,
                closeHour = key.closeHour,
                closeMinute = key.closeMinute,
                is24Hours = key.is24Hours
            )
        )
    }

    return result.ifEmpty {
        listOf(
            TimeRangeDraft(
                selectedDays = setOf(1, 2, 3, 4, 5),
                openHour = 8,
                openMinute = 0,
                closeHour = 18,
                closeMinute = 0,
                is24Hours = false
            )
        )
    }
}
