package io.livekit.android.compose.meet

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.compose.meet.ui.theme.LKMeetAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LKMeetAppTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    WebViewScreen()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebViewScreen() {
        // 用于处理屏幕共享权限的请求
        var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

        // 注册屏幕共享权限回调
        val mediaProjectionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // 用户同意了屏幕共享
                pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
            } else {
                // 用户拒绝了屏幕共享
                pendingPermissionRequest?.deny()
            }
            pendingPermissionRequest = null
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true

                    webViewClient = WebViewClient()

                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            if (request == null) return

                            // 检查是否包含屏幕共享的权限请求
                            val resources = request.resources
                            val wantsScreenShare = resources.contains("android.webkit.resource.DISPLAY_CAPTURE")

                            if (wantsScreenShare) {
                                // 拦截屏幕共享，手动触发安卓系统的录屏权限申请
                                pendingPermissionRequest = request
                                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            } else {
                                // 如果是普通的麦克风、摄像头请求，直接放行
                                request.grant(resources)
                            }
                        }
                    }

                    // 填入你的网址
                    loadUrl("https://tianlintong1.duckdns.org/")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
