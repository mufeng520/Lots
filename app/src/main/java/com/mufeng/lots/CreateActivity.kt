package com.mufeng.lots

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mufeng.lots.databinding.ActivityCreateBinding
import java.io.File
import java.util.Collections
import java.util.UUID

class CreateActivity : ComponentActivity() {
    private lateinit var binding: ActivityCreateBinding
    private val parentItems = mutableListOf<ParentItem>()
    private var currentFileName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fab.setOnClickListener { view ->
            val menu = popupMenu {
                section {
                    title = "Options"
                    item {
                        label = "保存"
//                        icon = R.drawable.abc_ic_menu_copy_mtrl_am_alpha
                        callback = {
                            saveToJson()
                        }
                    }
                    item {
                        label = "添加父项"
//                        icon = R.drawable.abc_ic_menu_copy_mtrl_am_alpha
                        callback = {
                            val newPosition = parentItems.size
                            parentItems.add(
                                ParentItem(
                                    type = ParentType.LIST,
                                    key = "",
                                    items = mutableListOf(
                                        ListItem("")
                                    )
                                )
                            )
                            // 通知适配器更新指定父项的子项列表
                            binding.lotteryListRecyclerview.adapter?.notifyItemInserted(newPosition)

                        }
                    }
                    item {
                        label = "添加子项"
                        callback = {
                            // 查找当前展开的父项位置
                            val expandedParent = parentItems.indexOfFirst { it.isExpanded }
                            val targetPosition = if (expandedParent != -1) {
                                expandedParent
                            } else {
                                // 如果没有展开的项，使用最后一项
                                parentItems.lastIndex
                            }

                            // 安全获取目标父项
                            val currentParent = parentItems[targetPosition]

                            // 添加子项
                            currentParent.items.add(ListItem(""))

                            // 局部刷新目标父项
                            binding.lotteryListRecyclerview.adapter?.notifyItemChanged(targetPosition)
                        }
                    }
                }
            }

            menu.show(this,view)
        }
        initSampleData()
        setupRecyclerView()
    }
    // 添加保存方法
    // 数据转换方法
    private fun parseTemplateData(): DeckTemplate {
        val infoMap = mutableMapOf<String, String>()
        val dataList = mutableListOf<String>()
        val dynamicSections = mutableMapOf<String, List<String>>()

        parentItems.forEach { parent ->
            when (parent.key) {
                "info" -> parent.items.filterIsInstance<KeyValueItem>()
                    .forEach { infoMap[it.systemKey] = it.value }
                "data" -> parent.items.filterIsInstance<ListItem>()
                    .mapTo(dataList) { it.value }
                else -> dynamicSections[parent.key] =
                    parent.items.filterIsInstance<ListItem>().map { it.value }
            }
        }
        return DeckTemplate(infoMap, dataList, dynamicSections)
    }
    private fun saveToJson() {
        val mapper = jacksonObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)

        val template = parseTemplateData()
        val title = parentItems
            .firstOrNull { it.key == "info" }
            ?.items?.filterIsInstance<KeyValueItem>()
            ?.firstOrNull { it.systemKey == "title" }
            ?.value ?: System.currentTimeMillis()

        val directory = File(getExternalFilesDir(null)?.path + "/card_data/")
        if (!directory.exists()) directory.mkdirs()

        val newFile = File(directory, "${title}.json")

        try {
            // 重命名逻辑
            currentFileName?.let { oldName ->
                val oldFile = File(directory, "$oldName.json")
                if (oldFile.exists() && oldName != title) {
                    oldFile.renameTo(newFile)
                }
            }

            // 如果文件不存在（首次保存或重命名成功）则写入
            if (!newFile.exists()) {
                mapper.writeValue(newFile, template.toJsonMap())
            }

            currentFileName = title.toString() // 更新当前文件名
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun initSampleData() {
        // 初始化info部分（固定结构）
        parentItems.add(
            ParentItem(
                type = ParentType.INFO,
                key = "info",
                items = mutableListOf(
                    KeyValueItem("title", "模板牌堆", "牌堆名称"),
                    KeyValueItem("author", "木风", "作者"),
                    KeyValueItem("version", "1.0.0", "版本"),
                    KeyValueItem("explain", "牌堆示例", "说明")
                ),
                canEditKey = false // 禁止修改info的键
            )
        )

        // 主牌堆部分
        parentItems.add(
            ParentItem(
                type = ParentType.LIST,
                key = "data",
                items = mutableListOf(
                    ListItem("")
                ),
                canEditKey = false
            )
        )

        // 动态添加其他部分

    }

    private fun setupRecyclerView() {
        binding.lotteryListRecyclerview.apply {
            layoutManager = LinearLayoutManager(this@CreateActivity)
            adapter = ParentAdapter(parentItems,
                onChildChanged = { parentPos, childPos, newValue ->
                    when (val item = parentItems[parentPos].items[childPos]) {
                        is KeyValueItem -> item.value = newValue
                        is ListItem -> item.value = newValue
                    }
                },
                onParentKeyChanged = { position, newKey ->
                    parentItems[position].key = newKey
                }
            )
        }
    }
}

// 数据模型
sealed class ParentType {
    object INFO : ParentType()
    object LIST : ParentType()
}

data class ParentItem(
    val type: ParentType,
    var key: String,
    val items: MutableList<Any>,
    var isExpanded: Boolean = false,
    val canEditKey: Boolean = true // 控制是否可编辑键名
)

data class KeyValueItem(
    val systemKey: String, // 系统使用的固定键
    var value: String,
    val displayHint: String // 显示给用户的提示文本
)

data class ListItem(
    var value: String,
    val id: String = UUID.randomUUID().toString()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeckTemplate(
    @JsonProperty("info")
    val info: Map<String, String>,

    @JsonProperty("data")
    val data: List<String>,

    @JsonIgnore
    val dynamicSections: Map<String, List<String>> = emptyMap()
) {
    // 合并动态字段到主结构
    fun toJsonMap(): Map<String, Any> {
        return mapOf("info" to info, "data" to data) + dynamicSections
    }
}


// 适配器实现
class ParentAdapter(
    private val items: List<ParentItem>,
    private val onChildChanged: (parentPos: Int, childPos: Int, newValue: String) -> Unit,
    private val onParentKeyChanged: (position: Int, newKey: String) -> Unit
) : RecyclerView.Adapter<ParentAdapter.ParentViewHolder>()
{

    private var lastExpandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item_expandable_p, parent, false)
        return ParentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextInputEditText = itemView.findViewById(R.id.etParentKey)
        private val rvChild: RecyclerView = itemView.findViewById(R.id.rvChildList)
        private val ivIndicator: ImageView = itemView.findViewById(R.id.ivIndicator)
        private lateinit var childAdapter: ChildAdapter

        init {
            setupChildRecyclerView()
            tvTitle.doAfterTextChanged {
                onParentKeyChanged(adapterPosition, it.toString())
            }
        }

        private fun setupChildRecyclerView() {
            childAdapter = ChildAdapter(
                onTextChanged = { childPos, newValue ->
                    onChildChanged(adapterPosition, childPos, newValue)
                }
            )

            ItemTouchHelper(DragCallback(childAdapter)).attachToRecyclerView(rvChild)

            rvChild.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = childAdapter
                isNestedScrollingEnabled = false
            }
        }

        fun bind(item: ParentItem) {
            tvTitle.apply {
                setText(item.key)
                isEnabled = item.canEditKey // 根据类型控制是否可编辑
                hint = when (item.type) {
                    ParentType.INFO -> "基本信息"
                    else -> "输入键名"
                }
            }
            tvTitle.setText(item.key)
            childAdapter.submitList(item.items)
            updateChildVisibility(item.isExpanded)
            updateIndicator(item.isExpanded)

            itemView.setOnClickListener {
                val pos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                toggleExpansion(pos, item)
            }
        }

        private fun toggleExpansion(position: Int, item: ParentItem) {
            item.isExpanded = !item.isExpanded
            if (item.isExpanded) {
                if (lastExpandedPosition != -1) {
                    items[lastExpandedPosition].isExpanded = false
                    notifyItemChanged(lastExpandedPosition)
                }
                lastExpandedPosition = position
            } else {
                lastExpandedPosition = -1
            }
            TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup)
            updateChildVisibility(item.isExpanded)
            updateIndicator(item.isExpanded)
        }

        private fun updateChildVisibility(show: Boolean) {
            rvChild.visibility = if (show) View.VISIBLE else View.GONE
        }

        private fun updateIndicator(expanded: Boolean) {
            ivIndicator.animate()
                .rotation(if (expanded) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }
}

class ChildAdapter(
    private val onTextChanged: (Int, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{

    var items = listOf<Any>()

    fun submitList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is KeyValueItem -> R.layout.recyclerview_item_expandable_key_value
            else -> R.layout.recyclerview_item_expandable_list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.recyclerview_item_expandable_key_value -> KeyValueHolder(
                LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            )
            else -> ListItemHolder(
                LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is KeyValueHolder -> holder.bind(items[position] as KeyValueItem)
            is ListItemHolder -> holder.bind(items[position] as ListItem)
        }
    }

    override fun getItemCount() = items.size

    inner class KeyValueHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tilInput: TextInputLayout = view.findViewById(R.id.tilInput)
        private val etValue: TextInputEditText = view.findViewById(R.id.etValue)

        fun bind(item: KeyValueItem) {
            tilInput.hint = item.displayHint // 显示友好提示
            etValue.setText(item.value)
            etValue.doAfterTextChanged {
                onTextChanged(adapterPosition, it.toString())
            }
        }
    }

    inner class ListItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val etItem: TextInputEditText = view.findViewById(R.id.etListItem)

        fun bind(item: ListItem) {
            etItem.setText(item.value)
            etItem.doAfterTextChanged {
                onTextChanged(adapterPosition, it.toString())
            }
        }
    }
}

class DragCallback(private val adapter: ChildAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
        makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        Collections.swap(adapter.items, from, to)
        adapter.notifyItemMoved(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}