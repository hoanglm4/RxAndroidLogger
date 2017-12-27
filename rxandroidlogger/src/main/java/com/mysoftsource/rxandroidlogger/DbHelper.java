package com.mysoftsource.rxandroidlogger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import rx.Observable;
import rx.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;

class DbHelper {
    private static final String TAG = DbHelper.class.getCanonicalName();
    private static final String INTERNAL_CACHE_SUB_DIR = "tbp_log_internal";
    private static final String INTERNAL_CACHE_FILE_NAME = "TBPLog.txt";
    private static final String EXTERNAL_CACHE_SUB_DIR = "tbp_log_external";
    private static final String EXTERNAL_FILE_NAME = "TBPLog_%1s_%2s_%3s_%4d_%5s_.txt";

    private static final long LIMIT_SAVE_LOG_TIME = 2 * 24 * 60 * 60 * 1000; // 2day
    private static final long PREF_DEFAULT_VALUE = -1;
    private static final String PREF_NAME = "tbp-log";
    private static final String PREF_CREATE_LOG_KEY = "pref_create_log_key";

    private static DbHelper sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final File mInternalFile;
    private long mLogCreatedLastTime;

    synchronized static void create(Context context, File defaultInternalFile) {
        if (sInstance == null) {
            sInstance = new DbHelper(context, defaultInternalFile);
        }
    }

    static DbHelper getInstance() {
        return sInstance;
    }

    private DbHelper(Context context, File defaultInternalFile) {
        mContext = context;
        mInternalFile = (defaultInternalFile == null) ? getInternalCacheFile(context) : defaultInternalFile;
        mPrefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        mLogCreatedLastTime = getLogCreatedLastTime();
        if (mLogCreatedLastTime == PREF_DEFAULT_VALUE) {
            saveLogCreatedLastTime();
        }
    }

    void saveLog(String logText) {
        Observable.defer(() -> {
            clearInternalLogFileIfNeed();

            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
            try {
                fw = new FileWriter(mInternalFile, true);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw);
                out.println(logText);
            } catch (IOException e) {
                Log.e(TAG, "saveLog>> is failed, cause = " + e);
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(bw);
                IOUtils.closeQuietly(fw);
            }
            return Observable.just(true);
        })
                .subscribeOn(Schedulers.io())
                .subscribe(success -> {
                    // do nothing
                }, throwable -> {
                    // do nothing
                    Log.e(TAG, "saveLog>> is failed, cause = " + throwable);
                });
    }

    Observable<File> getOnFileLog() {
        return Observable.defer(() -> {
            File outFile = getExternalLogFile();
            Log.i(TAG, "getOnFileLog>> before size = " + outFile.length());
            try {
                FileUtils.copyFile(mInternalFile, outFile);
            } catch (IOException e) {
                return Observable.error(e);
            }
            Log.i(TAG, "getOnFileLog after size = " + outFile.length());
            return Observable.just(outFile);
        })
                .doOnNext(outFile -> clearInternalLogFile())
                .subscribeOn(Schedulers.io());
    }

    private void clearInternalLogFile() {
        saveLogCreatedLastTime();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(mInternalFile);
            writer.print("");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "clearInternalLogFile>> is failed, cause = " + e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private void clearInternalLogFileIfNeed() {
        if (Math.abs(System.currentTimeMillis() - mLogCreatedLastTime) < LIMIT_SAVE_LOG_TIME) {
            return;
        }
        clearInternalLogFile();
    }

    private long getLogCreatedLastTime() {
        return mPrefs.getLong(PREF_CREATE_LOG_KEY, PREF_DEFAULT_VALUE);
    }

    private void saveLogCreatedLastTime() {
        mLogCreatedLastTime = System.currentTimeMillis();
        mPrefs.edit().putLong(PREF_CREATE_LOG_KEY, mLogCreatedLastTime).apply();
    }

    private File getInternalCacheFile(Context context) {
        final String cachePath = context.getCacheDir().getPath();
        File subFile = new File(cachePath + File.separator + INTERNAL_CACHE_SUB_DIR);
        subFile.mkdirs();
        return new File(subFile, INTERNAL_CACHE_FILE_NAME);
    }

    private File getExternalLogFile() {
        File externalStorageDir = new File(Environment.getExternalStorageDirectory(), String.format(Locale.US, EXTERNAL_FILE_NAME,
                Build.MODEL,
                Build.MANUFACTURER,
                getVersionName(),
                Build.VERSION.SDK_INT,
                DateTimeUtil.getCurrentTime()));
        return externalStorageDir;
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
