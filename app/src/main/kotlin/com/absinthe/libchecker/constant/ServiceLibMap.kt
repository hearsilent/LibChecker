package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object ServiceLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("com.xiaomi.push.service.XMPushService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.push.service.XMJobService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.PushMessageHandler", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.MessageHandleService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.amap.api.location.APSService", LibChip(R.drawable.ic_lib_amap, "高德地图 SDK")),
        Pair("com.tencent.bugly.beta.tinker.TinkerResultService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchForeService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchService\$InnerService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.DefaultTinkerResultService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.google.firebase.messaging.FirebaseMessagingService", LibChip(R.drawable.ic_lib_firebase, "FCM")),
        Pair("com.huawei.hms.support.api.push.service.HmsMsgService", LibChip(R.drawable.ic_lib_huawei, "Huawei Push")),
        Pair("com.qq.e.comm.DownloadService", LibChip(R.drawable.ic_lib_tencent, "腾讯广告 SDK")),
        Pair("com.vivo.push.sdk.service.CommandClientService", LibChip(R.drawable.ic_lib_vivo, "vivo Push")),
        Pair("com.heytap.mcssdk.PushService", LibChip(R.drawable.ic_lib_oppo, "OPPO Push")),
        Pair("com.heytap.mcssdk.AppPushService", LibChip(R.drawable.ic_lib_oppo, "OPPO Push")),
        Pair("com.taobao.accs.ChannelService", LibChip(R.drawable.ic_lib_taobao, "淘宝 Push")),
        Pair("com.taobao.accs.ChannelService\$KernelService", LibChip(R.drawable.ic_lib_taobao, "淘宝 Push")),
        Pair("com.taobao.accs.data.MsgDistributeService", LibChip(R.drawable.ic_lib_taobao, "淘宝 Push")),
        Pair("org.android.agoo.accs.AgooService", LibChip(R.drawable.ic_lib_taobao, "淘宝 Push")),
        Pair("com.taobao.accs.internal.AccsJobService", LibChip(R.drawable.ic_lib_taobao, "淘宝 Push")),
        Pair("com.tencent.android.tpush.service.XGPushServiceV3", LibChip(R.drawable.ic_lib_tencent, "信鸽推送")),
        Pair("com.tencent.android.tpush.service.XGPushService", LibChip(R.drawable.ic_lib_tencent, "信鸽推送")),
        Pair("com.tencent.android.tpush.rpc.XGRemoteService", LibChip(R.drawable.ic_lib_tencent, "信鸽推送")),
        Pair("com.umeng.update.net.DownloadingService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.UmengService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.XiaomiIntentService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.UmengDownloadResourceService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.UmengIntentService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.UmengMessageIntentReceiverService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.umeng.message.UmengMessageCallbackHandlerService", LibChip(R.drawable.ic_lib_umeng, "友盟推送")),
        Pair("com.meizu.cloud.pushsdk.NotificationService", LibChip(R.drawable.ic_lib_meizu, "Meizu Push")),
        Pair("androidx.work.impl.background.systemjob.SystemJobService", LibChip(R.drawable.ic_lib_jetpack, "Jetpack Work")),
        Pair("androidx.work.impl.foreground.SystemForegroundService", LibChip(R.drawable.ic_lib_jetpack, "Jetpack Work")),
        Pair("androidx.room.MultiInstanceInvalidationService", LibChip(R.drawable.ic_lib_jetpack, "Jetpack Room")),
        Pair("cn.jpush.android.service.PushService", LibChip(R.drawable.ic_lib_jpush, "极光推送")),
        Pair("cn.jpush.android.service.DownloadService", LibChip(R.drawable.ic_lib_jpush, "极光推送")),
        Pair("cn.jpush.android.service.DaemonService", LibChip(R.drawable.ic_lib_jpush, "极光推送")),
        Pair("com.huawei.agconnect.core.ServiceDiscovery", LibChip(R.drawable.ic_lib_huawei, "AppGallery Connect")),
        Pair("com.bytedance.sdk.openadsdk.multipro.aidl.BinderPoolService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.ss.android.socialbase.downloader.notification.DownloadNotificationService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.ss.android.socialbase.downloader.downloader.DownloadService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.ss.android.socialbase.downloader.downloader.IndependentProcessDownloadService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.ss.android.socialbase.appdownloader.DownloadHandlerService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.ss.android.socialbase.impls.DownloadHandlerService", LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")),
        Pair("com.tencent.qalsdk.service.QalService", LibChip(R.drawable.ic_lib_tencent_cloud, "腾讯云通信")),
        Pair("com.tencent.qalsdk.service.QalAssistService", LibChip(R.drawable.ic_lib_tencent_cloud, "腾讯云通信"))
        )
}