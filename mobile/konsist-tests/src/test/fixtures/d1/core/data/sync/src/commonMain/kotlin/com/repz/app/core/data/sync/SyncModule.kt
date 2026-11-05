package com.repz.app.core.data.sync

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// A separate Gradle module (`:core:data:sync`) whose scan package is a segment-extension of
// `:core:data`'s own scan - the exact bug D1 exists to catch: `pkg.startsWith(scan + ".")` would
// otherwise let `:core:data`'s scan silently also pick up classes meant for this module.
@Module
@ComponentScan("com.repz.app.core.data.sync")
class SyncModule
