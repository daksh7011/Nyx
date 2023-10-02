package `in`.technowolf.nyx.utils

infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)
infix fun Byte.shr(that: Int): Int = this.toInt().shr(that)

infix fun Byte.and(that: Int): Int = this.toInt().and(that)
infix fun Int.and(that: Byte): Int = this.and(that.toInt())
