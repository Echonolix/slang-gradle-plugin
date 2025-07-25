package net.echonolix.slang

import org.gradle.api.Project

abstract class SlangExtension(private val project: Project) {
    val compilerOptions = project.objects.newInstance(SlangCompilerOptions::class.java)

    fun compilerOptions(configure: SlangCompilerOptions.() -> Unit) {
        compilerOptions.configure()
    }
}