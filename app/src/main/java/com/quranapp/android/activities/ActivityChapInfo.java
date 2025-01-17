package com.quranapp.android.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.Log;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.ActivityChapterInfoBinding;
import com.quranapp.android.exc.NoInternetException;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.chapterInfo.ChapterInfoJSInterface;
import com.quranapp.android.utils.chapterInfo.ChapterInfoUtils;
import com.quranapp.android.utils.chapterInfo.ChapterInfoWebViewClient;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.widgets.PageAlert;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ActivityChapInfo extends ReaderPossessingActivity {
    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>();
    private ActivityChapterInfoBinding mBinding;
    public QuranMeta mQuranMeta;
    public QuranMeta.ChapterMeta mChapterInfoMeta;
    private FileUtils fileUtils;
    public String mLanguage;
    private PageAlert mPageAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_chapter_info;
    }

    @Override
    protected void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        fileUtils = FileUtils.newInstance(this);

        mBinding = ActivityChapterInfoBinding.bind(activityView);
        initThis();
    }

    @Override
    protected void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        mQuranMeta = mQuranMetaRef.get();

        initContent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initContent(intent);
    }

    private void initContent(Intent intent) {
        int DEFAULT_CHAPTER_INFO = -1;
        int chapterNo = -1;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            try {
                Uri data = intent.getData();
                List<String> pathSegments = data.getPathSegments();
                chapterNo = Integer.parseInt(pathSegments.get(1));

                String lang = data.getQueryParameter("language");
                if (lang == null) {
                    lang = data.getQueryParameter("lang");
                }
                mLanguage = lang;
            } catch (Exception ignored) {
                invalidParams();
            }
        } else {
            chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, DEFAULT_CHAPTER_INFO);
            mLanguage = intent.getStringExtra(Keys.KEY_LANGUAGE);
        }

        if (!QuranMeta.isChapterValid(chapterNo)) {
            invalidParams();
            return;
        }

        mChapterInfoMeta = mQuranMeta.getChapterMeta(chapterNo);
        if (mChapterInfoMeta == null) {
            invalidParams();
            return;
        }

        initWebView();
        loadContent();
    }

    private void initThis() {
        mBinding.title.setText(R.string.strTitleAboutSurah);
        mBinding.back.setOnClickListener(v -> finish());
    }

    private void initPageAlert() {
        mPageAlert = new PageAlert(this);
    }

    private void initWebView() {
        setupWebView(mBinding.webView);
    }

    private void loadContent() {
        if (TextUtils.isEmpty(mLanguage) || (!"en".equalsIgnoreCase(mLanguage) && !"ur".equalsIgnoreCase(mLanguage))) {
            mLanguage = "en";
        }
        mTaskRunner.callAsync(new LoadChapterInfoTask(mLanguage));
        //        renderDataTest();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.addJavascriptInterface(new ChapterInfoJSInterface(this), "ChapterInfoJSInterface");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String msg = "[" + consoleMessage.lineNumber() + "] " + consoleMessage.message();
                Log.d(msg);
                Logger.logMsg(msg);
                return true;
            }
        });
        webView.setWebViewClient(new ChapterInfoWebViewClient(this) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoader();
            }
        });
    }

    public String getBoilerPlateHTML() {
        return ResUtils.readAssetsTextFile(this, "chapter_info/chapter_info_page.html");
    }

    private void renderDataTest() {
        String boilerPlateHTML = getBoilerPlateHTML();
        mBinding.webView.loadData(boilerPlateHTML, "text/html", "UTF-8");
    }

    private void renderData(String rawData) throws JSONException {
        JSONObject jsonObject = new JSONObject(rawData);
        String data = jsonObject.getJSONObject("chapter_info").getString("text");
        String boilerPlateHTML = getBoilerPlateHTML();
        data = String.format(boilerPlateHTML, resolveDarkMode(), data);
        mBinding.webView.loadDataWithBaseURL(null, data, "text/html; charset=UTF-8", "utf-8", null);
    }

    private String resolveDarkMode() {
        return WindowUtils.isNightMode(this) ? "dark" : "light";
    }

    private void loadFailed() {
        hideLoader();
        if (mPageAlert == null) {
            initPageAlert();
        }

        mPageAlert.setMessage(R.string.strTitleOops, R.string.strMsgChapInfoFailedLoad);
        mPageAlert.setActionButton(R.string.strLabelRetry, this::loadContent);
        mPageAlert.show(mBinding.container);

        deleteSavedFileIfExists();
    }

    private void invalidParams() {
        hideLoader();
        if (mPageAlert == null) {
            initPageAlert();
        }

        mPageAlert.setMessage("Invalid params", null);
        mPageAlert.setActionButton(null, null);
        mPageAlert.show(mBinding.container);

        deleteSavedFileIfExists();
    }

    private void noInternet() {
        if (mPageAlert == null) {
            initPageAlert();
        }
        mPageAlert.setupForNoInternet(this::loadContent);
        mPageAlert.show(mBinding.container);
    }

    private void deleteSavedFileIfExists() {
        if (mLanguage != null && mChapterInfoMeta != null) {
            File chapterInfoFile = fileUtils.getChapterInfoFile(mLanguage, mChapterInfoMeta.chapterNo);
            chapterInfoFile.delete();
        }
    }

    public void showLoader() {
        mBinding.loader.setVisibility(View.VISIBLE);
    }

    public void hideLoader() {
        mBinding.loader.setVisibility(View.GONE);
    }

    private class LoadChapterInfoTask extends BaseCallableTask<String> {
        private final File chapterInfoFile;
        private final String lang;

        public LoadChapterInfoTask(String lang) {
            this.lang = lang;
            chapterInfoFile = fileUtils.getChapterInfoFile(lang, mChapterInfoMeta.chapterNo);
        }

        @Override
        public void preExecute() {
            if (mPageAlert != null) {
                mPageAlert.remove();
            }
            showLoader();
        }

        @Override
        public String call() throws Exception {
            if (chapterInfoFile.exists()) {
                String read = FileUtils.readTextFromFile(chapterInfoFile);
                if (read.length() > 0) {
                    return read;
                }
            } else {
                fileUtils.createFile(chapterInfoFile);
            }

            if (!NetworkStateReceiver.isNetworkConnected(ActivityChapInfo.this)) {
                throw new NoInternetException();
            }

            String urlStr = ChapterInfoUtils.prepareChapterInfoUrl(lang, mChapterInfoMeta.chapterNo);
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Length", "0");
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(180000);
            conn.setReadTimeout(180000);
            conn.setAllowUserInteraction(false);
            conn.connect();

            InputStreamReader isr = new InputStreamReader(conn.getInputStream());

            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            br.close();
            conn.disconnect();

            String data = sb.toString();
            FileUtils.writeTextIntoFile(chapterInfoFile, data);
            return data;
        }

        @Override
        public void onComplete(String result) {
            if (result == null) {
                chapterInfoFile.delete();
                loadFailed();
                return;
            }
            try {
                renderData(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailed(@NonNull @NotNull Exception e) {
            e.printStackTrace();
            if (e instanceof NoInternetException || e.getCause() instanceof NoInternetException) {
                chapterInfoFile.delete();
                noInternet();
            } else {
                loadFailed();
            }
        }
    }
}