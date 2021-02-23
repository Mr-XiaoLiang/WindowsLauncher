package com.lollipop.windowslauncher.fragment

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.listener.WindowInsetsHelper
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentAppListBinding
import com.lollipop.windowslauncher.databinding.ItemAppListBinding
import com.lollipop.windowslauncher.databinding.ItemAppListKeyBinding
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.*
import com.lollipop.windowslauncher.views.KeyNumberDrawable
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 应用列表的碎片
 */
class AppListFragment : BaseFragment() {

    private val appHelper = IconHelper.newHelper { null }

    private val viewBinding: FragmentAppListBinding by lazyBind()

    private val appList = ArrayList<AppListInfo>()

    private val keyPositionMap = HashMap<String, Int>()

    private val appListInsetsHelper: WindowInsetsHelper by lazy {
        WindowInsetsHelper(viewBinding.root)
    }

    private val appInfoAdapter: AppInfoAdapter by lazy {
        AppInfoAdapter(appList, ::onItemClick, ::onItemLongClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appHelper.onAppListChange {
            updateAppList()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appHelper.loadAppInfo(context)
    }

    override fun onDetach() {
        super.onDetach()
        appHelper.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.appListView.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = appInfoAdapter
//            addOnScrollListener(FloatingKeyHelper(viewBinding.floatingKey) { context, position ->
//                appHelper.getAppInfo(position).getLabelKey(context)
//            })
        }
        updateAppList()
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.floatingKey.apply {
//            floatingKeyView.setBackgroundColor(LColor.background)
//            floatingKeyValueView.setTextColor(LColor.foreground)
        }
    }

    override fun onInsetsChange(root: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.onInsetsChange(root, left, top, right, bottom)
        appListInsetsHelper.setInsetsByPadding(left, top, right, bottom)
    }

    private fun updateAppList() {
        val c = context?:return
        doAsync {
            val list = ArrayList<AppListInfo>()
            for (i in 0 until appHelper.appCount) {
                val appInfo = appHelper.getAppInfo(i)
                val key = getFistKey(appInfo.getLabel(c))
                list.add(AppListInfo(key, appInfo, true))
            }
            Collections.sort(list, ::appInfoCompare)
            var lastKey = ""
            appList.clear()
            keyPositionMap.clear()
            for (info in list) {
                // 如果不一致，那么先加一个key在前方
                if (info.key != lastKey) {
                    lastKey = info.key
                    keyPositionMap[lastKey] = appList.size
                    val emptyAppInfo = IconHelper.AppInfo(ComponentName("", ""), IntArray(0))
                    appList.add(AppListInfo(lastKey, emptyAppInfo, false))
                }
                // 把应用信息放进去
                appList.add(info)
            }
            log("appList.size = ${appList.size}")
            onUI {
                appInfoAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * 关键字的排序算法
     * 比较char的顺序
     */
    private fun appInfoCompare(o1: AppListInfo, o2: AppListInfo): Int {
        val key1 = o1.app.optLabel()?:o1.key
        val key2 = o2.app.optLabel()?:o2.key
        for (i in 0 until max(key1.length, key2.length)) {
            if (key1.length <= i) {
                return -1
            }
            if (key2.length <= i) {
                return 1
            }
            val diff = key1[i] - key2[i]
            if (diff != 0) {
                return diff
            }
        }
        return 0
    }

    private fun getFistKey(label: CharSequence): String {
        val labelString = label.toString()
        // 空标题，默认放到不可解析的分组
        if (labelString.isEmpty()) {
            return "#"
        }
        val labelFirst = labelString[0]
        val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(labelFirst)
        // 拼音解析结果为空，那么判定是否是字母
        if (pinyinArray == null || pinyinArray.isEmpty()) {
            return checkKeyChar(labelFirst)
        }
        val pinyinFast = pinyinArray[0]
        // 解析失败，放到不可解析的部分
        if (pinyinFast.isEmpty()) {
            return "#"
        }
        val key = pinyinFast[0]
        // 取拼音首字母，做一次检查
        return checkKeyChar(key)
    }

    private fun checkKeyChar(char: Char): String {
        // 如果是大写字母，那么直接使用
        if (char in 'A'..'Z') {
            return char.toString()
        }
        // 如果是小写字母，那么转为大写字母
        if (char in 'a'..'z') {
            return char.toUpperCase().toString()
        }
        // 都不是，那么放到不可解析的部分
        return "#"
    }

    private fun onItemClick(position: Int) {
        // TODO
    }

    private fun onItemLongClick(position: Int) {
        // TODO
    }

    private class FloatingKeyHelper(
        private val viewBinding: ItemAppListKeyBinding,
        private val getLabelKey: (Context, Int) -> CharSequence,
    ): RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val itemCount = recyclerView.adapter?.itemCount ?: 0
            if (itemCount < 1) {
                return
            }
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                onScrolledByLinearLayout(recyclerView, layoutManager)
            }
        }

        private fun onScrolledByLinearLayout(
            recyclerView: RecyclerView,
            layoutManager: LinearLayoutManager
        ) {
            val firstCompletely =
                layoutManager.findFirstCompletelyVisibleItemPosition()
            if (firstCompletely == 0) {
                setKey(0)
                topTo()
                return
            }

            val firstVisible = layoutManager.findFirstVisibleItemPosition()

            setKey(firstVisible)
            val firstKey = getLabelKey(recyclerView.context, firstVisible)
            val secondKey = getLabelKey(recyclerView.context, firstCompletely)

            if (firstKey == secondKey) {
                topTo()
                return
            }

            val secondView = layoutManager.findViewByPosition(firstCompletely)
            topTo(secondView)
        }

        private fun setKey(position: Int) {
            viewBinding.floatingKeyValueView.apply {
                text = getLabelKey(context, position)
            }
        }

        private fun topTo(view: View? = null) {
            val y = if (view == null) {
                0F
            } else {
                val top = view.top.toFloat()
                val height = viewBinding.root.height.toFloat()
                if (top > height) {
                    0F
                } else {
                    top - height
                }
            }
            viewBinding.root.translationY = y
        }

    }

    private class AppInfoAdapter(
        private val appList: ArrayList<AppListInfo>,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (Int) -> Unit,
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_APP = 0
            private const val VIEW_TYPE_KEY = 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == VIEW_TYPE_KEY) {
                return AppKeyHolder.create(parent, ::onHolderClick)
            }
            return AppInfoHolder.create(parent, ::onHolderClick, ::onHolderLongClick)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is AppInfoHolder -> {
                    holder.bind(appList[position])
                }
                is AppKeyHolder -> {
                    holder.bind(appList[position])
                }
            }
        }

        override fun getItemCount(): Int {
            return appList.size
        }

        private fun onHolderClick(holder: RecyclerView.ViewHolder) {
            onClick(holder.adapterPosition)
        }

        private fun onHolderLongClick(holder: RecyclerView.ViewHolder) {
            onLongClick(holder.adapterPosition)
        }

        override fun getItemViewType(position: Int): Int {
            return if (appList[position].isAppInfo) { VIEW_TYPE_APP } else { VIEW_TYPE_KEY }
        }
    }

    private data class AppListInfo(
        val key: String,
        val app: IconHelper.AppInfo,
        val isAppInfo: Boolean
    )

    private class AppInfoHolder private constructor(
        private val viewBinding: ItemAppListBinding,
        private val onClick: (AppInfoHolder) -> Unit,
        private val onLongClick: (AppInfoHolder) -> Unit,
    ) : RecyclerView.ViewHolder(viewBinding.root) {

        companion object {
            fun create(
                parent: ViewGroup,
                onClick: (AppInfoHolder) -> Unit,
                onLongClick: (AppInfoHolder) -> Unit,
            ): AppInfoHolder {
                return AppInfoHolder(parent.bind(), onClick, onLongClick)
            }
        }

        init {
            viewBinding.root.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    onClick(this@AppInfoHolder)
                }
                setOnLongClickListener {
                    onLongClick(this@AppInfoHolder)
                    true
                }
            }
        }

        fun bind(appInfo: AppListInfo) {
            val context = viewBinding.root.context
            appInfo.app.loadLabel(context) {
                viewBinding.nameView.text = it
            }
            viewBinding.nameView.setTextColor(LColor.foreground)
            viewBinding.iconView.load(appInfo.app)
            viewBinding.iconView.updateBackground()
        }

    }

    private class AppKeyHolder(
        private val viewBinding: ItemAppListBinding,
        private val onClick: (AppKeyHolder) -> Unit
    ): RecyclerView.ViewHolder(viewBinding.root) {
        companion object {
            fun create(
                parent: ViewGroup,
                onClick: (AppKeyHolder) -> Unit,
            ): AppKeyHolder {
                return AppKeyHolder(parent.bind(), onClick)
            }
        }

        private val keyNumberDrawable = KeyNumberDrawable().apply {
            style = KeyNumberDrawable.Style.Stroke
            setPadding(5)
            setStrokeWidth(3)
            setTextSize(22)
        }

        init {
            viewBinding.root.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    onClick(this@AppKeyHolder)
                }
            }
            viewBinding.iconView.setImageDrawable(keyNumberDrawable)
            viewBinding.iconView.setBackgroundColor(Color.TRANSPARENT)
        }

        fun bind(appInfo: AppListInfo) {
            keyNumberDrawable.backgroundColor = LColor.tileBackground
            keyNumberDrawable.foregroundColor = LColor.tileBackground
            keyNumberDrawable.text = appInfo.key
            keyNumberDrawable.invalidateSelf()
            viewBinding.nameView.text = ""
        }

    }

}