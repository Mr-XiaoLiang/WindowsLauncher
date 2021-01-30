package com.lollipop.windowslauncher.base

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.lollipop.iconcore.listener.*
import com.lollipop.windowslauncher.theme.LColor

/**
 * @author lollipop
 * @date 10/28/20 11:17
 * 基础的Activity 它提供基础的回调函数
 *
 */
open class BaseActivity: AppCompatActivity(),
    BackPressedProvider,
    OnWindowInsetsProvider,
    LColor.OnColorChangeListener {

    private val backPressedProviderHelper = BackPressedProviderHelper()

    private val windowInsetsProviderHelper = WindowInsetsProviderHelper()

    /**
     * 对当前activity设置默认的全屏Flag
     */
    fun initWindowFlag() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    /**
     * 绑定根节点的View，并且对它设置监听器
     * 它是窗口缩进信息的主要来源
     */
    fun initRootGroup(group: View) {
        group.fitsSystemWindows = true
        group.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val systemInsets = insets.getInsets(WindowInsets.Type.systemBars())
                onWindowInsetsChange(
                    group,
                    systemInsets.left, systemInsets.top,
                    systemInsets.right, systemInsets.bottom
                )
                windowInsetsProviderHelper.onInsetsChange(
                    group,
                    systemInsets.left, systemInsets.top,
                    systemInsets.right, systemInsets.bottom
                )
                WindowInsets.CONSUMED
            } else {
                onWindowInsetsChange(
                    group,
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
                windowInsetsProviderHelper.onInsetsChange(
                    group,
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
                insets.consumeSystemWindowInsets()
            }
        }
    }

    open val isLightStatusBar: Boolean
        get() {
            return LColor.style == LColor.Style.WHITE
        }

    open val isLightNavigationBar: Boolean
        get() {
            return LColor.style == LColor.Style.WHITE
        }

    private fun setViewFlag(open: Boolean, flag: Int) {
        val decorView = window?.decorView?:return
        val systemUiFlag = decorView.systemUiVisibility
        val has = systemUiFlag and flag != 0
        if (open == has) {
            return
        }
        if (open) {
            decorView.systemUiVisibility = (systemUiFlag or flag)
        } else {
            decorView.systemUiVisibility = (systemUiFlag xor flag)
        }
    }

    protected fun updateViewFlag() {
        setViewFlag(isLightStatusBar, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        setViewFlag(isLightNavigationBar, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    override fun onStart() {
        super.onStart()
        LColor.registerListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateViewFlag()
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        LColor.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedProviderHelper.destroy()
    }

    override fun onBackPressed() {
        if (backPressedProviderHelper.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    protected open fun onWindowInsetsChange(
        root: View, left: Int, top: Int, right: Int, bottom: Int) {
    }

    override fun addBackPressedListener(listener: BackPressedListener) {
        backPressedProviderHelper.addBackPressedListener(listener)
    }

    override fun removeBackPressedListener(listener: BackPressedListener) {
        backPressedProviderHelper.removeBackPressedListener(listener)
    }

    override fun addOnWindowInsetsProvider(listener: OnWindowInsetsListener) {
        windowInsetsProviderHelper.addOnWindowInsetsProvider(listener)
    }

    override fun removeOnWindowInsetsProvider(listener: OnWindowInsetsListener) {
        windowInsetsProviderHelper.removeOnWindowInsetsProvider(listener)
    }

    override fun onColorChanged() {
        updateViewFlag()
    }

}