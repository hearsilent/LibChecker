package com.absinthe.libchecker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.text.toSpannable
import androidx.fragment.app.FragmentActivity
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.SystemServices
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.bean.LibStringItem
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.ui.detail.AppDetailActivity
import com.absinthe.libchecker.ui.detail.EXTRA_DETAIL_BEAN
import com.absinthe.libchecker.ui.detail.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.ui.fragment.detail.EXTRA_LC_ITEM
import com.absinthe.libchecker.ui.fragment.detail.OverlayDetailBottomSheetDialogFragment
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.isTempApk
import com.absinthe.libchecker.utils.extensions.toClassDefType
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule
import com.google.android.material.progressindicator.LinearProgressIndicator
import rikka.material.app.DayNightDelegate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LCAppUtils {

  fun getCurrentSeason(): Int {
    return when (Calendar.getInstance(Locale.getDefault()).get(Calendar.MONTH) + 1) {
      3, 4, 5 -> SPRING
      6, 7, 8 -> SUMMER
      9, 10, 11 -> AUTUMN
      12, 1, 2 -> WINTER
      else -> -1
    }
  }

  fun setTitle(context: Context): Spannable {
    val sb = SpannableStringBuilder(context.getString(R.string.app_name))
    val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    when {
      date.endsWith("1225") -> {
        sb.append("\uD83C\uDF84")
      }
      date == "20220131" -> {
        sb.append("\uD83C\uDFEE")
      }
      date == "20220201" -> {
        sb.append("\uD83D\uDC2F")
      }
    }

    if (BuildConfig.IS_DEV_VERSION) {
      val spanString = SpannableString("   ")
      val span = CenterAlignImageSpan(
        R.drawable.ic_ci_label.getDrawable(context)!!.also {
          it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
        }
      )
      spanString.setSpan(span, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      sb.append(spanString)
    }
    return sb.toSpannable()
  }

  fun getAppIcon(packageName: String): Drawable {
    return runCatching {
      PackageManagerCompat.getPackageInfo(
        packageName,
        0
      ).applicationInfo.loadIcon(SystemServices.packageManager)
    }.getOrDefault(ColorDrawable(Color.TRANSPARENT))
  }

  suspend fun getRuleWithRegex(
    name: String,
    @LibType type: Int,
    packageName: String? = null,
    nativeLibs: List<LibStringItem>? = null
  ): Rule? {
    val ruleEntity = LCRules.getRule(name, type, true) ?: return null
    if (type == NATIVE) {
      if (packageName == null) {
        return ruleEntity
      }
      val isApk = packageName.isTempApk()
      val source = if (isApk) {
        File(packageName)
      } else {
        runCatching {
          File(PackageUtils.getPackageInfo(packageName).applicationInfo.sourceDir)
        }.getOrNull()
      }
      if (source == null) {
        return ruleEntity
      }
      if (!checkNativeLibValidation(packageName, name, nativeLibs)) {
        return null
      }
      return ruleEntity
    } else {
      return ruleEntity
    }
  }

  private val checkNativeLibs =
    listOf("libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so", "libapp.so")

  fun checkNativeLibValidation(
    packageName: String,
    nativeLib: String,
    otherNativeLibs: List<LibStringItem>? = null
  ): Boolean {
    if (!checkNativeLibs.contains(nativeLib)) {
      return true
    }
    val source = File(PackageUtils.getPackageInfo(packageName).applicationInfo.sourceDir)
    return when (nativeLib) {
      "libjiagu.so", "libjiagu_a64.so", "libjiagu_x86.so", "libjiagu_x64.so" -> {
        runCatching {
          PackageUtils.findDexClasses(
            source,
            listOf(
              "com.qihoo.util.QHClassLoader".toClassDefType()
            )
          ).any { it == "com.qihoo.util.QHClassLoader".toClassDefType() }
        }.getOrDefault(false)
      }
      "libapp.so" -> {
        runCatching {
          otherNativeLibs?.any { it.name == "libflutter.so" } == true || PackageUtils.findDexClasses(
            source,
            listOf(
              "io.flutter.FlutterInjector".toClassDefType()
            )
          ).any { it == "io.flutter.FlutterInjector".toClassDefType() }
        }.getOrDefault(false)
      }
      else -> true
    }
  }

  fun getNightMode(): Int {
    return when (GlobalValues.darkMode) {
      Constants.DARK_MODE_OFF -> DayNightDelegate.MODE_NIGHT_NO
      Constants.DARK_MODE_ON -> DayNightDelegate.MODE_NIGHT_YES
      Constants.DARK_MODE_FOLLOW_SYSTEM -> DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> DayNightDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
  }

  fun launchDetailPage(
    context: FragmentActivity,
    item: LCItem,
    refName: String? = null,
    refType: Int = NATIVE
  ) {
    if (item.abi.toInt() == OVERLAY) {
      OverlayDetailBottomSheetDialogFragment().apply {
        arguments = bundleOf(
          EXTRA_LC_ITEM to item
        )
        show(
          context.supportFragmentManager,
          OverlayDetailBottomSheetDialogFragment::class.java.name
        )
      }
    } else {
      val intent = Intent(context, AppDetailActivity::class.java)
        .putExtras(
          bundleOf(
            EXTRA_PACKAGE_NAME to item.packageName,
            EXTRA_REF_NAME to refName,
            EXTRA_REF_TYPE to refType,
            EXTRA_DETAIL_BEAN to DetailExtraBean(
              item.features,
              item.variant
            )
          )
        )
      context.startActivity(intent)
    }
  }

  fun createLoadingDialog(context: ContextThemeWrapper): AlertDialog {
    return BaseAlertDialogBuilder(context)
      .setView(
        LinearProgressIndicator(context).apply {
          layoutParams = ViewGroup.LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
          }
          trackCornerRadius = 3.dp
          isIndeterminate = true
        }
      )
      .setCancelable(false)
      .create()
  }
}

/**
 * From drakeet
 */
fun doOnMainThreadIdle(action: () -> Unit) {
  val handler = Handler(Looper.getMainLooper())

  val idleHandler = MessageQueue.IdleHandler {
    handler.removeCallbacksAndMessages(null)
    action()
    return@IdleHandler false
  }

  fun setupIdleHandler(queue: MessageQueue) {
    queue.addIdleHandler(idleHandler)
  }

  if (Looper.getMainLooper() == Looper.myLooper()) {
    setupIdleHandler(Looper.myQueue())
  } else {
    setupIdleHandler(Looper.getMainLooper().queue)
  }
}
