package com.lollipop.windowslauncher.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lollipop.iconcore.ui.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentDesktopBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.lazyBind

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 桌面的碎片
 */
class DesktopFragment: BaseFragment() {

    private val viewBinding: FragmentDesktopBinding by lazyBind()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.testView.setTextColor(LColor.foreground)
    }

}