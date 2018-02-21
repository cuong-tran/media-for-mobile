using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class VideoCaptureController : MonoBehaviour {
#if UNITY_IOS

#elif UNITY_ANDROID

    public AndroidVideoCapturer Capturer;
    public int FPS = 30;
    public int Bitrate = 3000;
    public string AlbumName = "AR";
    public string FileName = "Capture_";
    public RecordUIManager recUIManager;

    private float ratio;
    private bool capturing = false;

    void Start () {
        SetVideoParameters();
    }

    private void SetVideoParameters() {
        int w = Screen.width;
        int h = Screen.height;
        if (w > 720 || h > 1280)
        {
            ratio = (float)Screen.height / (float)Screen.width;
            w = 720;
            h = (int)(w * ratio);
        }
        Capturer.VideoWidth = w;
        Capturer.VideoHeight = h;
        Capturer.VideoFrameRate = FPS;
        Capturer.VideoBitRate = Bitrate;
        Capturer.AlbumName = AlbumName;
        Capturer.FileName = FileName;
    }

    private void OnApplicationFocus(bool onFocus)
    {
        if (!onFocus) // lost focus
        {
            StopREC();
        }
    }

    void OnDestroy()
    {
        StopREC();
    }

    public void StartREC()
    {
        if (capturing == false)
        {
            capturing = true;
            Capturer.StartCapturing();
            recUIManager.RecordStart();
        }
    }

    public void StartREC(AudioClip delay)
    {
        if (capturing == false)
        {
            StartCoroutine(DelayStart(delay.length));
        }
    }

    IEnumerator DelayStart(float delay)
    {
        yield return new WaitForSeconds(delay);
        StartREC();
    }

    public void StopREC()
    {
        if (capturing == true)
        {
            Capturer.StopCapturing();
            capturing = false;
            recUIManager.RecordStop();
        }
    }
#endif	
}
