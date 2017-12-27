package com.mysoftsource.rxandroidlogger.sample;

import android.app.Application;

import com.mysoftsource.rxandroidlogger.LogSetup;
import com.mysoftsource.rxandroidlogger.TBPLogHelper;

import timber.log.Timber;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogSetup.Builder options = new LogSetup.Builder();
        options.setDropboxAccessToken("JM5srPDuSpMAAAAAAAAUmLljWVbGxYfdy7T3W_Dsh1Sv8B7-7H87QTgdWghuQAR-");
        options.setDropboxPath("/TPBLog");
        TBPLogHelper.create(this, options.build());

        Timber.plant(TBPLogHelper.getInstance().getTBPDebugTree());
    }
}
