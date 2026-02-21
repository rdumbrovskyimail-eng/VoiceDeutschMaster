package com.voicedeutsch.master.data.local.file

import android.content.Context
import com.voicedeutsch.master.data.local.database.AppDatabase
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Exports/imports user data as JSON for backup and transfer.
 * Architecture lines 4.3, 5.3 (ExportImportManager).
 */
class ExportImportManager(
    private val context: Context,
    private val json: Json
) {

    @Serializable
    data class ExportData(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val userName: String = "",
        val cefrLevel: String = "",
        val wordsKnown: Int = 0,
        val rulesKnown: Int = 0,
        val totalSessions: Int = 0,
        val totalMinutes: Int = 0,
        val note: String = "VoiceDeutschMaster export"
    )

    /**
     * Exports a summary of user data to a JSON file.
     * Full DB export is handled by BackupManager (binary copy).
     */
    fun exportSummary(data: ExportData): File {
        val exportDir = File(context.filesDir, "exports")
        exportDir.mkdirs()
        val file = File(exportDir, "vdm_export_${System.currentTimeMillis()}.json")
        file.writeText(json.encodeToString(data))
        return file
    }

    /** Lists available export files. */
    fun listExports(): List<File> {
        val exportDir = File(context.filesDir, "exports")
        return exportDir.listFiles { _, name -> name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}