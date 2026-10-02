package com.example.kstview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KstParserTest {

    @Test
    fun testParseSimpleKst() {
        val rawText = """
            SENIN
            13-16
            FT1300
            TC715B
            MANAJEMEN PROYEK
            ADI NUGROHO
            SELASA
            07-10
            FT1464
            TC719B
            PEMROGRAMAN BERORIENTASI PLATFORM
            ANGLING KUSUMANDHITA RADEN
        """.trimIndent()

        val result = KstParser.parse(rawText)

        assertEquals(2, result.size)
        
        assertEquals("SENIN", result[0].hari)
        assertEquals("13-16", result[0].jam)
        assertEquals("FT1300", result[0].ruang)
        assertEquals("TC715B", result[0].kode_matkul)
        assertEquals("MANAJEMEN PROYEK", result[0].nama_matkul)
        assertEquals("ADI NUGROHO", result[0].dosen)

        assertEquals("SELASA", result[1].hari)
        assertEquals("07-10", result[1].jam)
        assertEquals("FT1464", result[1].ruang)
        assertEquals("TC719B", result[1].kode_matkul)
        assertEquals("PEMROGRAMAN BERORIENTASI PLATFORM", result[1].nama_matkul)
        assertEquals("ANGLING KUSUMANDHITA RADEN", result[1].dosen)
    }

    @Test
    fun testParseFullUkswKstSchedule() {
        val rawText = """
            Bagian Registrasi Dan Admisi
            Universitas Kristen Satya Wacana
            5 September 2026
            KARTU STUDI TETAP
            Semester 1 Tahun 2026-2027
            NIM : 672024190
            Nama : ERLANGGA BINTANG PRATAMA
            Fakultas : TEKNOLOGI INFORMASI
            Jurusan : TEKNIK INFORMATIKA
            Wali Studi : YERYMIA ALFA SUSETYO, S.Kom., M.
            Hari Jam Ruang Kode Matakuliah SKS A SKS B B/L/P Pengajar
            SENIN 13-16 FTI300 TC715B MANAJEMEN PROYEK 3 3 B ADI NUGROHO
            SELASA 07-10 FTI464 TC719B PEMROGRAMAN BERORIENTASI PLATFORM 3 3 B ANGLING KUSUMANDHITA RADEN
            RABU 07-10 FTI308 TC735E DEEP LEARNING 3 3 B EVANGS MAILOA
            KAMIS 10-12 FTI329 TC730F RISET OPERASI 3 3 B ERYKA PANDU EKA LIA D.
            16-18 FTI308 TC1710 KALKULUS 3 4 B NATALIA BAYU
            JUMAT 09-11 FTI302 TC015B ASIT RISET OPERASI 0 0 B ASISTEN 6
            13-15 FTI465 TC019G ASISTENSI KALKULUS 0 0 B ASISTEN 1
            SABTU 09-12 FTI451 TC367A KAPITA SELEKTA SOFTWARE ENGINEERING 3 3 B REZA NORMAND AGUNG WIJAYA
            Total 18 20
        """.trimIndent()

        val result = KstParser.parse(rawText)

        assertEquals(8, result.size)

        // Course 1: SENIN
        assertEquals("SENIN", result[0].hari)
        assertEquals("13-16", result[0].jam)
        assertEquals("FTI300", result[0].ruang)
        assertEquals("TC715B", result[0].kode_matkul)
        assertEquals("MANAJEMEN PROYEK", result[0].nama_matkul)
        assertEquals("ADI NUGROHO", result[0].dosen)

        // Course 4: KAMIS 1
        assertEquals("KAMIS", result[3].hari)
        assertEquals("10-12", result[3].jam)
        assertEquals("FTI329", result[3].ruang)
        assertEquals("TC730F", result[3].kode_matkul)
        assertEquals("RISET OPERASI", result[3].nama_matkul)
        assertEquals("ERYKA PANDU EKA LIA D.", result[3].dosen)

        // Course 5: KAMIS 2 (inherited day KAMIS)
        assertEquals("KAMIS", result[4].hari)
        assertEquals("16-18", result[4].jam)
        assertEquals("FTI308", result[4].ruang)
        assertEquals("TC1710", result[4].kode_matkul)
        assertEquals("KALKULUS", result[4].nama_matkul)
        assertEquals("NATALIA BAYU", result[4].dosen)

        // Course 6: JUMAT 1
        assertEquals("JUMAT", result[5].hari)
        assertEquals("09-11", result[5].jam)
        assertEquals("ASIT RISET OPERASI", result[5].nama_matkul)

        // Course 7: JUMAT 2
        assertEquals("JUMAT", result[6].hari)
        assertEquals("13-15", result[6].jam)
        assertEquals("ASISTENSI KALKULUS", result[6].nama_matkul)

        // Course 8: SABTU
        assertEquals("SABTU", result[7].hari)
        assertEquals("09-12", result[7].jam)
        assertEquals("KAPITA SELEKTA SOFTWARE ENGINEERING", result[7].nama_matkul)
    }

    @Test
    fun testIndonesianDayNameMapping() {
        val todayIndo = ScheduleWidget.getTodayIndonesianDayName()
        val validDays = setOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")
        assertTrue(validDays.contains(todayIndo))
    }
}
