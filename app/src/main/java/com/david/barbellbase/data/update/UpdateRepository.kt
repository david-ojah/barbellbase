package com.david.barbellbase.data.update

import android.content.Context
import android.os.Environment
import com.david.barbellbase.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val source: RemoteConfigUpdateSource,
    private val preferences: UpdatePreferences,
) {
    val currentVersion: String get() = BuildConfig.VERSION_NAME

    suspend fun check(manual: Boolean): Result<AvailableUpdate?> = runCatching {
        val remote = source.fetch(manual).getOrThrow()
        val update = remote?.takeIf { UpdateVersion.isNewer(it.version, currentVersion) }
        preferences.saveCheck(System.currentTimeMillis(), update)
        update
    }

    suspend fun download(onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val update = preferences.read().update ?: error("No update is available")

            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            directory.mkdirs()

            val fileName = update.apkUrl.substringAfterLast('/')
                .takeIf { it.endsWith(".apk", ignoreCase = true) }
                ?: "BarbellBase-${update.version}.apk"
            val target = File(directory, fileName)

            val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BarbellBase/${BuildConfig.VERSION_NAME}")
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) error("Download failed (HTTP $code)")
                val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes

                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (update.sha256.isBlank()) {
                target.delete()
                error("Update metadata is missing a checksum")
            }

            val actual = sha256(target)
            if (!actual.equals(update.sha256, ignoreCase = true)) {
                target.delete()
                error("The downloaded update failed its integrity check")
            }

            onProgress(1f)
            target
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
