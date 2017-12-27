package com.mysoftsource.rxandroidlogger;

import android.content.Context;
import android.os.Build;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class TBPLogHelper {
    private static final String TAG = TBPLogHelper.class.getCanonicalName();
    private static TBPLogHelper sInstance;

    private final TBPDebugTree mTBPDebugTree;
    private final LogSetup mOptions;

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
        DbHelper.create(context, mOptions.localFilePath);
        DropboxClientFactory.init(mOptions.dropboxAccessToken);
    }

    public TBPDebugTree getTBPDebugTree() {
        return mTBPDebugTree;
    }

    public Observable<String> storeDropBox() {
        return DbHelper.getInstance().getOnFileLog()
                .flatMap(localFile -> uploadAndFinish(localFile))
                .map(fileMetadata -> fileMetadata.getPathLower())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<FileMetadata> uploadAndFinish(File localFile) {
        return Observable.defer(() -> {
            String path = getDropboxPath(localFile);
            try (InputStream inputStream = new FileInputStream(localFile)) {
                FileMetadata fileMetadata = DropboxClientFactory.getClient().files().uploadBuilder(path)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
                return Observable.just(fileMetadata);
            } catch (DbxException | IOException e) {
                return Observable.error(e);
            }
        }).doOnNext(fileMetadata -> FileUtil.deleteWithoutException(localFile));
    }

    private String getDropboxPath(File localFile) {
        String remoteFileName = localFile.getName();
        StringBuilder pathBuilder = new StringBuilder(mOptions.dropboxPath);
        pathBuilder.append("/");
        pathBuilder.append(Build.MODEL);
        pathBuilder.append("/");
        pathBuilder.append(remoteFileName);
        return pathBuilder.toString();
    }
}
