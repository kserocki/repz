package com.repz.app.shared

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class BadModule {
    @Single
    fun badProvider() = BadDependency()
}

class BadDependency
