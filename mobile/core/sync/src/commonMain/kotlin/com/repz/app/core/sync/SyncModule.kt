package com.repz.app.core.sync

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * `core:sync`'s one `@Module` (ARCH §9, Konsist D3). Every binding is a class-level `@Single`
 * picked up by the scan below (`SyncEngine` today; the handler registry, status tracker and
 * coordinator follow); this class carries no provider functions of its own.
 */
@Module
@ComponentScan("com.repz.app.core.sync")
class SyncModule
