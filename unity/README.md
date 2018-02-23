[Source](https://software.intel.com/en-us/articles/intel-inde-media-pack-for-android-tutorials-video-capturing-for-unity3d-applications "Permalink to Intel® INDE Media for Mobile Tutorials - Video Capturing for Unity3d* Applications on Android*")

# Intel® INDE Media for Mobile Tutorials - Video Capturing for Unity3d* Applications on Android*

This tutorial explains how to use Intel® INDE Media for Mobile to add video capturing capability to [Unity][1] applications on Android.

**Prerequisites:**
* Unity 4.x Pro or [Unity 5.x Personal Edition](http://unity3d.com/get-unity)
* [Android SDK](https://developer.android.com/sdk/index.html)

This tutorial is about creating and compiling your own Unity Plugin for Android. Head over to the [INDE Media for Mobile GitHub page](https://github.com/INDExOS/media-for-mobile/tree/master/Tutorials/video-capturing-unity) to get access to the source code. So let's start.

Open Unity and create a new project. Under Project create a new directory named **/Plugins/** and then a directory **/Android/**.

Download and install Intel INDE by visiting http://intel.com/software/inde. After installing Intel INDE, choose to download and install the Media for Mobile. For additional assistance visit the Intel INDE [forum](http://software.intel.com/en-us/forums/intel-integrated-native-developer-experience-intel-inde).

Go to the installation folder of Media for Mobile -> libs and copy two jar files (android-\<version>.jar and domain-\<version>.jar) to your **/Assets/Plugins/Android/** folder.

![Assets/Plugins/Android folder][4]

In the same folder create a Java* file **Capturing.java** with the following code in it:

```java
    package com.intel.inde.mp.samples.unity;

    import com.intel.inde.mp.IProgressListener;
    import com.intel.inde.mp.domain.Resolution;
    import com.intel.inde.mp.android.graphics.FullFrameTexture;

    import android.opengl.GLES20;
    import android.os.Environment;
    import android.util.Log;
    import android.content.Context;

    import java.io.IOException;
    import java.io.File;

    public class Capturing
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

    	private static Capturing instance = null;

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

        public Capturing(Context context, int width, int height)
        {
    		videoCapture = new VideoCapture(context, progressListener);

    		this.width = width;
    		this.height = height;

    		texture = new FullFrameTexture();
    		sharedContext = new SharedContext();
    		instance = this;
        }

        public static Capturing getInstance()
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
```

Then create another Java file in the same directory. Name it** VideoCapture.java** and put the following contents in it:

```java
    package com.intel.inde.mp.samples.unity;

    import android.content.Context;
    import android.util.Log;

    import com.intel.inde.mp.*;
    import com.intel.inde.mp.android.AndroidMediaObjectFactory;
    import com.intel.inde.mp.android.AudioFormatAndroid;
    import com.intel.inde.mp.android.VideoFormatAndroid;

    import java.io.IOException;

    public class VideoCapture
    {
        private static final String TAG = "VideoCapture";

        private static final String Codec = "video/avc";
        private static int IFrameInterval = 1;

        private static final Object syncObject = new Object();
        private static volatile VideoCapture videoCapture;

        private static VideoFormat videoFormat;
        private static int videoWidth;
        private static int videoHeight;
        private GLCapture capturer;

        private boolean isConfigured;
        private boolean isStarted;
        private long framesCaptured;
    	private Context context;
    	private IProgressListener progressListener;

        public VideoCapture(Context context, IProgressListener progressListener)
        {
    		this.context = context;
            this.progressListener = progressListener;
        }

        public static void init(int width, int height, int frameRate, int bitRate)
        {
        	videoWidth = width;
        	videoHeight = height;

        	videoFormat = new VideoFormatAndroid(Codec, videoWidth, videoHeight);
        	videoFormat.setVideoFrameRate(frameRate);
            videoFormat.setVideoBitRateInKBytes(bitRate);
            videoFormat.setVideoIFrameInterval(IFrameInterval);
        }

        public void start(String videoPath) throws IOException
        {
            if (isStarted())
                throw new IllegalStateException(TAG + " already started!");

            capturer = new GLCapture(new AndroidMediaObjectFactory(context), progressListener);
            capturer.setTargetFile(videoPath);
            capturer.setTargetVideoFormat(videoFormat);

            AudioFormat audioFormat = new AudioFormatAndroid("audio/mp4a-latm", 44100, 2);
            capturer.setTargetAudioFormat(audioFormat);

            capturer.start();

            isStarted = true;
            isConfigured = false;
            framesCaptured = 0;
        }

        public void stop()
        {
            if (!isStarted())
                throw new IllegalStateException(TAG + " not started or already stopped!");

            try {
                capturer.stop();
                isStarted = false;
            } catch (Exception ex) {
            	Log.e(TAG, "--- Exception: GLCapture can't stop");
            }

            capturer = null;
            isConfigured = false;
        }

        private void configure()
        {
            if (isConfigured())
                return;

            try {
                capturer.setSurfaceSize(videoWidth, videoHeight);
                isConfigured = true;
            } catch (Exception ex) {
            }
        }

        public void beginCaptureFrame()
        {
            if (!isStarted())
                return;

            configure();
            if (!isConfigured())
            	return;

            capturer.beginCaptureFrame();
        }

        public void endCaptureFrame()
        {
            if (!isStarted() || !isConfigured())
                return;

            capturer.endCaptureFrame();
            framesCaptured++;
        }

        public boolean isStarted()
        {
            return isStarted;
        }

        public boolean isConfigured()
        {
            return isConfigured;
        }
    }
```

Create one more Java file. Name it **SharedContext.java** and put the following contents in it: 

```java
    package com.intel.inde.mp.samples.unity;

    import javax.microedition.khronos.egl.EGL10;
    import javax.microedition.khronos.egl.EGLConfig;
    import javax.microedition.khronos.egl.EGLContext;
    import javax.microedition.khronos.egl.EGLDisplay;
    import javax.microedition.khronos.egl.EGLSurface;

    import android.graphics.SurfaceTexture;
    import android.opengl.GLES11Ext;
    import android.opengl.GLES20;
    import android.opengl.GLUtils;
    import android.util.Log;

    public class SharedContext
    {
    	private static final String TAG = "SharedContext";

    	private EGL10 egl;
    	private EGLContext eglContext;
    	private EGLDisplay eglDisplay;
    	EGLConfig auxConfig;
    	private EGLSurface auxSurface = null;
    	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    	private static final int EGL_OPENGL_ES2_BIT = 4;
    	private int[] textures = new int[1];
    	private SurfaceTexture surfaceTexture;

    	SharedContext() {
    		egl = (EGL10)EGLContext.getEGL();

    		eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
    		if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
    			Log.e(TAG, "--- eglGetDisplay failed: " + GLUtils.getEGLErrorString(egl.eglGetError()));
    		}

    		int[] version = new int[2];
            if (!egl.eglInitialize(eglDisplay, version)) {
                Log.e(TAG, "--- eglInitialize failed: " + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            auxConfig = chooseEglConfig();
            if (auxConfig == null) {
                Log.e(TAG, "--- eglConfig not initialized");
            }

    		int[] contextAttrs = new int[] {
    				EGL_CONTEXT_CLIENT_VERSION, 2,
    				EGL10.EGL_NONE
    		};

    		// Create a shared context for this thread
    		EGLContext currentContext = egl.eglGetCurrentContext();
    		eglContext = egl.eglCreateContext(eglDisplay, auxConfig, currentContext, contextAttrs);
    		if (eglContext != null) {
    			Log.d(TAG, "--- eglContext created");
    		}

    		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
    		surfaceTexture = new SurfaceTexture(textures[0]);

    		auxSurface = egl.eglCreateWindowSurface(eglDisplay, auxConfig, surfaceTexture, null);
    		if (auxSurface == null || auxSurface == EGL10.EGL_NO_SURFACE) {
                Log.e(TAG,"--- createWindowSurface returned error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
            }
    	}

    	final int[] auxConfigAttribs = {
    		EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
    		EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
    		EGL10.EGL_RED_SIZE, 8,
    		EGL10.EGL_GREEN_SIZE, 8,
    		EGL10.EGL_BLUE_SIZE, 8,
    		EGL10.EGL_ALPHA_SIZE, 0,
    		EGL10.EGL_DEPTH_SIZE, 0,
    		EGL10.EGL_STENCIL_SIZE, 0,
    		EGL10.EGL_NONE
    	};

    	private EGLConfig chooseEglConfig() {
    		EGLConfig[] auxConfigs = new EGLConfig[1];
    		int[] auxConfigsCount = new int[1];
    		Log.d(TAG, "--- chooseEglConfig()");
    		if (!egl.eglChooseConfig(eglDisplay, auxConfigAttribs, auxConfigs, 1, auxConfigsCount)) {
    			throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(egl.eglGetError()));
    		} else if (auxConfigsCount[0] &gt; 0) {
    			return auxConfigs[0];
    		}
    		return null;
    	}

    	public void makeCurrent() {
    		if (!egl.eglMakeCurrent(eglDisplay, auxSurface, auxSurface, eglContext)) {
    			Log.e(TAG, "--- eglMakeCurrent failed: " + GLUtils.getEGLErrorString(egl.eglGetError()));
    		}
    	}

    	public void doneCurrent() {
    		if (!egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {
    			Log.e(TAG, "--- eglMakeCurrent failed: " + GLUtils.getEGLErrorString(egl.eglGetError()));
    		}
    	}
    }
```

**Important:**  Uncheck the **Auto Graphics API** option and leave only **OpenGLES2**:

![Unity PlayerSettings][5]

Please notice the package name **com.intel.inde.mp.samples.unity**. It has to be the same as in the player settings (Bundle identifier) in Unity. Moreover you have to use this name in the C# script to call our Java class. If it doesn't match you will get problems with the calls because your class definition won't be found by the VM and you will get a crash at launch.

We need to setup some simple 3D stuff for our test application. Of course, you can integrate Intel INDE Media for Mobile in your existing project. It's up to you. Be sure you have something moving in your scene.

Now, like for any other Android application, we need to setup a [manifest][6] XML file. This manifest file will tell at compilation time which activities should be launched and which functions are allowed to be accessed. In our case we can start from the default Unity manifest located in **C:\Program Files\UnityEditor\Data\PlaybackEngines\androidplayer\Apk**. So let's create a file **AndroidManifest.xml** under **/Plugins/Android** and place the following content in it:

```xml
    <!--?xml version="1.0" encoding="utf-8"?-->
    <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.intel.inde.mp.samples.unity" android:installlocation="preferExternal" android:theme="@android:style/Theme.NoTitleBar" android:versioncode="1" android:versionname="1.0">

        <uses-sdk android:minsdkversion="18">

    	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE">
        <uses-permission android:name="android.permission.INTERNET">

    	<!-- Microphone permissions-->
    	<uses-permission android:name="android.permission.RECORD_AUDIO">

    	<!-- Require OpenGL ES >= 2.0. -->
        <uses-feature android:glesversion="0x00020000" android:required="true">

        <application android:icon="@drawable/app_icon" android:label="@string/app_name" android:debuggable="true">
            <activity android:name="com.unity3d.player.UnityPlayerNativeActivity" android:label="@string/app_name">
                <intent-filter>
                    <action android:name="android.intent.action.MAIN">
                    <category android:name="android.intent.category.LAUNCHER">
                </category></action></intent-filter>
                <meta-data android:name="unityplayer.UnityActivity" android:value="true">
                <meta-data android:name="unityplayer.ForwardNativeEventsToDalvik" android:value="false">
            </meta-data></meta-data></activity>
        </application>

    </uses-feature></uses-permission></uses-permission></uses-permission></uses-sdk></manifest>
```

Notice the following important line:

```
    package="com.intel.inde.mp.samples.unity"
```

Now we have our AndroidManifest.xml and our Java files under **/Plugins/Android**. Instead of writing a long **cmd** line for compiling with **javac** with classpaths and so on, we are going to simplify the whole process by building an Apache Ant* script. Ant allows to quickly create a script for folders creation, call .exe or like in our case for classes generation. Another nice feature is that you have the possibility to import your Ant script into Eclipse*. **Notice**: if you are using other classes or libs you will need to adapt the following Ant script (you can check the official documentation at [http://ant.apache.org/manual/][7]). The below Ant script is only for the purpose of this tutorial.

Create a file named **build.xml** under **/Plugins/Android/** with the following content:

```xml
    <!--?xml version="1.0" encoding="UTF-8"?-->
    <project name="UnityCapturing">
        <!-- Change this in order to match your configuration -->
        <property name="sdk.dir" value="C:Androidsdk">
        <property name="target" value="android-22">
        <property name="unity.androidplayer.jarfile" value="C:Program FilesUnityEditorDataPlaybackEnginesandroidplayerdevelopmentbinclasses.jar">
        <!-- Source directory -->
        <property name="source.dir" value="ProjectPathAssetsPluginsAndroid">
        <!-- Output directory for .class files-->
        <property name="output.dir" value="ProjectPathAssetsPluginsAndroidclasses">
        <!-- Name of the jar to be created. Please note that the name should match the name of the class and the name
        placed in the AndroidManifest.xml-->
        <property name="output.jarfile" value="Capturing.jar">
          <!-- Creates the output directories if they don't exist yet. -->
        <target name="-dirs" depends="message">
            <echo>Creating output directory: ${output.dir} </echo>
            <mkdir dir="${output.dir}">
        </mkdir></target>
       <!-- Compiles this project's .java files into .class files. -->
        <target name="compile" depends="-dirs" description="Compiles project's .java files into .class files">
            <javac encoding="ascii" target="1.6" debug="true" destdir="${output.dir}" verbose="${verbose}" includeantruntime="false">
                <src path="${source.dir}">
                <classpath>
                    <pathelement location="${sdk.dir}platforms${target}android.jar">
    				<pathelement location="${source.dir}domain-1.2.2415.jar">
    				<pathelement location="${source.dir}android-1.2.2415.jar">
                    <pathelement location="${unity.androidplayer.jarfile}">
                </pathelement></pathelement></pathelement></pathelement></classpath>
            </src></javac>
        </target>
        <target name="build-jar" depends="compile">
            <zip zipfile="${output.jarfile}" basedir="${output.dir}">
        </zip></target>
        <target name="clean-post-jar">
             <echo>Removing post-build-jar-clean</echo>
             <delete dir="${output.dir}">
        </delete></target>
        <target name="clean" description="Removes output files created by other targets.">
            <delete dir="${output.dir}" verbose="${verbose}">
        </delete></target>
        <target name="message">
         <echo>Android Ant Build for Unity Android Plugin</echo>
            <echo>   message:      Displays this message.</echo>
            <echo>   clean:     Removes output files created by other targets.</echo>
            <echo>   compile:   Compiles project's .java files into .class files.</echo>
            <echo>   build-jar: Compiles project's .class files into .jar file.</echo>
        </target>
    </property></property></property></property></property></property></project>
```

Notice that you must adjust two paths (**source.dir, output.dir**) and, of course, the name of the output jar (**output.jarfile**).

If you don't have Ant, you can obtain it from the [Apache Ant home page][8]. Install it and make sure it is in your executable PATH. Before calling Ant, you need to declare the JAVA_HOME environment variable to specify the path where the Java Development Kit (JDK) is installed. Do not forget to add **<ant_home>/bin** to **PATH**.

Run the Windows* Command Processor (**cmd.exe**), change current directory to **/Plugins/Android** folder and type the following command to launch the build script:

```
    ant build-jar clean-post-jar
```

After a few seconds you should get the message that everything was correctly built!

![Ant output][9]

You've compiled your jar! Notice the new file **Capturing.jar** in the directory.

Switch to Unity. Create **Capture.cs** script with the following code in it:

```c#
    using UnityEngine;
    using System.Collections;
    using System.IO;
    using System;

    [RequireComponent(typeof(Camera))]
    public class Capture : MonoBehaviour
    {
    	public int videoWidth = 720;
    	public int videoHeight = 1094;
    	public int videoFrameRate = 15;
    	public int videoBitRate = 3000;

    	private string videoDir;
    	public string fileName = "game_capturing-";

    	private IntPtr capturingObject = IntPtr.Zero;
    	private float startTime = 0.0f;
    	private float nextCaptureTime = 0.0f;
    	public bool isRunning { get; private set; }

    	private AndroidJavaObject playerActivityContext = null;

    	private static IntPtr constructorMethodID = IntPtr.Zero;
    	private static IntPtr initCapturingMethodID = IntPtr.Zero;
    	private static IntPtr startCapturingMethodID = IntPtr.Zero;
    	private static IntPtr captureFrameMethodID = IntPtr.Zero;
    	private static IntPtr stopCapturingMethodID = IntPtr.Zero;

    	private static IntPtr getDirectoryDCIMMethodID = IntPtr.Zero;

    	void Start()
    	{
    		if (!Application.isEditor) {
    			// First, obtain the current activity context
    			using (AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer")) {
    				playerActivityContext = jc.GetStatic<androidjavaobject>("currentActivity");
    			}

    			// Search for our class
    			IntPtr classID = AndroidJNI.FindClass("com/intel/inde/mp/samples/unity/Capturing"); // com.intel.inde.mp.samples.unity // com/intel/penelope/Capturing

    			// Search for it's contructor
    			constructorMethodID = AndroidJNI.GetMethodID(classID, "<init>", "(Landroid/content/Context;II)V");
    			if (constructorMethodID == IntPtr.Zero) {
    				Debug.LogError("Can't find Capturing constructor.");
    				return;
    			}

    			// Register our methods
    			initCapturingMethodID = AndroidJNI.GetMethodID(classID, "initCapturing", "(IIII)V");
    			if (initCapturingMethodID == IntPtr.Zero) {
    				Debug.LogError("Can't find initCapturing() method.");
    				return;
    			}
    			startCapturingMethodID = AndroidJNI.GetMethodID(classID, "startCapturing", "(Ljava/lang/String;)V");
    			if (startCapturingMethodID == IntPtr.Zero) {
    				Debug.LogError("Can't find startCapturing() method.");
    				return;
    			}
    			captureFrameMethodID = AndroidJNI.GetMethodID(classID, "captureFrame", "(I)V");
    			if (captureFrameMethodID == IntPtr.Zero) {
    				Debug.LogError("Can't find captureFrame() method.");
    				return;
    			}
    			stopCapturingMethodID = AndroidJNI.GetMethodID(classID, "stopCapturing", "()V");
    			if (stopCapturingMethodID == IntPtr.Zero) {
    				Debug.LogError("Can't find stopCapturingMethodID() method.");
    				return;
    			}

    			// Register and call our static method
    			getDirectoryDCIMMethodID = AndroidJNI.GetStaticMethodID(classID, "getDirectoryDCIM", "()Ljava/lang/String;");
    			jvalue[] args = new jvalue[0];
    			videoDir = AndroidJNI.CallStaticStringMethod(classID, getDirectoryDCIMMethodID, args);

    			// Create Capturing object
    			jvalue[] constructorParameters = AndroidJNIHelper.CreateJNIArgArray(new object [] {
    				playerActivityContext, Screen.width, Screen.height
    			});
    			IntPtr local_capturingObject = AndroidJNI.NewObject(classID, constructorMethodID, constructorParameters);
    			if (local_capturingObject == IntPtr.Zero) {
    				Debug.LogError("--- Can't create Capturing object.");
    				return;
    			}
    			// Keep a global reference to it
    			capturingObject = AndroidJNI.NewGlobalRef(local_capturingObject);
    			AndroidJNI.DeleteLocalRef(local_capturingObject);

    			AndroidJNI.DeleteLocalRef(classID);
    		}
    		isRunning = false;
    		Debug.Log("--- videoFrameRate = " + videoFrameRate);
    		Debug.Log("--- 1.0f / videoFrameRate = " + 1.0f / videoFrameRate);
    	}

    	void OnRenderImage(RenderTexture src, RenderTexture dest)
    	{
    		Graphics.Blit(src, dest);
    		if (isRunning) {
    			float elapsedTime = Time.time - startTime;
    			if (elapsedTime &gt;= nextCaptureTime) {
    				CaptureFrame(src.GetNativeTexturePtr().ToInt32());
    				nextCaptureTime += 1.0f / videoFrameRate;
    			}
    		}
    	}

    	public void StartCapturing()
    	{
    		if (capturingObject == IntPtr.Zero)
    			return;

    		jvalue[] videoParameters =  new jvalue[4];
    		videoParameters[0].i = videoWidth;
    		videoParameters[1].i = videoHeight;
    		videoParameters[2].i = videoFrameRate;
    		videoParameters[3].i = videoBitRate;
    		AndroidJNI.CallVoidMethod(capturingObject, initCapturingMethodID, videoParameters);
    		DateTime date = DateTime.Now;
    		string fullFileName = fileName + date.ToString("ddMMyy-hhmmss.fff") + ".mp4";
    		jvalue[] args = new jvalue[1];
    		args[0].l = AndroidJNI.NewStringUTF(videoDir + fullFileName);
    		AndroidJNI.CallVoidMethod(capturingObject, startCapturingMethodID, args);

    		startTime = Time.time;
    		nextCaptureTime = 0.0f;
    		isRunning = true;
    	}

    	private void CaptureFrame(int textureID)
    	{
    		if (capturingObject == IntPtr.Zero)
    			return;

    		jvalue[] args = new jvalue[1];
    		args[0].i = textureID;
    		AndroidJNI.CallVoidMethod(capturingObject, captureFrameMethodID, args);
    	}

    	public void StopCapturing()
    	{
    		isRunning = false;

    		if (capturingObject == IntPtr.Zero)
    			return;

    		jvalue[] args = new jvalue[0];
    		AndroidJNI.CallVoidMethod(capturingObject, stopCapturingMethodID, args);
    	}
    }
```

Add this script to your Main Camera. Before starting capturing you have to configure the video format. Parameters names speak for themselves. You can tweak them directly from Unity Editor GUI.

We don't focus on **Start()**, **StartCapturing()** and **StopCapturing()** methods. They are trivial if you are familiar with the Java Native Interface ([JNI][10]). Let's go deeper. Check the **OnRenderImage()** method. **OnRenderImage()** is called after all rendering is complete to render the image. The incoming image is the source render texture. The result should end up in the destination render texture. It allows you to modify the final image by processing it with shader based filters. But we want to just copy the source texture into the destination render texture by calling [Graphics.Blit()][11] without any special effects. Before that we pass the native ("hardware") texture handle to the **captureFrame()** method  of our **Capturing.java** class.

**StartCapturing()** and **StopCapturing()** methods are public. So you can call them from another script. Let's create one more C# script called **CaptureGUI.cs**:

```c#
    using UnityEngine;
    using System.Collections;

    public class CaptureGUI : MonoBehaviour
    {
    	public Capture capture;
    	private GUIStyle style = new GUIStyle();

    	void Start()
    	{
    		style.fontSize = 48;
    		style.alignment = TextAnchor.MiddleCenter;
    	}

    	void OnGUI()
    	{
    		style.normal.textColor = capture.inProgress ? Color.red : Color.green;
    		if (GUI.Button(new Rect(10, 200, 350, 100), capture.inProgress ? "[Stop Recording]" : "[Start Recording]", style)) {
    			if (capture.inProgress) {
    				capture.StopCapturing();
    			} else {
    				capture.StartCapturing();
    			}
    		}
    	}

    }
```

Add this script to any object in your scene. Don't forget to assign your **Capture.cs** instance to public **capture** member.

It's all you need to know to be able to add video capturing capability to Unity applications. Now **Build &amp; Run** your test application for Android platform. You can find recorded videos in **/mnt/sdcard/DCIM/** folder of your Android device. Another [tutorial][12] may help you explore the logic of Capturing.java and VideoCapture.java code in more details.

**Known issues:**

[1]: http://unity3d.com/unity
[2]: https://github.com/INDExOS/media-for-mobile/tree/master/Tutorials/video-capturing-unity
[3]: http://software.intel.com/en-us/forums/intel-integrated-native-developer-experience-intel-inde
[4]: https://software.intel.com/sites/default/files/managed/c5/eb/tutorial_5_1.png
[5]: https://software.intel.com/sites/default/files/managed/ac/31/Unity_PlayerSettings.png "Unity PlayerSettings"
[6]: http://developer.android.com/guide/topics/manifest/manifest-intro.html
[7]: http://ant.apache.org/manual
[8]: http://ant.apache.org/
[9]: https://software.intel.com/sites/default/files/managed/07/5e/tutorial_5_3.png "Ant output"
[10]: http://developer.android.com/training/articles/perf-jni.html
[11]: http://docs.unity3d.com/Documentation/ScriptReference/Graphics.Blit.html
[12]: http://software.intel.com/en-us/articles/intel-inde-media-pack-for-android-tutorials-video-capturing-for-opengl-applications