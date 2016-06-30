package org.m4m.samples;

import android.media.MediaCodecInfo;
import android.media.MediaMetadataRetriever;

import org.m4m.AudioFormat;
import org.m4m.IVideoEffect;
import org.m4m.MediaComposer;
import org.m4m.MediaFile;
import org.m4m.MediaFileInfo;
import org.m4m.android.AndroidMediaObjectFactory;
import org.m4m.android.AudioFormatAndroid;
import org.m4m.android.VideoFormatAndroid;
import org.m4m.domain.Pair;
import org.m4m.effects.RotateEffect;

import java.io.IOException;

/**
 * Created by Jerry on 16/5/19.
 * To be or not to be!
 */
public class VideoTranscoder {

    private int width;
    private int height;

    public VideoTranscoder(int width, int height) {

        this.width = width;
        this.height = height;
    }

    public VideoTranscoder() {
        this(720, 720);
    }

    public String getOutPath() {
        return outPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

    public interface IVideoTranscoderListener {
        void onEachProgress(int index, float progress);

        void onDone();

        void onError();

    }

    public void concat(String[] inputFiles, String outFile, final IVideoTranscoderListener listener) {
        try {
            MediaFileInfo mediaFileInfo = new MediaFileInfo(new AndroidMediaObjectFactory(null));

            mediaFileInfo.setFileName(inputFiles[0]);


            long duration = mediaFileInfo.getDurationInMicroSec();

            audioFormat = (org.m4m.AudioFormat) mediaFileInfo.getAudioFormat();
            if (audioFormat == null) {
                if(listener!=null){

                    listener.onError();
                }
                return;
            }

            AndroidMediaObjectFactory factory = new AndroidMediaObjectFactory(null);
            MediaComposer mediaComposer = new org.m4m.MediaComposer(factory, new org.m4m.IProgressListener() {

                @Override
                public void onMediaStart() {

                }

                @Override
                public void onMediaProgress(float progress) {
                    if(listener!=null){
                        listener.onEachProgress(progress);
                    }
                }

                @Override
                public void onMediaDone() {
                    if(listener!=null){
                        listener.onDone();

                    }
                }

                @Override
                public void onMediaPause() {

                }

                @Override
                public void onMediaStop() {

                }

                @Override
                public void onError(Exception exception) {

                    if(listener!=null){

                        listener.onError();
                    }
                }
            });

            mediaComposer.setTargetFile(outFile);


            configureVideoEncoder(mediaComposer, width, height);
            configureAudioEncoder(mediaComposer);

//        IVideoEffect effect = new RotateEffect(video_rotation, factory.getEglUtil());
//        mediaComposer.addVideoEffect(effect);

            ///////////////////////////
//            long t = segmentTo - segmentFrom;
//            IVideoEffect effect = new RotateEffect(90, factory.getEglUtil());
//            //effect.setSegment(new FileSegment(0, t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//
//            MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(t, 2 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(1);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(2 * t, 3 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(2);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
            for (String file :
                    inputFiles) {
                mediaComposer.addSourceFile(file);
            }


            mediaComposer.start();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String outPath;
    public class VideoSegment{
        public String inputPath;
        public String tmpOutPath;
        public double from;
        public double to;
        public int rotation;
        public double scale;
        public int offsetX;
        public int offsetY;
    }
    public void run(VideoSegment[] videoSegments,String outPath,final IVideoTranscoderListener listener){
        try {
            MediaFileInfo mediaFileInfo = new MediaFileInfo(new AndroidMediaObjectFactory(null));

            mediaFileInfo.setFileName(videoSegments[0].inputPath);


            long duration = mediaFileInfo.getDurationInMicroSec();

            audioFormat = (org.m4m.AudioFormat) mediaFileInfo.getAudioFormat();
            if (audioFormat == null) {
                if(listener!=null){

                    listener.onError();
                }
                return;
            }

            AndroidMediaObjectFactory factory = new AndroidMediaObjectFactory(null);
            for(int i=0;i<videoSegments.length;i++) {

                String inputPath = videoSegments[i].inputPath;
                String tmpOutPath = videoSegments[i].tmpOutPath;

                double from = videoSegments[i].from;
                double to = videoSegments[i].to;
                int rotation = videoSegments[i].rotation;
                final int index=i;

                MediaComposer mediaComposer = new org.m4m.MediaComposer(factory, new org.m4m.IProgressListener() {

                    @Override
                    public void onMediaStart() {

                    }

                    @Override
                    public void onMediaProgress(float progress) {
                        if (listener != null) {
                            listener.onEachProgress(index, progress);
                        }
                    }

                    @Override
                    public void onMediaDone() {
                        if (listener != null) {
                            listener.onDone();

                        }
                    }

                    @Override
                    public void onMediaPause() {

                    }

                    @Override
                    public void onMediaStop() {

                    }

                    @Override
                    public void onError(Exception exception) {

                        if (listener != null) {

                            listener.onError();
                        }
                    }
                });


                mediaComposer.setTargetFile(tmpOutPath);

                configureVideoEncoder(mediaComposer, width, height);
                configureAudioEncoder(mediaComposer);

                mediaComposer.addSourceFile(inputPath);
                IVideoEffect effect = new RotateEffect(rotation, factory.getEglUtil());
                mediaComposer.addVideoEffect(effect);

                MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
                mediaFile.addSegment(new Pair<Long, Long>((long) (from * 1000000), (long) (to * 1000000)));


                mediaComposer.start();
            }
            String[] toConcat=new String[videoSegments.length];
            for(int i=0;i<videoSegments.length;i++){
                toConcat[i]=videoSegments[i].tmpOutPath;
            }
            this.concat(toConcat,outPath,listener);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void crop(String inputFile, double from, double to, String outFile, final IVideoTranscoderListener listener) {
        try {
            MediaFileInfo mediaFileInfo = new MediaFileInfo(new AndroidMediaObjectFactory(null));

            mediaFileInfo.setFileName(inputFile);


            long duration = mediaFileInfo.getDurationInMicroSec();

            audioFormat = (org.m4m.AudioFormat) mediaFileInfo.getAudioFormat();
            if (audioFormat == null) {
                if(listener!=null){

                    listener.onError();
                }
                return;
            }

            AndroidMediaObjectFactory factory = new AndroidMediaObjectFactory(null);
            MediaComposer mediaComposer = new org.m4m.MediaComposer(factory, new org.m4m.IProgressListener() {

                @Override
                public void onMediaStart() {

                }

                @Override
                public void onMediaProgress(float progress) {
                    if(listener!=null){
                        listener.onEachProgress(progress);
                    }
                }

                @Override
                public void onMediaDone() {
                    if(listener!=null){
                        listener.onDone();

                    }
                }

                @Override
                public void onMediaPause() {

                }

                @Override
                public void onMediaStop() {

                }

                @Override
                public void onError(Exception exception) {

                    if(listener!=null){

                        listener.onError();
                    }
                }
            });


            mediaComposer.setTargetFile(outFile);


            configureVideoEncoder(mediaComposer, width, height);
            configureAudioEncoder(mediaComposer);

//        IVideoEffect effect = new RotateEffect(video_rotation, factory.getEglUtil());
//        mediaComposer.addVideoEffect(effect);

            ///////////////////////////
//            long t = segmentTo - segmentFrom;
//            IVideoEffect effect = new RotateEffect(90, factory.getEglUtil());
//            //effect.setSegment(new FileSegment(0, t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//
//            MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(t, 2 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(1);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(2 * t, 3 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(2);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
            mediaComposer.addSourceFile(inputFile);
            IVideoEffect effect = new RotateEffect(retrieveRotation(inputFile), factory.getEglUtil());
            mediaComposer.addVideoEffect(effect);

            MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
            mediaFile.addSegment(new Pair<Long, Long>((long) (from * 1000000), (long) (to * 1000000)));




            mediaComposer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int retrieveRotation(String videoPath) throws IOException{


        // FileDescriptor fileDescriptor = new FileInputStream(android.net.Uri.parse(mediaUri1.getString()).getPath()).getFD();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(videoPath);

        String rotationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        return Integer.parseInt(rotationString);
    }
    public void cropAndconcat(final IVideoTranscoderListener listener) {
        try {
            long segmentTo = 4380160;
            long segmentFrom = 1434880;
//            String mediaUri1 = "/storage/emulated/0/DCIM/Camera/20160516_165204.mp4";
            String mediaUri1 = "/storage/emulated/0/DCIM/Camera/out41.mp4";
            String dstMediaPath = "/storage/emulated/0/DCIM/Camera/out4.mp4";

            MediaFileInfo mediaFileInfo = new MediaFileInfo(new AndroidMediaObjectFactory(null));

            mediaFileInfo.setFileName(mediaUri1);


            long duration = mediaFileInfo.getDurationInMicroSec();

            audioFormat = (org.m4m.AudioFormat) mediaFileInfo.getAudioFormat();
            if (audioFormat == null) {
                listener.onError();
                return;
            }

            AndroidMediaObjectFactory factory = new AndroidMediaObjectFactory(null);
            MediaComposer mediaComposer = new org.m4m.MediaComposer(factory, new org.m4m.IProgressListener() {

                @Override
                public void onMediaStart() {

                }

                @Override
                public void onMediaProgress(float progress) {

                    listener.onEachProgress(progress);
                }

                @Override
                public void onMediaDone() {
                    listener.onDone();
                }

                @Override
                public void onMediaPause() {

                }

                @Override
                public void onMediaStop() {

                }

                @Override
                public void onError(Exception exception) {
                    listener.onError();

                }
            });
            ;

            mediaComposer.setTargetFile(dstMediaPath);


            configureVideoEncoder(mediaComposer, 720, 720);
            configureAudioEncoder(mediaComposer);

//        IVideoEffect effect = new RotateEffect(video_rotation, factory.getEglUtil());
//        mediaComposer.addVideoEffect(effect);

            ///////////////////////////
//            long t = segmentTo - segmentFrom;
//            IVideoEffect effect = new RotateEffect(90, factory.getEglUtil());
//            //effect.setSegment(new FileSegment(0, t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//
//            MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(t, 2 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(1);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
//
//            effect = new RotateEffect(90, factory.getEglUtil());
//            effect.setSegment(new FileSegment(2 * t, 3 * t));
//            mediaComposer.addVideoEffect(effect);
//            mediaComposer.addSourceFile(mediaUri1);
//            mediaFile = mediaComposer.getSourceFiles().get(2);
//            mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));
            mediaComposer.addSourceFile(mediaUri1);
            mediaComposer.addSourceFile(mediaUri1);
            mediaComposer.addSourceFile(mediaUri1);


            mediaComposer.start();


        } catch (IOException e) {
            e.printStackTrace();
        }

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

}
