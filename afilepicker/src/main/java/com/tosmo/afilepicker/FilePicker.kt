package com.tosmo.filepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.zlylib.fileselectorlib.FileSelector
import com.zlylib.fileselectorlib.SelectCreator
import com.zlylib.fileselectorlib.SelectOptions
import com.zlylib.fileselectorlib.bean.EssFile
import com.zlylib.fileselectorlib.ui.FileSelectorActivity
import com.zlylib.fileselectorlib.utils.Const
import java.io.File

/**
 * @author Thomas Miao
 *
 * 文件选择器
 * <pre><code>
 * 使用参考：
 * class MainActivity: AppCompactActivity {
 *     val filePicker = FilePicker.Builder(this).setSingle().build()
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *             ...
 *         filePicker.register { ... }
 *         filePicker.pick()
 *     }
 * }
 * </code><pre>
 * @property register 注册获取到文件后所要做的事
 * @property unregister 与[register]对应
 * @property pick 开始选择文件
 */
class FilePicker private constructor(
    private val mInvoker: Any,
    private val mSetters: Collection<() -> Unit>
) {
    companion object {
        /**
         * 存储的根目录
         * @see Environment.getExternalStorageDirectory
         */
        val STORAGE_PATH = Environment.getExternalStorageDirectory().toString() + "/"
    }
    
    enum class SortType {
        /**
         * 文件名正序
         */
        BY_NAME_ASC,
        
        /**
         * 文件名反序
         */
        BY_NAME_DESC,
        
        /**
         * 最后修改时间正序
         */
        BY_TIME_ASC,
        
        /**
         * 最后修改时间反序
         */
        BY_TIME_DESC,
        
        /**
         * 文件大小正序
         */
        BY_SIZE_ASC,
        
        /**
         * 文件大小反序
         */
        BY_SIZE_DESC,
        
        /**
         * 文件后缀名正序
         */
        BY_EXTENSION_ASC,
        
        /**
         * 文件反缀名反序
         */
        BY_EXTENSION_DESC
    }
    
    
    /**
     * 文件选择器的构造者，通过此类可构造出拥有指定配置的文件选择器
     */
    class Builder private constructor(private val mCreator: SelectCreator) {
        private lateinit var mInvoker: Any
        
        private val mSetterMap = mutableMapOf<String, () -> Unit>()
        
        constructor(activity: AppCompatActivity) : this(FileSelector.from(activity)) {
            mInvoker = activity
        }
        
        constructor(fragment: Fragment) : this(FileSelector.from(fragment)) {
            mInvoker = fragment
        }
        
        /**
         * 根据提供的设置返回一个文件选择器
         */
        fun build(): FilePicker {
            return FilePicker(mInvoker, mSetterMap.values)
        }
        
        /**
         * 选择的最大数量，[maxCount]必须大于0
         */
        fun setMaxCount(maxCount: Int): Builder {
            if (maxCount > 0) {
                mSetterMap["setMaxCount"] = { mCreator.setMaxCount(maxCount) }
            }
            return this
        }
        
        /**
         * 目标路径
         */
        fun setTargetPath(path: String): Builder {
            mSetterMap["setTargetPath"] = { mCreator.setTargetPath(path) }
            return this
        }
        
        /**
         * 过滤文件后缀
         */
        fun setFileTypes(vararg fileTypes: String): Builder {
            mSetterMap["setFileTypes"] = { mCreator.setFileTypes(*fileTypes) }
            return this
        }
        
        /**
         * 排序
         */
        fun setSortType(sortType: SortType): Builder {
            mSetterMap["setSortType"] = { mCreator.setSortType(sortType.ordinal) }
            return this
        }
        
        /**
         * 选择单个文件
         */
        fun setSingle(): Builder {
            mSetterMap["setSingle"] = { mCreator.isSingle }
            return this
        }
        
        /**
         * 只显示文件夹
         */
        fun onlyShowFolder(): Builder {
            mSetterMap["onlyShowFolder"] = { mCreator.onlyShowFolder() }
            return this
        }
        
        /**
         * 只选择文件夹
         */
        fun onlySelectFolder(): Builder {
            mSetterMap["onlySelectFolder"] = { mCreator.onlySelectFolder() }
            return this
        }
        
        /**
         * 标题背景色
         */
        fun setTilteBg(@ColorInt color: Int): Builder {
            mSetterMap["setTitleBg"] = { mCreator.setTilteBg(color) }
            return this
        }
        
        /**
         * 标题前景色
         */
        fun setTitleColor(@ColorInt color: Int): Builder {
            mSetterMap["setTitleColor"] = { mCreator.setTitleColor(color) }
            return this
        }
        
        fun setTitleLeftColor(@ColorInt color: Int): Builder {
            mSetterMap["setTitleLeftColor"] = { mCreator.setTitleLiftColor(color) }
            return this
        }
        
        fun setTitleRightColor(@ColorInt color: Int): Builder {
            mSetterMap["setTitleRightColor"] = { mCreator.setTitleRightColor(color) }
            return this
        }
    }
    
    /**
     * 选择文件契约对象
     */
    private val mContract = object : ActivityResultContract<String, List<File>>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(context, FileSelectorActivity::class.java)
        }
        
        override fun parseResult(resultCode: Int, intent: Intent?): List<File> {
            return if (resultCode == Activity.RESULT_OK && intent != null) {
                when (SelectOptions.getInstance().isSingle) {
                    true -> intent.getParcelableArrayListExtra<EssFile>(
                        Const.EXTRA_RESULT_SELECTION
                    )?.map { it.file }
                    else -> intent.getStringArrayListExtra(
                        Const.EXTRA_RESULT_SELECTION
                    )?.map { File(it) }
                } ?: emptyList()
            } else emptyList()
        }
    }
    
    private lateinit var mLauncher: ActivityResultLauncher<String>
    
    private var mIsRegistered = false
    
    /**
     * 注册调用约定，只能在[Activity.onCreate]中调用，[callback]将在选择文件后调用
     */
    fun register(callback: (files: List<File>) -> Unit) {
        if (!mIsRegistered) {
            mLauncher = when (mInvoker) {
                is AppCompatActivity -> mInvoker
                is Fragment -> mInvoker
                else -> null
            }!!.registerForActivityResult(mContract) {
                callback(it)
            }
            mIsRegistered = true
        }
    }
    
    /**
     * 注销调用约定
     */
    fun unregister() {
        if (mIsRegistered) {
            mLauncher.unregister()
            mIsRegistered = false
        }
    }
    
    /**
     * 开始选择文件
     */
    fun pick() {
        if (mIsRegistered) {
            mSetters.forEach { it() }
            mLauncher.launch("")
        }
    }
}