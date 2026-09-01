package com.dorck.app.code.guard

import java.io.File

internal object GeneratedSourceDirectory {
    const val relativePath = "generated/codeguard/java"

    fun fromBuildDir(buildDir: File): File = File(buildDir, relativePath)
}
