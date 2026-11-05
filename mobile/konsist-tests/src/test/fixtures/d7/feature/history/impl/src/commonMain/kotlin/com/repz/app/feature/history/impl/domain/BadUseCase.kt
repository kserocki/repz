package com.repz.app.feature.history.impl.domain

import org.koin.core.annotation.Single

// A *UseCase class outside core:domain must be @Factory (feature use cases are stateless, one
// per call site) - @Single is the wrong kind for its role.
@Single
class BadUseCase
