package com.repz.app.shared.di

import org.koin.dsl.KoinAppDeclaration

/**
 * The one binding [NetworkModuleGraphTest] needs that `RepzApp`'s own graph can't supply by
 * itself: an Android `Context` for `PlatformModule.secureStringStorage`. iOS needs no extra
 * binding (`KeychainSecureStringStorage` takes no `Context`), so its `actual` is a no-op.
 */
internal expect val platformTestDeclaration: KoinAppDeclaration
