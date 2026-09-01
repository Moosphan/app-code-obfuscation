package com.dorck.app.code.guard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GeneratedSourceDirectoryTest {

    @Test
    fun `fromBuildDir places generated Java sources below build directory`() {
        val buildDir = File("test-project/build")

        assertEquals(
            File(buildDir, "generated/codeguard/java"),
            GeneratedSourceDirectory.fromBuildDir(buildDir)
        )
    }
}
