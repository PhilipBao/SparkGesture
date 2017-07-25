package com.sparkgesture.gesture_spark;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private boolean cameraStatus;
    private TextView noteText;
    private static final int brightUp = 0;
    private static final int brightDown = 1;

    private enum Gesture {
        UP,
        DOWN,
        NONE,
        LEFT,
        RIGHT
    }

    private Hashtable<Integer, Point2D> mActivePointers;
    private Hashtable<Integer, Gesture> mActiveGestures;
    private Gesture mDominateGesture;
    private int mDominateCnt;
    private boolean fired;

    private final float MIN_MOVE_DIST = 20.0f;

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
        setTitle("Qwk Screen Gestures");

        mActivePointers = new Hashtable<>();
        mActiveGestures = new Hashtable<>();
        mDominateGesture = Gesture.NONE;
        mDominateCnt = 0;
        fired = false;

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
            Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
            //settingIntent.putExtra("key", 0); //Optional parameters
            MainActivity.this.startActivity(settingIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onTouchEvent(MotionEvent event) {

        // get pointer index from the event object
        int pointerIndex = event.getActionIndex();

        // get pointer ID
        int pointerId = event.getPointerId(pointerIndex);

        // get masked (not specific to a pointer) action
        int maskedAction = event.getActionMasked();

        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                Log.d(TAG, "MotionEvent.DOWN: ~~~~~");
                // We have a new pointer. Lets add it to the list of pointers

                Point2D f = new Point2D();
                f.x = event.getX(pointerIndex);
                f.y = event.getY(pointerIndex);
                mActivePointers.put(pointerId, f);
                Log.d(TAG, "    ~~~~~");
                break;
            }
            case MotionEvent.ACTION_MOVE: { // a pointer was moved
                Log.d(TAG, "MotionEvent.ACTION_MOVE: ~~~~~");
                for (int size = event.getPointerCount(), i = 0; i < size; i++) {
                    int pntId = event.getPointerId(i);
                    Point2D point = mActivePointers.get(pntId);

                    if (point != null) {
                        float oldX = point.x;
                        float oldY = point.y;
                        point.x = event.getX(i);
                        point.y = event.getY(i);
                        Log.d(TAG, "oldX: " + oldX + " X: " + point.x + " oldY: " + oldY + " Y: " + point.y);
                        float deltaX = point.x - oldX;
                        float deltaY = point.y - oldY;
                        if (Math.abs(deltaX) < MIN_MOVE_DIST) {
                            point.x = oldX;
                        }
                        if (Math.abs(deltaY) < MIN_MOVE_DIST) {
                            point.y = oldY;
                        }
                        mActivePointers.put(pntId, point);

                        Gesture onGoingGesture = Gesture.NONE;

                        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > MIN_MOVE_DIST) {
                            if (deltaX < 0) {
                                onGoingGesture = Gesture.LEFT;
                            } else {
                                onGoingGesture = Gesture.RIGHT;
                            }
                        } else if (Math.abs(deltaY) > MIN_MOVE_DIST){
                            if (deltaY < 0) {
                                onGoingGesture = Gesture.UP;
                            } else {
                                onGoingGesture = Gesture.DOWN;
                            }
                        }

                        Log.d(TAG, "onGoingGesture: " + onGoingGesture + " deltaX: " + deltaX + " deltaY: " + deltaY);

                        if (mActiveGestures.get(pntId) != onGoingGesture && onGoingGesture != Gesture.NONE) {
                            if (onGoingGesture != mDominateGesture) {
                                --mDominateCnt;
                                if (mDominateCnt <= 0) {
                                    mDominateGesture = onGoingGesture;
                                    mDominateCnt = 1;
                                }
                            } else {
                                ++mDominateCnt;
                            }
                            mActiveGestures.put(pntId, onGoingGesture);
                            Log.d(TAG, "onGoingGesture: " + onGoingGesture + " deltaX: " + deltaX + " deltaY: " + deltaY);
                        }


                    }
                }
                Log.d(TAG, "dominate gesture: " + mDominateGesture + " with dominate count: " + mDominateCnt);
                Log.d(TAG, "    ~~~~~");
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                Log.d(TAG, "MotionEvent.UP: ~~~~~");
                mActivePointers.remove(pointerId);
                Gesture gestureEnd = mActiveGestures.get(pointerId);
                mActiveGestures.remove(pointerId);
                if (gestureEnd == mDominateGesture) {
                    if (!fired) {
                        Toast.makeText(this, "Fired Gesture: " + mDominateGesture + " Finger count: " + mDominateCnt + "!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Fired Gesture: " + mDominateGesture + " Finger count: " + (mDominateCnt) + "!");
                        doActionFromPrefs();
                        fired =true;
                    }
                    --mDominateCnt;
                    if (mDominateCnt <= 0) {
                        mDominateGesture = Gesture.NONE;
                        mDominateCnt = 0;
                    }
                }
                if (mActivePointers.isEmpty()) {
                    mDominateGesture = Gesture.NONE;
                    mDominateCnt = 0;
                    fired = false;
                }
                Log.d(TAG, "dominate gesture: " + mDominateGesture + " with dominate count: " + mDominateCnt);
                Log.d(TAG, "    ~~~~~");
                break;
            }
        }
        return true;
    }

    private void doActionFromPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String action = "";

        String a = sharedPreferences.getString("gesture0_direction_settings", "");
        String b = sharedPreferences.getString("gesture0_finger_cnt_settings", "");
        String c = mDominateGesture.toString();
        String d = Integer.toString(mDominateCnt);


        if (mDominateGesture.toString().equals(sharedPreferences.getString("gesture0_direction_settings", "").toUpperCase())
                 && Integer.toString(mDominateCnt).equals(sharedPreferences.getString("gesture0_finger_cnt_settings", ""))) {
            action = sharedPreferences.getString("gesture0_action_settings", "");
        }
        if (mDominateGesture.toString().equals(sharedPreferences.getString("gesture1_direction_settings", "").toUpperCase())
                && Integer.toString(mDominateCnt).equals(sharedPreferences.getString("gesture1_finger_cnt_settings", ""))) {
            action = sharedPreferences.getString("gesture1_action_settings", "");
        }
        if (mDominateGesture.toString().equals(sharedPreferences.getString("gesture2_direction_settings", "").toUpperCase())
                && Integer.toString(mDominateCnt).equals(sharedPreferences.getString("gesture2_finger_cnt_settings", ""))) {
            action = sharedPreferences.getString("gesture2_action_settings", "");
        }
        if (mDominateGesture.toString().equals(sharedPreferences.getString("gesture3_direction_settings", "").toUpperCase())
                && Integer.toString(mDominateCnt).equals(sharedPreferences.getString("gesture3_finger_cnt_settings", ""))) {
            action = sharedPreferences.getString("gesture3_action_settings", "");
        }

        doAction(action);
    }

    private void doAction(String action) {
        switch (action) {
            case "Flashlight": toggleFlashlight(); break;
            case "Camera": openCamera(); break;
            case "Screenshot": takeScreenShot(); break;
            case "Brightness Up": adjustBrightness(brightUp, 10); break;
            case "Normal/Silent/Vibrate Mode": changeRingerMode(); break;
            case "Pause/Play Music": pausePlayTrack(); break;
            case "Next Music": nextTrack(); break;
            default: break;
        }
    }

    private class Point2D {
        float x;
        float y;
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
                        int maxBright = 250;
                        int bright = Settings.System.getInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS);
                        if (bright > maxBright) {
                            return;
                        }

                        int clamped = Math.min(maxBright, bright + changeAmt);

                        Settings.System.putInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, clamped);
                        Log.v(TAG, String.valueOf(bright));
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }

                    setNotificationText("Brightness Increased!");
                    Log.v(TAG, "Brightness Increase");
                } else {
                    try {
                        int minBright = 5;
                        int bright = Settings.System.getInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS);
                        if (bright < minBright) {
                            return;
                        }

                        int clamped = Math.max(minBright, bright - changeAmt);

                        Settings.System.putInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, clamped);
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

    public void previousTrack() {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);

        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);

        if (getIntent().getExtras() == null) {
            setNotificationText("Previous Track - none");
        } else {
            setNotificationText("Previous Track - success");
        }

        Log.v(TAG, "Previous track");
    }

    public void nextTrack() {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);

        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);

        if (getIntent().getExtras() == null) {
            setNotificationText("Next Track - none");
        } else {
            setNotificationText("Next Track - success");
        }

        Log.v(TAG, "Next track");
    }

    // Quickly executing this function twice changes tracks for some reason
    public void pausePlayTrack() {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendOrderedBroadcast(i, null);

        i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendOrderedBroadcast(i, null);

        if (i.getExtras() == null) {
            setNotificationText("Pause/Play Track - none");
        } else {
            setNotificationText("Pause/Play Track - success");
        }

        Log.v(TAG, "Pause/Play Track");
    }

    private boolean isNotificationPolicyAccessGranted()  {
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return false;
    }

    public void setNotificationText(final String action) {
        noteText.setText(action);
    }
}
