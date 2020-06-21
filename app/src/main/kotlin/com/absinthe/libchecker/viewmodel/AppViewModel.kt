package com.absinthe.libchecker.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.Configuration
import com.absinthe.libchecker.api.request.ConfigurationRequest
import com.absinthe.libchecker.bean.AppItem
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.*
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.database.LCItem
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.AppUtils
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val dbItems: LiveData<List<LCItem>>
    val libReference: MutableLiveData<List<LibReference>> = MutableLiveData()
    val clickBottomItemFlag: MutableLiveData<Boolean> = MutableLiveData(false)
    var isInit = false

    private val tag = AppViewModel::class.java.simpleName
    private val repository: LCRepository

    init {
        val lcDao = LCDatabase.getDatabase(application).lcDao()
        repository = LCRepository(lcDao)
        dbItems = repository.allItems
    }

    fun initItems(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "initItems")

        repository.deleteAllItems()

        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        val newItems = ArrayList<AppItem>()
        var packageInfo: PackageInfo
        var versionCode: Long
        var appItem: AppItem
        var lcItem: LCItem

        for (info in appList) {
            try {
                packageInfo = PackageUtils.getPackageInfo(info)
                versionCode = PackageUtils.getVersionCode(packageInfo)

                appItem = AppItem().apply {
                    icon = info.loadIcon(context.packageManager)
                    appName = info.loadLabel(context.packageManager).toString()
                    packageName = info.packageName
                    versionName = PackageUtils.getVersionString(packageInfo)
                    abi = PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir)
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
                    updateTime = packageInfo.lastUpdateTime
                }
                lcItem = LCItem(
                    info.packageName,
                    info.loadLabel(context.packageManager).toString(),
                    packageInfo.versionName ?: "",
                    versionCode,
                    packageInfo.firstInstallTime,
                    packageInfo.lastUpdateTime,
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                    PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort(),
                    PackageUtils.isSplitsApk(packageInfo),
                    PackageUtils.isKotlinUsed(packageInfo)
                )

                GlobalValues.isShowSystemApps.value?.let {
                    if (it || (!it && !lcItem.isSystem)) {
                        newItems.add(appItem)
                    }
                }

                insert(lcItem)
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }

        //Sort
        when (GlobalValues.appSortMode.value) {
            Constants.SORT_MODE_DEFAULT -> newItems.sortWith(compareBy({ it.abi }, { it.appName }))
            Constants.SORT_MODE_UPDATE_TIME_DESC -> newItems.sortByDescending { it.updateTime }
        }

        withContext(Dispatchers.Main) {
            GlobalValues.isObservingDBItems.value = true
            AppItemRepository.allItems.value = newItems
        }
    }

    fun addItem() = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "addItems")

        dbItems.value?.let { value ->
            val newItems = ArrayList<AppItem>()
            var appItem: AppItem

            for (item in value) {
                try {
                    appItem = AppItem().apply {
                        icon = AppUtils.getAppIcon(item.packageName)
                            ?: ColorDrawable(Color.TRANSPARENT)
                        appName = item.label
                        packageName = item.packageName
                        versionName = PackageUtils.getVersionString(PackageUtils.getPackageInfo(item.packageName))
                        abi = item.abi.toInt()
                        isSystem = item.isSystem
                        updateTime = item.lastUpdatedTime
                    }

                    GlobalValues.isShowSystemApps.value?.let {
                        if (it || (!it && !item.isSystem)) {
                            newItems.add(appItem)
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            when (GlobalValues.appSortMode.value) {
                Constants.SORT_MODE_DEFAULT -> newItems.sortWith(
                    compareBy(
                        { it.abi },
                        { it.appName })
                )
                Constants.SORT_MODE_UPDATE_TIME_DESC -> newItems.sortByDescending { it.updateTime }
            }

            withContext(Dispatchers.Main) {
                AppItemRepository.allItems.value = newItems
            }
        }
    }

    fun requestChange(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        Log.d(tag, "requestChange")

        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)

        dbItems.value?.let { value ->
            var packageInfo: PackageInfo
            var versionCode: Long
            var lcItem: LCItem

            for (dbItem in value) {
                try {
                    appList.find { it.packageName == dbItem.packageName }?.let {
                        packageInfo = PackageUtils.getPackageInfo(it)
                        versionCode = PackageUtils.getVersionCode(packageInfo)

                        if (packageInfo.lastUpdateTime != dbItem.lastUpdatedTime) {
                            lcItem = LCItem(
                                it.packageName,
                                it.loadLabel(context.packageManager).toString(),
                                packageInfo.versionName ?: "null",
                                versionCode,
                                packageInfo.firstInstallTime,
                                packageInfo.lastUpdateTime,
                                (it.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                                PackageUtils.getAbi(it.sourceDir, it.nativeLibraryDir).toShort(),
                                PackageUtils.isSplitsApk(packageInfo),
                                PackageUtils.isKotlinUsed(packageInfo)
                            )
                            update(lcItem)
                        }

                        appList.remove(it)
                    } ?: run {
                        delete(dbItem)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    continue
                }
            }

            for (info in appList) {
                try {
                    packageInfo = PackageUtils.getPackageInfo(info)
                    versionCode = PackageUtils.getVersionCode(packageInfo)

                    lcItem = LCItem(
                        info.packageName,
                        info.loadLabel(context.packageManager).toString(),
                        packageInfo.versionName ?: "null",
                        versionCode,
                        packageInfo.firstInstallTime,
                        packageInfo.lastUpdateTime,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
                        PackageUtils.getAbi(info.sourceDir, info.nativeLibraryDir).toShort(),
                        PackageUtils.isSplitsApk(packageInfo),
                        PackageUtils.isKotlinUsed(packageInfo)
                    )

                    insert(lcItem)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    continue
                }
            }
        }
        GlobalValues.shouldRequestChange = false
    }

    fun collectPopularLibraries(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val appList = context.packageManager
            .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
        val map = HashMap<String, Int>()
        var libList: List<LibStringItem>
        var count: Int

        for (item in appList) {
            libList = PackageUtils.getNativeDirLibs(
                item.sourceDir,
                item.nativeLibraryDir
            )

            for (lib in libList) {
                count = map[lib.name] ?: 0
                map[lib.name] = count + 1
            }
        }

        for (entry in map) {
            if (entry.value > 3) {
                val properties: MutableMap<String, String> = java.util.HashMap()
                properties["Library name"] = entry.key
                properties["Library count"] = entry.value.toString()

                Analytics.trackEvent("Library", properties)
            }
        }
    }

    fun requestConfiguration() = viewModelScope.launch(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiManager.root)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val request = retrofit.create(ConfigurationRequest::class.java)
        val config = request.requestConfiguration()
        config.enqueue(object : Callback<Configuration> {
            override fun onFailure(call: Call<Configuration>, t: Throwable) {
                Log.e(tag, t.message ?: "")
            }

            override fun onResponse(call: Call<Configuration>, response: Response<Configuration>) {
                viewModelScope.launch(Dispatchers.Main) {
                    response.body()?.let {
                        Log.d(tag, "Configuration response: ${response.body()}")
                        GlobalValues.config = it
                    } ?: Log.e(tag, response.message())
                }
            }
        })
    }

    data class RefCountType(
        val count: Int,
        val type: LibReferenceActivity.Type
    )

    fun computeLibReference(context: Context, flag: LibReferenceActivity.Type) =
        viewModelScope.launch(Dispatchers.IO) {
            val appList = context.packageManager
                .getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES)
            val map = HashMap<String, RefCountType>()
            val refList = mutableListOf<LibReference>()
            val showSystem = GlobalValues.isShowSystemApps.value!!

            var libList: List<LibStringItem>
            var packageInfo: PackageInfo
            var count: Int

            when (flag) {
                LibReferenceActivity.Type.TYPE_ALL -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        libList = PackageUtils.getNativeDirLibs(
                            item.sourceDir,
                            item.nativeLibraryDir
                        )

                        for (lib in libList) {
                            count = map[lib.name]?.count ?: 0
                            map[lib.name] =
                                RefCountType(count + 1, LibReferenceActivity.Type.TYPE_NATIVE)
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_SERVICES
                                )

                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_SERVICE
                                    )
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_ACTIVITIES
                                )
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_ACTIVITY
                                    )
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_RECEIVERS
                                )
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                                    )
                                }
                            }

                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_PROVIDERS
                                )
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                LibReferenceActivity.Type.TYPE_NATIVE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        libList = PackageUtils.getNativeDirLibs(
                            item.sourceDir,
                            item.nativeLibraryDir
                        )

                        for (lib in libList) {
                            count = map[lib.name]?.count ?: 0
                            map[lib.name] =
                                RefCountType(count + 1, LibReferenceActivity.Type.TYPE_NATIVE)
                        }
                    }
                }
                LibReferenceActivity.Type.TYPE_SERVICE -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_SERVICES
                                )

                            packageInfo.services?.let {
                                for (service in it) {
                                    count = map[service.name]?.count ?: 0
                                    map[service.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_SERVICE
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                LibReferenceActivity.Type.TYPE_ACTIVITY -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_ACTIVITIES
                                )
                            packageInfo.activities?.let {
                                for (activity in it) {
                                    count = map[activity.name]?.count ?: 0
                                    map[activity.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_ACTIVITY
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_RECEIVERS
                                )
                            packageInfo.receivers?.let {
                                for (receiver in it) {
                                    count = map[receiver.name]?.count ?: 0
                                    map[receiver.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> {
                    for (item in appList) {

                        if (!showSystem && ((item.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                            continue
                        }

                        try {
                            packageInfo =
                                context.packageManager.getPackageInfo(
                                    item.packageName,
                                    PackageManager.GET_PROVIDERS
                                )
                            packageInfo.providers?.let {
                                for (provider in it) {
                                    count = map[provider.name]?.count ?: 0
                                    map[provider.name] = RefCountType(
                                        count + 1,
                                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            for (entry in map) {
                if (entry.value.count >= GlobalValues.libReferenceThreshold.value!! && entry.key.isNotBlank()) {

                    val libMap: BaseMap? = when (entry.value.type) {
                        LibReferenceActivity.Type.TYPE_NATIVE -> NativeLibMap
                        LibReferenceActivity.Type.TYPE_SERVICE -> ServiceLibMap
                        LibReferenceActivity.Type.TYPE_ACTIVITY -> ActivityLibMap
                        LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> ReceiverLibMap
                        LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> ProviderLibMap
                        else -> null
                    }
                    val chip = libMap?.getChip(entry.key)

                    refList.add(
                        LibReference(entry.key, chip, entry.value.count, entry.value.type)
                    )
                }
            }

            refList.sortByDescending { it.referredCount }

            withContext(Dispatchers.Main) {
                libReference.value = refList
            }
        }

    fun refreshRef() {
        libReference.value?.let { ref ->
            libReference.value =
                ref.filter { it.referredCount >= GlobalValues.libReferenceThreshold.value!! }
        }
    }

    private fun insert(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(item)
    }

    private fun update(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(item)
    }

    private fun delete(item: LCItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(item)
    }
}