package com.lollipop.windowslauncher.tile.view

import android.graphics.Rect
import android.view.View
import androidx.annotation.FloatRange
import androidx.core.view.ViewCompat
import com.lollipop.windowslauncher.tile.TileSize
import kotlin.math.max

/**
 * @author lollipop
 * @date 3/1/21 22:02
 * 磁块变化的辅助工具
 */
class TileAnimationHelper {

//    private val animationStep =

    private class Step {
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
                            val offsetX =  left - view.left
                            val offsetY =  top - view.top
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

        fun snapshotWith(
            from: TileLayoutHelper.Snapshot,
            to: TileLayoutHelper.Snapshot,
            viewProvider: (Int) -> View,
            sizeProvider: (x: Int, y: Int, size: TileSize) -> Rect
        ) {
            for (i in 0 until max(from.size, to.size)) {
                when {
                    from.size <= i -> {
                        val toLocal = sizeProvider(to.readX(i), to.readY(i), to.readSize(i))
                        addBlock(viewProvider(i), toLocal, toLocal, AnimationType.AlphaIn)
                    }
                    to.size <= i -> {
                        val fromLocal = sizeProvider(from.readX(i), from.readY(i), from.readSize(i))
                        addBlock(viewProvider(i), fromLocal, fromLocal, AnimationType.AlphaOut)
                    }
                    else -> {
                        val fromLocal = sizeProvider(from.readX(i), from.readY(i), from.readSize(i))
                        val toLocal = sizeProvider(to.readX(i), to.readY(i), to.readSize(i))
                        addBlock(viewProvider(i), fromLocal, toLocal, AnimationType.Move)
                    }
                }
            }
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