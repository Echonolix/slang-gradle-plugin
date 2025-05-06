package net.echonolix.slang

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import kotlin.io.path.absolute

abstract class SlangExtension(private val project: Project) {
    abstract val compilerExecutable: RegularFileProperty
    val compilerOptions = project.objects.newInstance(SlangCompilerOptions::class.java)

    init {
        compilerExecutable.convention(
            project.layout.file(project.provider {
                val slangcFromPath = findExecutableOnPath("slangc")
                    ?: error("slangc not found in PATH, please install it or set compilerExecutable to the executable.")
                slangcFromPath.absolute().toFile()
            })
        )
    }

    fun compilerOptions(configure: SlangCompilerOptions.() -> Unit) {
        compilerOptions.configure()
    }
}