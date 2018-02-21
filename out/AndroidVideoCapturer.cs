using UnityEngine;
using System.Collections;
using System.IO;
using System;

[RequireComponent(typeof(Camera))]
public class AndroidVideoCapturer : MonoBehaviour
{
    private int videoWidth=720;
    private int videoHeight = 1280;
    private int videoFrameRate = 15;
    private int videoBitRate = 3000;
    private string albumName="ar";
    private string fileName="capture";
    public int VideoWidth { set { videoWidth = value; } }
    public int VideoHeight { set { videoHeight = value; } }
    public int VideoFrameRate { set { videoFrameRate = value; } }
    public int VideoBitRate { set { videoBitRate = value; } }
    public string AlbumName { set { albumName = value; } }
    public string FileName { set { fileName = value; } }

    private string videoDir;
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
    private string fileFullPath;

    void Start()
    {
        if (!Application.isEditor)
        {
            // First, obtain the current activity context
            using (AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                playerActivityContext = jc.GetStatic<AndroidJavaObject>("currentActivity");
            }

            // Search for our class
			IntPtr classID = AndroidJNI.FindClass("com/sancalpa/capturer/Capturer");

            // Search for it's contructor
            constructorMethodID = AndroidJNI.GetMethodID(classID, "<init>", "(Landroid/content/Context;II)V");
            if (constructorMethodID == IntPtr.Zero)
            {
                Debug.LogError("Can't find Capturing constructor.");
                return;
            }

            // Register our methods
            initCapturingMethodID = AndroidJNI.GetMethodID(classID, "initCapturing", "(IIII)V");
            if (initCapturingMethodID == IntPtr.Zero)
            {
                Debug.LogError("Can't find initCapturing() method.");
                return;
            }
            startCapturingMethodID = AndroidJNI.GetMethodID(classID, "startCapturing", "(Ljava/lang/String;)V");
            if (startCapturingMethodID == IntPtr.Zero)
            {
                Debug.LogError("Can't find startCapturing() method.");
                return;
            }
            captureFrameMethodID = AndroidJNI.GetMethodID(classID, "captureFrame", "(I)V");
            if (captureFrameMethodID == IntPtr.Zero)
            {
                Debug.LogError("Can't find captureFrame() method.");
                return;
            }
            stopCapturingMethodID = AndroidJNI.GetMethodID(classID, "stopCapturing", "()V");
            if (stopCapturingMethodID == IntPtr.Zero)
            {
                Debug.LogError("Can't find stopCapturingMethodID() method.");
                return;
            }

            // Register and call our static method
            getDirectoryDCIMMethodID = AndroidJNI.GetStaticMethodID(classID, "getDirectoryDCIM", "()Ljava/lang/String;");
            jvalue[] args = new jvalue[0];
            videoDir = AndroidJNI.CallStaticStringMethod(classID, getDirectoryDCIMMethodID, args);

            // Create Capturing object
            jvalue[] constructorParameters = AndroidJNIHelper.CreateJNIArgArray(new object[] {
                playerActivityContext, Screen.width, Screen.height
            });
            IntPtr local_capturingObject = AndroidJNI.NewObject(classID, constructorMethodID, constructorParameters);
            if (local_capturingObject == IntPtr.Zero)
            {
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
        if (isRunning)
        {
            float elapsedTime = Time.time - startTime;
            if (elapsedTime >= nextCaptureTime)
            {
                CaptureFrame(src.GetNativeTexturePtr().ToInt32());
                nextCaptureTime += 1.0f / videoFrameRate;
            }
        }
    }

    public void StartCapturing()
    {
        if (capturingObject == IntPtr.Zero)
            return;
        jvalue[] videoParameters = new jvalue[4];
        videoParameters[0].i = videoWidth;
        videoParameters[1].i = videoHeight;
        videoParameters[2].i = videoFrameRate;
        videoParameters[3].i = videoBitRate;
        AndroidJNI.CallVoidMethod(capturingObject, initCapturingMethodID, videoParameters);
        DateTime date = DateTime.Now;
        string fullFileName = fileName + date.ToString("ddMMyy-hhmmss.fff") + ".mp4";
        fileFullPath = videoDir + albumName + "/" + fullFileName;
        Directory.CreateDirectory(videoDir + albumName);
        jvalue[] args = new jvalue[1];
        args[0].l = AndroidJNI.NewStringUTF(fileFullPath);
        AndroidJNI.CallVoidMethod(capturingObject, startCapturingMethodID, args);
        startTime = Time.time;
        nextCaptureTime = 0.0f;
        isRunning = true;
        Debug.Log(fileFullPath);
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

		AndroidJavaClass intentClass = new AndroidJavaClass("android.content.Intent");
		string action = intentClass.GetStatic<string>("ACTION_MEDIA_SCANNER_SCAN_FILE");

		// Intent intentObject = new Intent(action);
		AndroidJavaObject intentObject = new AndroidJavaObject("android.content.Intent", action);

        // Uri uriObject = Uri.parse("file:" + fileFullPath);
        AndroidJavaClass uriClass = new AndroidJavaClass("android.net.Uri");
		AndroidJavaObject uriObject = uriClass.CallStatic<AndroidJavaObject>("parse", "file:" + fileFullPath);

		// intentObject.setData(uriObject);
		intentObject.Call<AndroidJavaObject>("setData", uriObject);

		// this.sendBroadcast(intentObject);
		playerActivityContext.Call("sendBroadcast", intentObject);    
    }
}