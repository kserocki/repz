package com.repz.app.core.network.result

import com.repz.app.core.network.dto.problem.ProblemDetailsDto

private const val PLAIN_HTTP_PROBLEM_URI = "about:blank"
private const val PROBLEM_URN_PREFIX = "urn:repz:problem:"

private val KNOWN_PROBLEM_TYPES: Map<String, ProblemType> =
    mapOf(
        "${PROBLEM_URN_PREFIX}validation-error" to ProblemType.ValidationError,
        "${PROBLEM_URN_PREFIX}email-taken" to ProblemType.EmailTaken,
        "${PROBLEM_URN_PREFIX}invalid-credentials" to ProblemType.InvalidCredentials,
        "${PROBLEM_URN_PREFIX}invalid-refresh-token" to ProblemType.InvalidRefreshToken,
        "${PROBLEM_URN_PREFIX}invalid-access-token" to ProblemType.InvalidAccessToken,
        PLAIN_HTTP_PROBLEM_URI to ProblemType.PlainHttp,
    )

/** Resolves [ProblemDetailsDto.type] to a known [ProblemType], or [ProblemType.Other] for anything this build doesn't recognize. */
internal fun ProblemDetailsDto.toApiProblem(): ApiProblem =
    ApiProblem(
        type = KNOWN_PROBLEM_TYPES[type] ?: ProblemType.Other(type),
        title = title,
        status = status,
        detail = detail,
        requestId = requestId,
        fieldErrors = errors.orEmpty().map { ProblemFieldError(it.path, it.message) },
    )
