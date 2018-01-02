package com.mysoftsource.rxandroidlogger;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public class LogSetup {
    @NonNull
    public final String dropboxAccessToken;
    @NonNull
    public final String dropboxPath;
    @Nullable
    public final File localFilePath;
    @NonNull
    public final boolean isDeletePreviousFileLog;

    private LogSetup(@NonNull String dropboxAccessToken,
                     @NonNull String dropboxPath,
                     @Nullable File localFilePath,
                     @NonNull boolean isDeletePreviousFileLog) {
        this.dropboxAccessToken = dropboxAccessToken;
        this.dropboxPath = dropboxPath;
        this.localFilePath = localFilePath;
        this.isDeletePreviousFileLog = isDeletePreviousFileLog;
    }

    public static class Builder {
        @NonNull
        String dropboxAccessToken;
        @NonNull
        String dropboxPath;
        @Nullable
        File localFilePath;
        boolean isDeletePreviousFileLog = false;

        public Builder setDropboxAccessToken(String dropboxAccessToken) {
            this.dropboxAccessToken = dropboxAccessToken;
            return this;
        }

        public Builder setDropboxPath(String dropboxPath) {
            this.dropboxPath = dropboxPath;
            return this;
        }

        public Builder setLocalFilePath(File localFilePath) {
            this.localFilePath = localFilePath;
            return this;
        }

        public Builder setNeedDeletePreviousFileLog(boolean isDeletePreviousFileLog) {
            this.isDeletePreviousFileLog = isDeletePreviousFileLog;
            return this;
        }

        public LogSetup build() {
            if (dropboxAccessToken == null) {
                throw new IllegalStateException("missing set dropbox access token");
            }
            if (dropboxPath == null) {
                throw new IllegalStateException("missing set dropbox path");
            }
            return new LogSetup(dropboxAccessToken,
                    dropboxPath,
                    localFilePath,
                    isDeletePreviousFileLog);
        }
    }
}
