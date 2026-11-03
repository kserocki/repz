package com.repz.app.core.network.dto.problem

import kotlinx.serialization.Serializable

@Serializable
data class ProblemFieldErrorDto(
    val path: String,
    val message: String,
)

/** The top-level RFC 9457 Problem Details envelope (ADR-0016), returned by every 4xx/5xx response. */
@Serializable
data class ProblemDetailsDto(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val requestId: String,
    val errors: List<ProblemFieldErrorDto>? = null,
)

/** The smaller problem shape attached to one rejected item inside a sync push response. */
@Serializable
data class ItemProblemDto(
    val type: String,
    val title: String,
    val errors: List<ProblemFieldErrorDto>? = null,
)
