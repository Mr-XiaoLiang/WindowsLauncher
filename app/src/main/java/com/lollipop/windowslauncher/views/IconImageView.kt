package com.lollipop.windowslauncher.views

import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.*

/**
 * @author lollipop
 * @date 10/24/20 20:47
 * Icon的ImageView
 */
class IconImageView(context: Context, attr: AttributeSet?, defStyle: Int) :
    AdaptiveIconView(context, attr, defStyle), IconView {

    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context) : this(context, null)

    /**
     * 使用id加载一个图标
     * 如果图标无效，那么它会清空已有图标
     */
    override fun loadIcon(iconId: Int) {
        if (iconId == 0) {
            setImageDrawable(null)
            return
        }
        setImageResource(iconId)
    }

    /**
     * 以名字的形式加载一个图标
     */
    override fun loadIcon(iconName: String) {
        loadIcon(context.findDrawableId(iconName))
    }

    override var iconIndex = -1

    /**
     * 从图标包信息中加载图标
     */
    fun load(icon: IconHelper.IconInfo, def: Int = 0) {
        if (icon.resId == 0) {
            loadIcon(def)
            return
        }
        loadIcon(icon.resId)
    }

    /**
     * 从应用信息中加载图标
     */
    fun load(icon: IconHelper.AppInfo, iconIndex: Int = 0) {
        if (icon.iconPack.isEmpty()) {
            loadAppIcon(icon)
            return
        }
        loadIcon(icon.iconPack[iconIndex])
    }

    /**
     * 直接加载App的Icon
     * 包含一个异步的drawable加载过程
     */
    fun loadAppIcon(icon: IconHelper.AppInfo) {
        val drawable = icon.optIcon()
        if (drawable != null) {
            setIconDrawable(drawable)
            return
        }
        doAsync {
            val newDrawable = icon.loadIcon(context)
            onUI {
                setIconDrawable(newDrawable)
            }
        }
    }

    fun updateBackground() {
        updateBackground(LColor.primary)
    }

    fun updateBackground(color: Int) {
        setBackgroundColor(color)
    }

    fun setOutline(adaptiveOutline: Outline, defaultOutline: Outline) {
        adaptiveIconOutlineProvider = when (adaptiveOutline) {
            Outline.Oval -> OvalIconOutline()
            Outline.None -> null
        }
        defaultIconOutlineProvider = when (defaultOutline) {
            Outline.Oval -> OvalIconOutline()
            Outline.None -> null
        }
    }

    private fun setIconDrawable(loadIcon: Drawable) {
        if (this.drawable != loadIcon) {
            setImageDrawable(loadIcon)
        } else if (this.width > 0 && this.height > 0) {
            requestLayout()
        }
    }

    enum class Outline {
        Oval,
        None
    }

    private class OvalIconOutline : IconOutlineProvider {
        override fun getIconOutline(path: Path, left: Int, top: Int, iconWidth: Int) {
            path.reset()
            path.addOval(
                left.toFloat(),
                top.toFloat(),
                (left + iconWidth).toFloat(),
                (top + iconWidth).toFloat(),
                Path.Direction.CW
            )
        }
    }

}