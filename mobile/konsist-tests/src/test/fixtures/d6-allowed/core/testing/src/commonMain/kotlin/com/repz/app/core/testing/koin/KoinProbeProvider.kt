package com.repz.app.core.testing.koin

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

// core:testing's cross-module probe is exempt from D6 by design: it exists specifically to prove
// a bare provider function still resolves correctly next to a scanned class (ARCH §12.2 §3.5).
@Module
class KoinProbeProviderModule {
    @Single
    fun koinProbeProvidedDependency(): KoinProbeProvidedDependency = KoinProbeProvidedDependency()
}

class KoinProbeProvidedDependency
