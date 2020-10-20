package rest

import kotlinx.serialization.Serializable

@Serializable
data class BasicResponse(
    val requestSuccess: Boolean
)

@Serializable
data class Version(
    val major: Long,
    val minor: Long,
    val patch: Long
)