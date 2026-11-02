package com.narezany.calculator.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Material 3 Expressive motion tokens.
 *
 * AndroidX publishes these through `MotionScheme.expressive()`, but that API is
 * still `internal` in the stable `material3` 1.4.x line (it becomes public only
 * in 1.5.0, which requires compileSdk 37 / AGP 9.1). The token values below are
 * copied 1:1 from `androidx.compose.material3.tokens.ExpressiveMotionTokens`,
 * so the resulting motion is exactly the expressive scheme:
 *
 * | token          | dampingRatio | stiffness |
 * |----------------|--------------|-----------|
 * | defaultSpatial | 0.8          | 380       |
 * | defaultEffects | 1.0          | 1600      |
 * | fastSpatial    | 0.6          | 800       |
 * | fastEffects    | 1.0          | 3800      |
 * | slowSpatial    | 0.8          | 200       |
 * | slowEffects    | 1.0          | 800       |
 *
 * Spatial springs move geometry (size, corner radius, offset) and deliberately
 * overshoot a little; effects springs change colour and alpha without any
 * overshoot, so text never flashes.
 */
object ExpressiveMotion {

    /** Geometry: the default spatial spring, damping 0.8 / stiffness 380. */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Geometry for small, quick changes. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Geometry for large, gentle transitions. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    /** Colour / alpha: critically damped, no overshoot. */
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    /** Colour / alpha for the fastest state changes. */
    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 3800f)
}
