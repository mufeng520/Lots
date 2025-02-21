package com.mufeng.lots.ui.lots

import FileUtils
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.mufeng.lots.DrawActivity
import com.mufeng.lots.R
import com.mufeng.lots.databinding.FragmentLotsBinding

class LotsFragment : Fragment() {

    private var _binding: FragmentLotsBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMyAdapter: RecyclerAdapter
    private val mNewsList = mutableListOf<String>()
    private lateinit var fileUtils: FileUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileUtils = FileUtils(requireActivity() as AppCompatActivity)
}
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLotsBinding.inflate(inflater, container, false)


        binding.mainListRecyclerview.apply {
            //设置布局管理器
            //            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutManager = GridLayoutManager(requireActivity(), 2)

            //设置adapter
            mMyAdapter = RecyclerAdapter(mNewsList)
            adapter = mMyAdapter

            //设置列表界面背景
            var inputStream = requireContext().assets.open("background_funingna.jpg")
            var bitmap = BitmapFactory.decodeStream(inputStream)
            background = BitmapDrawable(resources,bitmap)
            background.alpha = 128
        }
        refreshFileList()

        //设置下拉刷新监听器
        binding.mainListRefresh.setOnRefreshListener {
            refreshFileList()
            binding.mainListRefresh.isRefreshing = false

        }
        //设置点击事件
        mMyAdapter?.setOnItemClickListener(object :RecyclerAdapter.IKotlinItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent()
                intent.setClass(requireActivity(), DrawActivity::class.java)
                intent.putExtra("main_data", mNewsList[position])
                startActivity(intent)
            }
        })
        mMyAdapter?.setOnItemLongClickListener(object :RecyclerAdapter.IKotlinItemLongClickListener {
            override fun onItemLongClick(position: Int, view: View){
                val menu = popupMenu {
                    section {
                        title = "Options"
                        item {
                            label = "复制"
                            callback = {

                            }
                        }
                        item {
                            label = "删除"
                            callback = {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("确认删除")
                                    .setMessage("确定要删除此项吗？")
                                    .setPositiveButton("删除") { _, _ ->
//                                        mNewsList.removeAt(position)
//                                        mMyAdapter.notifyItemRemoved(position)
                                    }
                                    .setNegativeButton("取消", null)
                                    .show()
                            }
                        }
                        item {
                            label = "分享"
                            callback = {

                            }
                        }
                    }
                }
                Log.d("Log--------->>>>", "长按了第${position+1}项，标题是${mNewsList[position]}")
                menu.show(requireContext(),view)
            }
        })




        return binding.root


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun refreshFileList() {
        mNewsList.clear()
        var File_list = fileUtils.getFileList("card_data")
        File_list?.forEach {
            if (it.isFile) {
                var path = it.name
                val news ="$path"
                mNewsList.add(news)
                Log.d(getString(R.string.log_tag),mNewsList.toString())
            }
        }
        mMyAdapter?.notifyDataSetChanged()
        binding.mainListRecyclerview.scrollToPosition(-1)

    }

}

class RecyclerAdapter(private val mNewsList: List<String>) : RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mTitleTv: TextView = view.findViewById(R.id.textView)
        val mTitleContent: TextView = view.findViewById(R.id.textView2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_list, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val news = mNewsList[position]
        holder.mTitleTv.text = news
//        holder.mTitleContent.text = news.content
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
        holder.itemView.setOnLongClickListener {
            itemLongClickListener?.onItemLongClick(position, it)
            true
        }
    }



    private var itemClickListener: IKotlinItemClickListener? = null
    fun setOnItemClickListener(listener: IKotlinItemClickListener) {
        this.itemClickListener = listener
    }
    interface IKotlinItemClickListener{
        fun onItemClick(position: Int)
    }
    private var itemLongClickListener: IKotlinItemLongClickListener? = null
    fun setOnItemLongClickListener(listener: IKotlinItemLongClickListener) {
        this.itemLongClickListener = listener
    }
    interface IKotlinItemLongClickListener{
        fun onItemLongClick(position: Int,view: View)
    }

    override fun getItemCount(): Int = mNewsList.size
}