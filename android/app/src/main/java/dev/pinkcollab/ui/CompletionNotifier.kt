package dev.pinkcollab.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.pinkcollab.R

/**
 * 完成通知：会话一轮任务结束时，若 App 不在前台，弹系统通知 + 震动。
 *
 * Android 13+ 需 POST_NOTIFICATIONS 运行时权限；无权限时静默跳过（不崩）。
 */
internal object CompletionNotifier {
    private const val ChannelId = "pinkcollab-completion"
    private const val NotificationId = 9001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ChannelId) != null) return
        val channel = NotificationChannel(
            ChannelId,
            "任务完成",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "会话任务完成时提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 40, 60, 40)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** 发送完成通知（带震动）。无权限或渠道不可用时静默返回。 */
    fun notify(context: Context, title: String, message: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 40, 60, 40))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NotificationId, notification)
        }
    }

    /** 申请通知权限（Android 13+）。 */
    fun requestPermissionIfNeeded(activity: android.app.Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPermission(activity)) return
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            4201,
        )
    }
}
