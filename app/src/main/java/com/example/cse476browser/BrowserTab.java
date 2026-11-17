package com.example.cse476browser;

import android.graphics.Bitmap;
import android.webkit.WebView;

public class BrowserTab {
    private final WebView webView;
    private String url;
    private String title;
    private Bitmap favicon;

    public BrowserTab(WebView webView) {
        this.webView = webView;
    }

    public WebView getWebView() {
        return webView;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getFavicon() {
        return favicon;
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;
    }
}
