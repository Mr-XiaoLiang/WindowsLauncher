package com.lollipop.windowslauncher.fragment

import android.content.Context
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

/**
 * @author lollipop
 * @date 1/30/21 14:01
 * 应用列表的碎片
 */
class AppListFragment : BaseFragment() {

    private val appHelper = IconHelper.newHelper { null }

    private val viewBinding: FragmentAppListBinding by lazyBind()

    private val appListInsetsHelper: WindowInsetsHelper by lazy {
        WindowInsetsHelper(viewBinding.root)
    }

    private val appInfoAdapter: AppInfoAdapter by lazy {
        AppInfoAdapter(appHelper, ::onItemClick, ::onItemLongClick)
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
        private val appHelper: IconHelper,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (Int) -> Unit,
    ): RecyclerView.Adapter<AppInfoHolder>() {

        init {
            appHelper.onAppListChange {
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoHolder {
            return AppInfoHolder.create(parent, ::onHolderClick, ::onHolderLongClick)
        }

        override fun onBindViewHolder(holder: AppInfoHolder, position: Int) {
//            holder.bind(appHelper.getAppInfo(position),
//                isShowKey(holder.itemView.context, position))
        }

        override fun getItemCount(): Int {
            return appHelper.appCount
        }

        private fun onHolderClick(holder: AppInfoHolder) {
            onClick(holder.adapterPosition)
        }

        private fun onHolderLongClick(holder: AppInfoHolder) {
            onLongClick(holder.adapterPosition)
        }

        fun isShowKey(context: Context, position: Int): Boolean {
            if (position == 0) {
                return true
            }
            val lastAppInfo = appHelper.getAppInfo(position - 1)
            val lastKey = lastAppInfo.getLabelKey(context)
            val thisKey = appHelper.getAppInfo(position).getLabelKey(context)
            if (thisKey == lastKey) {
                return false
            }
            return true
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
            setStrokeWidth(2)
            setTextSize(20)
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