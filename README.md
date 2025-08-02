# Paper Setup Gradle Plugin

Downloads the latest server jar for multiple server software types, accepts the EULA, and gives you a handy `runPaperServer` task with configurable JVM args.

## Supported Server Types

- **Paper** - The most popular Minecraft server software
- **Purpur** - Paper fork with additional features and configuration options
- **Velocity** - Modern, next-generation Minecraft proxy
- **Folia** - Paper fork designed for higher player counts
- **Advanced Slime Paper (ASP)** - Paper fork with Slime World Manager built-in

## Basic Usage

```kotlin
plugins {
    id("io.github.gateopenerz.paper-server") version "1.1.0"
}

paperServer {
    serverType.set("paper")  // paper, purpur, velocity, folia, advanced-slime-paper
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

## Advanced Slime Paper Configuration

When using Advanced Slime Paper, you have additional configuration options:

```kotlin
paperServer {
    serverType.set("advanced-slime-paper")  // or "asp"
    version.set("1.21.7")
    aspBranch.set("main")  // main, develop, paper_upstream
    includeAspPlugin.set(true)  // Automatically download the ASP Slime World Plugin
    jvmArgs.set("-Xms2G -Xmx4G")
    serverDir.set("development-server")
}
```

### ASP Branch Options

- **main** - Stable and well-tested (recommended)
- **develop** - Experimental with latest features but may include bugs
- **paper_upstream** - Bleeding-edge with untested automatic changes

## Configuration Options

| Property             | Type         | Default                | Description                                              |
|----------------------|--------------|------------------------|----------------------------------------------------------|
| `serverType`         | String       | `"paper"`              | Server software to download                              |
| `version`            | String       | `"1.21.7"`             | Minecraft version (supports snapshots)                   |
| `build`              | String       | `null`                 | Specific build number (optional, uses latest if not set) |
| `aspBranch`          | String       | `"main"`               | ASP branch (only for ASP)                                |
| `jvmArgs`            | String       | `"-Xms1G -Xmx2G"`      | JVM arguments for server                                 |
| `serverDir`          | String       | `"development-server"` | Directory for server files                               |
| `preLaunchTasks`     | List<String> | `[]`                   | Tasks to run before server starts                        |
| `plugins`            | List<String> | `[]`                   | Plugin identifiers to download                           |
| `pluginUrls`         | List<String> | `[]`                   | Direct URLs to plugin jars                               |
| `interactiveConsole` | Boolean      | `true`                 | Enable interactive server console                        |
| `includeAspPlugin`   | Boolean      | `true`                 | Download ASP plugin (ASP only)                           |

## Server Type Examples

### Paper Server
```kotlin
paperServer {
    serverType.set("paper")
    version.set("1.21.7")
}
```

### Purpur Server
```kotlin
paperServer {
    serverType.set("purpur")
    version.set("1.21.7")
}
```

### Velocity Proxy
```kotlin
paperServer {
    serverType.set("velocity")
    version.set("3.4.0")
}
```

### Folia Server (High Player Count)
```kotlin
paperServer {
    serverType.set("folia")
    version.set("1.21.8")
}
```

### Advanced Slime Paper with Slime Worlds
```kotlin
paperServer {
    serverType.set("advanced-slime-paper")
    version.set("1.21.8")
    aspBranch.set("main")
    includeAspPlugin.set(true)  // Includes Slime World Plugin
}
```

## Snapshot Versions and Specific Builds

### Snapshot Versions
You can use snapshot versions for any server type:

```kotlin
paperServer {
    serverType.set("velocity")
    version.set("3.4.0-SNAPSHOT")  // Snapshot version
}
```

```kotlin
paperServer {
    serverType.set("paper")
    version.set("1.21.8-SNAPSHOT")
}
```

### Full Version Strings with Build Numbers
You can specify the complete version string including build number:

```kotlin
paperServer {
    serverType.set("velocity")
    version.set("3.4.0-SNAPSHOT-523")  // Auto-parsed: version=3.4.0-SNAPSHOT, build=523
}
```

```kotlin
paperServer {
    serverType.set("paper")
    version.set("1.21.8-21")  // Auto-parsed: version=1.21.8, build=21
}
```

```kotlin
paperServer {
    serverType.set("purpur")
    version.set("1.21.8-2485")  // Auto-parsed: version=1.21.8, build=2485
}
```

### Specific Build Numbers
Optionally, specify a specific build number. If not specified, the latest build is used:

```kotlin
paperServer {
    serverType.set("paper")
    version.set("1.21.8")
    build.set("21")  // Download specific build 21
}
```

```kotlin
paperServer {
    serverType.set("purpur")
    version.set("1.21.8")
    build.set("2485")  // Download specific Purpur build 2485
}
```

```kotlin
paperServer {
    serverType.set("velocity")
    version.set("3.4.0-SNAPSHOT")
    build.set("432")  // Specific snapshot build
}
```

### Latest Builds (Default)
If you don't specify a build number, the latest build is automatically downloaded:

```kotlin
paperServer {
    serverType.set("paper")
    version.set("1.21.8")
    // No build specified = latest build
}
```

## Plugin Sources

The plugin supports downloading from multiple sources:

### Hangar (PaperMC's plugin repository)
```kotlin
plugins.set(listOf(
    "hangar:PluginName",                    // Latest version
    "hangar:PluginName:1.0.0",             // Specific version
    "hangar:ViaVersion:5.5.0-SNAPSHOT+794" // Snapshot version
))
```

### Modrinth
```kotlin
plugins.set(listOf(
    "modrinth:LuckPerms",      // Latest version
    "modrinth:WorldEdit:7.3.16" // Specific version
))
```

### Auto-detect (searches Hangar first, then Modrinth)
```kotlin
plugins.set(listOf(
    "LuckPerms",  // Will search Hangar first, then Modrinth
    "WorldEdit"
))
```

### Direct URLs
```kotlin
pluginUrls.set(listOf(
    "https://ci.example.com/job/MyPlugin/lastSuccessfulBuild/artifact/target/MyPlugin.jar",
    "https://github.com/author/plugin/releases/download/v1.0.0/plugin.jar"
))
```

## Tasks

- `setupPaperServer` - Downloads server jar, plugins, and accepts EULA
- `runPaperServer` - Starts the server with configured settings

## Notes

- The plugin automatically cleans up old server jars when downloading new versions
- EULA is automatically accepted
- For ASP servers, the Slime World Plugin is automatically downloaded unless disabled
- All server types support the same plugin download mechanisms
- Snapshot versions are supported for all server types
- If no build number is specified, the latest available build is automatically downloaded
- Specific build numbers can be used to ensure reproducible deployments
- **Auto-parsing**: Full version strings like `"3.4.0-SNAPSHOT-523"` are automatically parsed into version and build components
- Build numbers specified in `build.set()` take precedence over those parsed from version strings
- Snapshot versions are supported for all server types
- If no build number is specified, the latest available build is automatically downloaded
- Specific build numbers can be used to ensure reproducible deployments
- **Auto-parsing**: Full version strings like `"3.4.0-SNAPSHOT-523"` are automatically parsed into version and build components
- Build numbers specified in `build.set()` take precedence over those parsed from version strings