package pe.verderuta.ecomovil

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader

/**
 * EcoMovil para Android.
 *
 * Muestra el frontend de EcoMovil (carpeta ecomovil-web del repositorio),
 * empaquetado dentro del APK. Las páginas se sirven desde
 * http://appassets.androidplatform.net/assets/ y el frontend detecta ese host
 * para usar el API Gateway en http://10.0.2.2:8080, que es el localhost de la
 * PC vista desde el emulador de Android Studio.
 *
 * En un celular físico, cambiar la URL del servidor desde la pantalla de
 * inicio de sesión por la IP de la PC en la red local (ej. http://192.168.1.20:8080).
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        val assetLoader = WebViewAssetLoader.Builder()
            .setHttpAllowed(true)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            }
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(START_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    companion object {
        private const val START_URL = "http://appassets.androidplatform.net/assets/index.html"
    }
}
