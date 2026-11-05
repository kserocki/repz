package com.repz.app.core.designsystem.logo

/** A determinate [RepzLoader]'s per-pair state (MOT-25/MOT-18): [Locked] pairs are drawn solid and
 * stay put, [Loading] is the single pair currently running the indeterminate loop's slide-and-fade,
 * [Pending] pairs are drawn as a dashed outline. */
internal enum class PairState { Locked, Loading, Pending }
