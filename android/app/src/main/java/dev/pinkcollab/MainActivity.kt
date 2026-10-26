package dev.pinkcollab

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import dev.pinkcollab.ui.CollabViewModel
import dev.pinkcollab.ui.PinkCollabApp
import dev.pinkcollab.ui.AppPreferences

class MainActivity : ComponentActivity() {
    private val vm: CollabViewModel by viewModels()
    private var keeperEnabled = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The UI is dark-only (black + purple), so system bars always get light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { PinkCollabApp() }
    }

    override fun onStart() {
        super.onStart()
        keeperEnabled = AppPreferences(this).keeperEnabled
        // 回前台立即探活：进程解冻后不等 60s 兜底 ticker（reconnectAll 自带活跃过滤）。
        runCatching { vm.reconnectAllHosts() }
    }

    override fun onStop() {
        super.onStop()
        if (keeperEnabled) ConnectionKeeper.start(this) else ConnectionKeeper.stop(this)
    }
}
