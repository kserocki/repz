package com.repz.app.core.network.contract

import com.repz.app.core.network.dto.auth.AuthSessionDto
import com.repz.app.core.network.dto.auth.AuthTokensDto
import com.repz.app.core.network.dto.auth.LoginRequestDto
import com.repz.app.core.network.dto.auth.LogoutRequestDto
import com.repz.app.core.network.dto.auth.RefreshRequestDto
import com.repz.app.core.network.dto.auth.RegisterRequestDto
import com.repz.app.core.network.dto.health.HealthResponseDto
import com.repz.app.core.network.dto.problem.ProblemDetailsDto
import com.repz.app.core.network.dto.sync.PullResponseDto
import com.repz.app.core.network.dto.sync.PushRequestDto
import com.repz.app.core.network.dto.sync.PushResponseDto
import com.repz.app.core.network.dto.user.UpdateProfileRequestDto
import com.repz.app.core.network.dto.user.UserDto
import kotlinx.serialization.KSerializer

/** One `contract/fixtures` file, generated at build time into [generatedContractFixtures]. */
internal data class ContractFixture(
    val path: String,
    val json: String,
)

/** The fixture's folder, e.g. `syncPull/200`, `register/request`, `components/ProblemDetails`. */
internal val ContractFixture.operationKey: String get() = path.substringBeforeLast('/')

/**
 * Every documented request body and response status with content, hand-written so a drift between
 * the DTOs and the contract shows up as a missing or unused catalog entry, not a silent gap.
 */
internal val CONTRACT_CATALOG: Map<String, KSerializer<*>> =
    mapOf(
        "getHealth/200" to HealthResponseDto.serializer(),
        "register/request" to RegisterRequestDto.serializer(),
        "register/201" to AuthSessionDto.serializer(),
        "register/400" to ProblemDetailsDto.serializer(),
        "register/409" to ProblemDetailsDto.serializer(),
        "login/request" to LoginRequestDto.serializer(),
        "login/200" to AuthSessionDto.serializer(),
        "login/400" to ProblemDetailsDto.serializer(),
        "login/401" to ProblemDetailsDto.serializer(),
        "refreshTokens/request" to RefreshRequestDto.serializer(),
        "refreshTokens/200" to AuthTokensDto.serializer(),
        "refreshTokens/400" to ProblemDetailsDto.serializer(),
        "refreshTokens/401" to ProblemDetailsDto.serializer(),
        "logout/request" to LogoutRequestDto.serializer(),
        "logout/400" to ProblemDetailsDto.serializer(),
        "getMe/200" to UserDto.serializer(),
        "getMe/401" to ProblemDetailsDto.serializer(),
        "updateMe/request" to UpdateProfileRequestDto.serializer(),
        "updateMe/200" to UserDto.serializer(),
        "updateMe/400" to ProblemDetailsDto.serializer(),
        "updateMe/401" to ProblemDetailsDto.serializer(),
        "syncPush/request" to PushRequestDto.serializer(),
        "syncPush/200" to PushResponseDto.serializer(),
        "syncPush/400" to ProblemDetailsDto.serializer(),
        "syncPush/401" to ProblemDetailsDto.serializer(),
        "syncPush/413" to ProblemDetailsDto.serializer(),
        "syncPull/200" to PullResponseDto.serializer(),
        "syncPull/400" to ProblemDetailsDto.serializer(),
        "syncPull/401" to ProblemDetailsDto.serializer(),
        "components/ProblemDetails" to ProblemDetailsDto.serializer(),
    )
