package dev.pinkcollab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings

/**
 * 后台保活前台服务（社区标准做法，GKD 同款思路）：
 * 退后台后持有一个静音低优先级常驻通知，把进程优先级提到前台服务级，
 * 避免 ColorOS 等厂商 ROM 的 freezer 冻结进程导致网关 WebSocket 断开。
 *
 * 服务本身不管理连接（WebSocket 由 Repository 持有），只负责进程优先级；
 * Android 14+ 使用 specialUse 类型，避开 dataSync 的 6 小时时长上限。
 */
class ConnectionKeeperService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "连接保活", NotificationManager.IMPORTANCE_MIN).apply {
                        setShowBadge(false)
                        enableVibration(false)
                        setSound(null, null)
                    },
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            androidx.core.app.NotificationCompat.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("PinkCollab")
            .setContentText("保持会话连接")
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "connection-keeper"
        private const val NOTIFICATION_ID = 41

        fun start(context: Context) {
            val intent = Intent(context, ConnectionKeeperService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionKeeperService::class.java))
        }
    }
}

/** 保活开关的应用逻辑与厂商白名单引导，供设置页与主界面调用。 */
internal object ConnectionKeeper {
    fun start(context: Context) = ConnectionKeeperService.start(context)

    fun stop(context: Context) = ConnectionKeeperService.stop(context)

    /** 按偏好启停前台服务；enabled=true 且尚未忽略电池优化时顺带申请豁免。 */
    fun apply(context: Context, enabled: Boolean) {
        if (enabled) start(context) else stop(context)
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            // 个别 ROM 禁用该入口时退回常规电池优化列表。
            runCatching {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
