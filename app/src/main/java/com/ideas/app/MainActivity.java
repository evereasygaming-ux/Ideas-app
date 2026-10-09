package com.ideas.app;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_URL = "http://localhost:3000";

    private WebView webView;
    private View overlay;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button retryButton;
    private Button diagButton;

    private Bootstrap bootstrap;
    private ServerManager serverManager;
    private boolean serverLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        overlay = findViewById(R.id.overlay);
        progressBar = findViewById(R.id.progress);
        statusText = findViewById(R.id.status);
        retryButton = findViewById(R.id.retry);
diagButton = findViewById(R.id.diagnostics);

        configureWebView();

        bootstrap = new Bootstrap(this);
        serverManager = new ServerManager(this);

        retryButton.setOnClickListener(v -> startFlow());
        if (diagButton != null) {
            diagButton.setOnClickListener(v -> exportDiagnostics());
        }

        startFlow();
    }

    private void startFlow() {
        retryButton.setVisibility(View.GONE);
        if (bootstrap.isReady()) {
            setStatus("Starting server...", 100);
            startServer();
        } else {
            runBootstrap();
        }
    }

    private void runBootstrap() {
        showOverlay(true);
        setStatus("Preparing IDEAS runtime...", 0);
        bootstrap.run(new Bootstrap.Listener() {
            @Override
            public void onProgress(int percent, String message) {
                runOnUiThread(() -> setStatus(message, percent));
            }

            @Override
            public void onFinished(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        setStatus("Starting server...", 100);
                        startServer();
                    } else {
                        setStatus("Setup failed: " + message, 0);
                        retryButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void startServer() {
        showOverlay(true);
        serverManager.start((state, message) -> runOnUiThread(() -> {
            switch (state) {
                case "running":
                    setStatus(message, 100);
                    if (!serverLoaded) {
                        serverLoaded = true;
                        webView.loadUrl(SERVER_URL);
                    }
                    break;
                case "stopped":
                    serverLoaded = false;
                    setStatus("Server stopped. " + message, 0);
                    retryButton.setVisibility(View.VISIBLE);
                    showOverlay(true);
                    break;
                case "error":
                default:
                    setStatus("Server error: " + message, 0);
                    retryButton.setVisibility(View.VISIBLE);
                    showOverlay(true);
                    break;
            }
        }));
    }

    private void setStatus(String message, int percent) {
        statusText.setText(message);
        if (percent > 0) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(percent);
        }
    }

    private void showOverlay(boolean visible) {
        if (overlay != null) {
            overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith(SERVER_URL)) {
                    showOverlay(false);
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (serverManager != null) {
            serverManager.stop();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
