package com.mysoftsource.rxandroidlogger;

import android.content.Context;

import timber.log.Timber;

class TBPDebugTree extends Timber.DebugTree {
    private final Context mContext;

    TBPDebugTree(Context context) {
        mContext = context;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        super.log(priority, tag, message, t);
        StringBuilder builder = new StringBuilder(DateTimeUtil.getCurrentTime());
        builder.append(" ");
        builder.append(tag);
        builder.append(" ");
        builder.append(":");
        builder.append(" ");
        builder.append(message);
        builder.append(" ");
        builder.append(t != null ? t : "");

        DbHelper.getInstance().saveLog(builder.toString());
    }
}
