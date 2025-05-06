package net.echonolix.slang

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class SlangCompilerOptions {
    @get:Input
    abstract val target: Property<SlangCompileTarget>

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val extraOptions: ListProperty<String>

    @get:Input
    abstract val debug: Property<Boolean>

    init {
        target.convention(SlangCompileTarget.SPIR_V)
        profile.convention("glsl_450")
        debug.convention(false)
    }
}