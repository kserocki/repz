package com.repz.app.feature.history.impl

import org.koin.core.context.startKoin

fun bad() {
    startKoin {
        // trailing-lambda form, no parentheses call
    }
}
