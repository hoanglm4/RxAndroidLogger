package com.mysoftsource.rxandroidlogger;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.ListSharedLinksResult;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;

public class DropboxUploader extends Uploader {
    private static final String TAG = DropboxUploader.class.getCanonicalName();

    public DropboxUploader(String path, String dropboxAccessToken) {
        super(path);
        DropboxClientFactory.init(dropboxAccessToken);
    }

    @Override
    protected Observable<String> uploadAndFinish(File localFile) {
        return Observable.defer(() -> {
            String path = getRemotePath(localFile);
            try (InputStream inputStream = new FileInputStream(localFile)) {
                FileMetadata fileMetadata = DropboxClientFactory.getClient().files().uploadBuilder(path)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
                return Observable.just(fileMetadata.getPathLower());
            } catch (DbxException | IOException e) {
                return Observable.error(e);
            }
        });
    }

    @Override
    protected Observable<String> createSharedLink(String remoteFile) {
        TPBLog.i("createSharedLink>> remote file = " + remoteFile);
        return Observable.defer(() -> {
            try {
                SharedLinkMetadata sharedLinkMetadata = getSharedLink(remoteFile);
                if (sharedLinkMetadata == null || sharedLinkMetadata.getUrl() == null) {
                    sharedLinkMetadata = DropboxClientFactory.getClient()
                            .sharing()
                            .createSharedLinkWithSettings(remoteFile);
                }
                return Observable.just(sharedLinkMetadata.getUrl());
            } catch (DbxException e) {
                return Observable.error(e);
            }
        });
    }

    private SharedLinkMetadata getSharedLink(String remoteFile) {
        TPBLog.i("getSharedLink>> remote file = " + remoteFile);
        try {
            ListSharedLinksResult linksResult = DropboxClientFactory.getClient()
                    .sharing()
                    .listSharedLinksBuilder()
                    .withPath(remoteFile)
                    .start();
            TPBLog.i("getSharedLink>> links = %s", linksResult.getLinks().toString());
            return linksResult.getLinks().size() == 0 ? null : linksResult.getLinks().get(0);
        } catch (DbxException e) {
            TPBLog.e(e, "getSharedLink>> failed");
            return null;
        }
    }
}
