package net.echonolix.slang

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

val Project.slang: SlangExtension
    get() = extensions.getByType<SlangExtension>()

val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType<SourceSetContainer>()

fun findExecutableOnPath(executableName: String): Path? {
    val pathEnv = System.getenv("PATH") ?: return null
    val pathSeparator = File.pathSeparator

    return pathEnv.splitToSequence(pathSeparator)
        .map { Path(it) }
        .filter { it.exists() }
        .flatMap { it.listDirectoryEntries("$executableName*") }
        .find { it.nameWithoutExtension == executableName && it.isExecutable() }
}