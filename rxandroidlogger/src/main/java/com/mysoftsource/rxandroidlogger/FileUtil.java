package com.mysoftsource.rxandroidlogger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.UUID;

class FileUtil {
    private static final String TAG = FileUtil.class.getCanonicalName();

    private static final String INTERNAL_CACHE_SUB_DIR = "logger_internal";
    private static final String INTERNAL_CACHE_FILE_NAME = "TBPTBPLog.txt";
    private static final String EXTERNAL_CACHE_SUB_DIR = "logger_external";
    private static final String EXTERNAL_FILE_NAME = "TBPLog_%1s_%2s_%3s_%4d_%5s_%6s.txt.gz";

    static void deleteWithoutException(File file) {
        TPBLog.i("deleteWithoutException>> is called");
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            TPBLog.e("deleteWithoutException>> " + e);
        }
    }

    static File getInternalCacheFile(Context context) {
        final String cachePath = context.getCacheDir().getPath();
        File subFile = new File(cachePath + File.separator + INTERNAL_CACHE_SUB_DIR);
        subFile.mkdirs();
        return new File(subFile, INTERNAL_CACHE_FILE_NAME);
    }

    static File getExternalLogFile(Context context) {
        File externalStorageDir = new File(getExternalDir(), String.format(Locale.US, EXTERNAL_FILE_NAME,
                Build.MODEL,
                Build.MANUFACTURER,
                getVersionName(context),
                Build.VERSION.SDK_INT,
                DateTimeUtil.getCurrentTime(),
                generalClientId()));
        return externalStorageDir;
    }

    static File getExternalDir() {
        File externalDir = new File(Environment.getExternalStorageDirectory(), EXTERNAL_CACHE_SUB_DIR);
        externalDir.mkdirs();
        return externalDir;
    }

    static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    static void appendText(File file, String text) throws IOException {
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, true);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            out.println(text);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(bw);
            IOUtils.closeQuietly(fw);
        }
    }

    static void clearPrintWriter(File file) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            writer.print("");
        } catch (FileNotFoundException e) {
            TPBLog.e("clearPrintWriter>> is failed, cause = " + e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    static String generalClientId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
