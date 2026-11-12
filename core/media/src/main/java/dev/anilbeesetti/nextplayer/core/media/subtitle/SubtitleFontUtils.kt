package dev.anilbeesetti.nextplayer.core.media.subtitle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder

object SubtitleFontUtils {

    private const val FONTS_DIR_NAME = "fonts"

    fun getFontDir(context: Context): File {
        val dir = File(
            context.filesDir,
            FONTS_DIR_NAME,
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun cleanupUnusedFontFiles(context: Context, activeFileName: String? = null) {
        val fontDir = getFontDir(context)
        fontDir.listFiles()?.forEach { file ->
            if (file.isFile && (activeFileName.isNullOrBlank() || file.name != activeFileName)) {
                runCatching { file.delete() }
            }
        }
    }

    fun extractFontFileName(context: Context, uri: Uri, fallbackName: String? = null): String {
        if (!fallbackName.isNullOrBlank()) {
            return sanitizeFileName(fallbackName)
        }

        if (uri.scheme == "content") {
            val nameFromResolver = runCatching {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) cursor.getString(nameIndex) else null
                    } else {
                        null
                    }
                }
            }.getOrNull()
            if (!nameFromResolver.isNullOrBlank()) {
                return sanitizeFileName(nameFromResolver)
            }
        }

        val uriString = uri.toString()
        val decoded = runCatching {
            URLDecoder.decode(
                uriString,
                "UTF-8",
            )
        }.getOrDefault(uriString)
        val rawName = decoded.substringAfterLast(
            '/',
        ).substringAfterLast('%').substringAfterLast(':')
        if (rawName.endsWith(
                ".ttf",
                ignoreCase = true,
            ) ||
            rawName.endsWith(
                ".otf",
                ignoreCase = true,
            ) ||
            rawName.endsWith(
                ".woff",
                ignoreCase = true,
            ) ||
            rawName.endsWith(
                ".woff2",
                ignoreCase = true,
            )
        ) {
            return sanitizeFileName(rawName)
        }

        return "subtitle_font.ttf"
    }

    fun sanitizeFileName(fileName: String): String {
        val clean = fileName.replace(
            Regex("[^a-zA-Z0-9._-]"),
            "_",
        )
        return if (clean.contains('.')) clean else "$clean.ttf"
    }

    @SuppressLint("UseKtx")
    fun resolveFontFile(context: Context, uriString: String): File? {
        if (uriString.isBlank()) return null
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return null

        // 1. Direct file path or file:// URI
        if (uri.scheme == "file" || uri.scheme == null) {
            val path = uri.path ?: uriString
            val file = File(path)
            if (file.exists() && file.canRead()) return file
        }

        // 2. App internal storage check by exact filename or path
        val fontDir = getFontDir(context)
        val fileName = extractFontFileName(
            context,
            uri,
        )
        val internalFile = File(
            fontDir,
            fileName,
        )
        if (internalFile.exists() && internalFile.canRead()) {
            return internalFile
        }

        // Search for any existing font file in internal fontDir if specific file not found
        val fontFiles = fontDir.listFiles()?.filter { it.isFile && it.canRead() }
        if (!fontFiles.isNullOrEmpty()) {
            val matching = fontFiles.firstOrNull {
                uriString.contains(it.name) || uriString == it.absolutePath ||
                    uriString == Uri.fromFile(it).toString()
            }
            if (matching != null) return matching
        }

        // 3. Try to copy content:// URI if accessible
        if (uri.scheme == "content") {
            val copied = copyFontToInternalStorage(
                context,
                uri,
            )
            if (copied != null && copied.exists()) return copied

            // 4. Try parsing SAF document ID (e.g. primary:path/to/font.ttf)
            val safPath = parseSafDocumentPath(uriString)
            if (safPath != null) {
                val safFile = File(safPath)
                if (safFile.exists() && safFile.canRead()) {
                    return copyFileToInternalStorage(
                        context,
                        safFile,
                        safFile.name,
                    ) ?: safFile
                }
            }
        }

        return fontFiles?.firstOrNull()
    }

    @SuppressLint("UseKtx")
    fun loadTypeface(context: Context, uriString: String): Typeface? {
        if (uriString.isBlank()) return null
        val file = resolveFontFile(
            context,
            uriString,
        )
        if (file != null && file.exists() && file.canRead()) {
            val typeface = runCatching { Typeface.createFromFile(file) }.getOrNull()
            if (typeface != null) return typeface
        }

        // Fallback: try opening input stream via ContentResolver
        return runCatching {
            val uri = uriString.toUri()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val tempFile = File.createTempFile(
                    "font_temp_",
                    ".ttf",
                    context.cacheDir,
                )
                try {
                    tempFile.outputStream().use { out -> stream.copyTo(out) }
                    Typeface.createFromFile(tempFile)
                } finally {
                    tempFile.delete()
                }
            }
        }.getOrNull()
    }

    fun copyFontToInternalStorage(
        context: Context,
        uri: Uri,
        preferredName: String? = null,
    ): File? {
        return try {
            val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = extractFontFileName(
                context,
                uri,
                preferredName,
            )
            val fontDir = getFontDir(context)
            val destFile = File(
                fontDir,
                fileName,
            )
            stream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                cleanupUnusedFontFiles(
                    context,
                    destFile.name,
                )
                destFile
            } else {
                null
            }
        } catch (_: Exception) {
            val safPath = parseSafDocumentPath(uri.toString())
            if (safPath != null) {
                val safFile = File(safPath)
                if (safFile.exists() && safFile.canRead()) {
                    return copyFileToInternalStorage(
                        context,
                        safFile,
                        preferredName ?: safFile.name,
                    )
                }
            }
            null
        }
    }

    fun saveFontBytesToInternalStorage(
        context: Context,
        bytes: ByteArray,
        preferredName: String? = null,
    ): File? = try {
        val fileName = sanitizeFileName(preferredName ?: "subtitle_font.ttf")
        val destFile = File(
            getFontDir(context),
            fileName,
        )
        FileOutputStream(destFile).use { it.write(bytes) }
        if (destFile.exists() && destFile.length() > 0) {
            cleanupUnusedFontFiles(
                context,
                destFile.name,
            )
            destFile
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    @SuppressLint("UseKtx")
    fun getFontDisplayName(context: Context, uriString: String): String {
        if (uriString.isBlank()) return ""
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return ""

        if (uri.scheme == "content") {
            val nameFromResolver = runCatching {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) cursor.getString(nameIndex) else null
                    } else {
                        null
                    }
                }
            }.getOrNull()
            if (!nameFromResolver.isNullOrBlank()) return nameFromResolver
        }

        val decoded = runCatching {
            URLDecoder.decode(
                uriString,
                "UTF-8",
            )
        }.getOrDefault(uriString)
        val fileName = decoded.substringAfterLast('/').substringAfterLast('%')
        if (fileName.endsWith(
                ".ttf",
                ignoreCase = true,
            ) ||
            fileName.endsWith(
                ".otf",
                ignoreCase = true,
            ) ||
            fileName.endsWith(
                ".woff",
                ignoreCase = true,
            ) ||
            fileName.endsWith(
                ".woff2",
                ignoreCase = true,
            )
        ) {
            return fileName.substringAfterLast(':')
        }

        val internalFile = resolveFontFile(
            context,
            uriString,
        )
        if (internalFile != null && internalFile.exists()) {
            return internalFile.name
        }

        return "External Font"
    }

    private fun copyFileToInternalStorage(
        context: Context,
        file: File,
        preferredName: String? = null,
    ): File? = try {
        val fileName = sanitizeFileName(preferredName ?: file.name)
        val destFile = File(
            getFontDir(context),
            fileName,
        )
        file.inputStream().use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        if (destFile.exists() && destFile.length() > 0) {
            cleanupUnusedFontFiles(
                context,
                destFile.name,
            )
            destFile
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun parseSafDocumentPath(uriString: String): String? = try {
        val decoded = URLDecoder.decode(
            uriString,
            "UTF-8",
        )
        if (decoded.contains("primary:")) {
            val relativePath = decoded.substringAfter("primary:")

            @Suppress("DEPRECATION")
            val externalStorage = Environment.getExternalStorageDirectory().absolutePath
            "$externalStorage/$relativePath"
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
