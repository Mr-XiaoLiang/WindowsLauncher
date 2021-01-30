package com.lollipop.windowslauncher.activity

import android.os.Bundle
import com.lollipop.windowslauncher.base.BaseActivity
import com.lollipop.windowslauncher.databinding.ActivityMainBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.lazyBind

class MainActivity : BaseActivity() {

    private val viewBinding: ActivityMainBinding by lazyBind()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWindowFlag()
        setContentView(viewBinding.root)
        initRootGroup(viewBinding.root)
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.rootGroup.setBackgroundColor(LColor.background)
    }

}