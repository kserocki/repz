package com.repz.app.core.domain

class Bad {
    fun risky(): Int = runCatching { 1 / 0 }.getOrDefault(0)
}
