package net.echonolix.slang

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import java.io.File
import javax.inject.Inject

abstract class SlangCompile @Inject constructor(
    @get:Nested
    val compilerOptions: SlangCompilerOptions
) : SourceTask() {
    @get:InputFile
    abstract val compilerExecutable: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * {@inheritDoc}
     */
    @Internal("tracked via stableSources")
    override fun getSource(): FileTree {
        return super.getSource()
    }

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val stableSources = project.files(this.source)

    init {
        compilerExecutable.convention(project.slang.compilerExecutable)
    }

    private fun debugMessage(message: String = "") {
        if (compilerOptions.debug.get()) {
            println(message)
        }
    }

    @TaskAction
    fun compile(inputs: InputChanges) {
        if (inputs.isIncremental) {
            debugMessage("Performing incremental compilation...\n")
        } else {
            debugMessage("Performing full compilation...\n")
        }
        doCompile(inputs.getFileChanges(stableSources).toList())
    }

    private fun doCompile(fileChanges: List<FileChange>) {
        val compilerExecutableFile = compilerExecutable.get().asFile.absoluteFile
        val profile = compilerOptions.profile.get()
        val target = compilerOptions.target.get()
        val extraOptions = compilerOptions.extraOptions.get()

        debugMessage("Compiler: $compilerExecutableFile")
        debugMessage("Profile: $profile")
        debugMessage("Target: ${target.optionName}")
        debugMessage("Extra options: $extraOptions")
        debugMessage()
        debugMessage("Compiling ${fileChanges.size} files...")

        val outputDirectory = outputDir.get()

        fun String.outputPath(): File =
            outputDirectory.file("${this.removeSuffix(".slang")}.${target.fileExtension}").asFile.absoluteFile

        fileChanges.asSequence()
            .filter { it.changeType == ChangeType.REMOVED }
            .forEach { it.normalizedPath.outputPath().delete() }

        fileChanges.asSequence()
            .filter { it.changeType != ChangeType.REMOVED }
            .map { it.normalizedPath to it.file }
            .filter { (_, file) -> file.extension == "slang" }
            .forEach { (path, file) ->
                check(path.removeSuffix(".slang").length < path.length) { "File $file is not a .slang file" }
                val outputFile = path.outputPath()
                outputFile.parentFile.mkdirs()

                debugMessage("Compiling $file...")
                execOperations.exec {
                    executable(compilerExecutableFile)
                    args(
                        "-profile", profile,
                        "-target", target.optionName,
                    )
                    args(extraOptions)
                    args(file.absolutePath, "-o", outputFile.path)
                }
            }

        didWork = true
    }
}