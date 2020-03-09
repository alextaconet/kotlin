package org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate

class ProjectTemplatesPlugin(context: Context) : Plugin(context) {
    val template by dropDownSetting<ProjectTemplate>(
        "Template",
        GenerationPhase.INIT_TEMPLATE,
        parser = valueParserM { _, _ ->
            Failure(ParseError("Project templates is not supported in yaml for now"))
        }
    ) {
        values = ProjectTemplate.ALL
        isRequired = false
    }
}

fun SettingsWritingContext.applyProjectTemplate(projectTemplate: ProjectTemplate) {
    projectTemplate.setsValues.forEach { (setting, value) ->
        setting.setValue(value)
    }
    KotlinPlugin::modules.settingValue.withAllSubModules(includeSourcesets = true).forEach { module ->
        module.apply { initDefaultValuesForSettings() }
    }
}