package com.lollipop.windowslauncher.theme

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import com.lollipop.windowslauncher.utils.LSettings
import java.lang.ref.WeakReference

/**
 * @author lollipop
 * @date 1/30/21 14:33
 * Launcher的Color
 */
object LColor {

    private val listenerList = ArrayList<WeakReference<OnColorChangeListener>>()

    private val updateHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    var style: Style = Style.WHITE
        private set

    val background: Int
        get() {
            return style.back
        }

    val foreground: Int
        get() {
            return style.fore
        }

    var primary: Int = LSettings.DEF_PRIMARY_COLOR
        private set

    var primaryReversal: Int = LSettings.DEF_PRIMARY_COLOR_REVERSAL
        private set

    var tileTransparent = false
        private set

    val tileBackground: Int
        get() {
            return if (tileTransparent) {
                Color.TRANSPARENT
            } else {
                primary
            }
        }

    /**
     * 更新全局的主题色
     */
    fun update(style: Style, primary: Int, primaryReversal: Int) {
        this.style = style
        this.primary = primary
        this.primaryReversal = primaryReversal
        notifyColorChange()
    }

    /**
     * 解除注册颜色监听器
     */
    fun unregisterListener(listener: OnColorChangeListener) {
        for (ref in listenerList) {
            if (ref.get() == listener) {
                listenerList.remove(ref)
                return
            }
        }
    }

    /**
     * 注册颜色监听器
     */
    fun registerListener(listener: OnColorChangeListener) {
        for (ref in listenerList) {
            if (ref.get() == listener) {
                return
            }
        }
        listenerList.add(WeakReference(listener))
        // 添加的时候触发一次更新
        listener.onColorChanged()
    }

    private fun notifyColorChange() {
        synchronized(LColor) {
            // 同步方式检查回调函数引用
            val listeners = ArrayList<WeakReference<OnColorChangeListener>>()
            for (listener in listenerList) {
                if (listener.get() != null) {
                    listeners.add(listener)
                }
            }
            // 更新引用集合，去除无用引用
            listenerList.clear()
            listenerList.addAll(listeners)

            // 发起一轮新的更新通知
            // 使用Handler的原因在于跳出调用者的方法栈
            // 避免产生递归调用问题
            updateHandler.post {
                listeners.forEach {
                    it.get()?.onColorChanged()
                }
            }
        }
    }

    interface OnColorChangeListener {
        fun onColorChanged()
    }

    enum class Style(val back: Int, val fore: Int) {
        WHITE(Color.WHITE, Color.BLACK), BLACK(Color.BLACK, Color.WHITE)
    }

}