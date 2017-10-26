package org.m4m.android;

import android.util.Log;

import org.m4m.ILog;

/**
 * Created by Marcin on 26.10.2017.
 */

public class BridgeLog implements ILog
{
    public static final BridgeLog instance = new BridgeLog();

    @Override
    public void e(String tag, String message) {
        Log.e(tag, message);
    }
}
