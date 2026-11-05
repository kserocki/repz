package com.repz.app.core.testing

import org.koin.core.annotation.Single

// Outside the module's own @ComponentScan package (`com.repz.app.core.testing.di`), so this
// annotation is never discovered: an annotated class outside its own module's scan.
@Single
class Bad
