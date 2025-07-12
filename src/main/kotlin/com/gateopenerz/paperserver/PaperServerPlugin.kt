package com.gateopenerz.paperserver

import com.google.gson.Gson
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register
import java.io.File
import java.net.URI
import kotlin.collections.get

@Suppress("unused")
class PaperServerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {

            val ext = extensions.create(
                "paperServer",
                PaperServerExtension::class.java
            ).apply {
                version.convention("1.21.7")
                jvmArgs.convention("-Xms1G -Xmx2G")
                serverDir.convention("development-server")
            }

            val setup = tasks.register("setupPaperServer") {
                group = "paper"
                description = "Download latest Paper build & accept EULA"
                outputs.upToDateWhen { false }

                doLast {
                    val ver     = ext.version.get()
                    val baseDir = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                    val jarName = "paper-$ver.jar"
                    val jarFile = File(baseDir, jarName)
                    baseDir.mkdirs()

                    fun latestBuild(v: String): Int =
                        URI.create("https://api.papermc.io/v2/projects/paper/versions/$v/")
                            .toURL()
                            .openStream().bufferedReader().use {
                                val js = Gson().fromJson(it, Map::class.java)
                                @Suppress("UNCHECKED_CAST")
                                (js["builds"] as List<Double>).last().toInt()
                            }

                    val build = latestBuild(ver)
                    val tmp   = File(baseDir, "paper-$ver-$build.jar")

                    if (!jarFile.exists()) {
                        val url =
                            "https://api.papermc.io/v2/projects/paper/versions/$ver/" +
                                    "builds/$build/downloads/${tmp.name}"
                        println("Downloading Paper $ver build $build …")
                        URI(url).toURL().openStream().use { it.copyTo(tmp.outputStream()) }
                        tmp.renameTo(jarFile)
                    } else {
                        println("$jarName already exists – skipping download.")
                    }

                    val eula = File(baseDir, "eula.txt")
                    if (!eula.exists() || !eula.readText().contains("eula=true")) {
                        eula.writeText("eula=true")
                        println("EULA accepted.")
                    }
                }
            }

            tasks.register<JavaExec>("runPaperServer") {
                group = "paper"
                description = "Start Paper with configured JVM args"
                dependsOn(setup)

                val baseDir = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                val jarName = "paper-${ext.version.get()}.jar"
                val jarFile = File(baseDir, jarName)

                workingDir = baseDir
                mainClass.set("-jar")
                setClasspath(files(jarFile))
                args(jarName, "--nogui")

                ext.jvmArgs.orNull
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.split("\\s+".toRegex())
                    ?.let { jvmArgs(it) }

                doFirst {
                    logger.lifecycle(">>> Launching Paper server with JVM args: ${ext.jvmArgs.get()}")
                }
            }
        }
    }
}
