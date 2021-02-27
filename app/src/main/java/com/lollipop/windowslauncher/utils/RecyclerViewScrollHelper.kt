package com.lollipop.windowslauncher.utils

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * @author lollipop
 * @date 2/27/21 23:48
 * RecyclerView的滑动辅助类
 */
class RecyclerViewScrollHelper(
    private val recyclerView: RecyclerView
) {

    private val onceScrollListener = OnceScrollListener(::onScrollStateChanged)

    private var isSmooth = true

    private var targetPosition = 0

    fun scrollTo(position: Int, smooth: Boolean = true) {
        targetPosition = position
        isSmooth = smooth
        if (itemIsInScreen() && moveItemToTop(false)) {
            return
        }
        recyclerView.addOnScrollListener(onceScrollListener)
        if (smooth) {
            recyclerView.smoothScrollToPosition(position)
        } else {
            recyclerView.scrollToPosition(position)
            delay(10) {
                moveItemToTop()
            }
        }
    }

    private fun onScrollStateChanged(state: ScrollState): Boolean {
        if (state == ScrollState.DRAGGING) {
            return true
        }
        if (state == ScrollState.IDLE) {
            moveItemToTop()
            return true
        }
        return false
    }

    private fun moveItemToTop(checkItem: Boolean = true): Boolean {
        val position = targetPosition
        if (position < 0 || position >= recyclerView.adapter?.itemCount ?: 0) {
            return false
        }
        val layoutManager = recyclerView.layoutManager ?: return false
        if (checkItem && !itemIsInScreen()) {
            return false
        }
        val itemView: View = layoutManager.findViewByPosition(position) ?: return false

        val orientation: Int = when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.orientation
            }
            is GridLayoutManager -> {
                layoutManager.orientation
            }
            else -> {
                return false
            }
        }
        val top = if (orientation == RecyclerView.VERTICAL) {
            itemView.top
        } else {
            0
        }
        val left = if (orientation == RecyclerView.HORIZONTAL) {
            itemView.left
        } else {
            0
        }
        if (isSmooth) {
            recyclerView.smoothScrollBy(left, top)
        } else {
            recyclerView.scrollBy(left, top)
        }
        return true
    }

    private fun itemIsInScreen(): Boolean {
        val layoutManager = recyclerView.layoutManager ?: return false
        val position = targetPosition
        when (layoutManager) {
            is LinearLayoutManager -> {
                if (layoutManager.findFirstVisibleItemPosition() > position) {
                    return false
                }
                if (layoutManager.findLastVisibleItemPosition() < position) {
                    return false
                }
                layoutManager.findViewByPosition(position) ?: return false
            }
            is GridLayoutManager -> {
                if (layoutManager.findFirstVisibleItemPosition() > position) {
                    return false
                }
                if (layoutManager.findLastVisibleItemPosition() < position) {
                    return false
                }
                layoutManager.findViewByPosition(position) ?: return false
            }
            else -> {
                return false
            }
        }
        return true
    }

    private class OnceScrollListener(
        private val onStatusChange: (ScrollState) -> Boolean
    ) : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            val state = when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> ScrollState.IDLE
                RecyclerView.SCROLL_STATE_DRAGGING -> ScrollState.DRAGGING
                RecyclerView.SCROLL_STATE_SETTLING -> ScrollState.SETTLING
                else -> ScrollState.UNKNOWN
            }
            if (onStatusChange(state)) {
                recyclerView.removeOnScrollListener(this)
            }
        }
    }

    private enum class ScrollState {
        IDLE, DRAGGING, SETTLING, UNKNOWN
    }

}