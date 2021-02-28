package com.lollipop.windowslauncher.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.lollipop.windowslauncher.base.BaseActivity
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.ActivityMainBinding
import com.lollipop.windowslauncher.fragment.AppListFragment
import com.lollipop.windowslauncher.fragment.DesktopFragment
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.lazyBind

class MainActivity : BaseActivity(), DesktopFragment.OpenAppListCallback {

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
        viewBinding.indicatorView.setColor(LColor.foreground)
    }

    private fun initView() {
        viewBinding.pageGroup.adapter = PageAdapter(fragmentArray, supportFragmentManager)
        viewBinding.indicatorView.bindViewPager(viewBinding.pageGroup)
    }

    override fun canBack(): Boolean {
        if (viewBinding.pageGroup.currentItem != 0) {
            viewBinding.pageGroup.setCurrentItem(0, true)
            return false
        }
        return super.canBack()
    }


    override fun openAppList() {
        viewBinding.pageGroup.currentItem = 1
    }

    private class PageAdapter(
        private val fragments: Array<BaseFragment>,
        fragmentManager: FragmentManager
    ) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

    }

}