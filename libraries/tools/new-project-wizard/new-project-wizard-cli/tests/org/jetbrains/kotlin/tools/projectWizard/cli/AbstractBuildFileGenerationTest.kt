/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.tools.projectWizard.core.ExceptionError
import org.jetbrains.kotlin.tools.projectWizard.core.div
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.MavenPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GroovyDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.KotlinDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.YamlWizard
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractBuildFileGenerationTest : UsefulTestCase() {
    fun doTest(directoryPath: String) {
        val directory = Paths.get(directoryPath)
        val expectedDirectory = expectedDirectory(directory)

        for (buildSystem in BuildSystem.values()) {
            if (Files.exists(expectedDirectory / buildSystem.buildFileName)) {
                doTest(directory, buildSystem)
            }
        }
    }

    private fun doTest(directory: Path, buildSystem: BuildSystem) {
        val yaml = directory.resolve("settings.yaml").toFile().readText() + "\n" +
                defaultStructure + "\n" +
                buildSystem.yaml
        val tempDir = Files.createTempDirectory(null)
        val wizard = YamlWizard(yaml, tempDir.toString(), Plugins.allPlugins, isUnitTestMode = true)
        val result = wizard.apply(Services.IDEA_INDEPENDENT_SERVICES, GenerationPhase.ALL)
        result.onFailure { errors ->
            errors.forEach { error ->
                if (error is ExceptionError) {
                    throw error.exception
                }
            }
            fail(errors.joinToString("\n"))
        }

        val expectedDirectory = expectedDirectory(directory)

        compareFiles(
            expectedDirectory.allBuildFiles(buildSystem), expectedDirectory,
            tempDir.allBuildFiles(buildSystem), tempDir
        )
    }

    private fun Path.allBuildFiles(buildSystem: BuildSystem) =
        listFiles { it.fileName.toString() == buildSystem.buildFileName }

    private enum class BuildSystem(val buildFileName: String, val yaml: String) {
        GRADLE_KOTLIN_DSL(
            buildFileName = "build.gradle.kts",
            yaml = """buildSystem:
                            type: GradleKotlinDsl
                            gradle:
                              createGradleWrapper: false
                              version: 5.4.1""".trimIndent()
        ),
        GRADLE_GROOVY_DSL(
            buildFileName = "build.gradle",
            yaml = """buildSystem:
                            type: GradleGroovyDsl
                            gradle:
                              createGradleWrapper: false
                              version: 5.4.1""".trimIndent()
        ),
        MAVEN(
            buildFileName = "pom.xml",
            yaml = """buildSystem:
                            type: Maven""".trimIndent()
        )
    }

    private fun expectedDirectory(directory: Path): Path =
        (directory / EXPECTED_DIRECTORY_NAME).takeIf { Files.exists(it) } ?: directory

    companion object {
        private const val EXPECTED_DIRECTORY_NAME = "expected"

        private val defaultStructure =
            """structure:
              name: generatedProject
              groupId: testGroupId
              artifactId: testArtifactId
            """.trimIndent()
    }
}
