package com.repz.app.shared.di

import org.koin.core.annotation.Module

/**
 * Holds the platform-built bindings: `SecureStringStorage` (S1 spike, ADR-0008) and this app's
 * first Koin wiring of `RepzDatabase` (ADR-0023). `Context` is never an `actual class` constructor
 * parameter (Koin compiler plugin issue #37); each platform's `actual` adds it only as a `@Single`
 * provider function parameter. Reason 3 (platform-built types): neither binding can be a class
 * annotation.
 */
@Module
expect class PlatformModule
