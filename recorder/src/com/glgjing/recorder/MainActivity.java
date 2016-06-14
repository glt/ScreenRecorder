package com.glgjing.recorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
  private static final int RECORD_REQUEST_CODE  = 101;
  private static final int STORAGE_REQUEST_CODE = 102;
  private static final int AUDIO_REQUEST_CODE   = 103;

  private MediaProjectionManager projectionManager;
  private MediaProjection mediaProjection;
  private RecordService recordService;
  private Button startBtn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    setContentView(R.layout.activity_main);

    startBtn = (Button) findViewById(R.id.start_record);
    startBtn.setEnabled(false);
    startBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (recordService.isRunning()) {
          recordService.stopRecord();
          startBtn.setText(R.string.start_record);
        } else {
          Intent captureIntent = projectionManager.createScreenCaptureIntent();
          startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
        }
      }
    });

    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
    }

    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
    }

    Intent intent = new Intent(this, RecordService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(connection);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
      mediaProjection = projectionManager.getMediaProjection(resultCode, data);
      recordService.setMediaProject(mediaProjection);
      recordService.startRecord();
      startBtn.setText(R.string.stop_record);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == STORAGE_REQUEST_CODE || requestCode == AUDIO_REQUEST_CODE) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        finish();
      }
    }
  }

  private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      DisplayMetrics metrics = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(metrics);
      RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
      recordService = binder.getRecordService();
      recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
      startBtn.setEnabled(true);
      startBtn.setText(recordService.isRunning() ? R.string.stop_record : R.string.start_record);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {}
  };
}
