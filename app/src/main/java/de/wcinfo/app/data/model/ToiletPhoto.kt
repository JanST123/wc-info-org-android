package de.wcinfo.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToiletPhoto(
    val id: Int = 0,
    @SerialName("toilet_id")
    val toiletId: Int = 0,
    val url: String = "",
    @SerialName("url_thumb")
    val urlThumb: String = "",
    @SerialName("is_main")
    val isMain: Boolean = false,
    val filename: String? = null
) {
    val resolvedFilename: String
        get() {
            if (!filename.isNullOrBlank()) return filename
            val cleanUrl = url.substringBefore('?').trimEnd('/')
            return cleanUrl.substringAfterLast('/', "")
        }
}
