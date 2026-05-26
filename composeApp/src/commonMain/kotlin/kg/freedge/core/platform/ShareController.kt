package kg.freedge.core.platform

expect class ShareController() {
    fun shareText(text: String, imageBytes: ByteArray?)
}
