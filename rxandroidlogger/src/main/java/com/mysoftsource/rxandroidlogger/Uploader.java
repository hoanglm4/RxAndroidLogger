package com.mysoftsource.rxandroidlogger;

import android.os.Build;
import android.util.Log;

import java.io.File;

import rx.Observable;

public abstract class Uploader {
    private static final String TAG = Uploader.class.getCanonicalName();

    private String path;

    public Uploader(String path) {
        this.path = path;
    }

    protected String getRemotePath(File localFile) {
        String remoteFileName = localFile.getName();
        StringBuilder pathBuilder = new StringBuilder(path);
        pathBuilder.append("/");
        pathBuilder.append(Build.MODEL);
        pathBuilder.append("/");
        pathBuilder.append(remoteFileName);
        return pathBuilder.toString();
    }

    Observable<String> upload(File localFile) {
        TPBLog.d("upload>> update file = " + localFile.getAbsolutePath());
        return uploadAndFinish(localFile)
                .doOnNext(sharedLink -> FileUtil.deleteWithoutException(localFile))
                .flatMap(fileId -> createSharedLink(fileId));
    }

    protected abstract Observable<String> uploadAndFinish(File localFile);

    protected abstract Observable<String> createSharedLink(String remoteFile);
}
