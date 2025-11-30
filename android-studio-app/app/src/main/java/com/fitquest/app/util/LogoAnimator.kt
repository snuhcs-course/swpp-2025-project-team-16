package com.fitquest.app.util

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.LinearInterpolator

fun animateLogo(view: View) {
    val rotation = PropertyValuesHolder.ofKeyframe(
        View.ROTATION,
        Keyframe.ofFloat(0f, 0f),
        Keyframe.ofFloat(0.2f, 5f),
        Keyframe.ofFloat(0.4f, -5f),
        Keyframe.ofFloat(0.6f, 3f),
        Keyframe.ofFloat(0.8f, -3f),
        Keyframe.ofFloat(1f, 0f)
    )

    val scaleX = PropertyValuesHolder.ofKeyframe(
        View.SCALE_X,
        Keyframe.ofFloat(0f, 1f),
        Keyframe.ofFloat(0.2f, 1.02f),
        Keyframe.ofFloat(0.4f, 0.98f),
        Keyframe.ofFloat(0.6f, 1.01f),
        Keyframe.ofFloat(0.8f, 0.99f),
        Keyframe.ofFloat(1f, 1f)
    )

    val scaleY = PropertyValuesHolder.ofKeyframe(
        View.SCALE_Y,
        Keyframe.ofFloat(0f, 1f),
        Keyframe.ofFloat(0.2f, 1.02f),
        Keyframe.ofFloat(0.4f, 0.98f),
        Keyframe.ofFloat(0.6f, 1.01f),
        Keyframe.ofFloat(0.8f, 0.99f),
        Keyframe.ofFloat(1f, 1f)
    )

    val animator = ObjectAnimator.ofPropertyValuesHolder(view, rotation, scaleX, scaleY).apply {
        duration = 8000L
        interpolator = LinearInterpolator()
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.RESTART
    }

    animator.start()
}