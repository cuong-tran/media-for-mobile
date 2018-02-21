package jp.classmethod.sample.mp4parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.TextTrackImpl;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import org.m4m.android.Utils;
import org.m4m.samples.R;


public class MainActivity extends FragmentActivity implements LoaderCallbacks<Boolean> {

	//	public static final String VIDEO_RECORDING_FILE_NAME = "game_capturing-190814-034638.378.mp4";
	//	public static final String AUDIO_RECORDING_FILE_NAME = "audio_Capturing-190814-034638.422.wav"; // Input PCM file
	//	public static final String COMPRESSED_AUDIO_FILE_NAME = "converted.mp4.m4a"; // Output MP4/M4A file
	public static final String VIDEO_RECORDING_FILE_NAME = "recordedVideo.mp4";
	public static final String AUDIO_RECORDING_FILE_NAME = "recordedVoice.wav"; // Input PCM file
	public static final String COMPRESSED_AUDIO_FILE_NAME = "recordedVoice.m4a";
	public static final String OUTPUT_FILE_NAME = "output.mp4";
	public static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
	public static final int COMPRESSED_AUDIO_FILE_BIT_RATE = 64000; // 64kbps //705kbps //58kbps
	public static final int SAMPLING_RATE = 44100; //48000;
	public static final int BUFFER_SIZE = 48000;
	public static final int CODEC_TIMEOUT_IN_MS = 5000;
	public static final String CONVERT_AUDIO_TAG = "CONVERT AUDIO";
	public static final String TAG = "MP4Parser";

	private final MainActivity self = this;
	private ProgressDialog mProgressDialog;

	private String getAssetFilePath(String fileName)
	{
		return "file:///android_asset/+fileName";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		findViewById(R.id.append).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				mProgressDialog = ProgressDialog.show(self, null, null);
				Bundle args = new Bundle();
				args.putInt("type", 0);
				getSupportLoaderManager().initLoader(0, args, self);
				*/
				//				String root = Environment.getExternalStorageDirectory().toString();
				//				String audio = root + "/"+"audio_Capturing-190814-034638.422.m4a";
				//				String video = root + "/"+"game_capturing-190814-034638.378.mp4";
				//				String output = root + "/"+"output.mp4";
				Context c = getApplicationContext();
				String video = Utils.getAppDataVideoFilePath(c, VIDEO_RECORDING_FILE_NAME);
				String audio = Utils.getAppDataVideoFilePath(c, COMPRESSED_AUDIO_FILE_NAME);
				String output = Utils.getExternalStorageVideoFilePath(OUTPUT_FILE_NAME);

				Log.e("FILE", "audio:"+audio);
				Log.e("FILE", "video:"+video);
				Log.e("FILE", "output:"+output);
				mergeAudioWithVideo(video, audio, output);
			}
		});

		findViewById(R.id.crop).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				mProgressDialog = ProgressDialog.show(self, null, null);
				Bundle args = new Bundle();
				args.putInt("type", 1);
				getSupportLoaderManager().initLoader(0, args, self);
				*/
				Thread thread = new Thread(convert);
				thread.start();
			}
		});

		findViewById(R.id.sub_title).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mProgressDialog = ProgressDialog.show(self, null, null);
				Bundle args = new Bundle();
				args.putInt("type", 2);
				getSupportLoaderManager().initLoader(0, args, self);
			}
		});

	}

	@Override
	public Loader<Boolean> onCreateLoader(int id, Bundle args) {
		return new EditMovieTask(self, args.getInt("type"));
	}

	@Override
	public void onLoadFinished(Loader<Boolean> loader, Boolean succeed) {
		getSupportLoaderManager().destroyLoader(loader.getId());
		mProgressDialog.dismiss();
	}

	@Override
	public void onLoaderReset(Loader<Boolean> loader) {
	}

	public static class EditMovieTask extends AsyncTaskLoader<Boolean> {

		private int mType;

		public EditMovieTask(Context context, int type) {
			super(context);
			mType = type;
			forceLoad();
		}

		@Override
		public Boolean loadInBackground() {

			switch (mType) {
			case 0:
				return append();
			case 1:
				return crop();
			case 2:
				return subTitle();
			}

			return false;
		}

		private boolean append() {
			try {
				// 複数の動画を読み込み
				Context c = this.getContext();
				String f1 = Utils.getAppDataVideoFilePath(c,"sample1.mp4");
				String f2 = Utils.getAppDataVideoFilePath(c,"sample2.mp4");
				Movie[] inMovies = new Movie[]{
						MovieCreator.build(f1),
						MovieCreator.build(f2)};

				// 1つのファイルに結合
				List<Track> videoTracks = new LinkedList<Track>();
				List<Track> audioTracks = new LinkedList<Track>();
				for (Movie m : inMovies) {
					for (Track t : m.getTracks()) {
						if (t.getHandler().equals("soun")) {
							audioTracks.add(t);
						}
						if (t.getHandler().equals("vide")) {
							videoTracks.add(t);
						}
					}
				}
				Movie result = new Movie();
				if (audioTracks.size() > 0) {
					result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
				}
				if (videoTracks.size() > 0) {
					result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
				}

				// 出力
				Container out = new DefaultMp4Builder().build(result);
				String outputFilePath = Utils.getAppDataVideoFilePath(c,"output_append.mp4");
				FileOutputStream fos = new FileOutputStream(new File(outputFilePath));
				out.writeContainer(fos.getChannel());
				fos.close();
			} catch (Exception e) {
				return false;
			}

			return true;
		}

		private boolean crop() {
			try {
				Context c = this.getContext();
				// オリジナル動画を読み込み
				String filePath = Utils.getAppDataVideoFilePath(c,"sample1.mp4");
				Movie originalMovie = MovieCreator.build(filePath);

				// 分割
				Track track = originalMovie.getTracks().get(0);
				Movie movie = new Movie();
				movie.addTrack(new AppendTrack(new CroppedTrack(track, 200, 400)));

				// 出力
				Container out = new DefaultMp4Builder().build(movie);
				String outputFilePath = Utils.getAppDataVideoFilePath(c,"output_crop.mp4");
				FileOutputStream fos = new FileOutputStream(new File(outputFilePath));
				out.writeContainer(fos.getChannel());
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		private boolean subTitle() {
			try {
				// オリジナル動画を読み込み
				Context c = this.getContext();
				String filePath = Utils.getAppDataVideoFilePath(c,"sample1.mp4");
				Movie countVideo = MovieCreator.build(filePath);

				// SubTitleを追加
				TextTrackImpl subTitleEng = new TextTrackImpl();
				subTitleEng.getTrackMetaData().setLanguage("eng");

				subTitleEng.getSubs().add(new TextTrackImpl.Line(0, 1000, "Five"));
				subTitleEng.getSubs().add(new TextTrackImpl.Line(1000, 2000, "Four"));
				subTitleEng.getSubs().add(new TextTrackImpl.Line(2000, 3000, "Three"));
				subTitleEng.getSubs().add(new TextTrackImpl.Line(3000, 4000, "Two"));
				subTitleEng.getSubs().add(new TextTrackImpl.Line(4000, 5000, "one"));
				countVideo.addTrack(subTitleEng);

				// 出力
				Container container = new DefaultMp4Builder().build(countVideo);
				String outputFilePath = Utils.getAppDataVideoFilePath(c,"output_subtitle.mp4");
				FileOutputStream fos = new FileOutputStream(outputFilePath);
				FileChannel channel = fos.getChannel();
				container.writeContainer(channel);
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}
	
	public boolean mergeAudioWithVideo(String videoFilePath, String audioFilePath, String outputFilePath) {
		Movie video;
		try {
			video = new MovieCreator().build(videoFilePath);
		} catch (RuntimeException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		Movie audio;
		try {
			audio = new MovieCreator().build(audioFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return false;
		}

		Track audioTrack = audio.getTracks().get(0);
		video.addTrack(audioTrack);

		Container out = new DefaultMp4Builder().build(video);

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(outputFilePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		BufferedWritableFileByteChannel byteBufferByteChannel = new BufferedWritableFileByteChannel(fos);
		try {
			out.writeContainer(byteBufferByteChannel);
			byteBufferByteChannel.close();
			fos.close();
			Log.e("MP4Parser", "mergeAudioWithVideo: End of audio/video merge");
			//sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
			//sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))));
			//File dirToScan = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			String[] toBeScannedStr = new String[]{outputFilePath/*dirToScan.getAbsolutePath(), dirToScan.getPath()*/};
			MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {
				@Override
				public void onScanCompleted(String path, Uri uri) {
					Log.e("MP4Parser", "MEDIA SCAN COMPLETED: " + path);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
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

	Runnable convert = new Runnable() {
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			try {
				Context c = getApplicationContext();
				String filePath = Utils.getAppDataVideoFilePath(c,AUDIO_RECORDING_FILE_NAME);
				File inputFile = new File(filePath);
				FileInputStream fis = new FileInputStream(inputFile);
				String outputFilePath = Utils.getAppDataVideoFilePath(c,COMPRESSED_AUDIO_FILE_NAME);
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
							Log.e("bytesRead","Readed "+bytesRead);
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
							Log.v(CONVERT_AUDIO_TAG, "Output format changed - " + outputFormat);
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
					Log.v(CONVERT_AUDIO_TAG, "Conversion % - " + percentComplete);
				} while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				fis.close();
				mux.stop();
				mux.release();
				Log.v(CONVERT_AUDIO_TAG, "Compression done ...");
			} catch (FileNotFoundException e) {
				Log.e(CONVERT_AUDIO_TAG, "File not found!", e);
			} catch (IOException e) {
				Log.e(CONVERT_AUDIO_TAG, "IO exception!", e);
			}
			//mStop = false;
			// Notify UI thread...
		}
	};
	
	
	private void muxAudioWithVideo(String audioFilePath, String outputFilePath) {

		String outputFile = "";

		try {

			//File file = new File(Environment.getExternalStorageDirectory() + File.separator + "final2.mp4");
			File file = new File(outputFilePath);
			file.createNewFile();
			outputFile = file.getAbsolutePath();

			MediaExtractor videoExtractor = new MediaExtractor();
			AssetFileDescriptor afdd = getAssets().openFd("Produce.MP4");
			videoExtractor.setDataSource(afdd.getFileDescriptor(), afdd.getStartOffset(), afdd.getLength());

			MediaExtractor audioExtractor = new MediaExtractor();
			audioExtractor.setDataSource(audioFilePath);

			Log.d(TAG, "Video Extractor Track Count " + videoExtractor.getTrackCount());
			Log.d(TAG, "Audio Extractor Track Count " + audioExtractor.getTrackCount());

			MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

			videoExtractor.selectTrack(0);
			MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
			int videoTrack = muxer.addTrack(videoFormat);

			audioExtractor.selectTrack(0);
			MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
			int audioTrack = muxer.addTrack(audioFormat);

			Log.d(TAG, "Video Format " + videoFormat.toString());
			Log.d(TAG, "Audio Format " + audioFormat.toString());

			boolean sawEOS = false;
			int frameCount = 0;
			int offset = 100;
			int sampleSize = 256 * 1024;
			ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
			ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
			MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
			MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


			videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
			audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

			muxer.start();

			while (!sawEOS) {
				videoBufferInfo.offset = offset;
				videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);


				if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
					Log.d(TAG, "saw input EOS.");
					sawEOS = true;
					videoBufferInfo.size = 0;

				} else {
					videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
					//videoBufferInfo.flags = videoExtractor.getSampleFlags();
					muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo);
					videoExtractor.advance();


					frameCount++;
					Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(KB) " + videoBufferInfo.size / 1024);
					Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(KB) " + audioBufferInfo.size / 1024);

				}
			}

			Toast.makeText(getApplicationContext(), "frame:" + frameCount, Toast.LENGTH_SHORT).show();

			boolean sawEOS2 = false;
			int frameCount2 = 0;
			while (!sawEOS2) {
				frameCount2++;

				audioBufferInfo.offset = offset;
				audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

				if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
					Log.d(TAG, "saw input EOS.");
					sawEOS2 = true;
					audioBufferInfo.size = 0;
				} else {
					audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
					//audioBufferInfo.flags = audioExtractor.getSampleFlags();
					muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
					audioExtractor.advance();


					Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs + " Flags:" + videoBufferInfo.flags + " Size(KB) " + videoBufferInfo.size / 1024);
					Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs + " Flags:" + audioBufferInfo.flags + " Size(KB) " + audioBufferInfo.size / 1024);

				}
			}

			Toast.makeText(getApplicationContext(), "frame:" + frameCount2, Toast.LENGTH_SHORT).show();

			muxer.stop();
			muxer.release();


		} catch (IOException e) {
			Log.d(TAG, "Mixer Error 1 " + e.getMessage());
		} catch (Exception e) {
			Log.d(TAG, "Mixer Error 2 " + e.getMessage());
		}
	}

}
