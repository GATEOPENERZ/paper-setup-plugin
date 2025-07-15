package com.gateopenerz.paperserver

import com.google.gson.Gson
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register
import java.io.File
import java.net.URI
import javax.inject.Inject

@Suppress("unused")
abstract class PaperServerPlugin @Inject constructor(
    private val toolchains: JavaToolchainService
) : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {

            val ext = extensions.create("paperServer", PaperServerExtension::class.java).apply {
                version.convention("1.21.7")
                jvmArgs.convention("-Xms1G -Xmx2G")
                serverDir.convention("development-server")
                preLaunchTasks.convention(emptyList())
            }

            val setup = tasks.register("setupPaperServer") {
                group = "paper"
                description = "Download latest Paper build and accept EULA"

                doLast {
                    val version     = ext.version.get()
                    val serverDir   = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                    val latestBuild = fetchLatestBuild(version)
                    val jarName     = "paper-$version-$latestBuild.jar"
                    val jarFile     = File(serverDir, jarName)

                    serverDir.mkdirs()

                    if (!jarFile.exists()) {
                        serverDir.listFiles { f ->
                            f.name.startsWith("paper-") && f.name.endsWith(".jar")
                        }?.forEach(File::delete)

                        val url =
                            "https://api.papermc.io/v2/projects/paper/versions/$version/" +
                                    "builds/$latestBuild/downloads/$jarName"
                        println("Downloading Paper $version build $latestBuild …")
                        URI(url).toURL().openStream().use { i ->
                            jarFile.outputStream().use { o -> i.copyTo(o) }
                        }
                        println("Saved → ${jarFile.relativeTo(project.projectDir)}")
                    } else {
                        println("Paper $version build $latestBuild already present.")
                    }

                    val eula = File(serverDir, "eula.txt")
                    if (!eula.exists() || !eula.readText().contains("eula=true")) {
                        eula.writeText("eula=true")
                        println("EULA accepted.")
                    }
                }
            }

            val runServer = tasks.register<JavaExec>("runPaperServer") {
                group = "paper"
                description = "Start Paper with configured JVM args"
                dependsOn(setup)
                javaLauncher.set(toolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(21))
                })
                doFirst {
                    val serverDir = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                    val version   = ext.version.get()
                    val jar = serverDir.listFiles { f ->
                        f.isFile && f.name.startsWith("paper-$version-") && f.name.endsWith(".jar")
                    }?.singleOrNull() ?: throw GradleException(
                        "Paper jar for $version not found in $serverDir – run :setupPaperServer."
                    )

                    val flags = ext.jvmArgs.get().split("\\s+".toRegex()).filter(String::isNotBlank)
                    jvmArgs(flags)
                    workingDir = serverDir
                    mainClass.set("-jar")
                    classpath = files()
                    args(jar.absolutePath, "--nogui")
                    logger.lifecycle(">>> Launching Paper with JVM args: ${flags.joinToString(" ")}")
                    logger.lifecycle(">>> Using jar: ${jar.name}")
                }
            }
            afterEvaluate {
                ext.preLaunchTasks.get().forEach { taskName ->
                    runServer.configure { dependsOn(taskName) }
                }
            }
        }
    }

    private fun fetchLatestBuild(v: String): Int =
        URI("https://api.papermc.io/v2/projects/paper/versions/$v/")
            .toURL().openStream().bufferedReader().use {
                val json = Gson().fromJson(it, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                (json["builds"] as List<Double>).last().toInt()
            }
}
