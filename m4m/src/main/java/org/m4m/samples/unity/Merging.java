package org.m4m.samples.unity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
//import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.unity3d.player.UnityPlayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import org.m4m.android.Utils;

public class Merging {

    private static String unityGameObjectName = "VideoMergingMessageReceiver";
    private static final String unitySuccessCallbackName = "OnDone";
    private static final String unityErrorCallbackName = "OnError";

    public static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
    public static int COMPRESSED_AUDIO_FILE_BIT_RATE = 64000; // 64kbps //705kbps //58kbps
    public static int SAMPLING_RATE = 44100; //48000;
    public static final int BUFFER_SIZE = 48000;
    public static final int CODEC_TIMEOUT_IN_MS = 5000;
    private  String CONVERT_AUDIO_TAG = "CONVERT AUDIO";
    private Context context;

    /**
     * this method is called in Unity
     * @param name
     */
    public static void setUnityObjectName(String name) {
        unityGameObjectName = name;
    }

    /**
     * Send message to Unity's GameObject (named as Plugin.unityGameObjectName)
     * @param method name of the method in GameObject's script
     * @param message the actual message
     */
    private static void sendMessageToUnityObject(String method, String message){
        UnityPlayer.UnitySendMessage(unityGameObjectName, method, message);
    }

    public static void setBitRateAndSamplingRate(int bitRate, int samplingRate) {
        COMPRESSED_AUDIO_FILE_BIT_RATE = bitRate;
        SAMPLING_RATE = samplingRate;
    }

    public Merging(Context context)
    {
        this.context = context;
    }

    private static class BufferedWritableFileByteChannel implements WritableByteChannel {
        private static final int BUFFER_CAPACITY = 1000000;

        private boolean isOpen = true;
        private final OutputStream outputStream;
        private final ByteBuffer byteBuffer;
        private final byte[] rawBuffer = new byte[BUFFER_CAPACITY];

        private BufferedWritableFileByteChannel(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.byteBuffer = ByteBuffer.wrap(rawBuffer);
        }

        @Override
        public int write(ByteBuffer inputBuffer) throws IOException {
            int inputBytes = inputBuffer.remaining();

            if (inputBytes > byteBuffer.remaining()) {
                dumpToFile();
                byteBuffer.clear();

                if (inputBytes > byteBuffer.remaining()) {
                    throw new BufferOverflowException();
                }
            }

            byteBuffer.put(inputBuffer);
            return inputBytes;
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

        @Override
        public void close() throws IOException {
            dumpToFile();
            isOpen = false;
        }
        private void dumpToFile() {
            try {
                outputStream.write(rawBuffer, 0, byteBuffer.position());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String mergeAudioWithVideoInBackgroundThread(final String videoFilePath, final String audioFilePath, final String outputFilePath)
    {
        //final String outputFilePath = Utils.getExternalStorageVideoFilePath(outputFileName);
        Thread thread = new Thread(new Runnable() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                mergeAudioWithVideo(videoFilePath, audioFilePath, outputFilePath);
                // Notify UI thread... <- already notified
            }
        });
        thread.start();
        return outputFilePath;
    }

    private boolean mergeAudioWithVideo(String videoFilePath, String audioFilePath, String outputFilePath) {
        if (context == null)
        {
            sendMessageToUnityObject(unityErrorCallbackName, "context is null");
            return false;
        }

        //String videoFilePath = Utils.getAppDataFilePath(context, videoFileName);
        //String audioFilePath = Utils.getAppDataFilePath(context, audioFileName);

        // First convert audio from wav to m4a format
        String compressedAudioFilePath = audioFilePath.substring(0, audioFilePath.lastIndexOf('.')) + ".m4a";
        boolean audioConverted = this.convertAudioToM4A(audioFilePath, compressedAudioFilePath);
        if (!audioConverted)
        {
            sendMessageToUnityObject(unityErrorCallbackName, "audio conversion failed "+" #0");
            return false;
        }

        Movie video = null;
        try {
            video = MovieCreator.build(context, videoFilePath);
        } catch (RuntimeException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #1");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #2");
            e.printStackTrace();
            return false;
        }

        Movie audio = null;
        try {
            audio = MovieCreator.build(context, compressedAudioFilePath);
        } catch (IOException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #3");
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #4");
            e.printStackTrace();
            return false;
        }

        if (video == null || audio == null)
        {
            sendMessageToUnityObject(unityErrorCallbackName, "video=" + video + " audio=" + audio + " #4a");
            return false;
        }

        Track audioTrack = audio.getTracks().get(0);
        video.addTrack(audioTrack);

        Container out = new DefaultMp4Builder().build(video);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outputFilePath);
        } catch (FileNotFoundException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #5");
            e.printStackTrace();
            return false;
        }
        BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);
        try {
            out.writeContainer(byteBufferByteChannel);
            byteBufferByteChannel.close();
            fos.close();
            Log.e("MP4Parser", "mergeAudioWithVideo: End of audio/video merge");
            if (context != null)
            {
                Utils.scanMediaFilePath(context, outputFilePath);
            }
        } catch (IOException e) {
            sendMessageToUnityObject(unityErrorCallbackName, e.getMessage()+" #6");
            e.printStackTrace();
            return false;
        }
        sendMessageToUnityObject(unitySuccessCallbackName, "");
        return true;
    }

    private boolean convertAudioToM4A(String filePath, String outputFilePath)
    {
        try {
            Context c = context;
            File inputFile = new File(filePath);
            FileInputStream fis = new FileInputStream(inputFile);
            File outputFile = new File(outputFilePath );
            if (outputFile.exists()) outputFile.delete();

            MediaMuxer mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLING_RATE, 1);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);
            outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

            MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: Array of buffers
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();
            byte[] tempBuffer = new byte[BUFFER_SIZE];
            boolean hasMoreData = true;
            double presentationTimeUs = 0;
            int audioTrackIdx = 0;
            int totalBytesRead = 0;
            int percentComplete = 0;
            do {
                int inputBufIndex = 0;
                while (inputBufIndex != -1 && hasMoreData) {
                    inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        dstBuf.clear();

                        int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                        //Log.e("bytesRead","Readed "+bytesRead);
                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            totalBytesRead += bytesRead;
                            dstBuf.put(tempBuffer, 0, bytesRead);
                            codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                            presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE;
                        }
                    }
                }
                // Drain audio
                int outputBufIndex = 0;
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
                    if (outputBufIndex >= 0) {
                        ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                        encodedData.position(outBuffInfo.offset);
                        encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                        if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }else{
                            mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat = codec.getOutputFormat();
                        Log.e(CONVERT_AUDIO_TAG, "Output format changed - " + outputFormat);
                        audioTrackIdx = mux.addTrack(outputFormat);
                        mux.start();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        Log.e(CONVERT_AUDIO_TAG, "Output buffers changed during encode!");
                    } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // NO OP
                    } else {
                        Log.e(CONVERT_AUDIO_TAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
                    }
                }
                percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
                Log.e(CONVERT_AUDIO_TAG, "Conversion % - " + percentComplete);
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            fis.close();
            mux.stop();
            mux.release();
            Log.e(CONVERT_AUDIO_TAG, "Compression done ...");
            return true;
        } catch (FileNotFoundException e) {
            Log.e(CONVERT_AUDIO_TAG, "File not found!", e);
            return false;
        } catch (IOException e) {
            Log.e(CONVERT_AUDIO_TAG, "IO exception!", e);
            return false;
        }
    }

}
