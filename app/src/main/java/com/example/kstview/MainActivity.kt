package com.example.kstview

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditScheduleDialog(
    initialSchedule: CourseSchedule? = null,
    defaultDay: String = "SENIN",
    onDismiss: () -> Unit,
    onSaveSchedule: (CourseSchedule) -> Unit
) {
    var tipe by remember { mutableStateOf(initialSchedule?.tipe?.ifEmpty { "Kuliah" } ?: "Kuliah") }
    var hari by remember {
        mutableStateOf(
            initialSchedule?.hari?.ifEmpty { defaultDay } ?: if (defaultDay == "ALL") "SENIN" else defaultDay
        )
    }
    var namaMatkul by remember { mutableStateOf(initialSchedule?.nama_matkul ?: "") }
    var kodeMatkul by remember { mutableStateOf(initialSchedule?.kode_matkul ?: "") }
    var jam by remember { mutableStateOf(initialSchedule?.jam ?: "") }
    var ruang by remember { mutableStateOf(initialSchedule?.ruang ?: "") }
    var dosen by remember { mutableStateOf(initialSchedule?.dosen ?: "") }

    val daysList = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchedule == null) "Tambah Jadwal Baru" else "Edit Jadwal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tipe Selection: Kuliah vs Ngasdos
                Text(
                    text = "Kategori / Tipe Jadwal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tipe.equals("Kuliah", ignoreCase = true),
                        onClick = { tipe = "Kuliah" },
                        label = { Text("🎓 Kuliah") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = tipe.equals("Ngasdos", ignoreCase = true),
                        onClick = { tipe = "Ngasdos" },
                        label = { Text("👨‍🏫 Ngasdos") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hari Selection
                Text(
                    text = "Hari",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysList.forEach { dayItem ->
                        FilterChip(
                            selected = hari.equals(dayItem, ignoreCase = true),
                            onClick = { hari = dayItem },
                            label = { Text(dayItem.take(3)) }
                        )
                    }
                }

                // Nama Mata Kuliah
                OutlinedTextField(
                    value = namaMatkul,
                    onValueChange = { namaMatkul = it },
                    label = { Text("Nama Matkul / Kegiatan *") },
                    placeholder = { Text("e.g. Pemrograman Berbasis Objek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Jam / Waktu
                    OutlinedTextField(
                        value = jam,
                        onValueChange = { jam = it },
                        label = { Text("Jam / Waktu *") },
                        placeholder = { Text("07:00 - 09:30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Kode Matkul
                    OutlinedTextField(
                        value = kodeMatkul,
                        onValueChange = { kodeMatkul = it },
                        label = { Text("Kode Matkul") },
                        placeholder = { Text("TC719B") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ruang
                    OutlinedTextField(
                        value = ruang,
                        onValueChange = { ruang = it },
                        label = { Text("Ruang / Lab") },
                        placeholder = { Text("IF-101 / Lab AI") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Dosen / Koordinator
                    OutlinedTextField(
                        value = dosen,
                        onValueChange = { dosen = it },
                        label = { Text("Dosen / Dosen PJ") },
                        placeholder = { Text("Nama Dosen") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSchedule(
                        CourseSchedule(
                            hari = if (hari == "ALL") "SENIN" else hari.uppercase(),
                            jam = jam,
                            ruang = ruang,
                            kode_matkul = kodeMatkul,
                            nama_matkul = namaMatkul,
                            dosen = dosen,
                            tipe = tipe
                        )
                    )
                },
                enabled = namaMatkul.isNotBlank() && jam.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
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
