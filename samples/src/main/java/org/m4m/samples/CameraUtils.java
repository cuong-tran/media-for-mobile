/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.m4m.samples;

import org.m4m.ILog;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

class BridgeLog implements ILog
{
    public static final BridgeLog instance = new BridgeLog();

    @Override
    public void e(String tag, String message) {
        Log.e(tag, message);
    }
}

class Utils
{
    private static File getAndroidMoviesFolder(Context c) {
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator;
        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        return c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
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

class CameraUtils {
    private Camera.Parameters cameraParams;
    private int maxFps1;
    private int maxFps0;

    public CameraUtils(Camera.Parameters cameraParams) {
        this.cameraParams = cameraParams;
    }

    public int getMaxFps1() {
        return maxFps1;
    }

    public int getMaxFps0() {
        return maxFps0;
    }

    public CameraUtils invoke() {
        List<int[]> supportedPreviewFpsRange = cameraParams.getSupportedPreviewFpsRange();
        maxFps1 = 1;
        maxFps0 = 1;
        for (int[] fpsRange : supportedPreviewFpsRange) {
            if ((fpsRange[1] > maxFps1 && fpsRange[0] > maxFps0) ||
                    (fpsRange[1] > maxFps1 && fpsRange[0] == maxFps0) ||
                    (fpsRange[1] == maxFps1 && fpsRange[0] > maxFps0)){
                maxFps0 = fpsRange[0];
                maxFps1 = fpsRange[1];
            }
        }
        return this;
    }
}
