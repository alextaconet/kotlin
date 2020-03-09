package org.jetbrains.kotlin.tools.projectWizard.wizard.ui


import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.WritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSettingPropertyReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference

abstract class Component : Displayable {
    private val subComponents = mutableListOf<Component>()

    open fun onInit() {
        subComponents.forEach(Component::onInit)
    }

    protected fun <C : Component> C.asSubComponent(): C = also {
        this@Component.subComponents += it
    }
}

abstract class DynamicComponent(private val context: Context) : Component() {
    private var isInitialized: Boolean = false

    override fun onInit() {
        super.onInit()
        isInitialized = true
    }

    var <V : Any, T : SettingType<V>> SettingReference<V, T>.value: V?
        get() = read { notRequiredSettingValue() }
        set(value) = modify {
            value?.let { setValue(it) }
        }


    inline val <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.value: V?
        get() = reference.value

    init {
        write {
            eventManager.addSettingUpdaterEventListener { reference ->
                if (isInitialized) onValueUpdated(reference)
            }
        }
    }

    protected fun <T> read(reader: ReadingContext.() -> T): T =
        context.read(reader)

    protected fun <T> write(writer: WritingContext.() -> T): T =
        context.write(writer)

    protected fun <T> modify(modifier: SettingsWritingContext.() -> T): T =
        context.writeSettings(modifier)

    open fun onValueUpdated(reference: SettingReference<*, *>?) {}
}

interface FocusableComponent {
    fun focusOn() {}
}