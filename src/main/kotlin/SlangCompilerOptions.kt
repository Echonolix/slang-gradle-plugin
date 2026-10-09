package net.echonolix.slang

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import javax.inject.Inject
import kotlin.io.path.absolute

abstract class SlangCompilerOptions @Inject constructor(layout: ProjectLayout, providers: ProviderFactory) {
    /**
     * Slang release to download, for example `2026.19`. When set, `slangc` comes from the official GitHub
     * release archive verified against [compilerSha256], and [compilerExecutable] is ignored.
     */
    @get:Input
    @get:Optional
    abstract val compilerVersion: Property<String>

    /**
     * SHA-256 of the release archive per host classifier (`windows-x86_64`, `linux-x86_64`, ...). Windows uses the
     * `.zip` asset, other hosts the `.tar.gz` asset.
     */
    @get:Internal
    abstract val compilerSha256: MapProperty<String, String>

    /** SHA-256 for the current host; part of the task inputs so a re-pinned archive invalidates outputs. */
    @get:Input
    @get:Optional
    val hostCompilerSha256: Provider<String>
        get() = compilerSha256.getting(SlangHost.classifier)

    /** Explicit `slangc`. Used only when [compilerVersion] is not set; falls back to `slangc` on `PATH`. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val compilerExecutable: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
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

    /** Pins the downloaded compiler: [sha256] maps host classifiers to release archive hashes. */
    fun downloadCompiler(version: String, sha256: Map<String, String>) {
        compilerVersion.set(version)
        compilerSha256.set(sha256)
    }

    init {
        target.convention(SlangCompileTarget.SPIR_V)
        profile.convention("glsl_450")
        debugLogging.convention(false)
        generateReflectionInfo.convention(false)
        spirVReflectExecutable.convention(
            layout.file(providers.provider {
                findExecutableOnPath("spirv-reflect")?.absolute()?.toFile()
            })
        )
    }
}
