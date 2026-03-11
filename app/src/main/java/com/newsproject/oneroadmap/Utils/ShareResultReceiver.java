package com.newsproject.oneroadmap.Utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ShareResultReceiver extends BroadcastReceiver {
    private static OnShareTargetSelectedListener listener;

    public interface OnShareTargetSelectedListener {
        void onTargetSelected(String packageName);
    }

    public static void setListener(OnShareTargetSelectedListener l) {
        listener = l;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null) {
            ComponentName componentName = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
            if (componentName != null && listener != null) {
                listener.onTargetSelected(componentName.getPackageName());
            }
        }
    }
}
