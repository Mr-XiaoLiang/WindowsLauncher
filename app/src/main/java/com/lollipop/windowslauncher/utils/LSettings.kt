package com.lollipop.windowslauncher.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.SharedPreferencesUtils.get
import com.lollipop.windowslauncher.utils.SharedPreferencesUtils.set

/**
 * @author lollipop
 * @date 1/30/21 20:48
 */
object LSettings {

    private const val KEY_THEME_MODE = "KEY_THEME_MODE"

    private const val KEY_PRIMARY_COLOR = "KEY_PRIMARY_COLOR"

    private const val KEY_TILE_COL = "KEY_TILE_COL"

    private const val KEY_CREVICE_MODE = "KEY_CREVICE_MODE"

    const val TILE_COL_MAX = 12

    const val TILE_COL_MIN = 6

    fun setTileCol(context: Context, col: Int) {
        context[KEY_TILE_COL] = col.range(TILE_COL_MIN, TILE_COL_MAX)
    }

    fun getTileCol(context: Context, ): Int {
        return context[KEY_TILE_COL, TILE_COL_MIN].range(TILE_COL_MIN, TILE_COL_MAX)
    }

    fun setCreviceMode(context: Context, value: CreviceMode) {
        context[KEY_CREVICE_MODE] = value.name
    }

    fun getCreviceMode(context: Context): CreviceMode {
        val name = context[KEY_CREVICE_MODE, CreviceMode.M.name]
        return CreviceMode.valueOf(name)
    }

    fun setThemeMode(context: Context, value: ThemeMode) {
        context[KEY_THEME_MODE] = value.name
    }

    fun getThemeMode(context: Context): ThemeMode {
        val name = context[KEY_THEME_MODE, ThemeMode.AUTO.name]
        return ThemeMode.valueOf(name)
    }

    fun setPrimaryColor(context: Context, color: Int) {
        context[KEY_PRIMARY_COLOR] = color
    }

    fun getPrimaryColor(context: Context): Int {
        return context[KEY_PRIMARY_COLOR, Color.BLUE]
    }

    fun getNightMode(context: Context): Int {
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    fun getStyleByMode(context: Context): LColor.Style {
        when (getThemeMode(context)) {
            ThemeMode.AUTO -> {
                val currentNightMode = getNightMode(context)
                return if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    LColor.Style.BLACK
                } else {
                    LColor.Style.WHITE
                }
            }
            ThemeMode.LIGHT -> {
                return LColor.Style.WHITE
            }
            ThemeMode.DARK -> {
                return LColor.Style.BLACK
            }
        }
    }

    enum class ThemeMode {
        AUTO, LIGHT, DARK
    }

    enum class CreviceMode(val dp: Int) {
        L(18), M(10), S(5)
    }

}
