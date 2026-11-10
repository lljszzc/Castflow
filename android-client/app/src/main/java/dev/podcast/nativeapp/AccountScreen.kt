package dev.podcast.nativeapp
import android.webkit.*
import dev.chrisbanes.haze.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

@Composable fun AccountScreen(signedIn: Boolean, region: String, api: Api, getToken: () -> String?, onClose: () -> Unit, onSignedIn: () -> Unit, onSignOut: () -> Unit) {
    var web by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    BackHandler { if(web) web = false else onClose() }
    val glass = remember { HazeState() }
    CompositionLocalProvider(LocalGlassState provides glass) {
    Surface(Modifier.fillMaxSize(),color = Color.White,contentColor = Color.Black) {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFE7F1FA),Color(0xFFF5F4FA),Color.White))).hazeSource(glass))
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),verticalAlignment = Alignment.CenterVertically) {
                SoftIconButton(onClick = { if(web) web = false else onClose() }) { LocalizedIcon(Icons.AutoMirrored.Rounded.ArrowBack,"返回") }
                LocalizedText("Apple 账户",Modifier.weight(1f),fontSize = 20.sp,fontWeight = FontWeight.SemiBold)
                if(web) SoftTextButton(onClick = { checking = true; error = null; scope.launch { try { if(getToken().isNullOrBlank()) error("请先在 Apple 页面完成登录"); api.accountRegion(); onSignedIn() } catch(e: Exception) { error = e.message ?: "暂时无法确认登录，请重试" } finally { checking = false } } }, enabled = !checking) { LocalizedText(if(checking) "验证中" else "完成") }
            }
            if(web) {
                if(progress < 100 || checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { LocalizedText(it,Modifier.padding(16.dp),color = Color(0xFFBA2541)); SoftTextButton(onClick = { reload++; error = null }) { LocalizedText("重新载入") } }
                key(reload) { AndroidView(factory = { context -> WebView(context).apply {
                    configureAppleLogin(settings)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
                    webChromeClient = object : WebChromeClient() { override fun onProgressChanged(view: WebView?, value: Int) { progress = value } }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.url.let { it.scheme != "https" || !(it.host == "apple.com" || it.host?.endsWith(".apple.com") == true) }
                        override fun onPageFinished(view: WebView,url: String) { CookieManager.getInstance().flush() }
                        override fun onReceivedError(view: WebView,request: WebResourceRequest,e: WebResourceError) { if(request.isForMainFrame) error = "Apple 登录页面无法载入，请检查网络后重试" }
                    }; loadUrl("https://podcasts.apple.com/cn/new")
                } },modifier = Modifier.weight(1f),onRelease = { it.destroy() }) }
            } else Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment = Alignment.Center) {
                Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(84.dp).background(Color(0xFFE7F3FF),CircleShape),contentAlignment = Alignment.Center) { LocalizedIcon(Icons.Rounded.Podcasts,null,Modifier.size(44.dp),tint = Color(0xFF008CFF)) }
                LocalizedText(if(signedIn) "已连接 Apple 播客" else "连接你的播客资料库",Modifier.padding(top = 28.dp),fontSize = 27.sp,fontWeight = FontWeight.Bold)
                LocalizedText(if(signedIn) "账户地区：${region.uppercase()}" else "登录后继续收听，查看关注的节目和已保存单集。",Modifier.padding(vertical = 16.dp),color = Color.Gray)
                if(!signedIn) LocalizedText("接下来将在 Apple 官方页面完成登录。",color = Color.Gray,fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                SoftFilledButton(onClick = { web = true },Modifier.fillMaxWidth().height(52.dp)) { LocalizedText(if(signedIn) "管理 Apple 账户" else "继续登录",fontSize = 17.sp) }
                if(signedIn) SoftTextButton(onClick = onSignOut,Modifier.padding(top = 14.dp)) { LocalizedText("退出登录",color = Color(0xFFBA2541)) }
                SoftTextButton(onClick = onClose) { LocalizedText(if(signedIn) "返回收听" else "暂不登录") }
                }
                }
            }
        }
    }
}
}

// Apple sign-in requires JavaScript. Navigation is limited to Apple HTTPS origins;
// local content access, mixed content and JavaScript bridges are unavailable.
@android.annotation.SuppressLint("SetJavaScriptEnabled")
private fun configureAppleLogin(settings: WebSettings) {
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.domStorageEnabled = true
    settings.javaScriptEnabled = true
}
