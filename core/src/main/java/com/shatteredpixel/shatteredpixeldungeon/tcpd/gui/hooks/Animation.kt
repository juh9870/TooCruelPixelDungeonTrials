package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.watabou.noosa.Game
import com.watabou.utils.GameMath
import kotlin.math.sign

class AnimationState(var state: Boolean, var progress: Float)

inline fun <reified T : Any> Ui.useAnimation(
    tracker: Any,
    state: Boolean,
    durationSeconds: Float,
    crossinline animation: (progress: Float) -> T
): Pair<T, Boolean> {
    return use {
        val target = if (state) 1f else 0f

        val hook = get<AnimationState>(tracker).getOrInitWith { AnimationState(state, target) }

        val progress = hook.progress
        if (progress == target) {
            hook.state = state
            return@use Pair(animation(progress), state)
        }

        val step = (1f / durationSeconds) * Game.elapsed * sign(target - progress)

        hook.progress = GameMath.gate(0f, progress + step, 1f)

        Pair(animation(hook.progress), hook.state)
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


