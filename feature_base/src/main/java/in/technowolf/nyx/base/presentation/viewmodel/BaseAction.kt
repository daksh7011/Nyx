package `in`.technowolf.nyx.base.presentation.viewmodel

interface BaseAction<State>{
    fun reduce(state: State): State
}
