How to Use plugin:
    1. Copy:
    "android.jar", "domain.jar", "capturer.jar" files into "out" folder to /Assets/Plugin/Android/;
    2. Copy:
    AndroidScreenCapturer.cs and CaptureManager.cs into "out" folder to /Assets/Scripts/;
    3. CaptureManager give all public methods for manage screen capturing;
    
How to create Unity plugin:

    1. Take your changes in project files;
    2. For android, domain and capturer modules start gradle task "createJar" or "jar";
    3. Plugin files updated in "out" folder.
    
Thanx. 


Legal Information
-----------------

AndroidScreenCapturer is distributed under Apache License 2.0, see LICENSE.txt and NOTICE.txt files in the root folder for details.
