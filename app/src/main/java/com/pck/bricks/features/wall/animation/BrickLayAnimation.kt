package com.pck.bricks.features.wall.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Returns an [Animatable] that bounces from 0→1 via a spring whenever [trigger] changes to a
 * non-null value. Used by WallCanvas to animate the newly placed brick.
 */
@Composable
fun rememberBrickLayScale(trigger: Int?): Animatable<Float, *> {
    val animatable = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger != null) {
            animatable.snapTo(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    return animatable
}
