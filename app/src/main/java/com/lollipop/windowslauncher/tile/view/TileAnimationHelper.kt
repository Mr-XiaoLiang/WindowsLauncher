package com.lollipop.windowslauncher.tile.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewManager
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import com.lollipop.windowslauncher.tile.TileSize
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

/**
 * @author lollipop
 * @date 3/1/21 22:02
 * 磁块变化的辅助工具
 */
class TileAnimationHelper(
    private val tileLayout: TileLayout,
    private val sizeProvider: (x: Int, y: Int, size: TileSize) -> Rect
) : ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private val valueAnimator by lazy {
        ValueAnimator().apply {
            addUpdateListener(this@TileAnimationHelper)
            addListener(this@TileAnimationHelper)
        }
    }

    private val animationStep = LinkedList<Step>()

    private var currentStep: Step? = null

    fun notifyTileMove(
        duration: Long = 300L,
        from: TileLayoutHelper.Snapshot,
        to: TileLayoutHelper.Snapshot,
    ) {
        notifyTileChange(duration, from, to, -1, false)
    }

    fun notifyTileInstall(
        duration: Long = 300L,
        from: TileLayoutHelper.Snapshot,
        to: TileLayoutHelper.Snapshot,
        installIndex: Int
    ) {
        notifyTileChange(duration, from, to, installIndex, true)
    }

    fun notifyTileRemove(
        duration: Long = 300L,
        from: TileLayoutHelper.Snapshot,
        to: TileLayoutHelper.Snapshot,
        removeIndex: Int
    ) {
        notifyTileChange(duration, from, to, removeIndex, false)
    }

    private fun notifyTileChange(
        duration: Long = 300L,
        from: TileLayoutHelper.Snapshot,
        to: TileLayoutHelper.Snapshot,
        changeIndex: Int,
        isAdd: Boolean
    ) {
        val childCount = tileLayout.childCount
        val step = Step(duration)
        for (i in 0 until childCount) {
            if (i == changeIndex) {
                if (isAdd) {
                    step.install(tileLayout.getChildAt(i))
                } else {
                    step.remove(tileLayout.getChildAt(i))
                }
            } else if (isChanged(from, to, i)) {
                step.move(
                    tileLayout.getChildAt(i),
                    sizeProvider(from.readX(i), from.readY(i), from.readSize(i)),
                    sizeProvider(to.readX(i), to.readY(i), to.readSize(i))
                )
            }
        }
        animationStep.addLast(step)
        start()
    }

    private fun isChanged(
        from: TileLayoutHelper.Snapshot,
        to: TileLayoutHelper.Snapshot,
        index: Int
    ): Boolean {
        return from.readX(index) != to.readX(index) ||
                from.readY(index) != to.readY(index) ||
                from.readSize(index) != to.readSize(index)
    }

    fun start() {
        if (valueAnimator.isRunning) {
            return
        }
        if (animationStep.isEmpty()) {
            return
        }
        val newStop = animationStep.removeFirst()
        currentStep = newStop
        valueAnimator.duration = newStop.duration
        valueAnimator.setFloatValues(0F, 1F)
        valueAnimator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator?) {
        if (animation == valueAnimator) {
            currentStep?.doAnimation(animation.animatedValue as Float)
        }
    }

    override fun onAnimationStart(animation: Animator?) {
    }

    override fun onAnimationEnd(animation: Animator?) {
        if (animation == valueAnimator) {
            currentStep?.onAnimationEnd()
            start()
        }
    }

    override fun onAnimationCancel(animation: Animator?) {
    }

    override fun onAnimationRepeat(animation: Animator?) {
    }

    class Step(val duration: Long) {
        private val blockList = ArrayList<Block>()

        fun doAnimation(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
            blockList.forEach {
                when (it.type) {
                    AnimationType.AlphaIn -> {
                        it.target.alpha = progress
                    }
                    AnimationType.AlphaOut -> {
                        it.target.alpha = (1 - progress)
                    }
                    AnimationType.Move -> {
                        val view = it.target
                        val from = it.from
                        val to = it.to
                        // 如果位置发生了变化
                        if (from.left != to.left || from.top != to.top) {
                            val left = (to.left - from.left) * progress + from.left
                            val top = (to.top - from.top) * progress + from.top
                            val offsetX = left - view.left
                            val offsetY = top - view.top
                            ViewCompat.offsetLeftAndRight(view, offsetX.toInt())
                            ViewCompat.offsetTopAndBottom(view, offsetY.toInt())
                        }

                        // 如果尺寸发生了变化
                        if (from.width() != to.width() || from.height() != to.height()) {
                            // 计算横纵的缩放比例
                            val scaleX = to.width() * 1F / from.width() * progress
                            val scaleY = to.height() * 1F / from.height() * progress
                            // 设置缩放比例
                            view.scaleX = scaleX
                            view.scaleY = scaleY
                            // 因为是中心缩放，所以缩放过程中左边会溢出或者缩短，需要做等量的偏移来保证左上角对齐
                            view.translationX = (scaleX - 1) * from.width() / 2
                            view.translationY = (scaleY - 1) * from.height() / 2
                        }
                    }
                }
            }
        }

        fun onAnimationEnd() {
            // 动画结束的时候，清空临时的缩放设置，然后让layout去发起重新布局
            blockList.forEach {
                it.target.apply {
                    scaleX = 1F
                    scaleY = 1F
                    translationX = 0F
                    translationY = 0F
                }
            }
        }

        fun remove(view: View) {
            val bounds = view.getBounds()
            addBlock(view, bounds, bounds, AnimationType.AlphaOut)
        }

        fun install(view: View) {
            val bounds = view.getBounds()
            addBlock(view, bounds, bounds, AnimationType.AlphaIn)
        }

        fun move(
            view: View,
            from: Rect,
            to: Rect
        ) {
            addBlock(view, from, to, AnimationType.Move)
        }

        private fun View.getBounds(): Rect {
            return Rect(this.left, this.top, this.right, this.bottom)
        }

        private fun addBlock(target: View, from: Rect, to: Rect, type: AnimationType) {
            blockList.add(Block(target, from, to, type))
        }

    }

    private class Block(
        val target: View,
        val from: Rect,
        val to: Rect,
        val type: AnimationType
    )

    private enum class AnimationType {
        AlphaIn, AlphaOut, Move
    }
}