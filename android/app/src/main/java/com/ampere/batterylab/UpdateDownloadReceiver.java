package com.ampere.batterylab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives DownloadManager completion even when the app process was stopped. */
public final class UpdateDownloadReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        UpdateChecker.handleDownloadCompleted(context, intent);
    }
}
