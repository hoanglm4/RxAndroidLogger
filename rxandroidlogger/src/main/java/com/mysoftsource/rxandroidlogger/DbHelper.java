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
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;

class DbHelper {
    private static final String TAG = DbHelper.class.getCanonicalName();
    private static final String INTERNAL_CACHE_SUB_DIR = "tbp_log_internal";
    private static final String INTERNAL_CACHE_FILE_NAME = "TBPLog.txt";
    private static final String EXTERNAL_CACHE_SUB_DIR = "tbp_log_external";
    private static final String EXTERNAL_FILE_NAME = "TBPLog_%1s_%2s_%3s_%4d_%5s_.txt";

    private static final long MAXIMUM_FILE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int LIMIT_SPLIT_FILE_LOG = 3;
    private static final long PREF_DEFAULT_VALUE = -1;
    private static final String PREF_NAME = "tbp-log";
    private static final String PREF_CREATE_LOG_KEY = "pref_create_log_key";

    private static DbHelper sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final File mInternalFile;
    private final LogSetup mOptions;
    private final AtomicBoolean mIsReading = new AtomicBoolean(false);
    private final AtomicBoolean mIsWriting = new AtomicBoolean(false);
    private final Queue<String> mLogTextQueue = new ConcurrentLinkedQueue<>();

    private long mLogCreatedLastTime;

    synchronized static void create(Context context, LogSetup logSetup) {
        if (sInstance == null) {
            sInstance = new DbHelper(context, logSetup);
        }
    }

    static DbHelper getInstance() {
        return sInstance;
    }

    private DbHelper(Context context, LogSetup logSetup) {
        mContext = context;
        mOptions = logSetup;
        mInternalFile = (logSetup.localFilePath == null) ? getInternalCacheFile(context) : logSetup.localFilePath;
        mPrefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        mLogCreatedLastTime = getLogCreatedLastTime();
        if (mLogCreatedLastTime == PREF_DEFAULT_VALUE) {
            saveLogCreatedLastTime();
        }
    }

    void processSaveLog(String logText) {
        if (logText != null) {
            mLogTextQueue.add(logText);
        }

        if (mLogTextQueue.isEmpty()) {
            return;
        }

        if (mIsReading.get()) {
            return;
        }

        if (mIsWriting.get()) {
            return;
        }
        mIsWriting.set(true);
        String previousLogText = mLogTextQueue.poll();
        copyExternalAndClearLatestFileLogObservable()
                .flatMap(success -> saveLogObservable(previousLogText))
                .onErrorReturn(throwable -> {
                    Log.e(TAG, "processSaveLog>> " + throwable);
                    return false;
                })
                .doOnNext(success -> {
                    mIsWriting.set(false);
                    releaseWriting();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> processSaveLog(null));
    }

    Observable<File> getAndCopyExternalLatestFileLog() {
        if (mIsReading.compareAndSet(false, true)) {
            final File outFile = getExternalLogFile();
            return Observable.defer(() -> {
                Log.i(TAG, "getAndCopyExternalLatestFileLog>> before size = " + outFile.length());
                waitWritingRelease();
                try {
                    FileUtils.copyFile(mInternalFile, outFile);
                } catch (IOException e) {
                    Log.e(TAG, "getAndCopyExternalLatestFileLog is failed, error = " + e);
                    return Observable.error(e);
                }
                Log.i(TAG, "getAndCopyExternalLatestFileLog after size = " + outFile.length());
                return Observable.just(outFile);
            })
                    .doOnNext(file -> {
                        clearInternalLogFile();
                        mIsReading.set(false);
                    })
                    .doOnError(throwable -> {
                        Log.e(TAG, "getAndCopyExternalLatestFileLog>> error = " + throwable);
                        FileUtil.deleteWithoutException(outFile);
                        mIsReading.set(false);
                    })
                    .subscribeOn(Schedulers.io());
        } else {
            return Observable.error(new Throwable("Logcat file is getting!"));
        }
    }

    Observable<File> getAllExternalFileLog() {
        return getAndCopyExternalLatestFileLog()
                .flatMap(latestFile -> {
                    File externalDir = getExternalDir();
                    File[] fList = externalDir.listFiles();
                    return Observable.from(fList);
                });
    }

    private void waitWritingRelease() {
        synchronized (mIsWriting) {
            if (mIsWriting.get()) {
                try {
                    mIsWriting.wait(5000L);
                } catch (InterruptedException e) {
                    Log.e(TAG, "waitWritingRelease>> waiting is release" + e);
                } catch (IllegalMonitorStateException e) {
                    Log.e(TAG, "waitWritingRelease>> waiting is release" + e);
                }
            }
        }
    }

    private void releaseWriting() {
        synchronized (mIsWriting) {
            try {
                mIsWriting.notifyAll();
            } catch (IllegalMonitorStateException e) {
                // Do nothing
            }
        }
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

    private Observable<Boolean> saveLogObservable(String logText) {
        return Observable.defer(() -> {
            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
            try {
                fw = new FileWriter(mInternalFile, true);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw);
                out.println(logText);
            } catch (IOException e) {
                Log.e(TAG, "saveLogObservable>> is failed, cause = " + e);
                return Observable.error(e);
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(bw);
                IOUtils.closeQuietly(fw);
            }
            return Observable.just(true);
        }).subscribeOn(Schedulers.io());
    }

    private Observable<Boolean> copyExternalAndClearLatestFileLogObservable() {
        if (mInternalFile.length() < MAXIMUM_FILE_SIZE) {
            return Observable.just(false);
        }
        deletePreviousFileIfNeed();
        return getAndCopyExternalLatestFileLog()
                .doOnNext(file -> Log.i(TAG, "new file is created, fileName = " + file.getAbsolutePath()))
                .map(file -> true)
                .onErrorReturn(throwable -> {
                    Log.e(TAG, "copyExternalAndClearLatestFileLogObservable>> " + throwable);
                    return false;
                })
                .doOnNext(success -> Log.i(TAG, "copyExternalAndClearLatestFileLogObservable>> copy external file log is success = " + success));
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
        File externalStorageDir = new File(getExternalDir(), String.format(Locale.US, EXTERNAL_FILE_NAME,
                Build.MODEL,
                Build.MANUFACTURER,
                getVersionName(),
                Build.VERSION.SDK_INT,
                DateTimeUtil.getCurrentTime()));
        return externalStorageDir;
    }

    private void deletePreviousFileIfNeed() {
        if (mOptions.isDeletePreviousFileLog == false) {
            return;
        }
        File externalDir = getExternalDir();
        File[] files = externalDir.listFiles();
        if (files == null || files.length < LIMIT_SPLIT_FILE_LOG) {
            return;
        }
        Log.i(TAG, "deletePreviousFileIfNeed>> size of external file before delete = " + files.length);
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        int indexEnd = files.length + 1 - LIMIT_SPLIT_FILE_LOG;
        for (int i = 0; i < indexEnd; i++) {
            FileUtil.deleteWithoutException(files[i]);
        }
        File externalDirAfterDeleted = getExternalDir();
        Log.i(TAG, "deletePreviousFileIfNeed>> size of external file after delete = " + (externalDirAfterDeleted.listFiles() != null ? externalDirAfterDeleted.listFiles().length : 0));
    }

    private File getExternalDir() {
        File externalDir = new File(Environment.getExternalStorageDirectory(), EXTERNAL_CACHE_SUB_DIR);
        externalDir.mkdirs();
        return externalDir;
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
