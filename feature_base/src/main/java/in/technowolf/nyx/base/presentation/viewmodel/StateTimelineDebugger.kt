package `in`.technowolf.nyx.base.presentation.viewmodel

import `in`.technowolf.nyx.utils.NLog
import `in`.technowolf.nyx.utils.TagConstants.FEATURE_BASE
import kotlin.reflect.full.memberProperties

class StateTimelineDebugger(private val className: String) {
    private val stateTimeline = mutableListOf<StateTransition>()
    private var lastViewAction: BaseAction<*>? = null

    // Get list of properties from  ViewState instances (all have the same type)
    private val propertyNames by lazy {
        stateTimeline.first().previousState.javaClass.kotlin.memberProperties.map { it.name }
    }

    fun addAction(viewAction: BaseAction<*>) {
        lastViewAction = viewAction
    }

    fun addStateTransition(oldState: BaseState, newState: BaseState) {
        val lastViewAction = checkNotNull(lastViewAction) { "lastViewAction is null. Please log action before transition." }
        stateTimeline.add(StateTransition(oldState, lastViewAction, newState))
        this.lastViewAction = null
    }

    private fun getMessage() = getMessage(stateTimeline)

    private fun getMessage(stateTimeline: List<StateTransition>): String {
        if (stateTimeline.isEmpty()) {
            return "$className has no state transitions. \n"
        }

        var message = ""
        stateTimeline.forEach { stateTransition ->
            message += "Action: $className.${stateTransition.action.javaClass.simpleName}: \n"

            propertyNames.forEach { propertyName ->
                val logLine = getLogLine(stateTransition.previousState, stateTransition.nextState, propertyName)
                message += logLine
            }
        }
        return message
    }

    fun logAll() {
        NLog.d(FEATURE_BASE, getMessage())
    }

    fun logLast() {
        val lastStates = listOf(stateTimeline.last())
        NLog.d(FEATURE_BASE, getMessage(lastStates))
    }

    private fun getLogLine(previousState: BaseState, nextState: BaseState, propertyName: String): String {
        val previousValue = getPropertyValue(previousState, propertyName)
        val nextValue = getPropertyValue(nextState, propertyName)
        val indent = "\t"

        return if (previousValue != nextValue) {
            "$indent*$propertyName: $previousValue -> $nextValue \n"
        } else {
            "$indent*$propertyName: $nextValue\n"
        }
    }

    private fun getPropertyValue(baseState: BaseState, propertyName: String): String {
        baseState::class.memberProperties.forEach {
            if (propertyName == it.name) {
                var value = it.getter.call(baseState).toString()
                if (value.isBlank()) {
                    value = "\"\""
                }
                return value
            }
        }
        return ""
    }

    private data class StateTransition(
        val previousState: BaseState,
        val action: BaseAction<*>,
        val nextState: BaseState,
    )
}
