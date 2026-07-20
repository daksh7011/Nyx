package com.slothiesmooth.nyx.shared.data.source

/**
 * A single freshly-captured image and its suggested file name (`null` name = none available).
 */
data class PickedImage(val bytes: ByteArray, val suggestedName: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedImage) return false
        return bytes.contentEquals(other.bytes) && suggestedName == other.suggestedName
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + (suggestedName?.hashCode() ?: 0)
}

/**
 * System-camera capture (FileKit `openCameraPicker` on android/ios; unavailable on desktop/web).
 */
interface CameraSource {
    val isAvailable: Boolean
    suspend fun capture(): PickedImage?
}
