package com.mysoftsource.rxandroidlogger;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

class FileUtil {
    private static final String TAG = FileUtil.class.getCanonicalName();

    static void deleteWithoutException(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            Log.e(TAG, "deleteWithoutException>> " + e);
        }
    }
}
