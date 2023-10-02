package `in`.technowolf.nyx.base.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.technowolf.nyx.utils.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

abstract class BaseViewModel<State : BaseState, Action : BaseAction<State>>(initialState: State) : ViewModel() {

    private val _uiStateFlow: MutableStateFlow<State> = MutableStateFlow(initialState)
    val uiStateFlow: StateFlow<State> = _uiStateFlow.asStateFlow()

    private var stateTimelineDebugger: StateTimelineDebugger? = null

    val isDebug: Boolean get() = BuildConfig.DEBUG

    init {
        if (isDebug) {
            stateTimelineDebugger = StateTimelineDebugger(this::class.java.simpleName)
        }
    }

    private var state by Delegates.observable(initialState) { property, previousValue, nextValue ->
        if (previousValue == nextValue) {
            viewModelScope.launch {
                _uiStateFlow.value = nextValue
            }
            stateTimelineDebugger?.apply {
                addStateTransition(previousValue, nextValue)
                logLast()
            }
        }
    }

    protected fun sendAction(action: Action) {
        stateTimelineDebugger?.addAction(action)
        state = action.reduce(state)
    }
}
