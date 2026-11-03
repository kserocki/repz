package com.repz.app.shared.di

import org.koin.core.annotation.Module

/**
 * Holds the one binding that needs an Android `Context`: `SecureStringStorage` (S1 spike,
 * ADR-0008). `Context` is never an `actual class` constructor parameter (Koin compiler plugin
 * issue #37); each platform's `actual` adds it only as a `@Single` provider function parameter.
 */
@Module
expect class PlatformModule
