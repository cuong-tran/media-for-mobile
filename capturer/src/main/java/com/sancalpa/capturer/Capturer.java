package com.sancalpa.capturer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import org.m4m.IProgressListener;
import org.m4m.android.graphics.FullFrameTexture;

import java.io.File;
import java.io.IOException;

public class Capturer
{
    private static final String TAG = "Capturing";

    private static FullFrameTexture texture;

    private VideoCapture videoCapture;
    private int width = 0;
    private int height = 0;

    private int videoWidth = 0;
    private int videoHeight = 0;
    private int videoFrameRate = 0;

    private long nextCaptureTime = 0;
    private long startTime = 0;

    private static Capturer instance = null;

    private SharedContext sharedContext = null;
    private EncodeThread encodeThread = null;
    private boolean finalizeFrame = false;
    private boolean isRunning = false;

    private IProgressListener progressListener = new IProgressListener() {
        @Override
        public void onMediaStart() {
            startTime = System.nanoTime();
            nextCaptureTime = 0;
            encodeThread.start();
            isRunning = true;
        }

        @Override
        public void onMediaProgress(float progress) {
        }

        @Override
        public void onMediaDone() {
        }

        @Override
        public void onMediaPause() {
        }

        @Override
        public void onMediaStop() {
        }

        @Override
        public void onError(Exception exception) {
        }
    };

    private class EncodeThread extends Thread
    {
        private static final String TAG = "EncodeThread";

        private SharedContext sharedContext;
        private boolean isStopped = false;
        private int textureID;
        private boolean newFrameIsAvailable = false;

        EncodeThread(SharedContext sharedContext) {
            super();
            this.sharedContext = sharedContext;
        }

        @Override
        public void run() {
            while (!isStopped) {
                if (newFrameIsAvailable) {
                    synchronized (videoCapture) {
                        sharedContext.makeCurrent();
                        videoCapture.beginCaptureFrame();
                        GLES20.glViewport(0, 0, videoWidth, videoHeight);
                        texture.draw(textureID);
                        videoCapture.endCaptureFrame();
                        newFrameIsAvailable = false;
                        sharedContext.doneCurrent();
                    }
                }
            }
            isStopped = false;
            synchronized (videoCapture) {
                videoCapture.stop();
            }
        }

        public void queryStop() {
            isStopped = true;
        }

        public void pushFrame(int textureID) {
            this.textureID = textureID;
            newFrameIsAvailable = true;
        }
    }

    public Capturer(Context context, int width, int height)
    {
        videoCapture = new VideoCapture(context, progressListener);

        this.width = width;
        this.height = height;

        texture = new FullFrameTexture();
        sharedContext = new SharedContext();
        instance = this;
    }

    public static Capturer getInstance()
    {
        return instance;
    }

    public static String getDirectoryDCIM()
    {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator;
    }

    public void initCapturing(int width, int height, int frameRate, int bitRate)
    {
        Log.d(TAG, "--- initCapturing: " + width + "x" + height + ", " + frameRate + ", " + bitRate);
        videoFrameRate = frameRate;
        VideoCapture.init(width, height, frameRate, bitRate);
        videoWidth = width;
        videoHeight = height;

        encodeThread = new EncodeThread(sharedContext);
    }

    public void startCapturing(final String videoPath)
    {
        if (videoCapture == null) {
            return;
        }

        (new Thread() {
            public void run() {
                Log.d(TAG, "--- startCapturing");
                synchronized (videoCapture) {
                    try {
                        videoCapture.start(videoPath);
                    } catch (IOException e) {
                        Log.e(TAG, "--- startCapturing error");
                    }
                }
            }
        }).start();
    }

    public void captureFrame(int textureID)
    {
        encodeThread.pushFrame(textureID);
    }

    public void stopCapturing()
    {
        Log.d(TAG, "--- stopCapturing");
        isRunning = false;

        if (finalizeFrame) {
            finalizeFrame = false;
        }
        encodeThread.queryStop();
    }

    public boolean isRunning()
    {
        return isRunning;
    }
}
