package com.repz.app.feature.auth.impl

import org.koin.core.annotation.ComponentScan

// Outside app:shared, D1 requires @ComponentScan to sit on the module's one @Module class - this
// class only scans, it never declares @Module.
@ComponentScan("com.repz.app.feature.auth.impl")
class AuthModule
