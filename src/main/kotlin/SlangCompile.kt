package net.echonolix.slang

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
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
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

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

    private fun debugMessage(message: String = "") {
        if (compilerOptions.debugLogging.get()) {
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
        val compilerExecutableFile = compilerOptions.compilerExecutable.get().asFile.absoluteFile
        val profile = compilerOptions.profile.get()
        val target = compilerOptions.target.get()
        val extraOptions = compilerOptions.extraOptions.get()
        val generateReflectionInfo = compilerOptions.generateReflectionInfo.get()
        val spirvReflectPath = compilerOptions.spirVReflectExecutable.get().asFile.absolutePath

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
            .forEach {
                val spvFile = it.normalizedPath.outputPath()
                spvFile.delete()
                spvFile.parentFile.resolve("${spvFile.nameWithoutExtension}.yaml").delete()
            }

        fileChanges.asSequence()
            .filter { it.changeType != ChangeType.REMOVED }
            .map { it.normalizedPath to it.file }
            .filter { (_, file) -> file.extension == "slang" }
            .mapNotNull { (path, file) ->
                check(path.removeSuffix(".slang").length < path.length) { "File $file is not a .slang file" }
                val spvFile = path.outputPath()
                spvFile.parentFile.mkdirs()

                debugMessage("Compiling $file...")
                execOperations.exec {
                    executable(compilerExecutableFile)
                    args(
                        "-profile", profile,
                        "-target", target.optionName,
                    )
                    args(extraOptions)
                    args(file.absolutePath, "-o", spvFile.path)
                }

                didWork = true

                if (generateReflectionInfo) {
                    debugMessage("Generating reflection info for $spvFile...")
                    val outputFile = spvFile.parentFile.resolve("${spvFile.nameWithoutExtension}.yaml")
                    ProcessBuilder(spirvReflectPath, "-y", spvFile.path)
                        .redirectOutput(ProcessBuilder.Redirect.to(outputFile))
                        .start()
                } else {
                    null
                }
            }
            .forEach { it.waitFor() }
    }
}