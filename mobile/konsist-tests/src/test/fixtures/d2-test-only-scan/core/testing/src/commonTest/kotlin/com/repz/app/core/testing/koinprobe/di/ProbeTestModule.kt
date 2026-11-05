package com.repz.app.core.testing.koinprobe.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// A test-only scan must not count as the module's own scan for D2: it never ships with the real
// app, so a main class it happens to cover is still never registered.
@Module
@ComponentScan("com.repz.app.core.testing.koinprobe")
class ProbeTestModule
