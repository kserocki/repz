package com.repz.app.core.testing

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// The no-argument form only scans the `di` subpackage (ARCH §3.3), never a whole module's root
// package: D1 requires the explicit string argument.
@Module
@ComponentScan
class TestingModule
