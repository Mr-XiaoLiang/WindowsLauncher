package com.lollipop.windowslauncher.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.lollipop.iconcore.ui.BaseFragment
import com.lollipop.windowslauncher.base.BaseActivity
import com.lollipop.windowslauncher.databinding.ActivityMainBinding
import com.lollipop.windowslauncher.fragment.AppListFragment
import com.lollipop.windowslauncher.fragment.DesktopFragment
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.lazyBind

class MainActivity : BaseActivity() {

    private val viewBinding: ActivityMainBinding by lazyBind()

    private val fragmentArray: Array<BaseFragment> by lazy {
        arrayOf(DesktopFragment(), AppListFragment())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWindowFlag()
        setContentView(viewBinding.root)
        initRootGroup(viewBinding.root)
        initView()
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.rootGroup.setBackgroundColor(LColor.background)
    }

    private fun initView() {
        viewBinding.pageGroup.adapter = PageAdapter(fragmentArray, supportFragmentManager)
    }

    private class PageAdapter(
        private val fragments: Array<BaseFragment>,
        fragmentManager: FragmentManager
    ): FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_SET_USER_VISIBLE_HINT) {

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

    }

}