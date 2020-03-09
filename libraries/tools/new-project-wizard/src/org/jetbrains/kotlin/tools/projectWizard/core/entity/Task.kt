package org.jetbrains.kotlin.tools.projectWizard.core.entity


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import kotlin.reflect.KProperty1

typealias TaskReference = KProperty1<out Plugin, Task>
typealias Task1Reference<A, B> = KProperty1<out Plugin, Task1<A, B>>
typealias PipelineTaskReference = KProperty1<out Plugin, PipelineTask>

class TaskContext : EntityContext<Task, TaskReference>()

sealed class Task : EntityBase()

data class Task1<A, B : Any>(
    override val path: String,
    val action: WritingContext.(A) -> TaskResult<B>
) : Task() {
    class Builder<A, B : Any>(private val name: String) {
        private var action: WritingContext.(A) -> TaskResult<B> = { Failure() }

        fun withAction(action: WritingContext.(A) -> TaskResult<B>) {
            this.action = action
        }

        fun build(): Task1<A, B> = Task1(name, action)
    }
}

data class PipelineTask(
    override val path: String,
    val action: WritingContext.() -> TaskResult<Unit>,
    val before: List<PipelineTaskReference>,
    val after: List<PipelineTaskReference>,
    val phase: GenerationPhase,
    val isAvailable: Checker,
    val title: String?
) : Task() {
    class Builder(
        private val name: String,
        private val phase: GenerationPhase
    ) {
        private var action: WritingContext.() -> TaskResult<Unit> = { UNIT_SUCCESS }
        private val before = mutableListOf<PipelineTaskReference>()
        private val after = mutableListOf<PipelineTaskReference>()

        var isAvailable: Checker = ALWAYS_AVAILABLE_CHECKER

        var title: String? = null

        fun withAction(action: WritingContext.() -> TaskResult<Unit>) {
            this.action = action
        }

        fun runBefore(vararg before: PipelineTaskReference) {
            this.before.addAll(before)
        }

        fun runAfter(vararg after: PipelineTaskReference) {
            this.after.addAll(after)
        }

        fun build(): PipelineTask = PipelineTask(name, action, before, after, phase, isAvailable, title)
    }
}