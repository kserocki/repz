package com.repz.app.feature.workout.impl.domain

import com.repz.app.core.domain.preferences.UserPreferencesRepository
import com.repz.app.core.domain.preferences.WeightStep
import org.koin.core.annotation.Factory

/** Persists the keypad's Step chip choice device-wide, for `step.unitSystem` only. */
@Factory
class SetWeightStepUseCase(
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke(step: WeightStep) {
        preferences.setWeightStep(step)
    }
}
