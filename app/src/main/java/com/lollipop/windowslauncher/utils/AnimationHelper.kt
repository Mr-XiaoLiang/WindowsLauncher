package com.lollipop.windowslauncher.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import kotlin.math.abs

/**
 * @author lollipop
 * @date 1/30/21 22:28
 * 动画辅助类
 */
class AnimationHelper(
    private val duration: Long = 300L,
    private val onUpdate: (View, Float) -> Unit):
    ValueAnimator.AnimatorUpdateListener,
    Animator.AnimatorListener {

    companion object {
        const val PROGRESS_MAX = 1F
        const val PROGRESS_MIN = 0F

        const val OPEN_THRESHOLD = 0.999F
        const val CLOSE_THRESHOLD = 0.001F
    }

    private val animator: ValueAnimator by lazy {
        ValueAnimator().apply {
            addUpdateListener(this@AnimationHelper)
            addListener(this@AnimationHelper)
        }
    }

    private var target: View? = null
    private var progress: Float = PROGRESS_MIN

    private var onStartCallback: ((View, Float) -> Unit)? = null

    private var onEndCallback: ((View, Float) -> Unit)? = null

    val isOpen: Boolean
        get() {
            return progress >= OPEN_THRESHOLD
        }

    val isClose: Boolean
        get() {
            return progress <= CLOSE_THRESHOLD
        }

    fun bind(view: View?) {
        this.target = view
    }

    fun open(isAnimation: Boolean = true) {
        if (isAnimation) {
            doAnimation(true)
        } else {
            onProgressChange(PROGRESS_MAX)
            onAnimationEnd(animator)
        }
    }

    fun close(isAnimation: Boolean = true) {
        if (isAnimation) {
            doAnimation(false)
        } else {
            onProgressChange(PROGRESS_MIN)
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

    private fun doAnimation(isOpen: Boolean) {
        animator.cancel()
        val end = if (isOpen) {
            PROGRESS_MAX
        } else {
            PROGRESS_MIN
        }
        val d = (abs(progress - end) * duration).toLong()
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
            val callback = onStartCallback?:return
            target?.let {
                callback.invoke(it, progress)
            }
        }
    }

    override fun onAnimationEnd(animation: Animator?) {
        if (animation == animator) {
            val callback = onEndCallback?:return
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