package kg.freedge.core.platform

expect class ImageStorage() {
    fun save(fileName: String, bytes: ByteArray): String
    fun load(fileName: String): ByteArray?
    fun delete(fileName: String)
}
