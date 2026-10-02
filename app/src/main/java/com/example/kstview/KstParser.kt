package com.example.kstview

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CourseSchedule(
    val hari: String,
    val jam: String,
    val ruang: String,
    val kode_matkul: String,
    val nama_matkul: String,
    val dosen: String
)

object KstParser {
    private val json = Json { prettyPrint = true }

    fun extractTextFromPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                val text = stripper.getText(document)
                document.close()
                text
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parse(rawText: String): List<CourseSchedule> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val schedules = mutableListOf<CourseSchedule>()

        var currentDay = ""
        val dayRegex = Regex("^(SENIN|SELASA|RABU|KAMIS|JUMAT|SABTU|MINGGU)$", RegexOption.IGNORE_CASE)
        val dayInLineRegex = Regex("""\b(SENIN|SELASA|RABU|KAMIS|JUMAT|SABTU|MINGGU)\b""", RegexOption.IGNORE_CASE)
        val timeRegex = Regex("""\b(\d{1,2}[-:]\d{1,2})\b""")
        val sksRegex = Regex("""\s+(\d+)\s+(\d+)\s+([A-Z])(?:\s+|$)""")
        val codeRegex = Regex("""\b([A-Z]{2,4}\d{3,4}[A-Z]?)\b""")
        val roomRegex = Regex("""\b([A-Z]{2,4}\d{3,4})\b""")

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // Check if line is solely a day name
            val dayExactMatch = dayRegex.find(line)
            if (dayExactMatch != null) {
                currentDay = dayExactMatch.value.uppercase()
                i++
                continue
            }

            // Check if line contains a day at start or embedded
            val dayMatchInLine = dayInLineRegex.find(line)
            if (dayMatchInLine != null) {
                currentDay = dayMatchInLine.value.uppercase()
            }

            val timeMatch = timeRegex.find(line)
            if (timeMatch != null && currentDay.isNotEmpty()) {
                val jam = timeMatch.value

                // Full-line format check (e.g. "SENIN 13-16 FTI300 TC715B MANAJEMEN PROYEK 3 3 B ADI NUGROHO")
                val sksMatch = sksRegex.find(line)
                if (sksMatch != null) {
                    val sksStart = sksMatch.range.first
                    val sksEnd = sksMatch.range.last

                    var leftPart = line.substring(0, sksStart).trim()
                    val dosen = line.substring(sksEnd + 1).trim()

                    // Remove day if present in leftPart
                    dayMatchInLine?.value?.let { dayStr ->
                        leftPart = leftPart.replaceFirst(dayStr, "", ignoreCase = true).trim()
                    }

                    // Remove time
                    leftPart = leftPart.replaceFirst(jam, "").trim()

                    // Find room
                    val roomMatch = roomRegex.find(leftPart)
                    val ruang = roomMatch?.value ?: ""
                    if (ruang.isNotEmpty()) {
                        leftPart = leftPart.replaceFirst(ruang, "").trim()
                    }

                    // Find course code
                    val codeMatch = codeRegex.find(leftPart)
                    val kode = codeMatch?.value ?: ""
                    if (kode.isNotEmpty()) {
                        leftPart = leftPart.replaceFirst(kode, "").trim()
                    }

                    val namaMatkul = leftPart.trim()

                    schedules.add(
                        CourseSchedule(
                            hari = currentDay,
                            jam = jam,
                            ruang = ruang,
                            kode_matkul = kode,
                            nama_matkul = namaMatkul,
                            dosen = dosen
                        )
                    )
                    i++
                } else if (line.length > 15) {
                    // Line format without explicit SKS match
                    var workingText = line
                    dayMatchInLine?.value?.let { dayStr ->
                        workingText = workingText.replaceFirst(dayStr, "", ignoreCase = true).trim()
                    }
                    workingText = workingText.replaceFirst(jam, "").trim()

                    val roomMatch = roomRegex.find(workingText)
                    val ruang = roomMatch?.value ?: ""
                    if (ruang.isNotEmpty()) {
                        workingText = workingText.replaceFirst(ruang, "").trim()
                    }

                    val codeMatch = codeRegex.find(workingText)
                    val kode = codeMatch?.value ?: ""
                    if (kode.isNotEmpty()) {
                        workingText = workingText.replaceFirst(kode, "").trim()
                    }

                    schedules.add(
                        CourseSchedule(
                            hari = currentDay,
                            jam = jam,
                            ruang = ruang,
                            kode_matkul = kode,
                            nama_matkul = workingText.trim(),
                            dosen = ""
                        )
                    )
                    i++
                } else {
                    // Token-per-line format
                    val ruang = lines.getOrNull(i + 1)?.takeIf { roomRegex.matches(it) || it.startsWith("FT") } ?: ""
                    var nextOffset = if (ruang.isNotEmpty()) 2 else 1

                    val kode = lines.getOrNull(i + nextOffset)?.takeIf { codeRegex.matches(it) || it.startsWith("TC") } ?: ""
                    if (kode.isNotEmpty()) nextOffset++

                    val namaMatkul = lines.getOrNull(i + nextOffset) ?: ""
                    if (namaMatkul.isNotEmpty()) nextOffset++

                    val dosen = lines.getOrNull(i + nextOffset)?.takeIf { !dayRegex.matches(it) && !timeRegex.containsMatchIn(it) } ?: ""
                    if (dosen.isNotEmpty()) nextOffset++

                    schedules.add(
                        CourseSchedule(
                            hari = currentDay,
                            jam = jam,
                            ruang = ruang,
                            kode_matkul = kode,
                            nama_matkul = namaMatkul,
                            dosen = dosen
                        )
                    )
                    i += nextOffset
                }
            } else {
                i++
            }
        }

        return schedules
    }

    fun toJson(schedules: List<CourseSchedule>): String {
        return json.encodeToString(schedules)
    }
}
