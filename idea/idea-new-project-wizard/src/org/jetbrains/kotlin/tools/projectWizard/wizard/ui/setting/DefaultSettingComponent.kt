package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.openapi.ui.ComboBox
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.CheckboxComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.PathFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.TextFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent


object DefaultSettingComponent {
    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> create(
        setting: SettingReference<V, T>,
        context: Context
    ): SettingComponent<V, T> = when (setting.type) {
        VersionSettingType::class ->
            VersionSettingComponent(
                setting as SettingReference<Version, VersionSettingType>,
                context
            ) as SettingComponent<V, T>
        BooleanSettingType::class ->
            BooleanSettingComponent(
                setting as SettingReference<Boolean, BooleanSettingType>,
                context
            ) as SettingComponent<V, T>
        DropDownSettingType::class ->
            DropdownSettingComponent(
                setting as SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
                context
            ) as SettingComponent<V, T>
        StringSettingType::class ->
            StringSettingComponent(
                setting as SettingReference<String, StringSettingType>,
                context
            ) as SettingComponent<V, T>
        PathSettingType::class ->
            PathSettingComponent(
                setting as SettingReference<Path, PathSettingType>,
                context
            ) as SettingComponent<V, T>
        else -> TODO(setting.type.qualifiedName!!)
    }
}

class VersionSettingComponent(
    reference: SettingReference<Version, VersionSettingType>,
    context: Context
) : SettingComponent<Version, VersionSettingType>(reference, context) {
    private val settingLabel = label(setting.title)
    private val comboBox = ComboBox<Version>().apply {
        addItemListener { e ->
            if (e?.stateChange == ItemEvent.SELECTED) {
                value = e.item as Version
            }
        }
    }

    override fun onInit() {
        super.onInit()
        val values = read { reference.savedOrDefaultValue }?.let(::listOf).orEmpty()
        comboBox.model = DefaultComboBoxModel<Version>(values.toTypedArray())

        if (values.isNotEmpty()) {
            value = values.first()
        }
    }

    override val validationIndicator = null
    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(
                panel {
                    add(settingLabel, BorderLayout.CENTER)
                },
                BorderLayout.WEST
            )
            add(comboBox, BorderLayout.CENTER)
        }
    }
}


class DropdownSettingComponent(
    reference: SettingReference<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>,
    context: Context
) : UIComponentDelegatingSettingComponent<DisplayableSettingItem, DropDownSettingType<DisplayableSettingItem>>(
    reference,
    context
) {
    override val uiComponent = DropDownComponent(
        context = context,
        initialValues = setting.type.values,
        validator = setting.validator,
        filter = { value ->
            context.read { setting.type.filter(this, reference, value) }
        },
        labelText = setting.title,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class BooleanSettingComponent(
    reference: SettingReference<Boolean, BooleanSettingType>,
    context: Context
) : UIComponentDelegatingSettingComponent<Boolean, BooleanSettingType>(
    reference,
    context
) {
    override val uiComponent = CheckboxComponent(
        context = context,
        labelText = setting.title,
        initialValue = null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class StringSettingComponent(
    reference: SettingReference<String, StringSettingType>,
    context: Context,
    showLabel: Boolean = true
) : UIComponentDelegatingSettingComponent<String, StringSettingType>(
    reference,
    context
) {
    override val uiComponent = TextFieldComponent(
        context = context,
        initialValue = null,
        labelText = if (showLabel) setting.title else null,
        validator = setting.validator,
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}

class PathSettingComponent(
    reference: SettingReference<Path, PathSettingType>,
    context: Context
) : UIComponentDelegatingSettingComponent<Path, PathSettingType>(
    reference,
    context
) {
    override val uiComponent = PathFieldComponent(
        context = context,
        labelText = setting.title,
        validator = settingValidator { path -> setting.validator.validate(this, path) },
        onValueUpdate = { newValue -> value = newValue }
    ).asSubComponent()
}