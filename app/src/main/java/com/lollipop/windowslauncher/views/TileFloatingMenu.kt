package com.lollipop.windowslauncher.views

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.databinding.ItemTileFloatingMenuButtonBinding
import com.lollipop.windowslauncher.databinding.ItemTileFloatingMenuResizeBinding
import com.lollipop.windowslauncher.databinding.MenuTileFloatingBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.tile.Tile
import com.lollipop.windowslauncher.tile.TileSize
import com.lollipop.windowslauncher.utils.*
import kotlin.math.min

/**
 * @author lollipop
 * @date 3/19/21 22:43
 * 瓷砖的悬浮菜单
 */
class TileFloatingMenu private constructor(private val option: Option) {

    companion object {
        fun create(): Builder {
            return Builder()
        }
    }

    /**
     * 根结点
     */
    private val rootGroup: ViewGroup by lazy {
        AnchorLayoutGroup(option.anchor)
    }

    /**
     * 气泡窗口的ViewBinding
     */
    private val popupView: MenuTileFloatingBinding by rootGroup.withThis(true)

    /**
     * 重设尺寸的按钮列表
     */
    private val resizeButtonList: RecyclerView
        get() {
            return popupView.resizeButtonList
        }

    /**
     * 菜单按钮的列表
     */
    private val menuButtonList: RecyclerView
        get() {
            return popupView.menuButtonList
        }

    private fun show() {
        // 都是空的就不显示了
        if (option.resizeList.isEmpty() && option.buttonList.isEmpty()) {
            return
        }
        // 先隐藏
        popupView.popBody.visibleOrInvisible(false)
        // 准备列表
        initListView()
        // 将根结点添加到屏幕
        val dialogGroup = option.anchor.findDialogGroup() ?: return
        dialogGroup.addView(
            rootGroup,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        rootGroup.post {
            doAnimation()
        }
    }

    private fun doAnimation() {
        // TODO
    }

    private fun initListView() {
        resizeButtonList.visibleOrGone(option.resizeList.isNotEmpty()) {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = ResizeButtonAdapter(option.resizeList, ::onResizeClick)
        }
        menuButtonList.visibleOrGone(option.buttonList.isNotEmpty()) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = MenuButtonAdapter(option.buttonList, ::onMenuClick)
        }
    }

    private fun onResizeClick(tileSize: TileSize) {
        option.onResizeClickListener.onResizeClick(tileSize)
    }

    private fun onMenuClick(buttonInfo: ButtonInfo) {
        option.onMenuClickListener.onMenuClick(buttonInfo.id)
    }

    private class ResizeButtonAdapter(
        private val buttonList: Array<TileSize>,
        private val onItemClick: (TileSize) -> Unit
    ): RecyclerView.Adapter<ResizeButtonHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResizeButtonHolder {
            return ResizeButtonHolder.create(parent, ::onHolderClick)
        }

        override fun onBindViewHolder(holder: ResizeButtonHolder, position: Int) {
            holder.bind(buttonList[position])
        }

        override fun getItemCount(): Int {
            return buttonList.size
        }

        private fun onHolderClick(holder: ResizeButtonHolder) {
            onItemClick(buttonList[holder.adapterPosition])
        }

    }

    private class MenuButtonAdapter(
        private val buttonList: Array<ButtonInfo>,
        private val onItemClick: (ButtonInfo) -> Unit
    ): RecyclerView.Adapter<MenuButtonHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuButtonHolder {
            return MenuButtonHolder.create(parent, ::onHolderClick)
        }

        override fun onBindViewHolder(holder: MenuButtonHolder, position: Int) {
            holder.bind(buttonList[position].name)
        }

        override fun getItemCount(): Int {
            return buttonList.size
        }

        private fun onHolderClick(holder: MenuButtonHolder) {
            onItemClick(buttonList[holder.adapterPosition])
        }

    }

    private class ResizeButtonHolder private constructor(
        private val viewBinding: ItemTileFloatingMenuResizeBinding,
        private val onClickListener: (ResizeButtonHolder) -> Unit
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        companion object {
            fun create(
                parent: ViewGroup,
                onClickListener: (ResizeButtonHolder) -> Unit
            ): ResizeButtonHolder {
                return ResizeButtonHolder(parent.bind(), onClickListener)
            }
        }

        private val tilePreviewDrawable = TilePreviewDrawable()

        init {
            itemView.setOnClickListener {
                onClickListener.invoke(this)
            }
            viewBinding.resizePreviewView.setImageDrawable(tilePreviewDrawable)
        }

        fun bind(tileSize: TileSize) {
            tilePreviewDrawable.color = LColor.primary
            tilePreviewDrawable.tileSize = tileSize
            viewBinding.resizeNameView.setText(tileSize.nameId)
        }

    }

    private class MenuButtonHolder private constructor(
        private val viewBinding: ItemTileFloatingMenuButtonBinding,
        private val onClickListener: (MenuButtonHolder) -> Unit
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        companion object {
            fun create(
                parent: ViewGroup,
                onClickListener: (MenuButtonHolder) -> Unit
            ): MenuButtonHolder {
                return MenuButtonHolder(parent.bind(), onClickListener)
            }
        }

        init {
            itemView.setOnClickListener {
                onClickListener.invoke(this)
            }
        }

        fun bind(name: Int) {
            viewBinding.buttonName.setText(name)
        }

    }

    private class TilePreviewDrawable : Drawable() {

        var tileSize: TileSize = TileSize.S
            set(value) {
                field = value
                resetTileBounds()
            }

        var color: Int = Color.BLACK

        private val tileBounds = Rect()

        private val paint = Paint().apply {
            isAntiAlias = true
        }

        override fun draw(canvas: Canvas) {
            paint.color = color.alpha(0.3F)
            canvas.drawRect(bounds, paint)
            paint.color = color
            canvas.drawRect(tileBounds, paint)
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            resetTileBounds()
        }

        private fun resetTileBounds() {
            val height = tileSize.height
            val width = tileSize.width
            val maxHeight = TileSize.XL.height
            val maxWidth = TileSize.XL.width
            tileBounds.set(
                0,
                0,
                bounds.width() * width / maxWidth,
                bounds.height() * height / maxHeight
            )
            tileBounds.offset(bounds.left, bounds.top)
            invalidateSelf()
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSPARENT
        }
    }

    private class AnchorLayoutGroup(
        val anchor: View
    ) : FrameLayout(anchor.context) {

        private val childAnimationStyles = ArrayList<AnimationStyle>()

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            childAnimationStyles.clear()
            getLayoutOffset { isTop, maxHeight, baseLine ->
                val groupHeight = height
                val groupWidth = width
                for (index in 0 until childCount) {
                    getChildAt(index)?.let { child ->
                        val childHeight = child.measuredHeight
                        val childWidth = child.measuredWidth
                        val animationStyle = if (childHeight > maxHeight) {
                            AnimationStyle.Sheet
                        } else {
                            AnimationStyle.Expansion
                        }
                        childAnimationStyles.add(animationStyle)

                        val childTop = when (animationStyle) {
                            AnimationStyle.Expansion -> {
                                if (isTop) {
                                    baseLine - childHeight
                                } else {
                                    baseLine
                                }
                            }
                            AnimationStyle.Sheet -> {
                                groupHeight - childHeight
                            }
                        }
                        val childLeft = (groupWidth - childWidth) / 2

                        child.layout(
                            childLeft,
                            childTop,
                            childLeft + childHeight,
                            childTop + childHeight
                        )
                    }
                }
            }
        }

        private fun getLayoutOffset(
            callback: (isTop: Boolean, maxHeight: Int, baseLine: Int) -> Unit
        ) {
            val anchorLocation = anchor.getLocationInWindow()
            val myLocation = this.getLocationInWindow()

            val topSpace = anchorLocation[1] - myLocation[1]
            val bottomSpace = height - topSpace - anchor.height
            if (topSpace > bottomSpace) {
                callback(true, topSpace, topSpace)
            } else {
                callback(false, bottomSpace, height - bottomSpace)
            }
        }

        private fun View.getLocationInWindow(): IntArray {
            val location = IntArray(2)
            getLocationInWindow(location)
            return location
        }

    }

    class Builder {
        private val resizeList = ArrayList<TileSize>()
        private val buttonList = ArrayList<ButtonInfo>()
        private var menuClickListener: OnMenuClickListener? = null
        private var resizeClickListener: OnResizeClickListener? = null

        fun addResizeType(size: TileSize): Builder {
            if (!resizeList.contains(size)) {
                resizeList.add(size)
            }
            return this
        }

        fun addButton(name: Int, id: Int): Builder {
            // 避免名字和id完全一致的场景（因为没有意义）
            if (buttonList.none { it.id == id && it.name == name }) {
                buttonList.add(ButtonInfo(name, id))
            }
            return this
        }

        fun onClick(listener: OnMenuClickListener): Builder {
            menuClickListener = listener
            return this
        }

        fun onResize(listener: OnResizeClickListener): Builder {
            resizeClickListener = listener
            return this
        }

        fun showIn(anchor: View): TileFloatingMenu {
            return TileFloatingMenu(
                Option(
                    anchor = anchor,
                    resizeList = resizeList.toTypedArray(),
                    buttonList = buttonList.toTypedArray(),
                    onMenuClickListener = menuClickListener ?: OnMenuClickListener {},
                    onResizeClickListener = resizeClickListener ?: OnResizeClickListener {}
                )
            ).apply {
                show()
            }
        }

    }

    /**
     * 参数配置信息
     * 它是不可变的
     */
    private class Option(
        /**
         * 菜单关联的锚点
         */
        val anchor: View,

        /**
         * 重置大小的按钮列表
         */
        val resizeList: Array<TileSize>,

        /**
         * 菜单按钮列表
         */
        val buttonList: Array<ButtonInfo>,

        /**
         * 普通菜单按钮点击事件的监听器
         */
        val onMenuClickListener: OnMenuClickListener,

        /**
         * 尺寸重置按钮点击的监听器
         */
        val onResizeClickListener: OnResizeClickListener
    )

    /**
     * 按钮当描述信息
     */
    private class ButtonInfo(
        /**
         * 按钮名称
         */
        @StringRes
        val name: Int,

        /**
         * 按钮的ID
         */
        val id: Int
    )

    /**
     * 动画类型
     */
    private enum class AnimationStyle {

        /**
         * 展开模式
         * 当高度允许的时候，在锚点附近展开
         */
        Expansion,

        /**
         * 底部抽屉模式
         * 当锚点附近无法完整展开菜单时
         * 在底部放置菜单布局
         */
        Sheet
    }

    /**
     * 菜单被点击时的监听器
     */
    fun interface OnMenuClickListener {
        /**
         * 当菜单被点击时
         * @param id 被点击按钮的对应id
         */
        fun onMenuClick(id: Int)
    }

    /**
     * 尺寸重置按钮被点击时的监听器
     */
    fun interface OnResizeClickListener {

        /**
         * 尺寸重置按钮被点击时
         * @param newSize 被点击的尺寸信息
         */
        fun onResizeClick(newSize: TileSize)
    }

}