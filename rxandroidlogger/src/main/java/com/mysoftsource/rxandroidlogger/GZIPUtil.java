package com.mysoftsource.rxandroidlogger;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class GZIPUtil {
    private static final String TAG = GZIPUtil.class.getCanonicalName();

    static void copyAndCompress(File file, File gzipFile) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPOutputStream gzipOS = null;
        try {
            fis = new FileInputStream(file);
            fos = new FileOutputStream(gzipFile);
            gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
            gzipOS.finish();
        } finally {
            IOUtils.closeQuietly(gzipOS);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(fis);
        }
    }

    private static void decompressGzipFile(String gzipFile, String newFile) throws IOException {
        FileInputStream fis = null;
        GZIPInputStream gis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(gzipFile);
            gis = new GZIPInputStream(fis);
            fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(gis);
            IOUtils.closeQuietly(fis);
        }
    }
}
