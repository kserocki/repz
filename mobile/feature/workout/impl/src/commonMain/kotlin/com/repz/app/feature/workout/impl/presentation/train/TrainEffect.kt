package com.repz.app.feature.workout.impl.presentation.train

sealed interface TrainEffect {
    data object OpenLiveWorkout : TrainEffect
}
