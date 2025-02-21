package com.mufeng.lots

import FileUtils
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.mufeng.lots.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            val menu = popupMenu {
                section {
                    title = "Options"
                    item {
                        label = "新建牌堆"
//                        icon = R.drawable.abc_ic_menu_copy_mtrl_am_alpha
                        callback = {
                            val intent = Intent()
                            intent.setClass(this@MainActivity, CreateActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }

            menu.show(this,view)
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // 将每个菜单 ID 作为一组 ID 传递，因为每个菜单都应被视为顶级目标。
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_Lots, R.id.nav_gallery, R.id.nav_slideshow),
            binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)


        //onCreate结束
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_import ->{
                return true
            }
            R.id.nav_create ->{
                val intent = Intent()
                intent.setClass(this@MainActivity, CreateActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

fun isAppFirstLaunch(context: Context): Boolean {
    // 获取SharedPreferences对象
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    // 从SharedPreferences中读取isFirstLaunch的值，如果不存在则返回true
    val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)
    // 如果是第一次启动，更新isFirstLaunch为false
    if (isFirstLaunch) {
        sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
    }
    Log.d("Log--------->>>>",isFirstLaunch.toString())
    return isFirstLaunch
}

