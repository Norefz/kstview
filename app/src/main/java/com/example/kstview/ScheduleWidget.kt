package com.example.kstview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale

class ScheduleWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val VIEW_TYPE_KEY = stringPreferencesKey("view_type") // "daily" or "full"

        fun getTodayIndonesianDayName(): String {
            return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "SENIN"
                Calendar.TUESDAY -> "SELASA"
                Calendar.WEDNESDAY -> "RABU"
                Calendar.THURSDAY -> "KAMIS"
                Calendar.FRIDAY -> "JUMAT"
                Calendar.SATURDAY -> "SABTU"
                Calendar.SUNDAY -> "MINGGU"
                else -> ""
            }
        }

        fun formatJamWidget(jam: String): String {
            val clean = jam.trim()
            val parts = clean.split(Regex("""[-:]"""))
            if (parts.size == 2) {
                val start = parts[0].toIntOrNull()
                val end = parts[1].toIntOrNull()
                if (start != null && end != null) {
                    return String.format(Locale.US, "%02d:00 - %02d:00", start, end)
                }
            }
            return clean
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val scheduleJson = ScheduleDataStore.getSchedule(context).first()
        val schedules = try {
            Json.decodeFromString<List<CourseSchedule>>(scheduleJson)
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val viewType = prefs[VIEW_TYPE_KEY] ?: "daily"

            val todayIndoDay = getTodayIndonesianDayName()
            val todayEnglishDay = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US)?.uppercase() ?: ""

            val displayedSchedules = if (viewType == "daily") {
                schedules.filter {
                    it.hari.equals(todayIndoDay, ignoreCase = true) ||
                            it.hari.equals(todayEnglishDay, ignoreCase = true)
                }
            } else {
                schedules
            }

            GlanceTheme {
                WidgetContent(viewType, todayIndoDay, displayedSchedules)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        viewType: String,
        todayDayName: String,
        schedules: List<CourseSchedule>
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // iOS Calendar Style Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (viewType == "daily") "JADWAL HARI INI" else "SEMUA KULIAH",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.primary
                        )
                    )
                    Text(
                        text = if (viewType == "daily") todayDayName else "${schedules.size} Mata Kuliah",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                }

                // Apple-style small pill tag
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewType == "daily") "Hari Ini" else "Semua",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Schedule Items or Minimalist iOS Empty State
            if (schedules.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (viewType == "daily") "Tidak Ada Kuliah Hari Ini" else "Jadwal Kosong",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = if (viewType == "daily") "Nikmati waktu luangmu ✨" else "Impor KST PDF di aplikasi",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(schedules) { schedule ->
                        WidgetItemCard(schedule = schedule, showDay = viewType == "full")
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetItemCard(schedule: CourseSchedule, showDay: Boolean) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Timeline Accent Bar
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(38.dp)
                    .background(GlanceTheme.colors.primary)
            ) {}

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Details Stack
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatJamWidget(schedule.jam),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.primary
                        )
                    )

                    if (showDay) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = "· ${schedule.hari.uppercase()}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GlanceTheme.colors.primary
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Text(
                        text = if (schedule.ruang.startsWith("Ruang", ignoreCase = true)) schedule.ruang else "Ruang ${schedule.ruang}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Course Name
                Text(
                    text = schedule.nama_matkul,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(1.dp))

                // Lecturer or Course Code
                val metaText = when {
                    schedule.dosen.isNotEmpty() -> schedule.dosen
                    schedule.kode_matkul.isNotEmpty() -> schedule.kode_matkul
                    else -> ""
                }
                if (metaText.isNotEmpty()) {
                    Text(
                        text = metaText,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}
