package net.echonolix.slang

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import kotlin.io.path.absolute

abstract class SlangExtension(private val project: Project) {
    abstract val compilerExecutable: RegularFileProperty
    abstract val spirVReflectExecutable: RegularFileProperty
    val compilerOptions = project.objects.newInstance(SlangCompilerOptions::class.java)
    abstract val generateReflectionInfo: Property<Boolean>

    init {
        compilerExecutable.convention(
            project.layout.file(project.provider {
                val slangcFromPath = findExecutableOnPath("slangc")
                    ?: error("slangc not found in PATH, please install it or set compilerExecutable to the executable.")
                slangcFromPath.absolute().toFile()
            })
        )
        spirVReflectExecutable.convention(
            project.layout.file(project.provider {
                findExecutableOnPath("spirv-reflect")?.absolute()?.toFile()
            })
        )
        generateReflectionInfo.convention(false)
    }

    fun compilerOptions(configure: SlangCompilerOptions.() -> Unit) {
        compilerOptions.configure()
    }
}