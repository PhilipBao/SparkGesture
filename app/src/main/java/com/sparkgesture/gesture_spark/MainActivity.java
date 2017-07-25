package com.sparkgesture.gesture_spark;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private boolean cameraStatus;
    private TextView noteText;
    private static final int brightUp = 0;
    private static final int brightDown = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        noteText = (TextView) this.findViewById(R.id.notification_text);
        setTitle("Screen Gestures");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // This example shows an Activity, but you would use the same approach if
    // you were subclassing a View.
    // https://developer.android.com/training/gestures/detector.html
    @Override
    public boolean onTouchEvent(MotionEvent event){

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.d(TAG,"Action was DOWN");
                //toggleFlashlight();
                //takeScreenShot();
                //openCamera();
                //adjustBrightness(brightUp, 2);
                changeRingerMode();

                return true;
            case (MotionEvent.ACTION_MOVE) :
                Log.d(TAG,"Action was MOVE");
                return true;
            case (MotionEvent.ACTION_UP) :
                Log.d(TAG,"Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL) :
                Log.d(TAG,"Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                Log.d(TAG,"Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }

    public void toggleFlashlight() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Log.v(TAG, "Ask for camera permissions");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        0);
            }
        } else {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually front camera is at 0 position.
            try {
                cameraId = camManager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (!cameraStatus) {
                        camManager.setTorchMode(cameraId, true);
                        cameraStatus = true;
                        setNotificationText("Torch On!");
                        Log.v(TAG, "Torch Off");
                    } else {
                        camManager.setTorchMode(cameraId, false);
                        cameraStatus = false;
                        setNotificationText("Torch Off!");
                        Log.v(TAG, "Torch On");
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void takeScreenShot() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Log.v(TAG, "Ask for write external storage permissions");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);
            }
        } else {
            Date now = new Date();
            android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

            try {
                // image naming and path  to include sd card  appending name you choose for file
                String mPath = "/sdcard/DCIM/Camera/" + now + ".jpg";

                // create bitmap screen capture
                View v1 = getWindow().getDecorView().getRootView();
                v1.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                v1.setDrawingCacheEnabled(false);

                File imageFile = new File(mPath);

                FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

                MediaScannerConnection.scanFile(this,
                        new String[]{imageFile.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i(TAG, "Scanned " + path + ":");
                                Log.i(TAG, "-> uri=" + uri);
                            }
                        });

                setNotificationText("Screenshot Captured!");
                Log.v(TAG, "Screenshot Captured");
            } catch (Throwable e) {
                // Several error may come out with file handling or OOM
                e.printStackTrace();
            }
        }
    }

    public void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 0);
        setNotificationText("Camera Opened!");
        Log.v(TAG, "Open Camera");
    }

    public void dismissAlarm() {
        Intent dismissIntent = new Intent();
        dismissIntent.setAction("com.android.deskclock.ALARM_DISMISS");
        sendBroadcast(dismissIntent);
        Log.v(TAG, "Dismiss Alarm");
    }

    public void adjustBrightness(final int upDown, final int changeAmt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                // 0 - up
                // 1 - down
                if (upDown == 0) {
                    /*Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);*/
                    try {
                        int bright = Settings.System.getInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS);
                        if (bright > 250) {
                            return;
                        }

                        Settings.System.putInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, bright + changeAmt);
                        Log.v(TAG, String.valueOf(bright));
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }

                    setNotificationText("Brightness Increased!");
                    Log.v(TAG, "Brightness Increase");
                } else {
                    try {
                        int bright = Settings.System.getInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS);
                        if (bright < 5) {
                            return;
                        }

                        Settings.System.putInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, bright - changeAmt);
                        Log.v(TAG, String.valueOf(bright));
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }

                    setNotificationText("Brightness Decreased!");
                    Log.v(TAG, "Brightness Decrease");
                }
            } else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                Log.v(TAG, "Ask for write android settings permissions");
            }
        }
    }

    public void changeRingerMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.
                    ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        } else {
            final AudioManager mode = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

            if (mode.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                mode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                setNotificationText("Ringer Mode - Silent!");
                Log.v(TAG, "Ringer silent");
            } else if (mode.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                mode.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                setNotificationText("Ringer Mode - Vibrate!");
                Log.v(TAG, "Ringer vibrate");
            } else if (mode.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                mode.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                setNotificationText("Ringer Mode - Normal!");
                Log.v(TAG, "Ringer normal");
            }
        }
    }

    private boolean isNotificationPolicyAccessGranted()  {
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return false;
    }

    private void setNotificationText(final String action) {
        noteText.setText(action);
    }
}
