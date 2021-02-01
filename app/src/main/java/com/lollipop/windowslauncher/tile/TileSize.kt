package com.lollipop.windowslauncher.tile

/**
 * @author lollipop
 * @date 1/31/21 15:39
 * 瓷砖大小
 */
enum class TileSize(val width: Int, val height: Int) {

    /** 小 1*1 **/
    S(1, 1),
    /** 中 2*2 **/
    M(2, 2),
    /** 大 2*4 **/
    L(4, 2),
    /** 加大 4*4 **/
    XL(4, 4),

}