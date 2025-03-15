package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.watabou.noosa.Game
import com.watabou.utils.GameMath
import kotlin.math.sign

class AnimationState(startingState: Boolean) {
    var state: Boolean = startingState
    var progress: Float = targetForState(startingState)
    var easingUp: ((Float) -> Float)? = null
    var easingDown: ((Float) -> Float)? = null

    inline fun <T> animate(
        targetState: Boolean,
        durationSeconds: Float,
        crossinline animation: (progress: Float) -> T
    ): T {
        val target = targetForState(targetState)

        var easing: ((Float) -> Float)? = null
        var progress = this.progress
        if (progress != target) {
            val step = (1f / durationSeconds) * Game.elapsed * sign(target - progress)

            easing = if (step > 0) easingUp else easingDown

            progress = GameMath.gate(0f, progress + step, 1f)
        }

        if (progress == target) {
            this.state = targetState
        }

        this.progress = progress

        return animation(easing?.let { it(progress) } ?: progress)
    }

    fun done(targetState: Boolean): Boolean {
        return progress == targetForState(targetState)
    }

    @Suppress("NOTHING_TO_INLINE")
    @PublishedApi
    internal inline fun targetForState(targetState: Boolean): Float {
        return if (targetState) 1f else 0f
    }
}

inline fun <reified T : Any> Ui.useAnimation(
    tracker: Any,
    state: Boolean,
    durationSeconds: Float,
    crossinline animation: (progress: Float) -> T
): T {
    return use {
        val hook = get<AnimationState>(tracker).getOrInitWith { AnimationState(state) }

        hook.animate(state, durationSeconds, animation)
    }
}

class LoopingState {
    var progress: Float = 0f

    var repeats: Int = 0

    fun active(): Boolean {
        return progress > 0f
    }

    fun paused(): Boolean {
        return progress < 0f
    }

    inline fun <T> animate(
        running: Boolean,
        durationSeconds: Float,
        pauseInSeconds: Float,
        crossinline animation: (progress: Float) -> T
    ): T {
        val reset = 1f + pauseInSeconds / durationSeconds
        while (progress > 1f) {
            repeats += 1
            progress -= reset
            if (!running) {
                progress = 0f
            }
        }

        val step = (1f / durationSeconds) * Game.elapsed
        // finish the loop if it's not running
        if (running || progress > 0f) {
            progress += step
        }

        return animation(GameMath.gate(0f, progress, 1f))
    }
}

inline fun <reified T : Any> Ui.useLooping(
    tracker: Any,
    running: Boolean,
    durationSeconds: Float,
    pauseInSeconds: Float = 0f,
    crossinline animation: (progress: Float) -> T
): T {
    return use {
        val state by get<LoopingState>(tracker).also { it.getOrInit(LoopingState()) }
        state.animate(running, durationSeconds, pauseInSeconds, animation)
    }
}


