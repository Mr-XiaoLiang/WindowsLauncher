package com.lollipop.windowslauncher.utils

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import kotlin.math.abs

/**
 * @author lollipop
 * @date 1/30/21 22:28
 * 动画辅助类
 */
class AnimationHelper(
    var duration: Long = 300L,
    private val onUpdate: (View, Float) -> Unit
) :
    ValueAnimator.AnimatorUpdateListener,
    Animator.AnimatorListener {

    companion object {
        const val PROGRESS_MAX = 1F
        const val PROGRESS_MIN = 0F

        const val THRESHOLD = 0.001F
    }

    private val animator: ValueAnimator by lazy {
        ValueAnimator().apply {
            addUpdateListener(this@AnimationHelper)
            addListener(this@AnimationHelper)
            repeatCount
        }
    }

    private var target: View? = null
    private var progress: Float = PROGRESS_MIN

    private var onStartCallback: ((View, Float) -> Unit)? = null

    private var onEndCallback: ((View, Float) -> Unit)? = null

    private var startProgress = PROGRESS_MIN

    private var endProgress = PROGRESS_MAX

    fun setInterpolator(run: TimeInterpolator) {
        animator.interpolator = run
    }

    fun reset(progress: Float) {
        this.progress = progress
    }

    fun repeatCount(count: Int) {
        this.animator.repeatCount = count
    }

    fun repeatInfinite(isInfinite: Boolean) {
        repeatCount(if (isInfinite) {ValueAnimator.INFINITE} else { 0 })
    }

    fun progressIs(float: Float): Boolean {
        return abs(float - progress) <= THRESHOLD
    }

    fun bind(view: View?) {
        this.target = view
    }

    fun open(isAnimation: Boolean = true) {
        run(isAnimation, PROGRESS_MIN, PROGRESS_MAX)
    }

    fun close(isAnimation: Boolean = true) {
        run(isAnimation, PROGRESS_MAX, PROGRESS_MIN)
    }

    fun run(
        isAnimation: Boolean = true,
        startProgress: Float = PROGRESS_MAX,
        endProgress: Float = PROGRESS_MIN,
        delay: Long = 0
    ) {
        this.startProgress = startProgress
        this.endProgress = endProgress
        if (isAnimation) {
            doAnimation(delay)
        } else {
            onProgressChange(endProgress)
            onAnimationEnd(animator)
        }
    }

    fun onStart(callback: (View, Float) -> Unit) {
        this.onStartCallback = callback
    }

    fun onEnd(callback: (View, Float) -> Unit) {
        this.onEndCallback = callback
    }

    fun destroy() {
        target = null
        animator.cancel()
    }

    private fun doAnimation(delay: Long) {
        animator.cancel()
        animator.startDelay = delay
        val start = startProgress
        val end = endProgress
        val d = (abs(progress - end) / abs(start - end) * duration).toLong()
        animator.setFloatValues(progress, end)
        animator.duration = d
        animator.start()
    }

    private fun onProgressChange(progress: Float) {
        this.progress = progress
        target?.let {
            onUpdate.invoke(it, progress)
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        if (animation == animator) {
            onProgressChange(animation.animatedValue as Float)
        }
    }

    override fun onAnimationStart(animation: Animator?) {
        if (animation == animator) {
            val callback = onStartCallback ?: return
            target?.let {
                callback.invoke(it, progress)
            }
        }
    }

    override fun onAnimationEnd(animation: Animator?) {
        if (animation == animator) {
            val callback = onEndCallback ?: return
            target?.let {
                callback.invoke(it, progress)
            }
        }
    }

    override fun onAnimationCancel(animation: Animator?) {
    }

    override fun onAnimationRepeat(animation: Animator?) {
    }


}