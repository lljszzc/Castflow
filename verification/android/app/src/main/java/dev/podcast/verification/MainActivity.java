package dev.podcast.verification;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.view.View;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.regex.*;
import org.json.*;

/** Authentication experiment only. All account reads below use native HTTP. */
public class MainActivity extends Activity {
    private WebView web;
    private TextView status;
    private Button check;
    private final java.util.concurrent.ExecutorService io = Executors.newSingleThreadExecutor();
    private static final String ORIGIN = "https://podcasts.apple.com";

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 36, 20, 20);
        status = new TextView(this);
        status.setText("登录后点击验证。结果仅显示状态与数量，凭据不写入日志。\n验证工具：尚未代表完整应用。重新打开后可验证会话是否保留。");
        root.addView(status);
        check = new Button(this);
        check.setText("使用当前会话验证原生资料库接口");
        root.addView(check);
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                UriPolicy policy = new UriPolicy(request.getUrl().toString());
                return !policy.allowed;
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
            }
        });
        web.loadUrl(ORIGIN + "/cn/new");
        check.setOnClickListener(v -> verify());
        if (getIntent().getBooleanExtra("autoVerify", false)) verify();
    }

    private void verify() {
        String cookies = CookieManager.getInstance().getCookie(ORIGIN);
        String token = null;
        if (cookies != null) for (String cookie : cookies.split(";")) {
            String item = cookie.trim();
            if (item.startsWith("media-user-token=")) token = item.substring("media-user-token=".length());
        }
        if (token == null || token.isEmpty()) {
            status.setText("尚未找到登录会话。请在下方 Apple 页面完成登录后重试。");
            return;
        }
        final String user = token;
        check.setEnabled(false);
        status.setText("正在通过原生 HTTPS 请求验证……");
        io.execute(() -> {
            String result;
            try {
                String html = get(ORIGIN + "/cn/new", null, null).body;
                Matcher script = Pattern.compile("<script[^>]+src=\"(/assets/index[^\"]+\\.js)\"").matcher(html);
                if (!script.find()) throw new IOException("Public script unavailable");
                String js = get(ORIGIN + script.group(1), null, null).body;
                Matcher jwt = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matcher(js);
                if (!jwt.find()) throw new IOException("Public token unavailable");
                String bearer = jwt.group();
                StringBuilder out = new StringBuilder("原生接口验证结果\n");
                String[] paths = {"storefront", "library/podcasts?limit=5", "library/podcast-episodes?limit=5", "recent/played/podcast-episodes?limit=5", "continue-playing/podcast-episodes?limit=5"};
                for (String path : paths) {
                    Response r = get("https://amp-api.podcasts.apple.com/v1/me/" + path, bearer, user);
                    out.append(path.split("\\?")[0]).append(": ").append(r.code);
                    if (r.code == 200) {
                        JSONArray data = new JSONObject(r.body).optJSONArray("data");
                        out.append("，数量 ").append(data == null ? "未知" : data.length());
                    }
                    out.append('\n');
                }
                out.append("本工具未修改资料库。令牌刷新、付费内容及播放器尚未验证。");
                result = out.toString();
            } catch (Exception e) {
                result = "验证未完成：" + e.getClass().getSimpleName() + "。可检查网络后重试。";
            }
            final String message = result;
            runOnUiThread(() -> { status.setText(message); check.setEnabled(true); });
        });
    }

    private static Response get(String url, String bearer, String user) throws Exception {
        URL address = new URL(url);
        if (!address.getProtocol().equals("https") || !(address.getHost().equals("podcasts.apple.com") || address.getHost().equals("amp-api.podcasts.apple.com"))) throw new IOException("Host rejected");
        HttpURLConnection connection = (HttpURLConnection) address.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(25000);
        if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        if (user != null) connection.setRequestProperty("media-user-token", user);
        connection.setRequestProperty("Origin", ORIGIN);
        try {
            int code = connection.getResponseCode();
            InputStream stream = code < 400 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return new Response(code, "");
            try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 5_000_000) throw new IOException("Response too large");
                    output.write(buffer, 0, count);
                }
                return new Response(code, output.toString("UTF-8"));
            }
        } finally { connection.disconnect(); }
    }
    private static class Response {
        final int code; final String body;
        Response(int code, String body) { this.code = code; this.body = body; }
    }
    private static class UriPolicy {
        final boolean allowed;
        UriPolicy(String value) {
            android.net.Uri uri = android.net.Uri.parse(value);
            String host = uri.getHost();
            allowed = "https".equals(uri.getScheme()) && host != null && (host.equals("apple.com") || host.endsWith(".apple.com"));
        }
    }
    @Override protected void onDestroy() {
        web.destroy(); io.shutdownNow(); super.onDestroy();
    }
}
