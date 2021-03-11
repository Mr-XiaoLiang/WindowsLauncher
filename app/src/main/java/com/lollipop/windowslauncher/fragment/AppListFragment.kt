package com.lollipop.windowslauncher.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.lollipop.windowslauncher.base.BaseFragment
import com.lollipop.windowslauncher.databinding.FragmentAppListBinding
import com.lollipop.windowslauncher.databinding.ItemAppListBinding
import com.lollipop.windowslauncher.listener.WindowInsetsHelper
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.*
import com.lollipop.windowslauncher.views.IconImageView
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

    companion object {
        private val keyIconStyle: KeyNumberDrawable.() -> Unit = {
            style = KeyNumberDrawable.Style.Stroke
            setPadding(3)
            setStrokeWidth(3)
            setTextSize(24)
        }
    }

    private val appHelper = IconHelper.newHelper { null }

    private val viewBinding: FragmentAppListBinding by lazyBind()

    private val appList = ArrayList<AppListInfo>()

    private val keyPositionMap = HashMap<String, Int>()

    private val appListInsetsHelper by lazy {
        WindowInsetsHelper(viewBinding.appListContent)
    }

    private val alphabetInsetsHelper by lazy {
        WindowInsetsHelper(viewBinding.alphabetView)
    }

    private val appInfoAdapter by lazy {
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
        viewBinding.floatingKey.apply {
            nameView.visibleOrGone(false)
            iconView.setImageDrawable(KeyNumberDrawable().apply(keyIconStyle))
            iconView.setBackgroundColor(Color.TRANSPARENT)
            iconView.setIconWeight(0.8F, 1F)
        }
        viewBinding.appListView.apply {
            layoutManager = AppLayoutManager(view.context)
            adapter = appInfoAdapter
            addOnScrollListener(FloatingKeyHelper(viewBinding.floatingKey) { position ->
                appList[position].key
            })
        }
        viewBinding.alphabetView.apply {
            setBackgroundColor(0xA0000000.toInt())
            space = 10.dp2px().toInt()
            bindKeyStatusProvider(::isAlphabetKeyEnable)
            bindKeyClickListener(::onAlphabetKeyClick)
        }
        viewBinding.searchBtn.setOnClickListener {
            viewBinding.alphabetView.open()
        }
        updateAppList()
    }

    private fun onAlphabetKeyClick(key: String, index: Int) {
        val position = keyPositionMap[key] ?: -1
        if (position >= 0) {
            viewBinding.appListView.smoothScrollToPosition(position)
        }
        viewBinding.alphabetView.close()
    }

    private fun isAlphabetKeyEnable(key: String, index: Int): Boolean {
        val position = keyPositionMap[key] ?: -1
        return position >= 0
    }

    override fun onColorChanged() {
        super.onColorChanged()
        viewBinding.floatingKey.apply {
            root.setBackgroundColor(LColor.background)
            iconView.drawable?.let {
                if (it is KeyNumberDrawable) {
                    it.backgroundColor = LColor.tileBackground
                    it.foregroundColor = LColor.tileBackground
                    it.invalidateSelf()
                }
            }
        }
        viewBinding.searchBtn.updateColor()
        viewBinding.alphabetView.onColorChanged(
            LColor.primary, LColor.primaryReversal, LColor.foreground
        )
    }

    override fun onInsetsChange(root: View, left: Int, top: Int, right: Int, bottom: Int) {
        super.onInsetsChange(root, left, top, right, bottom)
        appListInsetsHelper.setInsetsByPadding(left, top, right, bottom)
        alphabetInsetsHelper.setInsetsByPadding(left, top, right, bottom)
    }

    private fun updateAppList() {
        val c = context ?: return
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
            onUI {
                appInfoAdapter.notifyDataSetChanged()
                viewBinding.alphabetView.updateKeyStatus()
            }
        }
    }

    /**
     * 关键字的排序算法
     * 比较char的顺序
     */
    private fun appInfoCompare(o1: AppListInfo, o2: AppListInfo): Int {
        val key1 = o1.key
        val key2 = o2.key
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

    /**
     * 获取应用名称中的第一个字母，
     * 期间涉及汉字转拼音的过程
     */
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

    /**
     * 检查Key的值
     * 确保在A～Z之间
     * 对于小写字母，将转换为大写
     * 其他非法字符转换为#
     */
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

    /**
     * Item点击事件
     */
    private fun onItemClick(position: Int) {
        val info = appList[position]
        if (info.isAppInfo) {
            startActivity(Intent().apply {
                component = info.app.pkg
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } else {
            viewBinding.searchBtn.callOnClick()
        }
    }

    private fun onItemLongClick(position: Int) {
        // TODO
    }

    private class FloatingKeyHelper(
        private val viewBinding: ItemAppListBinding,
        private val getLabelKey: (Int) -> String,
    ) : RecyclerView.OnScrollListener() {

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
            val firstKey = getLabelKey(firstVisible)
            val secondKey = getLabelKey(firstCompletely)
            if (firstKey == secondKey) {
                topTo()
                return
            }

            val secondView = layoutManager.findViewByPosition(firstCompletely)
            topTo(secondView)
        }

        private fun setKey(position: Int) {
            viewBinding.iconView.drawable?.let { icon ->
                if (icon is KeyNumberDrawable) {
                    icon.text = getLabelKey(position)
                    icon.invalidateSelf()
                }
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
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            return if (appList[position].isAppInfo) {
                VIEW_TYPE_APP
            } else {
                VIEW_TYPE_KEY
            }
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
            viewBinding.iconView.setOutline(IconImageView.Outline.None, IconImageView.Outline.Oval)
            viewBinding.iconView.setIconWeight(0.8F, 0.5F)
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
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        companion object {
            fun create(
                parent: ViewGroup,
                onClick: (AppKeyHolder) -> Unit,
            ): AppKeyHolder {
                return AppKeyHolder(parent.bind(), onClick)
            }
        }

        private val keyNumberDrawable = KeyNumberDrawable().apply(keyIconStyle)

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
            viewBinding.nameView.visibleOrGone(false)
            viewBinding.iconView.setIconWeight(0.8F, 1F)
        }

        fun bind(appInfo: AppListInfo) {
            keyNumberDrawable.backgroundColor = LColor.tileBackground
            keyNumberDrawable.foregroundColor = LColor.tileBackground
            keyNumberDrawable.text = appInfo.key
            keyNumberDrawable.invalidateSelf()
        }

    }

    private class AppLayoutManager(
        context: Context, orientation: Int, reverseLayout: Boolean
    ) : LinearLayoutManager(context, orientation, reverseLayout) {

        constructor(context: Context) : this(context, RecyclerView.VERTICAL, false)

        val topSnappedSmoothScroller by lazy {
            TopSnappedSmoothScroller(context, ::computeScrollVectorForPosition)
        }

        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State?,
            position: Int
        ) {
            topSnappedSmoothScroller.targetPosition = position
            startSmoothScroll(topSnappedSmoothScroller)
        }

        class TopSnappedSmoothScroller(
            context: Context,
            private val computeScrollVectorForPositionCallback: (Int) -> PointF?
        ) : LinearSmoothScroller(context) {

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return computeScrollVectorForPositionCallback(targetPosition)
            }

            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START //设置滚动位置
            }
        }
    }
}