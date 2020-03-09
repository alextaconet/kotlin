package org.jetbrains.kotlin.tools.projectWizard.plugins

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.context.WritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PomIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Paths

class StructurePlugin(context: Context) : Plugin(context) {
    val projectPath by valueSetting("Root path", GenerationPhase.PROJECT_GENERATION, pathParser) {
        defaultValue = value(Paths.get("."))
    }
    val name by stringSetting("Name", GenerationPhase.PROJECT_GENERATION)

    val groupId by stringSetting("Group ID", GenerationPhase.PROJECT_GENERATION) {
        isSavable = true
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Group ID", setOf('.', '_')))
    }
    val artifactId by stringSetting("Artifact ID", GenerationPhase.PROJECT_GENERATION) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Artifact ID", setOf('_')))
    }
    val version by stringSetting("Version", GenerationPhase.PROJECT_GENERATION) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Version", setOf('_', '-', '.')))
        defaultValue = value("1.0-SNAPSHOT")
    }

    val createProjectDir by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        withAction {
            service<FileSystemWizardService>().createDirectory(StructurePlugin::projectPath.reference.settingValue)
        }
    }
}

val ReadingContext.projectPath
    get() = StructurePlugin::projectPath.reference.settingValue

val ReadingContext.projectName
    get() = StructurePlugin::name.reference.settingValue


fun WritingContext.pomIR() = PomIR(
    artifactId = StructurePlugin::artifactId.reference.settingValue,
    groupId = StructurePlugin::groupId.reference.settingValue,
    version = Version.fromString(StructurePlugin::version.reference.settingValue)
)
