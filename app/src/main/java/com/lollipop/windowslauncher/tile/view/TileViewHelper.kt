package com.lollipop.windowslauncher.tile.view

import android.animation.ValueAnimator
import android.graphics.drawable.Animatable
import android.view.View
import androidx.core.view.ViewCompat
import com.lollipop.windowslauncher.tile.Tile

/**
 * @author lollipop
 * @date 2/15/21 17:11
 * 磁块的View辅助类
 */
class TileViewHelper(private val tileView: TileView) : TileView {

    companion object {
        const val ANIMATION_DURATION_SHORT = 150L
        const val ANIMATION_DURATION_LONG = 300L
    }

    private val tileAnimatorList = ArrayList<Animatable>()

    private val moveAnimation: ValueAnimator by lazy {
        ValueAnimator()
    }

    private var myTile: Tile? = null

    override fun onResume() {
        tileAnimatorList.forEach {
            it.start()
        }
    }

    override fun onPause() {
        tileAnimatorList.forEach {
            it.stop()
        }
    }

    override fun onBind(tile: Tile) {
        myTile = tile
    }

    override fun moveTo(x: Int, y: Int, delay: Long) {
        TODO("Not yet implemented")
    }

    fun notifyTileChange() {
        myTile?.let {
            tileView.bind(it)
        }
    }

    fun onAttached() {

    }

    fun onDetached() {

    }

    class AnimationTask(
        private val target: View,
        val atTime: Long = -1L,
        val duration: Long = ANIMATION_DURATION_LONG
    ) {

        private val attributeList = ArrayList<Attribute<*>>()

        fun moveX(start: Int = target.left, end: Int): AnimationTask {
            attributeList.add(IntAttribute(start, end, {
                ViewCompat.offsetLeftAndRight(target, (it - target.x).toInt())
            }, { _, offset ->
                ViewCompat.offsetLeftAndRight(target, offset)
            }))
            return this
        }

        fun moveY(start: Int = target.top, end: Int): AnimationTask {
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

        fun before() {
            attributeList.forEach {
                it.before()
            }
        }

        fun execute(progress: Float) {
            attributeList.forEach {
                it.execute(progress)
            }
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
    ): Attribute<Int>(start, to) {

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
    ): Attribute<Float>(start, to) {
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