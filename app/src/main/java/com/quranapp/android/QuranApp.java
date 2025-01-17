package com.quranapp.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatDelegate;

import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.app.NotificationUtils;

import java.util.Objects;

public class QuranApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        initBeforeBaseAttach(base);
        super.attachBaseContext(base);
    }

    private void initBeforeBaseAttach(Context base) {
        updateTheme(base);
    }

    private void updateTheme(Context base) {
        AppCompatDelegate.setDefaultNightMode(AppUtils.resolveThemeModeFromSP(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createNotificationChannels(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String process = getProcessName();
            if (!Objects.equals(getPackageName(), process)) WebView.setDataDirectorySuffix(process);
        }
    }
}