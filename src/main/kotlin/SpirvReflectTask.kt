package net.echonolix.slang

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

abstract class SpirvReflectTask : DefaultTask() {
    @get:InputFiles
    abstract val inputFiles: Property<FileCollection>

    @get:InputFile
    abstract val spirVReflectExecutable: RegularFileProperty

    @TaskAction
    fun runSpirvReflect() {
        val spirvReflectPath = spirVReflectExecutable.get().asFile.absolutePath
        inputFiles.get().asSequence()
            .flatMap { it.walk() }
            .filter { it.isFile }
            .filter { it.extension == "spv" }
            .map {
                val inputFile = it.absoluteFile.normalize()
                val outputFile = inputFile.parentFile.resolve("${inputFile.nameWithoutExtension}.yaml")
                ProcessBuilder(spirvReflectPath, "-y", inputFile.path)
                    .redirectOutput(ProcessBuilder.Redirect.to(outputFile))
                    .start()
            }
            .forEach { it.waitFor() }
    }
}