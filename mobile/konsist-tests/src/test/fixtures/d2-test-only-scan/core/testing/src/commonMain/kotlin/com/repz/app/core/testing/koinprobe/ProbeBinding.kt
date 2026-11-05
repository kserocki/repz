package com.repz.app.core.testing.koinprobe

import org.koin.core.annotation.Single

// Covered only by ProbeTestModule's commonTest @ComponentScan below, never by a main-source one -
// so nothing in the real app ever registers it. D2 must fail this, not pass it just because a
// test-source scan happens to cover the package (mirrors the real cross-module Koin probe in
// core:testing's commonTest).
@Single
class ProbeBinding
