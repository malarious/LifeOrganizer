package com.geepscontrol.app

import android.content.Context
import android.os.Environment
import com.geepscontrol.core.data.DataFileGateway
import com.geepscontrol.core.database.DATABASE_NAME
import com.geepscontrol.core.database.GeepsDatabase
import java.io.File

class AndroidDataFileGateway(
    private val context: Context,
    private val database: GeepsDatabase,
) : DataFileGateway {
    private val exportDirectory: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "LifeMinder").apply { mkdirs() }

    override suspend fun writeExport(
        fileName: String,
        mimeType: String,
        content: String,
    ): String {
        val target = File(exportDirectory, fileName)
        val temporary = File(exportDirectory, "$fileName.tmp")
        temporary.writeText(content)
        check(temporary.length() > 0) { "Export was empty" }
        if (target.exists()) target.delete()
        check(temporary.renameTo(target)) { "Could not finalize export" }
        return target.absolutePath
    }

    override suspend fun readImport(fileName: String): String? = File(exportDirectory, fileName).takeIf(File::isFile)?.readText()

    override suspend fun backupDatabase(): String {
        val source = context.getDatabasePath(DATABASE_NAME)
        require(source.isFile) { "Database does not exist" }
        val folder = File(exportDirectory, "backup-${System.currentTimeMillis()}").apply { mkdirs() }
        listOf(source, File(source.path + "-wal"), File(source.path + "-shm")).filter(File::isFile).forEach { file ->
            file.copyTo(File(folder, file.name), overwrite = true)
        }
        File(context.filesDir, "pixel-studio")
            .takeIf(File::isDirectory)
            ?.copyRecursively(File(folder, "pixel-studio"), overwrite = true)
        require(hasSqliteHeader(File(folder, source.name))) { "Backup validation failed" }
        return folder.absolutePath
    }

    override suspend fun restoreDatabase(): String {
        val source = context.getDatabasePath(DATABASE_NAME)
        val backup = exportDirectory.listFiles()?.filter { it.isDirectory && it.name.startsWith("backup-") }?.maxByOrNull { it.lastModified() } ?: error("No backup found")
        val replacement = File(backup, source.name)
        require(hasSqliteHeader(replacement)) { "Selected backup is not a valid SQLite database" }
        database.close()
        val rollback = File(exportDirectory, "pre-restore-${System.currentTimeMillis()}").apply { mkdirs() }
        listOf(source, File(source.path + "-wal"), File(source.path + "-shm")).filter(File::isFile).forEach { it.copyTo(File(rollback, it.name), overwrite = true) }
        listOf("", "-wal", "-shm").forEach { suffix ->
            val incoming = File(backup, source.name + suffix)
            val target = File(source.path + suffix)
            if (incoming.exists()) {
                incoming.copyTo(target, overwrite = true)
            } else if (target.exists()) {
                target.delete()
            }
        }
        val pixelBackup = File(backup, "pixel-studio")
        if (pixelBackup.isDirectory) {
            val pixelTarget = File(context.filesDir, "pixel-studio")
            pixelTarget.deleteRecursively()
            pixelBackup.copyRecursively(pixelTarget, overwrite = true)
        }
        require(hasSqliteHeader(source)) { "Restore verification failed; rollback is at ${rollback.absolutePath}" }
        return "Restored ${backup.name}. Restart Life Minder before continuing. Rollback: ${rollback.absolutePath}"
    }

    override suspend fun deleteAllLocalData(): String {
        database.close()
        val databaseDeleted = context.deleteDatabase(DATABASE_NAME)
        File(context.filesDir, "pixel-studio").deleteRecursively()
        check(databaseDeleted) { "Database could not be deleted" }
        return "All local database data deleted. Restart Life Minder."
    }

    private fun hasSqliteHeader(file: File): Boolean {
        if (!file.isFile || file.length() < 16) return false
        return file.inputStream().use { input -> ByteArray(16).also { input.read(it) }.decodeToString() == "SQLite format 3\u0000" }
    }
}
