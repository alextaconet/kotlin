package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.context.WritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.cached
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.enumSettingImpl
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.StdlibType
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleConfigurationData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.correspondingStdlib
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Path
import kotlin.properties.ReadOnlyProperty


sealed class ModuleCondifuratorSettingsEnvironment {
    abstract val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
}

class ModuleBasedConfiguratorSettingsEnvironment(
    private val configurator: ModuleConfigurator,
    private val module: Module
) : ModuleCondifuratorSettingsEnvironment() {
    override val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
        get() = ModuleBasedConfiguratorSettingReference(configurator, module, this)
}

class IdBasedConfiguratorSettingsEnvironment(
    private val configurator: ModuleConfigurator,
    private val moduleId: Identificator
) : ModuleCondifuratorSettingsEnvironment() {
    override val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
        get() = IdBasedConfiguratorSettingReference(configurator, moduleId, this)
}

fun <T> withSettingsOf(
    moduleId: Identificator,
    configurator: ModuleConfigurator,
    function: ModuleCondifuratorSettingsEnvironment.() -> T
): T = function(IdBasedConfiguratorSettingsEnvironment(configurator, moduleId))

fun <T> withSettingsOf(
    module: Module,
    configurator: ModuleConfigurator = module.configurator,
    function: ModuleCondifuratorSettingsEnvironment.() -> T
): T = function(ModuleBasedConfiguratorSettingsEnvironment(configurator, module))


abstract class ModuleConfiguratorSettings : SettingsOwner {
    final override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any?, ModuleConfiguratorSetting<V, T>> = cached { name ->
        ModuleConfiguratorSetting(create(name).buildInternal())
    }

    @Suppress("UNCHECKED_CAST")
    final override fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: DropDownSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, DropDownSettingType<V>>> =
        super.dropDownSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, DropDownSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: StringSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<String, StringSettingType>> =
        super.stringSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<String, StringSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: BooleanSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Boolean, BooleanSettingType>> =
        super.booleanSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Boolean, BooleanSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ValueSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, ValueSettingType<V>>> =
        super.valueSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, ValueSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: VersionSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Version, VersionSettingType>> =
        super.versionSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Version, VersionSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ListSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<List<V>, ListSettingType<V>>> =
        super.listSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<List<V>, ListSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: PathSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Path, PathSettingType>> =
        super.pathSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Path, PathSettingType>>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified E> enumSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<E, DropDownSettingType<E>>> where E : Enum<E>, E : DisplayableSettingItem =
        enumSettingImpl(title, neededAtPhase, init) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<E, DropDownSettingType<E>>>
}

interface ModuleConfiguratorWithSettings : ModuleConfigurator {
    fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = emptyList()
    fun getPluginSettings(): List<PluginSettingReference<Any, SettingType<Any>>> = emptyList()


    fun SettingsWritingContext.initDefaultValuesFor(module: Module) {
        withSettingsOf(module) {
            getConfiguratorSettings().forEach { setting ->
                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
            }
        }
    }
}

val ModuleConfigurator.settings
    get() = when (this) {
        is ModuleConfiguratorWithSettings -> getConfiguratorSettings()
        else -> emptyList()
    }

fun ReadingContext.allSettingsOfModuleConfigurator(moduleConfigurator: ModuleConfigurator) = when (moduleConfigurator) {
    is ModuleConfiguratorWithSettings -> buildList<Setting<Any, SettingType<Any>>> {
        +moduleConfigurator.getConfiguratorSettings()
        +moduleConfigurator.getPluginSettings().map { it.pluginSetting }
    }
    else -> emptyList()
}

fun Module.getConfiguratorSettings() = buildList<SettingReference<*, *>> {
    +configurator.settings.map { setting ->
        ModuleBasedConfiguratorSettingReference(configurator, this@getConfiguratorSettings, setting)
    }
    configurator.safeAs<ModuleConfiguratorWithSettings>()?.getPluginSettings()?.let { +it }
}


interface ModuleConfigurator : DisplayableSettingItem, EntitiesOwnerDescriptor {
    val moduleKind: ModuleKind
    override val text: String
        get() = id

    val suggestedModuleName: String? get() = null
    val canContainSubModules: Boolean get() = false
    val requiresRootBuildFile: Boolean get() = false

    fun createBuildFileIRs(
        readingContext: ReadingContext,
        configurationData: ModuleConfigurationData,
        module: Module
    ): List<BuildSystemIR> =
        emptyList()

    fun createModuleIRs(
        readingContext: ReadingContext,
        configurationData: ModuleConfigurationData,
        module: Module
    ): List<BuildSystemIR> =
        emptyList()

    fun createStdlibType(configurationData: ModuleConfigurationData, module: Module): StdlibType? =
        safeAs<ModuleConfiguratorWithModuleType>()?.moduleType?.correspondingStdlib()

    fun createRootBuildFileIrs(configurationData: ModuleConfigurationData): List<BuildSystemIR> = emptyList()
    fun createKotlinPluginIR(configurationData: ModuleConfigurationData, module: Module): KotlinBuildSystemPluginIR? =
        null

    fun WritingContext.runArbitraryTask(
        configurationData: ModuleConfigurationData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = UNIT_SUCCESS

    companion object {
        val ALL = buildList<ModuleConfigurator> {
            +RealNativeTargetConfigurator.configurators
            +NativeForCurrentSystemTarget
            +JsBrowserTargetConfigurator
            +JsNodeTargetConfigurator
            +CommonTargetConfigurator
            +JvmTargetConfigurator
            +AndroidTargetConfigurator
            +MppModuleConfigurator
            +JvmSinglePlatformModuleConfigurator
            +AndroidSinglePlatformModuleConfigurator
            +IOSSinglePlatformModuleConfigurator
            +JsSingleplatformModuleConfigurator
        }

        init {
            ALL.groupBy(ModuleConfigurator::id)
                .forEach { (id, configurators) -> assert(configurators.size == 1) { id } }
        }

        private val BY_ID = ALL.associateBy(ModuleConfigurator::id)

        fun getParser(moduleIdentificator: Identificator): Parser<ModuleConfigurator> =
            valueParserM { value, path ->
                val (id) = value.parseAs<String>(path)
                BY_ID[id].toResult { ConfiguratorNotFoundError(id) }
            } or mapParser { map, path ->
                val (id) = map.parseValue<String>(path, "name")
                val (configurator) = BY_ID[id].toResult { ConfiguratorNotFoundError(id) }
                val (settingsWithValues) = configurator.settings.mapComputeM { setting ->
                    val (settingValue) = map[setting.path].toResult { ParseError("No value was found for a key `$path.${setting.path}`") }
                    val reference = withSettingsOf(moduleIdentificator, configurator) { setting.reference }
                    setting.type.parse(this, settingValue, setting.path).map { reference to it }
                }.sequence()
                updateState { it.withSettings(settingsWithValues) }
                configurator
            }
    }
}

interface GradleModuleConfigurator : ModuleConfigurator {
    fun ReadingContext.createSettingsGradleIRs(module: Module): List<BuildSystemIR> = emptyList()
}