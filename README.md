# ScreenRecorder
## Android 5.0+ 屏幕录制
从 Android 4.4 开始支持手机端本地录屏，但首先需要获取 root 权限才行，Android 5.0 引入 MediaProject，
可以不用 root 就可以录屏，但需要弹权限获取窗口，需要用户允许才行，这里主要介绍 Android 5.0+ 利用
MediaProject 在非 root 情况下实现屏幕录制。

## 基本原理
在 Android 5.0，Google 终于开放了视频录制的接口，其实严格来说，是屏幕采集的接口，也就是 MediaProjection
和 MediaProjectionManager。 

## 具体实现步骤
### 1 申请权限
在 AndroidManifest 中添加权限
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```
Android 6.0 加入的动态权限申请，如果应用的 `targetSdkVersion` 是 23，申请敏感权限还需要动态申请
```
if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    != PackageManager.PERMISSION_GRANTED) {  
  ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
}
if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {  
  ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
}
```
### 2 获取 MediaProjectionManager 实例
`MediaProjectionManager ` 也是系统服务的一种，通过 `getSystemService` 来获取实例
```
MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
```
### 3 发起屏幕捕捉请求
```
Intent captureIntent= projectionManager.createScreenCaptureIntent(); 
startActivityForResult(captureIntent, REQUEST_CODE);
```
### 4 获取 MediaProjection
 通过 `onActivityResult` 返回结果获取 `MediaProjection `
```
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
  }
}
```
### 5 创建虚拟屏幕
这一步就是通过 `MediaProject` 录制屏幕的关键所在，`VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR` 参数是指创建屏幕镜像，所以我们实际录制内容的是屏幕镜像，但内容和实际屏幕是一样的，并且这里我们把 `VirtualDisplay` 的渲染目标 Surface 设置为 `MediaRecorder` 的 `getSurface`，后面我就可以通过 `MediaRecorder` 将屏幕内容录制下来，并且存成 video 文件

```
private void createVirtualDisplay() {
  virtualDisplay = mediaProjection.createVirtualDisplay(
        "MainScreen",
        width,
        height,
        dpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mediaRecorder.getSurface(),
        null, null);
}
```
### 6 录制屏幕数据
这里利用 `MediaRecord` 将屏幕内容保存下来，当然也可以利用其它方式保存屏幕内容，例如：`ImageReader`
```
private void initRecorder() {
  File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".mp4");
  mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
  mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
  mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
  mediaRecorder.setOutputFile(file.getAbsolutePath());
  mediaRecorder.setVideoSize(width, height);
  mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
  mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
  mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
  mediaRecorder.setVideoFrameRate(30);
  try {
    mediaRecorder.prepare();
  } catch (IOException e) {
    e.printStackTrace();
  }
}

public boolean startRecord() {
  if (mediaProjection == null || running) {
    return false;
  }
  initRecorder();
  createVirtualDisplay();
  mediaRecorder.start();
  running = true;
  return true;
}
```
