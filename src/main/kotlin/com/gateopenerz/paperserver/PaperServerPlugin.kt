package com.gateopenerz.paperserver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject

@Suppress("unused")
abstract class PaperServerPlugin @Inject constructor(
    private val toolchains: JavaToolchainService
) : Plugin<Project> {
    private val gson = Gson()

    override fun apply(project: Project) {
        with(project) {
            val ext = extensions.create("paperServer", PaperServerExtension::class.java).apply {
                version.convention("1.21.7")
                jvmArgs.convention("-Xms1G -Xmx2G")
                serverDir.convention("development-server")
                preLaunchTasks.convention(emptyList())
                plugins.convention(emptyList())
                pluginUrls.convention(emptyList())
                interactiveConsole.convention(true)
            }
            val setup = tasks.register("setupPaperServer") {
                group = "paper"
                description = "Download latest Paper build, plugins and accept EULA"
                doLast {
                    val serverDirFile = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                    val pluginsDir = File(serverDirFile, "plugins").apply { mkdirs() }
                    downloadPaper(project, ext)
                    downloadUrlPlugins(project, ext, pluginsDir)
                    downloadNamedPlugins(project, ext, pluginsDir)
                    acceptEula(serverDirFile)
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
                    val serverDirFile = layout.projectDirectory.dir(ext.serverDir.get()).asFile
                    val version = ext.version.get()
                    val jar = serverDirFile.listFiles { f ->
                        f.isFile && f.name.startsWith("paper-$version-") && f.name.endsWith(".jar")
                    }?.singleOrNull() ?: throw GradleException(
                        "Paper jar for $version not found in $serverDirFile – run :setupPaperServer."
                    )
                    val flags = ext.jvmArgs.get().split("\\s+".toRegex()).filter(String::isNotBlank)
                    jvmArgs(flags)
                    workingDir = serverDirFile
                    mainClass.set("-jar")
                    classpath = files()
                    args(jar.absolutePath, "--nogui")
                    logger.lifecycle(">>> Launching Paper with JVM args: ${flags.joinToString(" ")}")
                    logger.lifecycle(">>> Using jar: ${jar.name}")
                    if (ext.interactiveConsole.get()) {
                        logger.lifecycle(">>> Server console is interactive. Type 'stop' to shut down.")
                    } else {
                        logger.lifecycle(">>> Server console is non-interactive.")
                    }
                }
            }
            afterEvaluate {
                ext.preLaunchTasks.get().forEach { taskName ->
                    runServer.configure { dependsOn(taskName) }
                }
                if (ext.interactiveConsole.get()) {
                    runServer.configure { standardInput = System.`in` }
                }
            }
        }
    }

    private fun downloadPaper(project: Project, ext: PaperServerExtension) {
        val version = ext.version.get()
        val serverDir = project.layout.projectDirectory.dir(ext.serverDir.get()).asFile
        val latestBuild = fetchLatestPaperBuild(version)
        val jarName = "paper-$version-$latestBuild.jar"
        val jarFile = File(serverDir, jarName)
        serverDir.mkdirs()
        if (!jarFile.exists()) {
            serverDir.listFiles { f -> f.name.startsWith("paper-") && f.name.endsWith(".jar") }?.forEach(File::delete)
            val url = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$latestBuild/downloads/$jarName"
            println("Downloading Paper $version build $latestBuild …")
            downloadFile(url, jarFile)
            println("Saved → ${jarFile.relativeTo(project.projectDir)}")
        } else {
            println("Paper $version build $latestBuild already present.")
        }
    }

    private fun downloadUrlPlugins(project: Project, ext: PaperServerExtension, pluginsDir: File) {
        ext.pluginUrls.get().forEach { pluginUrl ->
            val fileName = pluginUrl.substring(pluginUrl.lastIndexOf('/') + 1)
            val pluginFile = File(pluginsDir, fileName)
            if (!pluginFile.exists()) {
                println("Downloading plugin from URL: $fileName")
                downloadFile(pluginUrl, pluginFile)
                println("Saved plugin → ${pluginFile.relativeTo(project.projectDir)}")
            } else {
                println("Plugin $fileName already present.")
            }
        }
    }

    private fun downloadNamedPlugins(project: Project, ext: PaperServerExtension, pluginsDir: File) {
        val fullMcVersion = ext.version.get()
        ext.plugins.get().forEach { pluginIdentifier ->
            val parts = pluginIdentifier.split(":")
            val source: String?
            val pluginName: String
            val pluginVersion: String?
            when {
                parts.size >= 3 && parts[0] in listOf("hangar", "modrinth") -> {
                    source = parts[0]
                    pluginName = parts[1]
                    pluginVersion = parts.drop(2).joinToString(":")
                }
                parts.size == 2 && parts[0] in listOf("hangar", "modrinth") -> {
                    source = parts[0]
                    pluginName = parts[1]
                    pluginVersion = null
                }
                else -> {
                    source = null
                    pluginName = parts[0]
                    pluginVersion = null
                }
            }
            println("Searching for plugin: $pluginName" + (source?.let { " on $it" } ?: "") + (pluginVersion?.let { " version $it" } ?: ""))
            val url = findPluginUrl(project, pluginName, fullMcVersion, source, pluginVersion)
            if (url != null) {
                val fileName = url.substring(url.lastIndexOf('/') + 1)
                val pluginFile = File(pluginsDir, fileName)
                if (!pluginFile.exists()) {
                    println("Downloading plugin: $fileName from $url")
                    downloadFile(url, pluginFile)
                    println("Saved plugin → ${pluginFile.relativeTo(project.projectDir)}")
                } else {
                    println("Plugin $fileName already present.")
                }
            } else {
                project.logger.warn("Could not find a download for plugin '$pluginName' " + (pluginVersion?.let { "version $it " } ?: "") + "for Minecraft $fullMcVersion" + (source?.let { " on $it" } ?: "."))
            }
        }
    }

    private fun findPluginUrl(project: Project, pluginName: String, mcVersion: String, source: String?, pluginVersion: String?): String? {
        return when (source) {
            "hangar" -> findOnHangar(project, pluginName, mcVersion, pluginVersion)
            "modrinth" -> findOnModrinth(project, pluginName, mcVersion, pluginVersion)
            null -> findOnHangar(project, pluginName, mcVersion, pluginVersion) ?: findOnModrinth(project, pluginName, mcVersion, pluginVersion)
            else -> null
        }
    }

    private fun findOnHangar(project: Project, pluginName: String, mcVersion: String, pluginVersion: String?): String? {
        try {
            val encodedName = URLEncoder.encode(pluginName, "UTF-8")
            val searchUrl = "https://hangar.papermc.io/api/v1/projects?q=$encodedName&limit=1"
            val connection = openUrlConnection(searchUrl)
            if (connection.responseCode >= 400) {
                project.logger.warn("Hangar API returned HTTP ${connection.responseCode} for search: ${connection.responseMessage}")
                return null
            }
            val searchResult = gson.fromJson<Map<String, Any>>(
                InputStreamReader(connection.inputStream),
                object : TypeToken<Map<String, Any>>() {}.type
            )
            @Suppress("UNCHECKED_CAST")
            val plugin = (searchResult["result"] as? List<Map<String, Any>>)?.firstOrNull() ?: return null
            val namespace = plugin["namespace"] as? Map<String, String>
            val owner = namespace?.get("owner")
            val slug = namespace?.get("slug")
            val finalSlug = slug ?: (plugin["slug"] as? String)
            if (owner == null || finalSlug == null) {
                project.logger.warn("Could not extract owner/slug from Hangar project response for $pluginName")
                return null
            }
            val versionsUrl = "https://hangar.papermc.io/api/v1/projects/$owner/$finalSlug/versions?limit=100"
            val versionsConnection = openUrlConnection(versionsUrl)
            if (versionsConnection.responseCode >= 400) {
                project.logger.warn("Hangar API returned HTTP ${versionsConnection.responseCode} for versions: ${versionsConnection.responseMessage}")
                return null
            }
            val versionsResult = gson.fromJson<Map<String, Any>>(
                InputStreamReader(versionsConnection.inputStream),
                object : TypeToken<Map<String, Any>>() {}.type
            )
            @Suppress("UNCHECKED_CAST")
            val allVersions = versionsResult["result"] as? List<Map<String, Any>> ?: emptyList()
            val targetVersion = if (pluginVersion != null) {
                var found = allVersions.firstOrNull { it["name"] as? String == pluginVersion }
                if (found == null && pluginVersion.contains("SNAPSHOT", ignoreCase = true)) {
                    found = allVersions.firstOrNull {
                        (it["name"] as? String)?.startsWith(pluginVersion) == true
                    }
                }
                found
            } else {
                allVersions.firstOrNull {
                    (it["platformDependencies"] as? Map<String, List<String>>)?.get("PAPER")?.contains(mcVersion) == true
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (targetVersion?.get("downloads") as? Map<String, Map<String, String>>)?.get("PAPER")?.get("downloadUrl")
        } catch (e: Exception) {
            project.logger.warn("Hangar search failed for $pluginName: ${e.message}")
            return null
        }
    }

    private fun findOnModrinth(project: Project, pluginName: String, mcVersion: String, pluginVersion: String?): String? {
        try {
            val encodedName = URLEncoder.encode(pluginName, "UTF-8")
            val facets = URLEncoder.encode("[[\"project_type:plugin\"],[\"categories:paper\"]]", "UTF-8")
            val searchUrl = "https://api.modrinth.com/v2/search?query=$encodedName&facets=$facets&limit=1"

            val connection = openUrlConnection(searchUrl)

            if (connection.responseCode >= 400) {
                project.logger.warn("Modrinth API returned HTTP ${connection.responseCode}: ${connection.responseMessage}")
                return null
            }

            val searchResult = gson.fromJson<Map<String, Any>>(
                InputStreamReader(connection.inputStream),
                object : TypeToken<Map<String, Any>>() {}.type
            )
            @Suppress("UNCHECKED_CAST")
            val hit = (searchResult["hits"] as? List<Map<String, Any>>)?.firstOrNull() ?: return null
            val projectId = hit["project_id"] as String
            val versionsUrl = "https://api.modrinth.com/v2/project/$projectId/version"
            val versionsConnection = openUrlConnection(versionsUrl)

            if (versionsConnection.responseCode >= 400) {
                project.logger.warn("Modrinth API returned HTTP ${versionsConnection.responseCode}: ${versionsConnection.responseMessage}")
                return null
            }

            val allVersions = gson.fromJson<List<Map<String, Any>>>(
                InputStreamReader(versionsConnection.inputStream),
                object : TypeToken<List<Map<String, Any>>>() {}.type
            )
            val targetVersion = if (pluginVersion != null) {
                allVersions.firstOrNull { it["version_number"] == pluginVersion }
            } else {
                allVersions.firstOrNull {
                    (it["game_versions"] as? List<String>)?.contains(mcVersion) == true &&
                            (it["loaders"] as? List<String>)?.contains("paper") == true
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (targetVersion?.get("files") as? List<Map<String, Any>>)?.firstOrNull()?.get("url") as? String
        } catch(e: Exception) {
            project.logger.warn("Modrinth search failed for $pluginName: ${e.message}")
            return null
        }
    }

    private fun acceptEula(serverDir: File) {
        val eula = File(serverDir, "eula.txt")
        if (!eula.exists() || !eula.readText().contains("eula=true")) {
            eula.writeText("eula=true")
            println("EULA accepted.")
        }
    }

    private fun downloadFile(url: String, dest: File) {
        try {
            openUrlConnection(url).inputStream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
        } catch (e: Exception) {
            throw GradleException("Failed to download file from $url", e)
        }
    }

    private fun openUrlConnection(url: String): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "paper-setup-plugin/1.0.2 (github.com/GATEOPENERZ/paper-setup-plugin)")
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        return connection
    }

    private fun fetchLatestPaperBuild(v: String): Int =
        openUrlConnection("https://api.papermc.io/v2/projects/paper/versions/$v/").inputStream.use {
            @Suppress("UNCHECKED_CAST")
            val json = gson.fromJson(InputStreamReader(it), Map::class.java)
            (json["builds"] as List<Double>).last().toInt()
        }
}