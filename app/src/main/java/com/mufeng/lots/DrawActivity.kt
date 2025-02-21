package com.mufeng.lots

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.res.ResourcesCompat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.mufeng.lots.databinding.ActivityDrawBinding
import java.io.File
import kotlin.random.Random
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class DrawActivity: ComponentActivity() {
    private lateinit var binding: ActivityDrawBinding
    private lateinit var lotteryManager: LotteryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // 初始化抽签界面，默认纯文本
        lotteryManager = LotteryManager(LotteryType.TEXT.strategy)
        lotteryManager.safeChangeStrategy(LotteryType.TEXT.strategy, binding.lotteryContainer)
        setupStrategySelector()

        //设置抽卡界面背景
        var inputStream = assets.open("background_hutao.jpg")
        var bitmap = BitmapFactory.decodeStream(inputStream)
        binding.Layout.background = BitmapDrawable(resources,bitmap)
        binding.Layout.background.alpha = 128

        val data = intent.getStringExtra("main_data")
        // 解析 JSON 字符串为 User 类型的对象
        val objectmapper = jacksonObjectMapper()
        val basepath =Environment.getExternalStorageDirectory().path+"/Dfap"
        Log.d("Log--------->>>>",basepath)
        val user = objectmapper.readTree(File(
            (getExternalFilesDir(null)?.path + "/card_data/" + data)
        ))
        binding.button1.setOnClickListener {
//            binding.textView.text = user["info"]["title"].asText()

        }
        binding.button2.setOnClickListener {
            lotteryManager.executeStrategy(binding.lotteryContainer, this, randomReplace(user))
        }
        binding.button3.setOnClickListener {
        }




    }


    private fun randomReplace(data: JsonNode): String {
        // 随机选择data["data"]中的一项
        val selectedItems = data["data"] as ArrayNode
        val selectedItem = selectedItems[Random.nextInt(selectedItems.size())].asText()
        // 定义一个递归函数用于替换{}中的内容
        fun replaceBraces(item: String): String {
            // 查找{}中的键
            val start = item.indexOf("{")
            val end = item.indexOf("}")

            // 如果没有找到{}，直接返回item
            if (start == -1 || end == -1) {
                return item
            }
            // 获取{}中的键
            val key = item.substring(start + 1, end)

            // 检查键是否存在于data中
            if (data.has(key)) {
                // 从data中获取键对应的值列表，并随机选择一个值
                val values = data[key] as ArrayNode
                val value = values[Random.nextInt(values.size())].asText()


                // 替换{}中的内容
                val newItem = item.substring(0, start) + value + item.substring(end + 1)

                // 递归替换剩余的{}内容
                return replaceBraces(newItem)
            } else {
                // 如果找不到键对应的值，返回原item并打印错误信息
                println("Error: Key '$key' not found in data.")
                return item
            }
        }

        // 调用递归函数进行替换
        val result = replaceBraces(selectedItem)

        return result
    }


    // 策略选择器UI
    private fun setupStrategySelector() {

        binding.strategyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when(checkedId) {
                R.id.radio_text -> LotteryType.TEXT
                R.id.radio_tube -> LotteryType.TUBE
                R.id.radio_roulette -> LotteryType.ROULETTE
                else -> throw IllegalArgumentException("未知选项")
            }

            // 切换策略时的UI更新
            lotteryManager.safeChangeStrategy(selectedType.strategy, binding.lotteryContainer)
        }
    }
}