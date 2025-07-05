package net.echonolix.slang

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.plugins.ide.idea.model.IdeaModel

class SlangPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("idea")

        val extension = target.extensions.create<SlangExtension>("slang", target)
        val ideaModel = target.extensions.getByType(IdeaModel::class.java)

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

            val generateReflectionInfo = target.tasks.register(
                getTaskName("generate", "spirvReflectionInfo"),
                SpirvReflectTask::class.java
            ) {
                inputFiles.convention(slangCompile.map { it.outputs.files })
                spirVReflectExecutable.convention(extension.spirVReflectExecutable)
            }

            slangCompile.configure {
                if (extension.generateReflectionInfo.get()) {
                    finalizedBy(generateReflectionInfo)
                }
            }

            slangCompile.configure {
                this.source(slangSourceDirectorySet)
            }

            slangSourceDirectorySet.destinationDirectory.convention(target.layout.buildDirectory.dir("slang/${name}"))
            slangSourceDirectorySet.compiledBy(slangCompile) { it.outputDir }

            (target.tasks.findByName(jarTaskName) as Jar?)?.apply {
                from(slangCompile.map { it.outputDir.asFileTree })
            }

            (target.tasks.findByName(sourcesJarTaskName) as Jar?)?.apply {
                from(slangCompile.map { it.source.asFileTree })
            }

            (target.tasks.findByName(classesTaskName))?.apply {
                dependsOn(slangCompile)
            }

            target.dependencies {
                runtimeClasspathConfigurationName(slangCompile.map { target.files(it.outputDir.asFile) })
            }

            ideaModel.module {
                sourceDirs.add(slangSrcDir.asFile)
            }
        }
    }
}