package com.gateopenerz.paperserver

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("unused")
abstract class PaperServerExtension @Inject constructor(objects: ObjectFactory) {
    abstract val serverType: Property<String>
    abstract val version: Property<String>
    abstract val build: Property<String>
    abstract val jvmArgs: Property<String>
    abstract val serverDir: Property<String>
    abstract val preLaunchTasks: ListProperty<String>
    abstract val plugins: ListProperty<String>
    abstract val pluginUrls: ListProperty<String>
    abstract val interactiveConsole: Property<Boolean>
    abstract val includeAspPlugin: Property<Boolean>
    abstract val aspBranch: Property<String>

    init {
        serverType.convention("paper")
        version.convention("1.21.8")
        jvmArgs.convention("-Xms1G -Xmx2G")
        serverDir.convention("development-server")
        preLaunchTasks.convention(emptyList())
        plugins.convention(emptyList())
        pluginUrls.convention(emptyList())
        interactiveConsole.convention(true)
    }
}