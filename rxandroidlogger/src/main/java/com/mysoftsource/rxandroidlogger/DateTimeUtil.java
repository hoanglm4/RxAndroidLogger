package com.mysoftsource.rxandroidlogger;

import java.text.SimpleDateFormat;
import java.util.Date;

class DateTimeUtil {
    private static final String PATTERN_TIME_FULL_DATE = "yyyy-MM-dd HH:mm:ss.SSS";

    static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_TIME_FULL_DATE);
        try {
            sdf.applyPattern(PATTERN_TIME_FULL_DATE);
            return sdf.format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
