package com.repz.app.core.testing.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// Scans only the `di` subpackage (the no-argument form's real effect), not the whole `:core:testing`
// module - so `Bad` below, which lives one package up, is never registered.
@Module
@ComponentScan("com.repz.app.core.testing.di")
class TestingModule
