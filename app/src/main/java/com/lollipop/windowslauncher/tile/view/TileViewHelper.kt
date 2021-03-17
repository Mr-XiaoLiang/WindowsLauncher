package com.lollipop.windowslauncher.tile.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.view.View
import androidx.core.view.ViewCompat
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.utils.LSettings
import com.lollipop.windowslauncher.utils.dp2px
import com.lollipop.windowslauncher.utils.onUI
import com.lollipop.windowslauncher.utils.task

/**
 * @author lollipop
 * @date 2/15/21 17:11
 * 磁块的View辅助类
 */
class TileViewHelper(private val tileView: TileView<*>) {

    companion object {
        const val ANIMATION_DURATION_SHORT = 150L
        const val ANIMATION_DURATION_LONG = 300L
    }

    private val tileAnimatorList = ArrayList<Animatable>()

    private val moveAnimation by lazy {
        AnimationTask(tileView, ANIMATION_DURATION_SHORT)
    }

    var myTile: Tile? = null
        private set

    fun onResume() {
        tileAnimatorList.forEach {
            it.start()
        }
    }

    fun onPause() {
        tileAnimatorList.forEach {
            it.stop()
        }
    }

    fun onBind(tile: Tile) {
        myTile = tile
    }

    fun moveTo(x: Int, y: Int, delay: Long, duration: Long = ANIMATION_DURATION_SHORT) {
        moveAnimation.reset()
            .duration(duration)
            .moveX(end = x)
            .moveY(end = y)
            .onEnd { tileView.callLayoutTile() }
            .delay(delay)
    }

    fun resize(newSize: Rect, delay: Long, duration: Long = ANIMATION_DURATION_SHORT) {
        val scaleX = newSize.width() * 1F / tileView.width
        val scaleY = newSize.height() * 1F / tileView.height
        val translationX = (newSize.width() - tileView.width) * 0.5F
        val translationY = (newSize.height() - tileView.height) * 0.5F
        val oldTranslationX = tileView.translationX
        val oldTranslationY = tileView.translationY
        moveAnimation.reset()
            .duration(duration)
            .scaleX(1F, scaleX)
            .scaleY(1F, scaleY)
            .translationX(oldTranslationX, oldTranslationX + translationX)
            .translationY(oldTranslationY, oldTranslationY + translationY)
            .onEnd { tileView.callLayoutTile() }
            .delay(delay)
    }

    fun float(delay: Long, duration: Long = ANIMATION_DURATION_SHORT) {
        val space = LSettings.getCreviceMode(tileView.context).dp.dp2px()
        val scale = space / tileView.width + 1
        moveAnimation.reset()
            .duration(duration)
            .scaleX(end = scale)
            .scaleY(end = scale)
            .translationZ(end = 10F.dp2px())
            .delay(delay)
    }

    fun sink(delay: Long, duration: Long = ANIMATION_DURATION_SHORT) {
        moveAnimation.reset()
            .duration(duration)
            .scaleX(end = 1F)
            .scaleY(end = 1F)
            .translationZ(end = 0F)
            .delay(delay)
    }

    fun alpha(alpha: Float, delay: Long, duration: Long = ANIMATION_DURATION_SHORT) {
        moveAnimation.reset()
            .duration(duration)
            .alpha(end = alpha)
            .onEnd { tileView.callLayoutTile() }
            .delay(delay)
    }

    fun notifyTileChange() {
        myTile?.let {
            onUI {
                tileView.bind(it)
            }
        }
    }

    fun onAttached() {

    }

    fun onDetached() {
        moveAnimation.cancel()
    }

    class AnimationTask(
        private val target: View,
        private var duration: Long = ANIMATION_DURATION_LONG
    ) : ValueAnimator.AnimatorUpdateListener,
        Animator.AnimatorListener {

        private val attributeList = ArrayList<Attribute<*>>()

        private var animatorInit = false

        private var onEndCallback: (() -> Unit)? = null

        private val animator by lazy {
            animatorInit = true
            ValueAnimator().apply {
                addUpdateListener(this@AnimationTask)
                addListener(this@AnimationTask)
            }
        }

        private val runner = task {
            start()
        }

        fun moveX(start: Int = target.x.toInt(), end: Int): AnimationTask {
            attributeList.add(IntAttribute(start, end, {
                ViewCompat.offsetLeftAndRight(target, (it - target.x).toInt())
            }, { _, offset ->
                ViewCompat.offsetLeftAndRight(target, offset)
            }))
            return this
        }

        fun moveY(start: Int = target.y.toInt(), end: Int): AnimationTask {
            attributeList.add(IntAttribute(start, end, {
                ViewCompat.offsetTopAndBottom(target, (it - target.y).toInt())
            }, { _, offset ->
                ViewCompat.offsetTopAndBottom(target, offset)
            }))
            return this
        }

        fun moveZ(start: Float = target.z, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.z = start
            }, { now, _ ->
                target.z = now
            }))
            return this
        }

        fun scaleX(start: Float = target.scaleX, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.scaleX = start
            }, { now, _ ->
                target.scaleX = now
            }))
            return this
        }

        fun scaleY(start: Float = target.scaleY, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.scaleY = start
            }, { now, _ ->
                target.scaleY = now
            }))
            return this
        }

        fun alpha(start: Float = target.alpha, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.alpha = start
            }, { now, _ ->
                target.alpha = now
            }))
            return this
        }


        fun translationX(start: Float = target.translationX, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.translationX = start
            }, { now, _ ->
                target.translationX = now
            }))
            return this
        }

        fun translationY(start: Float = target.translationY, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.translationY = start
            }, { now, _ ->
                target.translationY = now
            }))
            return this
        }

        fun translationZ(start: Float = target.translationZ, end: Float): AnimationTask {
            attributeList.add(FloatAttribute(start, end, {
                target.translationZ = start
            }, { now, _ ->
                target.translationZ = now
            }))
            return this
        }

        fun duration(d: Long): AnimationTask {
            this.duration = d
            return this
        }

        private fun before() {
            attributeList.forEach {
                it.before()
            }
        }

        private fun execute(progress: Float) {
            attributeList.forEach {
                it.execute(progress)
            }
        }

        fun reset(): AnimationTask {
            cancel()
            attributeList.clear()
            return this
        }

        private fun start() {
            animator.cancel()
            if (duration < 0L) {
                onAnimationStart(animator)
                execute(1F)
                onAnimationEnd(animator)
                return
            }
            animator.duration = duration
            animator.setFloatValues(0F, 1F)
            animator.start()
        }

        fun onEnd(callback: () -> Unit): AnimationTask {
            this.onEndCallback = callback
            return this
        }

        fun delay(delay: Long = 0) {
            runner.cancel()
            if (delay < 0L) {
                runner.run()
                return
            }
            runner.delay(delay)
        }

        fun cancel(): AnimationTask {
            runner.cancel()
            if (animatorInit) {
                animator.cancel()
            }
            return this
        }

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            if (animation == animator) {
                val progress = animation.animatedValue as Float
                execute(progress)
            }
        }

        override fun onAnimationStart(animation: Animator?) {
            if (animation == animator) {
                before()
            }
        }

        override fun onAnimationEnd(animation: Animator?) {
            onEndCallback?.invoke()
            onEndCallback = null
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationRepeat(animation: Animator?) {
        }

    }

    abstract class Attribute<T : Any>(
        private val start: T,
        private val to: T
    ) {

        fun before() {
            before(start)
        }

        abstract fun before(start: T)

        fun execute(progress: Float) {
            run(progress, start, to)
        }

        abstract fun run(progress: Float, start: T, to: T)

    }

    class IntAttribute(
        start: Int, to: Int,
        private val prepare: (now: Int) -> Unit,
        private val callback: (now: Int, offset: Int) -> Unit
    ) : Attribute<Int>(start, to) {

        private var now = start

        override fun run(progress: Float, start: Int, to: Int) {
            val newValue = ((to - start) * progress + start).toInt()
            val offset = newValue - now
            now = newValue
            callback(now, offset)
        }

        override fun before(start: Int) {
            prepare(start)
        }

    }

    class FloatAttribute(
        start: Float, to: Float,
        private val prepare: (now: Float) -> Unit,
        private val callback: (now: Float, offset: Float) -> Unit
    ) : Attribute<Float>(start, to) {
        private var now = start
        override fun run(progress: Float, start: Float, to: Float) {
            val newValue = (to - start) * progress + start
            val offset = newValue - now
            now = newValue
            callback(now, offset)
        }

        override fun before(start: Float) {
            prepare(start)
        }

    }

}