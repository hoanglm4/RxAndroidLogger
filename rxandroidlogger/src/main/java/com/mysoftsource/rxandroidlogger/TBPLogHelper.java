package com.mysoftsource.rxandroidlogger;

import android.content.Context;
import android.text.TextUtils;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class TBPLogHelper {
    private static final String TAG = TBPLogHelper.class.getCanonicalName();
    private static TBPLogHelper sInstance;

    private final TBPDebugTree mTBPDebugTree;
    private final LogSetup mOptions;
    private final Uploader mUploader;
    private final Observable<String> mSharedStoreAllLog;

    public synchronized static TBPLogHelper create(Context context, LogSetup options) {
        if (sInstance == null) {
            sInstance = new TBPLogHelper(context, options);
        }
        return sInstance;
    }

    public static TBPLogHelper getInstance() {
        return sInstance;
    }

    private TBPLogHelper(Context context, LogSetup options) {
        mOptions = options;
        mTBPDebugTree = new TBPDebugTree(context);
        DbHelper.create(context, mOptions);
        mUploader = createUploader();
        mSharedStoreAllLog = createSharedAllLog();
    }

    private Uploader createUploader() {
        if (!TextUtils.isEmpty(mOptions.dropboxAccessToken)) {
            return new DropboxUploader(mOptions.serverFilePath, mOptions.dropboxAccessToken);
        }
        // for default
        return new DropboxUploader(mOptions.serverFilePath, mOptions.dropboxAccessToken);
    }

    private Observable<String> createSharedAllLog() {
        return DbHelper.getInstance().getAllExternalFileLog()
                .concatMap(localFile -> mUploader.upload(localFile))
                .publish()
                .refCount();
    }

    public TBPDebugTree getTBPDebugTree() {
        return mTBPDebugTree;
    }

    public Observable<String> storeAllLog() {
        return mSharedStoreAllLog.observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Please using storeAllLog()
     *
     */
    @Deprecated
    public Observable<String> storeAllPreviousLogToDropBox() {
        return storeAllLog();
    }
}
