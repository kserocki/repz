package com.repz.app.core.network.testing

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.repz.app.core.network.client.RepzHttpClients
import com.repz.app.core.network.client.createRepzHttpClients
import com.repz.app.core.network.config.ApiConfig
import com.repz.app.core.network.session.PersistentTokenStore
import com.repz.app.core.network.session.SecureStringStorage
import com.repz.app.core.network.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

/** A hand-written fake, no mocking library (N7): an in-memory stand-in for the platform Keystore/Keychain. */
internal class InMemorySecureStringStorage : SecureStringStorage {
    private var stored: String? = null

    override suspend fun read(): String? = stored

    override suspend fun write(value: String): Boolean {
        stored = value
        return true
    }

    override suspend fun delete() {
        stored = null
    }
}

/** A hand-written fake [LogWriter] (N7) capturing every logged message string for assertions. */
internal class RecordingLogWriter : LogWriter() {
    val messages = mutableListOf<String>()

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        messages += message
    }
}

internal fun testLogger(writer: LogWriter = RecordingLogWriter()): Logger = Logger(StaticConfig(logWriterList = listOf(writer)))

internal fun testApiConfig(): ApiConfig = ApiConfig(baseUrl = "http://test.invalid")

internal fun testSessionManager(storage: SecureStringStorage = InMemorySecureStringStorage()): SessionManager =
    SessionManager(PersistentTokenStore(storage), testLogger())

/** Builds a real [RepzHttpClients] over a [MockEngine], so tests exercise the production plugin setup. */
internal fun testClients(
    sessionManager: SessionManager = testSessionManager(),
    logger: Logger = testLogger(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): RepzHttpClients {
    val engine = MockEngine { request -> handler(request) }
    return createRepzHttpClients(engine, testApiConfig(), sessionManager, logger)
}
