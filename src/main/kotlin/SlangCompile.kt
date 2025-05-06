package net.echonolix.slang

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.internal.instrumentation.api.annotations.ToBeReplacedByLazyProperty
import org.gradle.process.internal.DefaultExecSpec
import org.gradle.process.internal.ExecActionFactory
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import javax.inject.Inject

abstract class SlangCompile @Inject constructor(
    @get:Nested
    val compilerOptions: SlangCompilerOptions
) : SourceTask() {
    @get:InputFile
    abstract val compilerExecutable: RegularFileProperty

//    @get:InputDirectory
//    abstract val sourceDir: FileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val execActionFactory: ExecActionFactory

    @get:Inject
    abstract val objectFactory: ObjectFactory

    /**
     * {@inheritDoc}
     */
    @Internal("tracked via stableSources")
    @ToBeReplacedByLazyProperty
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

        val defaultSpec = objectFactory.newInstance(DefaultExecSpec::class.java)
        defaultSpec.executable(compilerExecutableFile)
        defaultSpec.args("-profile")
        defaultSpec.args(profile)
        defaultSpec.args("-target")
        defaultSpec.args(target.optionName)
        debugMessage("Compiler: $compilerExecutableFile")
        debugMessage("Profile: $profile")
        debugMessage("Target: ${target.optionName}")
        debugMessage("Compiler options: ${defaultSpec.args}")
        debugMessage()
        debugMessage("Compiling ${fileChanges.size} files...")

        val outputDirectory = outputDir.get()
        fileChanges.asSequence()
            .map { it.normalizedPath to it.file }
            .filter { (_, file) -> file.extension == "slang" }
            .forEach { (path, file) ->
                val pathWithoutExtension = path.removeSuffix(".slang")
                check(pathWithoutExtension.length < path.length) { "File $file is not a .slang file" }
                val outputFile =
                    outputDirectory.file("${pathWithoutExtension}.${target.fileExtension}").asFile.absoluteFile
                outputFile.parentFile.mkdirs()

                debugMessage("Compiling $file...")
                val execAction = execActionFactory.newExecAction()
                defaultSpec.copyTo(execAction)
                execAction.args(file.absolutePath)
                execAction.args("-o")
                execAction.args(outputFile.path)
                execAction.args(extraOptions)
                execAction.execute()
            }

        didWork = true
    }
}