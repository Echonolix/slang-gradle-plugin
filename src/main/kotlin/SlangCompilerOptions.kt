package net.echonolix.slang

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import javax.inject.Inject
import kotlin.io.path.absolute

abstract class SlangCompilerOptions {
    @get:Inject
    abstract val project: Project

    @get:InputFile
    abstract val compilerExecutable: RegularFileProperty

    @get:InputFile
    abstract val spirVReflectExecutable: RegularFileProperty

    @get:Input
    abstract val target: Property<SlangCompileTarget>

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val extraOptions: ListProperty<String>

    @get:Input
    abstract val debugLogging: Property<Boolean>

    @get:Input
    abstract val generateReflectionInfo: Property<Boolean>

    init {
        target.convention(SlangCompileTarget.SPIR_V)
        profile.convention("glsl_450")
        debugLogging.convention(false)
        generateReflectionInfo.convention(false)
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
    }
}