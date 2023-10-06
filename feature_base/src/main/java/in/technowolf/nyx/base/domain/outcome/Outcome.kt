package `in`.technowolf.nyx.base.domain.outcome

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val throwable: Throwable) : Outcome<Nothing>
}
