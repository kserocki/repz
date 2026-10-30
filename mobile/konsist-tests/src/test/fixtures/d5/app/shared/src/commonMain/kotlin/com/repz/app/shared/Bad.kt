package com.repz.app.shared

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class FirstModule {
    @Single
    fun provideThing(): Thing = Thing()
}

@Module
class SecondModule {
    @Single
    fun provideThingAgain(): Thing = Thing()
}

class Thing
