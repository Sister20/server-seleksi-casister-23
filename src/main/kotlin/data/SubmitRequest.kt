package `fun`.suggoi.seleksister20.data

import kotlinx.serialization.Serializable

@Serializable
data class SubmitRequest(val fullName: String? = null, val link: String? = null, val message: String? = null)