package com.example.kstview

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleModelUnitTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun testCourseScheduleDefaultTypeIsKuliah() {
        val course = CourseSchedule(
            hari = "SENIN",
            jam = "07:00 - 09:30",
            ruang = "IF-101",
            kode_matkul = "TC719B",
            nama_matkul = "Pemrograman Berbasis Objek",
            dosen = "Dr. Budi"
        )
        assertEquals("Kuliah", course.tipe)
    }

    @Test
    fun testCourseScheduleNgasdosType() {
        val course = CourseSchedule(
            hari = "SELASA",
            jam = "10:00 - 12:00",
            ruang = "Lab AI",
            kode_matkul = "TC715B",
            nama_matkul = "Praktikum Kecerdasan Buatan",
            dosen = "Dr. Andi",
            tipe = "Ngasdos"
        )
        assertEquals("Ngasdos", course.tipe)

        val encodedJson = json.encodeToString(listOf(course))
        val decodedList = json.decodeFromString<List<CourseSchedule>>(encodedJson)

        assertEquals(1, decodedList.size)
        assertEquals("Ngasdos", decodedList[0].tipe)
    }

    @Test
    fun testBackwardCompatibilityWithLegacyJson() {
        val legacyJson = """
            [
              {
                "hari": "RABU",
                "jam": "13-16",
                "ruang": "TC715B",
                "kode_matkul": "FTI300",
                "nama_matkul": "MANAJEMEN PROYEK",
                "dosen": "ADI NUGROHO"
              }
            ]
        """.trimIndent()

        val decodedList = json.decodeFromString<List<CourseSchedule>>(legacyJson)
        assertEquals(1, decodedList.size)
        assertEquals("Kuliah", decodedList[0].tipe)
    }
}
