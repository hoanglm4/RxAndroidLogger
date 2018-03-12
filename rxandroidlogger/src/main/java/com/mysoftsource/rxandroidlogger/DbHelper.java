package com.mysoftsource.rxandroidlogger;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class DbHelper {
    private static final long MAXIMUM_FILE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int LIMIT_SPLIT_FILE_LOG = 3;

    private static DbHelper sInstance;
    private final Context mContext;
    private final File mInternalFile;
    private final LogSetup mOptions;
    private final AtomicBoolean mIsReading = new AtomicBoolean(false);
    private final AtomicBoolean mIsWriting = new AtomicBoolean(false);
    private final Queue<String> mLogTextQueue = new ConcurrentLinkedQueue<>();

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
        mInternalFile = (logSetup.localFilePath == null) ? FileUtil.getInternalCacheFile(context) : logSetup.localFilePath;
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
                    TPBLog.e("processSaveLog>> " + throwable);
                    return false;
                })
                .doOnNext(success -> {
                    mIsWriting.set(false);
                    releaseWriting();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> processSaveLog(null));
    }

    private Observable<Boolean> copyExternalAndClearLatestFileLogObservable() {
        if (mInternalFile.length() < MAXIMUM_FILE_SIZE) {
            return Observable.just(false);
        }
        deletePreviousFileIfNeed();
        return getAndCopyExternalLatestFileLog()
                .doOnNext(file -> TPBLog.i("new file is created, fileName = " + file.getAbsolutePath()))
                .map(file -> true)
                .onErrorReturn(throwable -> {
                    TPBLog.e("copyExternalAndClearLatestFileLogObservable>> " + throwable);
                    return false;
                })
                .doOnNext(success -> TPBLog.i("copyExternalAndClearLatestFileLogObservable>> copy external file log is success = " + success));
    }

    Observable<File> getAndCopyExternalLatestFileLog() {
        if (mIsReading.compareAndSet(false, true)) {
            return Observable.defer(() -> {
                final File outFile = FileUtil.getExternalLogFile(mContext);
                TPBLog.i("getAndCopyExternalLatestFileLog>> before size = " + outFile.length());
                waitWritingRelease();
                try {
                    GZIPUtil.copyAndCompress(mInternalFile, outFile);
                } catch (IOException e) {
                    FileUtil.deleteWithoutException(outFile);
                    return Observable.error(e);
                }
                TPBLog.i("getAndCopyExternalLatestFileLog after size = " + outFile.length());
                return Observable.just(outFile);
            })
                    .doOnNext(file -> {
                        clearInternalLogFile();
                        mIsReading.set(false);
                    })
                    .doOnError(throwable -> {
                        TPBLog.e("getAndCopyExternalLatestFileLog>> error = " + throwable);
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
                    File externalDir = FileUtil.getExternalDir();
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
                    TPBLog.e("waitWritingRelease>> waiting is release" + e);
                } catch (IllegalMonitorStateException e) {
                    TPBLog.e("waitWritingRelease>> waiting is release" + e);
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
        FileUtil.clearPrintWriter(mInternalFile);
    }

    private Observable<Boolean> saveLogObservable(String logText) {
        return Observable.fromCallable(() -> {
            FileUtil.appendText(mInternalFile, logText);
            return true;
        })
                .doOnError(throwable -> TPBLog.e("saveLogObservable>> is failed, cause = " + throwable))
                .subscribeOn(Schedulers.io());
    }

    private void deletePreviousFileIfNeed() {
        if (mOptions.isDeletePreviousFileLog == false) {
            return;
        }
        File externalDir = FileUtil.getExternalDir();
        File[] files = externalDir.listFiles();
        if (files == null || files.length < LIMIT_SPLIT_FILE_LOG) {
            return;
        }
        TPBLog.i("deletePreviousFileIfNeed>> size of external file before delete = " + files.length);
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        int indexEnd = files.length + 1 - LIMIT_SPLIT_FILE_LOG;
        for (int i = 0; i < indexEnd; i++) {
            FileUtil.deleteWithoutException(files[i]);
        }
        File externalDirAfterDeleted = FileUtil.getExternalDir();
        TPBLog.i("deletePreviousFileIfNeed>> size of external file after delete = " + (externalDirAfterDeleted.listFiles() != null ? externalDirAfterDeleted.listFiles().length : 0));
    }
}
