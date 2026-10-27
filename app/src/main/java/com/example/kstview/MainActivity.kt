package com.example.kstview

import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.kstview.ui.theme.KSTViewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KSTViewTheme {
                MainScheduleScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScheduleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var schedules by remember { mutableStateOf<List<CourseSchedule>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingScheduleIndex by remember { mutableStateOf<Int?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val days = listOf("ALL", "SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU")

    // Determine default selected tab based on current day
    val defaultTab = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0 // ALL for Sunday or default
        }
    }
    var selectedTabIndex by remember { mutableIntStateOf(defaultTab) }

    LaunchedEffect(Unit) {
        val savedJson = ScheduleDataStore.getSchedule(context).first()
        schedules = parseScheduleJson(savedJson)
    }

    val saveSchedulesAndUpdate = { newList: List<CourseSchedule> ->
        schedules = newList
        val json = KstParser.toJson(newList)
        scope.launch {
            ScheduleDataStore.saveSchedule(context, json)
        }
        updateAllWidgets(context)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val extractedText = KstParser.extractTextFromPdf(context, it)
                val parsed = KstParser.parse(extractedText)
                if (parsed.isNotEmpty()) {
                    saveSchedulesAndUpdate(parsed)
                    Toast.makeText(context, "Berhasil mengimpor ${parsed.size} jadwal dari PDF", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Tidak ada data jadwal ditemukan dalam PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val selectedDay = days[selectedTabIndex]
    val filteredSchedules = remember(schedules, selectedDay) {
        if (selectedDay == "ALL") schedules
        else schedules.filter { it.hari.equals(selectedDay, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KST View",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (schedules.isNotEmpty()) {
                            Text(
                                text = "${schedules.size} Jadwal Tersimpan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload PDF Schedule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Jadwal Form",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import PDF") },
                                onClick = {
                                    showMenu = false
                                    pdfLauncher.launch("application/pdf")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.UploadFile, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tambah Jadwal Form") },
                                onClick = {
                                    showMenu = false
                                    showAddDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.EditNote, contentDescription = null)
                                }
                            )
                            if (schedules.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear Schedule", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showClearDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Day Filter Tab Bar
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                days.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = day,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Schedule Content List or Empty State
            if (filteredSchedules.isEmpty()) {
                EmptyScheduleView(
                    hasAnySchedule = schedules.isNotEmpty(),
                    selectedDay = selectedDay,
                    onUploadPdf = { pdfLauncher.launch("application/pdf") },
                    onAddForm = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(filteredSchedules) { schedule ->
                        CourseScheduleCard(
                            schedule = schedule,
                            onEdit = {
                                editingScheduleIndex = schedules.indexOf(schedule)
                            },
                            onDelete = {
                                val updated = schedules.toMutableList()
                                updated.remove(schedule)
                                saveSchedulesAndUpdate(updated)
                                Toast.makeText(context, "Jadwal dihapus", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Add New Schedule Form Dialog
    if (showAddDialog) {
        AddEditScheduleDialog(
            initialSchedule = null,
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onSaveSchedule = { newSchedule ->
                val updated = schedules.toMutableList()
                updated.add(newSchedule)
                saveSchedulesAndUpdate(updated)
                showAddDialog = false
                Toast.makeText(context, "Jadwal ${newSchedule.tipe} berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Existing Schedule Form Dialog
    editingScheduleIndex?.let { index ->
        if (index in schedules.indices) {
            AddEditScheduleDialog(
                initialSchedule = schedules[index],
                defaultDay = selectedDay,
                onDismiss = { editingScheduleIndex = null },
                onSaveSchedule = { updatedSchedule ->
                    val updated = schedules.toMutableList()
                    updated[index] = updatedSchedule
                    saveSchedulesAndUpdate(updated)
                    editingScheduleIndex = null
                    Toast.makeText(context, "Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Clear Schedule Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Schedule") },
            text = { Text("Are you sure you want to remove all saved schedules?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        schedules = emptyList()
                        scope.launch {
                            ScheduleDataStore.saveSchedule(context, "[]")
                        }
                        updateAllWidgets(context)
                        showClearDialog = false
                        Toast.makeText(context, "Schedule cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class FormattedTime(
    val startTime: String,
    val endTime: String,
    val durationText: String
)

fun formatJam(jam: String): FormattedTime {
    val clean = jam.trim()
    val parts = clean.split(Regex("""[-:]"""))
    if (parts.size == 2) {
        val startNum = parts[0].toIntOrNull()
        val endNum = parts[1].toIntOrNull()

        val startFormatted = if (startNum != null) String.format(Locale.US, "%02d:00", startNum) else parts[0]
        val endFormatted = if (endNum != null) String.format(Locale.US, "%02d:00", endNum) else parts[1]

        val duration = if (startNum != null && endNum != null && endNum > startNum) {
            "${endNum - startNum} jam"
        } else {
            ""
        }

        return FormattedTime(startFormatted, endFormatted, duration)
    }
    return FormattedTime(clean, "", "")
}

@Composable
fun CourseScheduleCard(
    schedule: CourseSchedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatted = remember(schedule.jam) { formatJam(schedule.jam) }
    val isNgasdos = schedule.tipe.equals("Ngasdos", ignoreCase = true)

    val badgeContainerColor = if (isNgasdos) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val badgeContentColor = if (isNgasdos) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val badgeText = if (schedule.kode_matkul.isNotEmpty()) {
        "${if (isNgasdos) "Ngasdos" else "Kuliah"} · ${schedule.kode_matkul}"
    } else {
        if (isNgasdos) "Ngasdos" else "Kuliah"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Side: Time Container Box (07:00 | 10:00)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isNgasdos) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.width(76.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeFormatted.startTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isNgasdos) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "|",
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isNgasdos) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.5f)
                    )
                    Text(
                        text = timeFormatted.endTime.ifEmpty { "-" },
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isNgasdos) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Side: Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top Pill Badge: Kuliah / Ngasdos · TC719B
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeContainerColor
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeContentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Card Actions: Edit / Delete
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Jadwal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Jadwal",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Course Name
                Text(
                    text = schedule.nama_matkul,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Location / Room
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Room",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (schedule.ruang.startsWith("Ruang", ignoreCase = true)) schedule.ruang else "Ruang ${schedule.ruang.ifEmpty { "-" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lecturer
                if (schedule.dosen.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Lecturer",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = schedule.dosen,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Duration
                if (timeFormatted.durationText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Duration",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeFormatted.durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyScheduleView(
    hasAnySchedule: Boolean,
    selectedDay: String,
    onUploadPdf: () -> Unit,
    onAddForm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasAnySchedule) {
                Text(
                    text = "Tidak Ada Jadwal di Hari $selectedDay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tidak ada jadwal kuliah atau ngasdos pada hari $selectedDay. Selamat beristirahat!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Belum Ada Jadwal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Impor file KST PDF atau tambahkan jadwal kuliah/ngasdos secara manual lewat form.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onUploadPdf) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload PDF")
                    }
                    OutlinedButton(onClick = onAddForm) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Jadwal")
                    }
                }
            }
        }
    }
}

tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun showTimePicker(
    context: android.content.Context,
    initialTimeStr: String,
    onTimeSelected: (String) -> Unit
) {
    var hour = 8
    var minute = 0

    try {
        val clean = initialTimeStr.trim()
        val isPm = clean.contains("PM", ignoreCase = true)
        val isAm = clean.contains("AM", ignoreCase = true)

        val parts = clean.replace(Regex("""(?i)[a-z]"""), "").trim().split(Regex("""[:\s]"""))
        if (parts.isNotEmpty()) {
            parts[0].trim().toIntOrNull()?.let { hour = it }
        }
        if (parts.size >= 2) {
            parts[1].trim().toIntOrNull()?.let { minute = it }
        }

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
    } catch (_: Exception) {}

    val activity = context.findActivity() ?: return

    TimePickerDialog(
        activity,
        { _, selectedHour, selectedMinute ->
            val formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        },
        hour,
        minute,
        true
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScheduleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    iconColor: Color,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            interactionSource = interactionSource,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = {
                        Box(modifier = Modifier.offset(x = (-8).dp)) {
                            innerTextField()
                        }
                    },
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.offset(x = (-8).dp)
                        )
                    },
                    placeholder = {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.offset(x = (-8).dp)
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .padding(start = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = iconColor,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        focusedBorderColor = iconColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(start = 0.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    container = {
                        OutlinedTextFieldDefaults.ContainerBox(
                            enabled = true,
                            isError = false,
                            interactionSource = interactionSource,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = iconColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )
            }
        )

        if (onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScheduleDialog(
    initialSchedule: CourseSchedule? = null,
    defaultDay: String = "SENIN",
    onDismiss: () -> Unit,
    onSaveSchedule: (CourseSchedule) -> Unit
) {
    val context = LocalContext.current
    var tipe by remember { mutableStateOf(initialSchedule?.tipe?.ifEmpty { "Kuliah" } ?: "Kuliah") }
    var hari by remember {
        mutableStateOf(
            initialSchedule?.hari?.ifEmpty { defaultDay } ?: if (defaultDay == "ALL") "SENIN" else defaultDay
        )
    }
    var namaMatkul by remember { mutableStateOf(initialSchedule?.nama_matkul ?: "") }
    var kodeMatkul by remember { mutableStateOf(initialSchedule?.kode_matkul ?: "") }

    var jamMulai by remember {
        mutableStateOf(
            initialSchedule?.jam?.let {
                val parts = it.split(Regex("""[-:]"""))
                if (parts.size >= 2) ScheduleWidget.formatJamDisplay(parts[0]) else it
            } ?: "08:00"
        )
    }
    var jamSelesai by remember {
        mutableStateOf(
            initialSchedule?.jam?.let {
                val parts = it.split(Regex("""[-:]"""))
                if (parts.size >= 2) ScheduleWidget.formatJamDisplay(parts[1]) else ""
            } ?: "09:40"
        )
    }

    var ruang by remember { mutableStateOf(initialSchedule?.ruang ?: "") }
    var dosen by remember { mutableStateOf(initialSchedule?.dosen ?: "") }

    val daysListShort = listOf(
        "SENIN" to "Sen",
        "SELASA" to "Sel",
        "RABU" to "Rab",
        "KAMIS" to "Kam",
        "JUMAT" to "Jum",
        "SABTU" to "Sab",
        "MINGGU" to "Min"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top drag handle bar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header with blue calendar badge icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (initialSchedule == null) "Tambah jadwal baru" else "Edit jadwal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$tipe · ${ScheduleWidget.formatDayTitle(hari)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Segmented Button: Kuliah vs Ngasdos
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        val isKuliahSelected = tipe.equals("Kuliah", ignoreCase = true)
                        Surface(
                            onClick = { tipe = "Kuliah" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isKuliahSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            border = if (isKuliahSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isKuliahSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kuliah",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val isNgasdosSelected = tipe.equals("Ngasdos", ignoreCase = true)
                        Surface(
                            onClick = { tipe = "Ngasdos" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNgasdosSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            border = if (isNgasdosSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isNgasdosSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.CoPresent,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ngasdos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Hari Selection Pills
                Column {
                    Text(
                        text = "Hari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysListShort.forEach { (fullDay, shortDay) ->
                            val isSelected = hari.equals(fullDay, ignoreCase = true)
                            Surface(
                                onClick = { hari = fullDay },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortDay,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Field 1: Nama matkul (Full-Width)
                CustomScheduleTextField(
                    value = namaMatkul,
                    onValueChange = { namaMatkul = it },
                    label = "Nama matkul",
                    placeholder = "Nama matkul",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconColor = Color(0xFF64B5F6),
                    modifier = Modifier.fillMaxWidth()
                )

                // Row 1: Mulai & Selesai (Time Fields)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomScheduleTextField(
                        value = jamMulai,
                        onValueChange = { jamMulai = it },
                        label = "Mulai",
                        placeholder = "08:00",
                        icon = Icons.Default.Schedule,
                        iconColor = Color(0xFFFFB74D),
                        readOnly = true,
                        onClick = {
                            showTimePicker(context, jamMulai) { selected ->
                                jamMulai = selected
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CustomScheduleTextField(
                        value = jamSelesai,
                        onValueChange = { jamSelesai = it },
                        label = "Selesai",
                        placeholder = "09:40",
                        icon = Icons.Default.Schedule,
                        iconColor = Color(0xFFFFB74D),
                        readOnly = true,
                        onClick = {
                            showTimePicker(context, jamSelesai) { selected ->
                                jamSelesai = selected
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Kode & Ruang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomScheduleTextField(
                        value = kodeMatkul,
                        onValueChange = { kodeMatkul = it },
                        label = "Kode",
                        placeholder = "Kode",
                        icon = Icons.Default.Code,
                        iconColor = Color(0xFFB39DDB),
                        modifier = Modifier.weight(1f)
                    )

                    CustomScheduleTextField(
                        value = ruang,
                        onValueChange = { ruang = it },
                        label = "Ruang",
                        placeholder = "Ruang",
                        icon = Icons.Default.LocationOn,
                        iconColor = Color(0xFFEF9A9A),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Field 2: Dosen (Full-Width with Green Icon/Border)
                CustomScheduleTextField(
                    value = dosen,
                    onValueChange = { dosen = it },
                    label = "Dosen",
                    placeholder = "Dosen",
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF81C784),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val combinedJam = if (jamSelesai.isNotBlank()) "$jamMulai - $jamSelesai" else jamMulai
                    onSaveSchedule(
                        CourseSchedule(
                            hari = if (hari == "ALL") "SENIN" else hari.uppercase(),
                            jam = combinedJam,
                            ruang = ruang,
                            kode_matkul = kodeMatkul,
                            nama_matkul = namaMatkul,
                            dosen = dosen,
                            tipe = tipe
                        )
                    )
                },
                enabled = namaMatkul.isNotBlank() && jamMulai.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

private fun parseScheduleJson(jsonStr: String): List<CourseSchedule> {
    return try {
        Json.decodeFromString(jsonStr)
    } catch (_: Exception) {
        emptyList()
    }
}

private fun updateAllWidgets(context: android.content.Context) {
    CoroutineScope(Dispatchers.Default).launch {
        try {
            GlanceAppWidgetManager(context).getGlanceIds(ScheduleWidget::class.java).forEach { glanceId ->
                ScheduleWidget().update(context, glanceId)
            }
        } catch (_: Exception) { }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScheduleScreenPreview() {
    KSTViewTheme {
        MainScheduleScreen()
    }
}
