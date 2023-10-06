package `in`.technowolf.nyx.base.domain.outcome

inline fun <T> Outcome<T>.mapSuccess(
    onOutcome: Outcome.Success<T>.() -> Outcome<T>,
): Outcome<T> {
    if (this is Outcome.Success) {
        return onOutcome(this)
    }
    return this
}
