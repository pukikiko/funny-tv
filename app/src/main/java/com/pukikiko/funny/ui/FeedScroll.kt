package com.pukikiko.funny.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Past this much of the screen, letting go commits the swipe. */
private const val COMMIT_FRACTION = 0.18f

/** A flick this fast commits regardless of how far it travelled (px/s). */
private const val FLING_VELOCITY = 900f

private const val SETTLE_MS = 220

/**
 * Vertical position of the feed while it is being dragged or animated. The
 * video tracks the finger, then settles to the neighbour or springs back.
 */
class FeedScroll(
    private val feed: FeedController,
    private val scope: CoroutineScope,
    private val heightPx: Float
) {
    val offset = Animatable(0f)

    /** True while a committed swipe is animating; input is ignored until it lands. */
    var settling by mutableStateOf(false)
        private set

    fun onDrag(delta: Float) {
        if (settling) return
        scope.launch {
            // Only allow dragging downwards when there is something to go back to.
            val lowerBound = if (feed.canGoBack) heightPx else 0f
            offset.snapTo((offset.value + delta).coerceIn(-heightPx, lowerBound))
        }
    }

    fun onDragEnd(velocity: Float) {
        if (settling) return
        val travelled = offset.value
        val flung = abs(velocity) > FLING_VELOCITY
        val farEnough = abs(travelled) > heightPx * COMMIT_FRACTION

        // A fling wins over distance, so a short fast flick still counts.
        val direction = when {
            flung -> if (velocity < 0) -1 else 1
            farEnough -> if (travelled < 0) -1 else 1
            else -> 0
        }

        when {
            direction < 0 -> next()
            direction > 0 && feed.canGoBack -> previous()
            else -> springBack()
        }
    }

    fun next() {
        if (settling) return
        scope.launch {
            settling = true
            try {
                if (feed.ensureNext()) {
                    offset.animateTo(-heightPx, tween(SETTLE_MS, easing = FastOutSlowInEasing))
                    feed.advance()
                    offset.snapTo(0f)
                } else {
                    offset.animateTo(0f, settleSpring())
                }
            } finally {
                settling = false
            }
        }
    }

    fun previous() {
        if (settling || !feed.canGoBack) return
        scope.launch {
            settling = true
            try {
                offset.animateTo(heightPx, tween(SETTLE_MS, easing = FastOutSlowInEasing))
                feed.goBack()
                offset.snapTo(0f)
            } finally {
                settling = false
            }
        }
    }

    private fun springBack() {
        scope.launch { offset.animateTo(0f, settleSpring()) }
    }

    private fun settleSpring() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
