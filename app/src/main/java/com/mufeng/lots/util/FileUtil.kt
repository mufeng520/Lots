import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件读写工具类，支持动态权限申请、错误处理及版本兼容
 * @param activity 上下文需为 Activity，用于权限申请回调
 */
class FileUtils(private val activity: AppCompatActivity) {

    // 定义权限请求合约
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // 初始化权限请求回调
    init {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("FileUtils", "权限已授予")
            } else {
                Log.w("FileUtils", "权限被拒绝")
            }
        }
    }

    /**
     * 写入文件（自动处理权限和路径）
     * @param filePath 文件路径（内部存储无需权限，外部存储需权限）
     * @param content 写入内容
     * @param isExternal 是否写入外部存储
     */
    fun writeFile(filePath: String, content: String, isExternal: Boolean = true) {
        try {
            if (isExternal && !checkExternalStoragePermission()) {
                // 请求权限并保存待执行操作（此处简化逻辑，实际需队列管理）
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                throw FileException.PermissionDeniedException("需要存储权限")
            }

            val file = if (isExternal) {
                activity.getExternalFilesDir(null)?.apply {
                    if (!exists()) mkdirs()
                }?: throw FileException.DirectoryUnavailableException("外部存储不可用")
            } else {
                File(activity.filesDir, filePath) // 内部存储路径
            }

            // 创建父目录（如果不存在）
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()

            FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            Log.i("FileUtils", "文件写入成功: ${file.absolutePath}")

        } catch (e: Exception) {
            handleFileException(e, "写入文件失败")
        }
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @param isExternal 是否外部存储
     */
    fun readFile(filePath: String, isExternal: Boolean = true): String? {
        return try {
            if (isExternal && !checkExternalStoragePermission()) {
                throw FileException.PermissionDeniedException("需要存储权限")
            }

            val file = if (isExternal) {
                activity.getExternalFilesDir(null)?.apply {
                    if (!exists()) mkdirs()
                }?: throw FileException.DirectoryUnavailableException("外部存储不可用")
            } else {
                File(activity.filesDir, filePath)
            }

            if (!file.exists()) {
                throw FileException.FileNotFoundException("文件不存在")
            }

            file.readText()
        } catch (e: Exception) {
            handleFileException(e, "读取文件失败")
            null
        }
    }

    fun getFileList(filePath: String, isExternal: Boolean = true): List<File>? {
        return try {
            val baseDir = if (isExternal) {
                // 应用私有外部存储目录（无需权限）
                activity.getExternalFilesDir(null) ?: throw FileException.DirectoryUnavailableException("外部存储不可用")
            } else {
                // 内部存储目录
                activity.filesDir
            }

            val targetDir = File(baseDir, filePath).apply {
                // 创建目录（如果不存在）
                if (!exists()) mkdirs()
                if (!isDirectory) throw FileException.NotADirectoryException("路径不是目录")
            }

            // 获取文件名列表（过滤隐藏文件）
//            targetDir.list()?.filterNot { it.startsWith(".") } ?: emptyList()
            targetDir.walk().toList()
        } catch (e: Exception) {
            handleFileException(e, "获取文件列表失败")
            null
        }
    }

    /**
     * 检查外部存储权限
     */
    private fun checkExternalStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 统一异常处理
     */
    private fun handleFileException(e: Exception, defaultMessage: String) {
        val exception = when (e) {
            is FileException -> e
            is SecurityException -> FileException.PermissionDeniedException("权限不足")
            is IOException -> FileException.IOExceptionWrapper("IO异常", e)
            else -> FileException.GenericException("未知错误: ${e.message}")
        }
        Log.e("FileUtils", exception.message ?: defaultMessage, e)
        showErrorToast(exception)
    }

    /**
     * 显示错误提示（可扩展为 Snackbar 或 Dialog）
     */
    private fun showErrorToast(exception: FileException) {
        val message = when (exception) {
            is FileException.PermissionDeniedException -> "请授予存储权限"
            is FileException.FileNotFoundException -> "文件不存在"
            else -> "操作失败，请重试"
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 辅助方法：URI 转实际路径
     */
    private fun uriToFilePath(uri: Uri): String? {
        val cursor = activity.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
}

/**
 * 自定义文件异常类（密封类）
 */
sealed class FileException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PermissionDeniedException(message: String) : FileException(message)
    class FileNotFoundException(message: String) : FileException(message)
    class DirectoryUnavailableException(message: String) : FileException(message)
    class FileWriteException(message: String, cause: Throwable? = null) : FileException(message, cause)
    class IOExceptionWrapper(message: String, cause: Throwable? = null) : FileException(message, cause)
    class GenericException(message: String) : FileException(message)
    class NotADirectoryException(message: String) : FileException(message)
}

val jsonstr = """{\"info\":{
                    \"title\":\"模板牌堆\",
                    \"author\":\"木风\",
                    \"version\": \"1.0.0\",
                    \"explain\":\"牌堆示例，info字段和data字段必须有\"
                },
                \"data\":[
                    \"{aaa}and{bbb}{df}\",
                    \"{aaa}or{bbb}\"
                ],
                \"aaa\":[
                    \"{bbb}\",
                    \"b\",
                    \"c\"
                ],
                \"bbb\":[
                    \"{ccc}\",
                    \"{2}\",
                    \"3\"
                ],
                \"ccc\":[
                    \"{2}\",
                    \"{3}\",
                    \"4\"
                ],
                \"2\": [
                    \"到此为止\"
                ]}"""