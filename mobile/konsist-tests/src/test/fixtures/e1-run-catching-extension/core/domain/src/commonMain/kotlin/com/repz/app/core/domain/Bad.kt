package com.repz.app.core.domain

class Bad {
    fun risky(value: Int): Int = value.runCatching { this / 0 }.getOrDefault(0)
}
