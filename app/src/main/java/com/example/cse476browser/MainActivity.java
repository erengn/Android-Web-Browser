package com.example.cse476browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BrowserTabAdapter.OnTabClickListener {

    private static final String HOME_PAGE = "https://www.google.com";

    private final ArrayList<BrowserTab> tabs = new ArrayList<>();
    private int currentTabIndex = -1;

    private BrowserTabAdapter tabAdapter;
    private RecyclerView tabList;
    private EditText addressBar;
    private TextView pageTitle;
    private TextView tabCountText;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ProgressBar progressBar;
    private FrameLayout webViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabList = findViewById(R.id.tabRecyclerView);
        addressBar = findViewById(R.id.addressBar);
        pageTitle = findViewById(R.id.pageTitleText);
        tabCountText = findViewById(R.id.tabCountText);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        progressBar = findViewById(R.id.pageProgressBar);
        webViewContainer = findViewById(R.id.webViewContainer);
        Button goButton = findViewById(R.id.goButton);
        Button newTabButton = findViewById(R.id.newTabButton);
        ImageButton reloadButton = findViewById(R.id.reloadButton);

        tabAdapter = new BrowserTabAdapter(tabs, this);
        tabList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tabList.setAdapter(tabAdapter);

        goButton.setOnClickListener(v -> loadUrl(addressBar.getText().toString()));
        newTabButton.setOnClickListener(v -> createNewTab(HOME_PAGE));

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterPressed = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                loadUrl(addressBar.getText().toString());
                return true;
            }
            return false;
        });

        backButton.setOnClickListener(v -> {
            if (getCurrentWebView().canGoBack()) {
                getCurrentWebView().goBack();
            }
        });
        forwardButton.setOnClickListener(v -> {
            if (getCurrentWebView().canGoForward()) {
                getCurrentWebView().goForward();
            }
        });
        reloadButton.setOnClickListener(v -> getCurrentWebView().reload());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getCurrentWebView().canGoBack()) {
                    getCurrentWebView().goBack();
                } else {
                    finish();
                }
            }
        });

        createNewTab(HOME_PAGE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTabIndex >= 0) {
            getCurrentWebView().onResume();
        }
    }

    @Override
    protected void onPause() {
        if (currentTabIndex >= 0) {
            getCurrentWebView().onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (BrowserTab tab : tabs) {
            tab.getWebView().destroy();
        }
        tabs.clear();
        super.onDestroy();
    }

    private void createNewTab(String url) {
        WebView webView = createWebView();
        BrowserTab tab = new BrowserTab(webView);
        webView.setWebViewClient(new TabWebViewClient(tab));
        webView.setWebChromeClient(new TabChromeClient(tab));

        tabs.add(tab);
        tabAdapter.notifyItemInserted(tabs.size() - 1);

        tab.setUrl(url);
        webView.loadUrl(url);
        switchToTab(tabs.size() - 1);
        updateTabCount();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView() {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        return webView;
    }

    private void switchToTab(int index) {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            getCurrentWebView().onPause();
        }

        currentTabIndex = index;
        WebView webView = getCurrentWebView();

        webViewContainer.removeAllViews();
        webViewContainer.addView(webView);
        webView.onResume();

        tabAdapter.setSelectedPosition(index);
        tabList.scrollToPosition(index);

        addressBar.clearFocus();
        hideKeyboard();
        updateAddressBar();
        updatePageInfo();
    }

    private void closeTab(int index) {
        BrowserTab tab = tabs.remove(index);
        webViewContainer.removeView(tab.getWebView());
        tab.getWebView().destroy();
        tabAdapter.notifyItemRemoved(index);

        if (tabs.isEmpty()) {
            currentTabIndex = -1;
            createNewTab(HOME_PAGE);
        } else if (index == currentTabIndex) {
            currentTabIndex = -1;
            switchToTab(Math.min(index, tabs.size() - 1));
        } else {
            if (index < currentTabIndex) {
                currentTabIndex--;
            }
            tabAdapter.setSelectedPosition(currentTabIndex);
        }
        updateTabCount();
    }

    private void updateTabCount() {
        tabCountText.setText(getString(R.string.tab_count, tabs.size()));
    }

    private void loadUrl(String input) {
        String url = formatUrl(input);
        if (url == null) {
            return;
        }
        BrowserTab tab = tabs.get(currentTabIndex);
        tab.setUrl(url);
        tab.getWebView().loadUrl(url);

        addressBar.clearFocus();
        hideKeyboard();
        updateAddressBar();
    }

    private String formatUrl(String input) {
        String text = input.trim();
        if (text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return text;
        }
        if (!text.contains(" ") && text.contains(".")) {
            return "https://" + text;
        }
        return "https://www.google.com/search?q=" + Uri.encode(text);
    }

    private boolean isCurrentTab(BrowserTab tab) {
        return currentTabIndex >= 0 && currentTabIndex < tabs.size() && tabs.get(currentTabIndex) == tab;
    }

    private WebView getCurrentWebView() {
        return tabs.get(currentTabIndex).getWebView();
    }

    private void updateAddressBar() {
        if (currentTabIndex < 0 || addressBar.hasFocus()) {
            return;
        }
        String url = tabs.get(currentTabIndex).getUrl();
        addressBar.setText(url != null ? url : "");
    }

    private void updatePageInfo() {
        BrowserTab tab = tabs.get(currentTabIndex);
        String title = tab.getTitle();
        pageTitle.setText(title != null ? title : "");

        WebView webView = tab.getWebView();
        backButton.setEnabled(webView.canGoBack());
        backButton.setAlpha(webView.canGoBack() ? 1f : 0.3f);
        forwardButton.setEnabled(webView.canGoForward());
        forwardButton.setAlpha(webView.canGoForward() ? 1f : 0.3f);
    }

    private void onTabChanged(BrowserTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) {
            return;
        }
        tabAdapter.notifyItemChanged(index);
        if (index == currentTabIndex) {
            updateAddressBar();
            updatePageInfo();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
    }

    @Override
    public void onTabClick(int position) {
        if (position != currentTabIndex) {
            switchToTab(position);
        }
    }

    @Override
    public void onTabClose(int position) {
        closeTab(position);
    }

    private class TabWebViewClient extends WebViewClient {
        private final BrowserTab tab;

        TabWebViewClient(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            return !(url.startsWith("http://") || url.startsWith("https://"));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            tab.setUrl(url);
            tab.setFavicon(favicon);
            if (isCurrentTab(tab)) {
                progressBar.setVisibility(View.VISIBLE);
            }
            onTabChanged(tab);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            tab.setUrl(url);
            tab.setTitle(view.getTitle());
            if (isCurrentTab(tab)) {
                progressBar.setVisibility(View.GONE);
            }
            onTabChanged(tab);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            tab.setUrl(url);
            onTabChanged(tab);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                Toast.makeText(MainActivity.this, R.string.page_load_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class TabChromeClient extends WebChromeClient {
        private final BrowserTab tab;

        TabChromeClient(BrowserTab tab) {
            this.tab = tab;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            tab.setTitle(title);
            onTabChanged(tab);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            tab.setFavicon(icon);
            onTabChanged(tab);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (isCurrentTab(tab)) {
                progressBar.setProgress(newProgress);
            }
        }
    }
}
