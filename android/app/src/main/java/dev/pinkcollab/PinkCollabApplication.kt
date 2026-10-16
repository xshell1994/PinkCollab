package dev.pinkcollab

import android.app.Application
import android.content.Intent
import dev.pinkcollab.data.CredentialStore
import dev.pinkcollab.data.GatewayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 进程级依赖宿主。
 *
 * [repository] 挂在 [applicationScope]（而非某个 ViewModel 的 viewModelScope）上，因此
 * 只要进程存活，WebSocket 连接、自动重连与协议状态就不会因 Activity/ViewModel 销毁而中断——
 * 这是「App 放后台仍保持连接」的前提。配合前台服务 [ConnectionService]，系统会尽量保留进程。
 */
class PinkCollabApplication : Application() {
    /** 进程级作用域：连接与协议状态不随界面生命周期取消。 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 全进程共享的网关仓库单例。 */
    internal val repository: GatewayRepository by lazy {
        GatewayRepository(applicationScope, CredentialStore(this), this)
    }

    override fun onCreate() {
        super.onCreate()
        // 触发仓库初始化：加载已配对主机并建立连接。
        repository
        // 常驻前台服务，降低后台被系统回收/冻结的概率。
        runCatching { startForegroundService(Intent(this, ConnectionService::class.java)) }
    }
}
