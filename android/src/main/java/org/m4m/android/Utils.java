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
    private static File getAndroidMoviesFolder(Context c) {
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator;
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        return c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
    }

    public static String getAndroidMoviesFolderPath(Context c) {
        File file = getAndroidMoviesFolder(c);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    public static String getVideoFilePath(Context c, String filename) {
        // return getAndroidMoviesFolder().getAbsolutePath() + "/capture.mp4";
        File file = new File(Utils.getAndroidMoviesFolder(c), filename);
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
