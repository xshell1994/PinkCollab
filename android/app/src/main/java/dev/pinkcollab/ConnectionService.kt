package dev.pinkcollab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * 常驻前台服务：让 App 退到后台后仍保持与各主机的实时连接。
 *
 * 连接本身由 [PinkCollabApplication.repository] 的进程级作用域持有，本服务不直接管理 socket，
 * 只提供一条常驻通知把进程提升为前台服务，避免系统在后台冻结/回收进程导致连接中断。
 */
class ConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NotificationId, buildNotification(), type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ChannelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(ChannelId, "后台连接", NotificationManager.IMPORTANCE_MIN).apply {
                description = "保持与主机的实时连接"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("PinkCollab")
            .setContentText("保持与主机的连接")
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private companion object {
        const val ChannelId = "pinkcollab-connection"
        const val NotificationId = 9000
    }
}
