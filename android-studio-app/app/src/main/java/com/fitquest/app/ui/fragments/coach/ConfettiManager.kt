package com.fitquest.app.ui.fragments.coach

import android.graphics.Color
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class ConfettiManager(
    private val konfettiView: KonfettiView
) {
    fun showMilestone() {
        konfettiView.start(
            Party(
                speed = 10f,
                maxSpeed = 50f,
                damping = 0.88f,
                spread = 360,
                emitter = Emitter(duration = 900, TimeUnit.MILLISECONDS).max(120),
                colors = listOf(
                    Color.parseColor("#FFD54F"), // Yellow
                    Color.parseColor("#FF6E40"), // Orange Accent
                    Color.parseColor("#4DB6AC"), // Mint
                    Color.parseColor("#9575CD")  // Lavender
                ),
                position = Position.Relative(0.5, 0.9),
                size = listOf(Size.SMALL, Size.MEDIUM, Size.LARGE)
            )
        )
    }

    fun showMilestone10() {
        konfettiView.start(
            Party(
                speed = 20f,
                maxSpeed = 42f,
                damping = 0.9f,
                spread = 360,
                emitter = Emitter(duration = 2200, TimeUnit.MILLISECONDS).perSecond(250),
                colors = listOf(
                    Color.parseColor("#FF8A65"),
                    Color.parseColor("#4DB6AC"),
                    Color.parseColor("#FFD54F"),
                    Color.parseColor("#9575CD"),
                    Color.parseColor("#81D4FA"),
                    Color.parseColor("#FF5252")
                ),
                position = Position.Relative(0.5, 0.4)
            )
        )
    }

    fun showGoalAchieved() {
        konfettiView.start(
            Party(
                speed = 10f,
                maxSpeed = 28f,
                damping = 0.88f,
                spread = 300,
                emitter = Emitter(duration = 1200, TimeUnit.MILLISECONDS).max(120),
                colors = listOf(
                    Color.parseColor("#FF8A65"), // Orange
                    Color.parseColor("#4DB6AC"), // Mint
                    Color.parseColor("#FFD54F"), // Yellow
                    Color.parseColor("#81D4FA")  // Sky Blue
                ),
                position = Position.Relative(0.5, 0.9)
            )
        )
    }
}