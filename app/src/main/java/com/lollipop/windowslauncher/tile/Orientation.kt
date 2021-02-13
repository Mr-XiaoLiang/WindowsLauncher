package com.lollipop.windowslauncher.tile

/**
 * @author lollipop
 * @date 2/13/21 17:03
 * 方向
 */
enum class Orientation {
    Vertical,
    Horizontal;

    val isVertical: Boolean
        get() {
            return this == Vertical
        }
}