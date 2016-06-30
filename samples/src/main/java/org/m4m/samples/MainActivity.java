package org.m4m.samples;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaCodecInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.m4m.AudioFormat;
import org.m4m.IVideoEffect;
import org.m4m.MediaComposer;
import org.m4m.MediaFile;
import org.m4m.MediaFileInfo;
import org.m4m.android.AndroidMediaObjectFactory;
import org.m4m.android.AudioFormatAndroid;
import org.m4m.android.VideoFormatAndroid;
import org.m4m.domain.FileSegment;
import org.m4m.domain.Pair;
import org.m4m.effects.RotateEffect;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final int IMPORT_FROM_GALLERY_REQUEST = 1;
    protected final String videoMimeType = "video/avc";
    protected final int videoFrameRate = 30;
    protected final int videoIFrameInterval = 1;
    // Audio
    protected final String audioMimeType = "audio/mp4a-latm";
    protected final int audioSampleRate = 44100;
    protected final int audioChannelCount = 2;
    protected final int audioBitRate = 96 * 1024;
    protected int videoBitRateInKBytes = 5000;
    private AudioFormat audioFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.lis);
        Button temp = new Button(MainActivity.this);
        temp.setText("i am new");
        temp.setWidth(80);
        rl.addView(temp);
        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    long segmentTo = 4380160;
                    long segmentFrom = 1434880;
                    String mediaUri1 = "/storage/emulated/0/DCIM/Camera/20160516_165204.mp4";

                    MediaFileInfo mediaFileInfo = new MediaFileInfo(new AndroidMediaObjectFactory(null));

                    mediaFileInfo.setFileName(mediaUri1);


                    long duration = mediaFileInfo.getDurationInMicroSec();

                    audioFormat = (org.m4m.AudioFormat) mediaFileInfo.getAudioFormat();
                    if (audioFormat == null) {
                        showMessageBox("Audio format info unavailable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                    }

                    String dstMediaPath = "/storage/emulated/0/DCIM/Camera/out1.mp4";
                    AndroidMediaObjectFactory factory = new AndroidMediaObjectFactory(null);
                    MediaComposer mediaComposer = new org.m4m.MediaComposer(factory, new org.m4m.IProgressListener() {

                        @Override
                        public void onMediaStart() {

                        }

                        @Override
                        public void onMediaProgress(float progress) {

                        }

                        @Override
                        public void onMediaDone() {
                            Log.e("LJR", "onMediaDone"+" over!!");
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
                    });
                    ;

                    mediaComposer.setTargetFile(dstMediaPath);


                    configureVideoEncoder(mediaComposer, 480, 480);
                    configureAudioEncoder(mediaComposer);

//        IVideoEffect effect = new RotateEffect(video_rotation, factory.getEglUtil());
//        mediaComposer.addVideoEffect(effect);

                    ///////////////////////////
                    long t = segmentTo - segmentFrom;
                    IVideoEffect effect = new RotateEffect(90, factory.getEglUtil());
                    effect.setSegment(new FileSegment(0, t));
                    mediaComposer.addVideoEffect(effect);
                    mediaComposer.addSourceFile(mediaUri1);

                    MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
                    mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));

                    effect = new RotateEffect(90, factory.getEglUtil());
                    effect.setSegment(new FileSegment(t, 2 * t));
                    mediaComposer.addVideoEffect(effect);
                    mediaComposer.addSourceFile(mediaUri1);
                    mediaFile = mediaComposer.getSourceFiles().get(1);
                    mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));

                    effect = new RotateEffect(90, factory.getEglUtil());
                    effect.setSegment(new FileSegment(2 * t, 3 * t));
                    mediaComposer.addVideoEffect(effect);
                    mediaComposer.addSourceFile(mediaUri1);
                    mediaFile = mediaComposer.getSourceFiles().get(2);
                    mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));


                    mediaComposer.start();


                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void showMessageBox(String message, DialogInterface.OnClickListener listener) {

        if (message == null) {
            message = "";
        }

        if (listener == null) {
            listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            };
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage(message);
        b.setPositiveButton("OK", listener);
        AlertDialog d = b.show();

        ((TextView) d.findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
    }

    protected void configureVideoEncoder(org.m4m.MediaComposer mediaComposer, int width, int height) {

        VideoFormatAndroid videoFormat = new VideoFormatAndroid(videoMimeType, width, height);

        videoFormat.setVideoBitRateInKBytes(videoBitRateInKBytes);
        videoFormat.setVideoFrameRate(videoFrameRate);
        videoFormat.setVideoIFrameInterval(videoIFrameInterval);

        mediaComposer.setTargetVideoFormat(videoFormat);
    }

    protected void configureAudioEncoder(org.m4m.MediaComposer mediaComposer) {

        /**
         * TODO: Audio resampling is unsupported by current m4m release
         * Output sample rate and channel count are the same as for input.
         */
        AudioFormatAndroid aFormat = new AudioFormatAndroid(audioMimeType, audioFormat.getAudioSampleRateInHz(), audioFormat.getAudioChannelCount());

        aFormat.setAudioBitrateInBytes(audioBitRate);
        aFormat.setAudioProfile(MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        mediaComposer.setTargetAudioFormat(aFormat);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {

            case IMPORT_FROM_GALLERY_REQUEST: {
                if (resultCode == RESULT_OK) {
                    Uri selectedVideo = intent.getData();
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedVideo, proj, null, null, null);
                    if (cursor.moveToFirst()) {
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                        if (selectedVideo == null) {
                            showToast("Invalid URI.");
                            return;
                        }
                        showToast(selectedVideo.toString());
                        Log.e("LJR", "onActivityResult" + selectedVideo.toString());
                    }
                    break;
                }

            }
        }
    }

    public void showToast(String title) {
        Toast.makeText(this, title, Toast.LENGTH_SHORT).show();
    }
}