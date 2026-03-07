// Путь: src/test/java/com/voicedeutsch/master/data/local/file/ExportImportManagerTest.kt
package com.voicedeutsch.master.data.local.file

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ExportImportManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var json: Json
    private lateinit var sut: ExportImportManager

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir
        json = Json { ignoreUnknownKeys = true }
        sut = ExportImportManager(context, json)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildExportData(
        version: Int = 1,
        exportedAt: Long = 1000L,
        userName: String = "Max",
        cefrLevel: String = "B1",
        wordsKnown: Int = 150,
        rulesKnown: Int = 20,
        totalSessions: Int = 42,
        totalMinutes: Int = 630,
        note: String = "VoiceDeutschMaster export",
    ) = ExportImportManager.ExportData(
        version = version,
        exportedAt = exportedAt,
        userName = userName,
        cefrLevel = cefrLevel,
        wordsKnown = wordsKnown,
        rulesKnown = rulesKnown,
        totalSessions = totalSessions,
        totalMinutes = totalMinutes,
        note = note,
    )

    // ── ExportData data class ─────────────────────────────────────────────────

    @Test
    fun exportData_defaultValues_areCorrect() {
        val data = ExportImportManager.ExportData()
        assertEquals(1, data.version)
        assertEquals("", data.userName)
        assertEquals("", data.cefrLevel)
        assertEquals(0, data.wordsKnown)
        assertEquals(0, data.rulesKnown)
        assertEquals(0, data.totalSessions)
        assertEquals(0, data.totalMinutes)
        assertEquals("VoiceDeutschMaster export", data.note)
    }

    @Test
    fun exportData_defaultExportedAt_isRecentTimestamp() {
        val before = System.currentTimeMillis()
        val data = ExportImportManager.ExportData()
        val after = System.currentTimeMillis()
        assertTrue(data.exportedAt in before..after)
    }

    @Test
    fun exportData_copy_changesOneField() {
        val original = buildExportData(userName = "Max")
        val copy = original.copy(userName = "Anna")
        assertEquals("Anna", copy.userName)
        assertEquals(original.cefrLevel, copy.cefrLevel)
        assertEquals(original.wordsKnown, copy.wordsKnown)
    }

    @Test
    fun exportData_equals_twoIdenticalInstances() {
        val a = buildExportData()
        val b = buildExportData()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun exportData_notEquals_differentUserName() {
        val a = buildExportData(userName = "Max")
        val b = buildExportData(userName = "Anna")
        assertNotEquals(a, b)
    }

    // ── exportSummary ─────────────────────────────────────────────────────────

    @Test
    fun exportSummary_returnsFile_thatExists() {
        val data = buildExportData()
        val file = sut.exportSummary(data)
        assertTrue(file.exists())
    }

    @Test
    fun exportSummary_createsExportsDirectory() {
        val data = buildExportData()
        sut.exportSummary(data)
        assertTrue(File(tempDir, "exports").exists())
    }

    @Test
    fun exportSummary_fileHasJsonExtension() {
        val file = sut.exportSummary(buildExportData())
        assertTrue(file.name.endsWith(".json"))
    }

    @Test
    fun exportSummary_fileNameContainsVdmExport() {
        val file = sut.exportSummary(buildExportData())
        assertTrue(file.name.startsWith("vdm_export_"))
    }

    @Test
    fun exportSummary_fileContentIsValidJson() {
        val data = buildExportData()
        val file = sut.exportSummary(data)
        val content = file.readText()
        assertDoesNotThrow {
            json.decodeFromString<ExportImportManager.ExportData>(content)
        }
    }

    @Test
    fun exportSummary_fileContentDeserializesCorrectly() {
        val data = buildExportData(
            userName = "Klaus",
            cefrLevel = "C1",
            wordsKnown = 500,
            rulesKnown = 60,
            totalSessions = 100,
            totalMinutes = 1500,
            note = "test note",
        )
        val file = sut.exportSummary(data)
        val loaded = json.decodeFromString<ExportImportManager.ExportData>(file.readText())

        assertEquals("Klaus", loaded.userName)
        assertEquals("C1", loaded.cefrLevel)
        assertEquals(500, loaded.wordsKnown)
        assertEquals(60, loaded.rulesKnown)
        assertEquals(100, loaded.totalSessions)
        assertEquals(1500, loaded.totalMinutes)
        assertEquals("test note", loaded.note)
        assertEquals(1, loaded.version)
    }

    @Test
    fun exportSummary_multipleExports_createMultipleFiles() {
        sut.exportSummary(buildExportData(userName = "A"))
        Thread.sleep(2) // ensure different timestamps
        sut.exportSummary(buildExportData(userName = "B"))
        val exports = sut.listExports()
        assertEquals(2, exports.size)
    }

    @Test
    fun exportSummary_fileIsInExportsSubdir() {
        val file = sut.exportSummary(buildExportData())
        assertTrue(file.absolutePath.contains("exports"))
    }

    @Test
    fun exportSummary_exportsDirectoryAlreadyExists_doesNotThrow() {
        File(tempDir, "exports").mkdirs()
        assertDoesNotThrow { sut.exportSummary(buildExportData()) }
    }

    @Test
    fun exportSummary_emptyData_writesValidJson() {
        val file = sut.exportSummary(ExportImportManager.ExportData())
        val content = file.readText()
        assertTrue(content.isNotBlank())
        assertDoesNotThrow {
            json.decodeFromString<ExportImportManager.ExportData>(content)
        }
    }

    // ── listExports ───────────────────────────────────────────────────────────

    @Test
    fun listExports_noExportsDir_returnsEmptyList() {
        val result = sut.listExports()
        assertEquals(emptyList<File>(), result)
    }

    @Test
    fun listExports_emptyDir_returnsEmptyList() {
        File(tempDir, "exports").mkdirs()
        val result = sut.listExports()
        assertEquals(emptyList<File>(), result)
    }

    @Test
    fun listExports_afterOneExport_returnsOneFile() {
        sut.exportSummary(buildExportData())
        val result = sut.listExports()
        assertEquals(1, result.size)
    }

    @Test
    fun listExports_sortedByLastModifiedDescending() {
        val exportsDir = File(tempDir, "exports").apply { mkdirs() }
        val older = File(exportsDir, "vdm_export_1000.json").apply {
            writeText("{}")
            setLastModified(1000L)
        }
        val newer = File(exportsDir, "vdm_export_3000.json").apply {
            writeText("{}")
            setLastModified(3000L)
        }
        val newest = File(exportsDir, "vdm_export_5000.json").apply {
            writeText("{}")
            setLastModified(5000L)
        }

        val result = sut.listExports()

        assertEquals(3, result.size)
        assertEquals(newest.name, result[0].name)
        assertEquals(newer.name, result[1].name)
        assertEquals(older.name, result[2].name)
    }

    @Test
    fun listExports_nonJsonFilesIgnored() {
        val exportsDir = File(tempDir, "exports").apply { mkdirs() }
        File(exportsDir, "vdm_export_1000.json").writeText("{}")
        File(exportsDir, "readme.txt").writeText("ignore me")
        File(exportsDir, "data.csv").writeText("also ignore")

        val result = sut.listExports()

        assertEquals(1, result.size)
        assertTrue(result[0].name.endsWith(".json"))
    }

    @Test
    fun listExports_allFilesHaveJsonExtension() {
        sut.exportSummary(buildExportData())
        sut.exportSummary(buildExportData())
        val result = sut.listExports()
        assertTrue(result.all { it.name.endsWith(".json") })
    }

    @Test
    fun listExports_returnsFileObjects_thatExist() {
        sut.exportSummary(buildExportData())
        val result = sut.listExports()
        assertTrue(result.all { it.exists() })
    }

    @Test
    fun listExports_afterDeletingFile_returnsRemainingFiles() {
        sut.exportSummary(buildExportData(userName = "A"))
        Thread.sleep(2)
        sut.exportSummary(buildExportData(userName = "B"))

        val files = sut.listExports()
        assertEquals(2, files.size)

        files.first().delete()
        val remaining = sut.listExports()
        assertEquals(1, remaining.size)
    }
}
