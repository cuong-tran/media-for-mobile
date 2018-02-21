package org.m4m.android;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Marcin on 26.10.2017.
 */

public class Utils
{
    public static void scanMediaFilePath(Context context, String filePath)
    {
        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))));
        //File dirToScan = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        String[] toBeScannedStr = new String[]{filePath/*dirToScan.getAbsolutePath(), dirToScan.getPath()*/};
        MediaScannerConnection.scanFile(context, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.d("scanMediaFilePath", "MEDIA SCAN COMPLETED: " + path);
            }
        });
    }

    public static String getExternalStorageDCIMDirFilePath(String filename) {
        File dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        return getFilePath(dirPath, filename);
    }

    public static String getExternalStorageVideoFilePath(String filename) {
        File dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        return getFilePath(dirPath, filename);
    }

    public static String getExternalStorageFilePath(String filename) {
        return getFilePath(Environment.getExternalStorageDirectory(), filename);
    }

    public static String getAppDataVideoFilePath(Context c, String filename) {
        return getFilePath(c.getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename);
    }

    public static String getAppDataFilePath(Context c, String filename) {
        return getFilePath(c.getExternalFilesDir(null), filename);
    }

    public static File getAppDataFile(Context c, String filename) {
        return getFile(c.getExternalFilesDir(null), filename);
    }

    public static String getFilePath(File dirPath, String filename) {
        File file = getFile(dirPath, filename);
        return file.getAbsolutePath();
    }

    public static File getFile(File dirPath, String filename) {
        if (!dirPath.exists())
            dirPath.mkdirs();
        File file = new File(dirPath, filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    public static String className(Object object)
    {
        return object.getClass().getSimpleName();
    }
}
