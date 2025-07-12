package com.gateopenerz.paperserver

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("unused")
abstract class PaperServerExtension @Inject constructor(objects: ObjectFactory) {
    abstract val version: Property<String>
    abstract val jvmArgs: Property<String>
    abstract val serverDir: Property<String>
}
