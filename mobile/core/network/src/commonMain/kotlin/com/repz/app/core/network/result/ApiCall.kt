package com.repz.app.core.network.result

import com.repz.app.core.common.coroutines.rethrowIfCancellation
import com.repz.app.core.network.dto.problem.ProblemDetailsDto
import com.repz.app.core.network.session.RefreshUnavailableException
import com.repz.app.core.network.session.SessionManager
import com.repz.app.core.network.session.SessionStatus
import com.repz.app.core.network.session.SignOutReason
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException

private val PROBLEM_JSON_CONTENT_TYPE = ContentType.parse("application/problem+json")

/**
 * The one place an HTTP call turns into a [NetworkResult]: runs [request], decodes a 2xx body with
 * [decode], maps anything else to a [NetworkError]. [sessionManager], when given, lets a 401 from
 * the authenticated client be reported as [NetworkError.SessionExpired] once the refresh flow has
 * already signed the session out, instead of a generic [NetworkError.Api]. Never throws, except a
 * programming error or [kotlinx.coroutines.CancellationException] (Konsist E1).
 */
internal suspend fun <T> apiCall(
    sessionManager: SessionManager? = null,
    decode: suspend (HttpResponse) -> T,
    request: suspend () -> HttpResponse,
): NetworkResult<T> {
    val response =
        try {
            request()
        } catch (e: RefreshUnavailableException) {
            return NetworkResult.Failure(e.error)
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            return NetworkResult.Failure(e.toTransportFailure() ?: throw e)
        }

    return try {
        if (response.status.isSuccess()) {
            NetworkResult.Success(decode(response))
        } else {
            NetworkResult.Failure(response.toNetworkError(sessionManager))
        }
    } catch (e: SerializationException) {
        NetworkResult.Failure(NetworkError.MalformedResponse(e.message ?: "the response body did not match the expected shape"))
    } catch (e: ContentConvertException) {
        // Ktor wraps a SerializationException thrown by the content negotiation converter in its
        // own exception type, which is not itself a SerializationException.
        NetworkResult.Failure(NetworkError.MalformedResponse(e.message ?: "the response body did not match the expected shape"))
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        NetworkResult.Failure(e.toTransportFailure() ?: throw e)
    }
}

/** A convenience for calls whose success body carries nothing (e.g. a 204 `logout`). */
internal suspend fun apiCall(request: suspend () -> HttpResponse): NetworkResult<Unit> = apiCall(decode = {}, request = request)

private suspend fun HttpResponse.toNetworkError(sessionManager: SessionManager?): NetworkError {
    if (status == HttpStatusCode.Unauthorized && sessionManager.hasJustExpired()) {
        return NetworkError.SessionExpired
    }
    val problem = if (contentType()?.withoutParameters() == PROBLEM_JSON_CONTENT_TYPE) body<ProblemDetailsDto>().toApiProblem() else null
    return NetworkError.Api(status.value, problem)
}

private fun SessionManager?.hasJustExpired(): Boolean {
    val currentStatus = this?.status?.value ?: return false
    return currentStatus is SessionStatus.SignedOut && currentStatus.reason == SignOutReason.Expired
}
