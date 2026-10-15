package net.echonolix.slang

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.plugins.ide.idea.model.IdeaModel

class SlangPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("idea")

        val extension = target.extensions.create<SlangExtension>("slang", target)
        val ideaModel = target.extensions.getByType(IdeaModel::class.java)
        val slangToolchain = SlangToolchainService.register(target.gradle)

        target.sourceSets.configureEach {
            val slangSourceDirectorySet = target.objects.sourceDirectorySet("slang", "slang")
            extensions.add("slang", slangSourceDirectorySet)
            val slangSrcDir = target.layout.projectDirectory.dir("src/${name}/slang")
            slangSourceDirectorySet.srcDir(slangSrcDir)
            this.allSource.srcDir(slangSrcDir)

            val slangCompile = target.tasks.register(
                getCompileTaskName("slang"),
                SlangCompile::class.java,
                extension.compilerOptions
            )

            slangCompile.configure {
                this.source(slangSourceDirectorySet)
                toolchainService.set(slangToolchain)
                usesService(slangToolchain)
                val pinnedCompiler = compilerOptions.compilerVersion
                outputs.cacheIf("a pinned compiler version is set") { pinnedCompiler.isPresent }
            }

            slangSourceDirectorySet.destinationDirectory.convention(target.layout.buildDirectory.dir("slang/${name}"))
            slangSourceDirectorySet.compiledBy(slangCompile) { it.outputDir }

            // Generated shaders are resources, not a private runtime configuration entry.
            // The task-backed directory wires processResources, IDE source-set outputs,
            // project dependency variants and jars to the same compiler outputs.
            resources.srcDir(slangCompile.flatMap { it.outputDir })

            (target.tasks.findByName(sourcesJarTaskName) as Jar?)?.apply {
                from(slangCompile.map { it.source.asFileTree })
            }

            (target.tasks.findByName(classesTaskName))?.apply {
                dependsOn(slangCompile)
            }

            ideaModel.module {
                sourceDirs.add(slangSrcDir.asFile)
            }
        }
    }
}
