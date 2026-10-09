package net.echonolix.slang

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** Host platform classifier as used in Slang release asset names, for example `windows-x86_64`. */
object SlangHost {
    val classifier: String by lazy {
        val osName = System.getProperty("os.name")
        val os = when {
            osName.startsWith("Windows", ignoreCase = true) -> "windows"
            osName.startsWith("Linux", ignoreCase = true) -> "linux"
            osName.startsWith("Mac", ignoreCase = true) -> "macos"
            else -> throw GradleException("Unsupported host OS for Slang downloads: $osName")
        }
        val arch = when (val a = System.getProperty("os.arch").lowercase()) {
            "amd64", "x86_64" -> "x86_64"
            "aarch64", "arm64" -> "aarch64"
            else -> throw GradleException("Unsupported host architecture for Slang downloads: $a")
        }
        "$os-$arch"
    }

    val isWindows: Boolean get() = classifier.startsWith("windows")
}

/**
 * Downloads, verifies and extracts official Slang release archives into a cache shared by all builds on the
 * machine (`<gradle user home>/caches/slang-gradle-plugin`). Extraction is serialized within the build and across
 * processes with a file lock.
 */
abstract class SlangToolchainService : BuildService<SlangToolchainService.Params> {
    interface Params : BuildServiceParameters {
        val cacheDirectory: DirectoryProperty
    }

    private val logger = Logging.getLogger(SlangToolchainService::class.java)

    /** Returns `slangc` from the release [version] for the host, verifying the archive against [sha256]. */
    fun slangc(version: String, sha256: String): File {
        val root = extract(releaseUrl(version), sha256)
        val name = if (SlangHost.isWindows) "slangc.exe" else "slangc"
        val executable = File(root, "bin/$name")
        if (!executable.isFile) throw GradleException("$name not found in Slang $version release archive")
        if (!SlangHost.isWindows) executable.setExecutable(true)
        return executable
    }

    private fun releaseUrl(version: String): String {
        val extension = if (SlangHost.isWindows) "zip" else "tar.gz"
        return "https://github.com/shader-slang/slang/releases/download/v$version/" +
            "slang-$version-${SlangHost.classifier}.$extension"
    }

    @Synchronized
    private fun extract(url: String, sha256: String): File {
        val expected = sha256.lowercase()
        val key = expected.take(16)
        val cache = parameters.cacheDirectory.get().asFile.also { it.mkdirs() }
        val target = File(cache, key)
        val marker = File(target, ".complete")
        if (marker.isFile) return target

        FileChannel.open(File(cache, "$key.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            .use { channel ->
                channel.lock().use {
                    if (marker.isFile) return target
                    target.deleteRecursively()
                    target.mkdirs()
                    val archive = File(cache, "$key-${url.substringAfterLast('/')}")
                    download(url, expected, archive)
                    untar(archive, target)
                    marker.writeText(url)
                    archive.delete()
                }
            }
        return target
    }

    private fun download(url: String, expectedSha256: String, destination: File) {
        logger.lifecycle("Downloading $url")
        val partial = File(destination.path + ".part")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        try {
            if (connection.responseCode != 200) {
                throw GradleException("Download of $url failed with HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != expectedSha256) {
            partial.delete()
            throw GradleException("SHA-256 mismatch for $url: expected $expectedSha256, got $actual")
        }
        Files.move(partial.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    // Windows 10+ ships bsdtar as tar.exe, which also reads .zip; Linux and macOS have tar.
    private fun untar(archive: File, destination: File) {
        val process = ProcessBuilder("tar", "-xf", archive.absolutePath, "-C", destination.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw GradleException("Extracting $archive failed:\n$output")
        }
    }

    companion object {
        fun register(gradle: Gradle): Provider<SlangToolchainService> =
            gradle.sharedServices.registerIfAbsent("slangToolchain", SlangToolchainService::class.java) {
                parameters.cacheDirectory.set(File(gradle.gradleUserHomeDir, "caches/slang-gradle-plugin"))
            }
    }
}
