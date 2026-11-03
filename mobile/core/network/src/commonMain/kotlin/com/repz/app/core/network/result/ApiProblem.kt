package com.repz.app.core.network.result

/** The decoded shape of an RFC 9457 Problem Details response (ADR-0016), with [type] resolved to a known [ProblemType]. */
data class ApiProblem(
    val type: ProblemType,
    val title: String,
    val status: Int,
    val detail: String?,
    val requestId: String,
    val fieldErrors: List<ProblemFieldError>,
)

data class ProblemFieldError(
    val path: String,
    val message: String,
)

/** The closed set of `urn:repz:problem:<name>` values the top-level Problem Details envelope can carry (ADR-0016). */
sealed interface ProblemType {
    data object ValidationError : ProblemType

    data object EmailTaken : ProblemType

    data object InvalidCredentials : ProblemType

    data object InvalidRefreshToken : ProblemType

    data object InvalidAccessToken : ProblemType

    /** `about:blank`: the server attached no specific problem type (a generic 4xx/5xx). */
    data object PlainHttp : ProblemType

    /** Any `urn:repz:problem:<name>` this build doesn't know, kept instead of failing to decode. */
    data class Other(
        val uri: String,
    ) : ProblemType
}
