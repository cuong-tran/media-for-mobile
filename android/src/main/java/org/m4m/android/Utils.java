package org.m4m.android;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.m4m.ILog;

import java.io.File;
import java.io.IOException;

/**
 * Created by Marcin on 26.10.2017.
 */

public class Utils
{
    public static String getVideoFilePath(Context c, String filename) {
        File file = new File(c.getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    public static String className(Object object)
    {
        return object.getClass().getSimpleName();
    }
}
