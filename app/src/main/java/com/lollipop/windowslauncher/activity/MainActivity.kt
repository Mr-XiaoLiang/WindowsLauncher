package com.lollipop.windowslauncher.activity

import android.os.Bundle
import com.lollipop.windowslauncher.base.BaseActivity
import com.lollipop.windowslauncher.databinding.ActivityMainBinding
import com.lollipop.windowslauncher.utils.lazyBind

class MainActivity : BaseActivity() {

    private val viewBinding: ActivityMainBinding by lazyBind()

    override val isLightStatusBar: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWindowFlag()
        setContentView(viewBinding.root)
        initRootGroup(viewBinding.root)
    }

}