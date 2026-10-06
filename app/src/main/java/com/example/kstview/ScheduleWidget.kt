package com.example.kstview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
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
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale

@Suppress("RestrictedApi")
private fun glanceColor(color: Color): ColorProvider = ColorProvider(color)

data class CourseWidgetTheme(
    val accentColor: Color,
    val boxDrawable: Int,
    val pillDrawable: Int
)

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

        fun formatDayTitle(dayStr: String): String {
            return when (dayStr.uppercase()) {
                "SENIN" -> "Senin"
                "SELASA" -> "Selasa"
                "RABU" -> "Rabu"
                "KAMIS" -> "Kamis"
                "JUMAT" -> "Jumat"
                "SABTU" -> "Sabtu"
                "MINGGU" -> "Minggu"
                else -> dayStr.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }

        fun formatJamDisplay(jam: String): String {
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

        private val PASTEL_THEMES = listOf(
            CourseWidgetTheme(Color(0xFFFFA8C5), R.drawable.bg_pink_box, R.drawable.bg_pill_pink),
            CourseWidgetTheme(Color(0xFF82ECC2), R.drawable.bg_mint_box, R.drawable.bg_pill_mint),
            CourseWidgetTheme(Color(0xFFC3B1E1), R.drawable.bg_lavender_box, R.drawable.bg_pill_lavender),
            CourseWidgetTheme(Color(0xFF90E0EF), R.drawable.bg_blue_box, R.drawable.bg_pill_blue)
        )

        private val COURSE_ICON_RES = listOf(
            R.drawable.ic_analytics,
            R.drawable.ic_code,
            R.drawable.ic_book,
            R.drawable.ic_school
        )
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

            WidgetContent(viewType, todayIndoDay, displayedSchedules)
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
                .background(ImageProvider(R.drawable.bg_widget_card))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header Section
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    // Subtitle: Calendar Icon + Jadwal hari ini
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_calendar),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(glanceColor(Color(0xFFA5A3D0))),
                            modifier = GlanceModifier.width(14.dp).height(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = if (viewType == "daily") "Jadwal hari ini" else "Semua jadwal",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = glanceColor(Color(0xFFA5A3D0))
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    // Title: Kamis. (or Semua.)
                    val titleText = if (viewType == "daily") formatDayTitle(todayDayName) else "Semua"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = titleText,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = glanceColor(Color.White)
                            )
                        )
                        Text(
                            text = ".",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = glanceColor(Color(0xFFFF5C8D))
                            )
                        )
                    }
                }

                // Yellow Class Counter Badge: Book Icon + 2 kelas
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.bg_yellow_badge))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_book),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(glanceColor(Color(0xFF201E50))),
                            modifier = GlanceModifier.width(14.dp).height(14.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = "${schedules.size} kelas",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = glanceColor(Color(0xFF201E50))
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Thin Horizontal Divider
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ImageProvider(R.drawable.divider_line))
            ) {}

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Schedule Items or Empty State
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
                                fontSize = 14.sp,
                                color = glanceColor(Color.White)
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = if (viewType == "daily") "Nikmati waktu luangmu ✨" else "Impor KST PDF di aplikasi",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = glanceColor(Color(0xFFA5A3D0))
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    itemsIndexed(schedules) { index, schedule ->
                        val theme = PASTEL_THEMES[index % PASTEL_THEMES.size]
                        val iconRes = COURSE_ICON_RES[index % COURSE_ICON_RES.size]

                        WidgetItemRow(
                            schedule = schedule,
                            showDay = viewType == "full",
                            theme = theme,
                            iconRes = iconRes,
                            showDivider = index < schedules.size - 1
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetItemRow(
        schedule: CourseSchedule,
        showDay: Boolean,
        theme: CourseWidgetTheme,
        iconRes: Int,
        showDivider: Boolean
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon Container Box
                Box(
                    modifier = GlanceModifier
                        .width(42.dp)
                        .height(42.dp)
                        .background(ImageProvider(theme.boxDrawable)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(Color(0xFF201E50))),
                        modifier = GlanceModifier.width(22.dp).height(22.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Center: Course Title & Lecturer
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = schedule.nama_matkul,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = glanceColor(Color.White)
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_person),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(glanceColor(theme.accentColor)),
                            modifier = GlanceModifier.width(12.dp).height(12.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = schedule.dosen.ifEmpty { schedule.kode_matkul },
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = glanceColor(Color(0xFFA5A3D0))
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Right: Time & Location Pill
                Column(horizontalAlignment = Alignment.End) {
                    // Time Range Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_clock),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(glanceColor(theme.accentColor)),
                            modifier = GlanceModifier.width(12.dp).height(12.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = (if (showDay) "${schedule.hari} " else "") + formatJamDisplay(schedule.jam),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = glanceColor(theme.accentColor)
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Location Pill
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(theme.pillDrawable))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_location),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(glanceColor(Color(0xFF201E50))),
                                modifier = GlanceModifier.width(10.dp).height(10.dp)
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = if (schedule.ruang.startsWith("Ruang", ignoreCase = true)) schedule.ruang else "Ruang ${schedule.ruang}",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = glanceColor(Color(0xFF201E50))
                                )
                            )
                        }
                    }
                }
            }

            if (showDivider) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ImageProvider(R.drawable.divider_line))
                ) {}
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}
