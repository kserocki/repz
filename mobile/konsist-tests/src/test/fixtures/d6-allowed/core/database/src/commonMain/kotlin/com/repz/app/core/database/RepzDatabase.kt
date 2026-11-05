package com.repz.app.core.database

interface CurrentUserDao

class RepzDatabase {
    fun currentUserDao(): CurrentUserDao = object : CurrentUserDao {}
}
