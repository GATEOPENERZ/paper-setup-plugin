# Paper Setup Gradle Plugin

Downloads the latest PaperMC server jar, accepts the EULA
and gives you a handy `runPaperServer` task with configurable JVM args.

```kotlin
plugins {
    id("io.github.gateopenerz.paper-server") version "1.0.2"
}

paperServer {
    version.set("1.21.7")
    jvmArgs.set("-Xms2G -Xmx4G")
    serverDir.set("development-server")
    preLaunchTasks.set(listOf("build"))
    
    plugins.set(listOf(
    "modrinth:LuckPerms",
    "hangar:ViaVersion:5.4.2-SNAPSHOT+784",
    "modrinth:WorldEdit",
    ))
    
    pluginUrls.set(listOf(
        "https://github.com/user/plugin/releases/download/v1.0.0/plugin.jar",
        "https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar",
        "https://builds.papermc.io/downloads/some-plugin.jar"
    ))
}
```