package kg.freedge.core.platform

expect class Haptics() {
    fun performSuccess()
    fun performError()
    fun performClick()
}
