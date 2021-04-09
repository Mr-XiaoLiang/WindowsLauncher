package com.lollipop.windowslauncher.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * @author lollipop
 * @date 4/8/21 23:21
 * 拖拽的容器，它会在需要的时候拦截事件，
 * 并且拖拽传递事件
 * ！！
 */
class DragGroup(context: Context, attrs: AttributeSet?, style: Int) :
    FrameLayout(context, attrs, style) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

}