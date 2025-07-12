# Paper Setup Gradle Plugin

Downloads the latest PaperMC server jar, accepts the EULA
and gives you a handy `runPaperServer` task with configurable JVM args.

```kotlin
plugins {
    id("io.github.gateopenerz.paper-server") version "1.0.0"
}

paperServer {
    version.set("1.21.7")
    jvmArgs.set("-Xms2G -Xmx4G")
    serverDir.set("development-server")
}
```