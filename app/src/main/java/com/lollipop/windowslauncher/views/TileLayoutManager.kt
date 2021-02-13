package com.lollipop.windowslauncher.views

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.tile.Orientation
import com.lollipop.windowslauncher.tile.TileLayoutHelper
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.biggerThen

/**
 * @author lollipop
 * @date 1/31/21 21:09
 *
 * 砖块的布局管理器
 * 由于考虑到桌面的场景，因此暂未实现Item的复用
 */
class TileLayoutManager(
    private var spanCount: Int,
    tileSizeProvider: ((Int) -> TileSize),
    insetProvider: InsetsProvider
) : RecyclerView.LayoutManager() {

    /**
     * 方向
     */
    private var orientation = Orientation.Vertical

    /**
     * 窗口缩进
     * 仅用于布局的开始及结束
     */
    private val layoutInsets = Rect()

    /**
     * 滑动距离的计数器
     */
    private var scrollOffset = 0

    /**
     * 最大的可滑动距离
     */
    private val maxScrollOffset: Int
        get() {
            return (layoutHelper.getContentLength(tileSideLength)
                    - boundsLength
                    + startOffsetByOrientation(orientation)
                    + endOffsetByOrientation(orientation)).biggerThen(0)
        }

    /**
     * 边界长度
     * 不同的布局模式下的参照物是不同的
     */
    private val boundsLength: Int
        get() {
            return if (orientation.isVertical) {
                height
            } else {
                width
            }
        }

    /**
     * 磁块的大小
     */
    private val tileSideLength: Int
        get() {
            return if (orientation.isVertical) {
                width / spanCount
            } else {
                height / spanCount
            }
        }

    /**
     * 布局边界的位置
     * 相对于滑动而言
     */
    private val boundsStart: Int
        get() {
            return scrollOffset
        }

    /**
     * 滑动布局的边界
     * 相对于滑动而言
     */
    private val boundsEnd: Int
        get() {
            return boundsStart + boundsLength
        }

    /**
     * 布局辅助器
     * 磁块的位置计算及测量在这里进行
     */
    private val layoutHelper = TileLayoutHelper(
        ::spanCount,
        ::getItemCount,
        ::orientation,
        tileSizeProvider,
        insetProvider,
    )

    /**
     * 根据方向获取开始方向的初始偏移量
     */
    private fun startOffsetByOrientation(o: Orientation): Int {
        if (o != orientation) {
            return 0
        }
        return if (o.isVertical) {
            layoutInsets.top
        } else {
            layoutInsets.left
        }
    }

    /**
     * 根据方向获取结束方向的初始偏移量
     */
    private fun endOffsetByOrientation(o: Orientation): Int {
        if (o != orientation) {
            return 0
        }
        return if (o.isVertical) {
            layoutInsets.bottom
        } else {
            layoutInsets.right
        }
    }

    /**
     * 重新设置列数
     */
    fun setSpanCount(value: Int) {
        this.spanCount = value
        requestLayout()
    }

    /**
     * 重新设置方向
     */
    fun setOrientation(value: Orientation) {
        if (this.orientation == value) {
            return
        }
        this.orientation = value
        requestLayout()
    }

    fun setStartPadding(left: Int, top: Int, right: Int, bottom: Int) {
        layoutInsets.set(left, top, right, bottom)
    }

    override fun requestLayout() {
        super.requestLayout()
        scrollOffset = 0
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        recycler ?: return
        state ?: return
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }
        if (childCount == 0 && state.isPreLayout) {
            return
        }

        detachAndScrapAttachedViews(recycler)

        layoutBounds()

        layoutChildren(recycler)
    }

    private fun layoutBounds() {
        layoutHelper.relayout()
    }

    /**
     * child的滑动用的是{@View offsetTopAndBottom(Int)}的方法
     * 也就是说，是直接对View做偏移量，那么处理滑动时的复用，
     * 就需要检查View的位置，然后移除它，同时添加新的View
     * 由于RecyclerView本身不记录位置，那么我们添加的时候，应该遵照当前显示的位置
     * 同时，布局后的位置应该是滑动前的（先排版，后滑动）
     */
    private fun layoutChildren(recycler: RecyclerView.Recycler) {
        val tileWidth = tileSideLength
        val verticalOffset = startOffsetByOrientation(Orientation.Vertical)
        val horizontalOffset = startOffsetByOrientation(Orientation.Horizontal)
        for (i in 0 until itemCount) {
            val block = layoutHelper.getBlock(i)
            if (!block.isActive) {
                continue
            }
            val view = recycler.getViewForPosition(i)
            addView(view)
            if (view is TileItemShell) {
                view.setOrientation(orientation)
            }
            view.measure(
                View.MeasureSpec.makeMeasureSpec(
                    block.size.width * tileWidth - block.insetHorizontal,
                    View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                    block.size.height * tileWidth - block.insetVertical,
                    View.MeasureSpec.EXACTLY),
            )
            view.layout(
                block.getLeft(tileWidth) + horizontalOffset,
                block.getTop(tileWidth) + verticalOffset,
                block.getRight(tileWidth) + horizontalOffset,
                block.getBottom(tileWidth) + verticalOffset,
            )
        }
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun canScrollVertically(): Boolean {
        return orientation.isVertical
    }

    override fun canScrollHorizontally(): Boolean {
        return !orientation.isVertical
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        recycler ?: return 0
        val offset = checkScrollOffsetBy(dy)
        offsetChildrenVertical(offset * -1)
        return offset
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        recycler ?: return 0
        val offset = checkScrollOffsetBy(dx)
        offsetChildrenHorizontal(offset * -1)
        return offset
    }

    private fun checkScrollOffsetBy(offset: Int): Int {
        return checkScrollOffset(scrollOffset + offset)
    }

    private fun checkScrollOffset(offset: Int): Int {
        var newOffset = offset
            if (newOffset < 0) {
            newOffset = 0
        } else if (newOffset > maxScrollOffset) {
            newOffset = maxScrollOffset
        }
        val actual = newOffset - scrollOffset
        scrollOffset = newOffset
        return actual
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position >= itemCount || itemCount == 0) {
            return
        }
        val block = layoutHelper.getBlock(position)
        if (orientation.isVertical) {
            offsetChildrenVertical(checkScrollOffset(block.getTop(tileSideLength)) * -1)
        } else {
            offsetChildrenHorizontal(checkScrollOffset(block.getLeft(tileSideLength)) * -1)
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        recyclerView?:return
        startSmoothScroll(LinearSmoothScroller(recyclerView.context).apply {
            targetPosition = position
        })
    }

    fun interface InsetsProvider {
        fun getInsets(
            insets: Rect,
            x: Int,
            y: Int,
            size: TileSize,
            orientation: Orientation
        )
    }

}