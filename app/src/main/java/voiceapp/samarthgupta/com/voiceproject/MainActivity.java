package voiceapp.samarthgupta.com.voiceproject;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.hanks.htextview.scale.ScaleTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    ScaleTextView stvText1, stvText2, stvText3, stvText4;
    final int OVERLAY_REQUEST_CODE = 1222;
//    FloatingActionButton fabRun, fabStartWidget;


    FloatingActionButton fabRun, fabStartWidget;

    final int WRITE_EXTERNAL = 1234;
    final int CAMERA_PERMISSION = 1256;
    final int AUDIO_PERMISSION = 1378;


    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stvText1 = (ScaleTextView) findViewById(R.id.tv_text_1);
        stvText2 = (ScaleTextView) findViewById(R.id.tv_text_2);
        stvText3 = (ScaleTextView) findViewById(R.id.tv_text_3);
        stvText4 = (ScaleTextView) findViewById(R.id.tv_text_4);

        fabRun = (FloatingActionButton) findViewById(R.id.fab_run);
        fabStartWidget = (FloatingActionButton) findViewById(R.id.fab_floating_widget);

        //Widget goes away when activity is opened
        if (isMyServiceRunning(FloatingWidgetVoiceService.class)) {
            stopService(new Intent(MainActivity.this, FloatingWidgetVoiceService.class));
        }

        //Handles permissions and starts service
        permissionHandler();

        //Start Run
        fabRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(MainActivity.this, MyRunningService.class));
            }
        });

        //Start floating widget
        fabStartWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isOverlayPermissionGiven()) {
                    askForOverlayPermission();
                } else {

                    //If voice service is running then close it since voice service already in widget
                    if (isMyServiceRunning(MyListenerService.class)) {
                        stopService(new Intent(MainActivity.this, MyListenerService.class));
                    }

                    //Start widget service
                    startService(new Intent(MainActivity.this, FloatingWidgetVoiceService.class));

                    //Close activity
                    finish();
                }
            }
        });

    }

    //For overlay permission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (isOverlayPermissionGiven()) {

                //Start widget service
                startService(new Intent(MainActivity.this, FloatingWidgetVoiceService.class));

                //If voice service is running then close it since voice service already in widget
                if (isMyServiceRunning(MyListenerService.class)) {
                    stopService(new Intent(MainActivity.this, MyListenerService.class));
                }

                //Close activity
                finish();

            } else {

                Toast.makeText(this,
                        "Draw over other app permission not available. Floating widget can't be added",
                        Toast.LENGTH_LONG).show();
                return;
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //For overlay permission
    private void askForOverlayPermission() {

        //CHECK OVERLAY PERMISSION
        if (!isOverlayPermissionGiven()) {
            Intent intent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_REQUEST_CODE);

            }
        } else {
            Toast.makeText(this, "Application needs overlay permission to run", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    //For overlay permission
    private boolean isOverlayPermissionGiven() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this));
    }


    //For other permissionMap
    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        starting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        starting();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        starting();
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopping();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopping();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopping();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }

        return false;
    }


    private void setTextOnTextViews(String receivedText) {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < receivedText.length(); i++) {
            builder.append(receivedText.charAt(i));

            if (i < 30) {
            } else if (i == 30) {
                stvText1.animateText(builder.toString());
                builder = new StringBuilder();
            } else if (i > 30 && i < 60) {
            } else if (i == 60) {
                stvText2.animateText(builder.toString());
                builder = new StringBuilder();
            } else if (i > 60 && i < 90) {
            } else if (i == 90) {
                stvText3.animateText(builder.toString());
                builder = new StringBuilder();
            } else if (i > 90 && i < 120) {

            } else if (i == 120) {
                stvText4.animateText(builder.toString());
                builder = new StringBuilder();
            }


        }


        String left = builder.toString();
        int textViewNumber = (receivedText.length() / 30) + 1;
        if (textViewNumber == 1) {
            stvText1.animateText(left);
        } else if (textViewNumber == 2) {
            stvText2.animateText(left);
        } else if (textViewNumber == 3) {
            stvText3.animateText(left);
        } else if (textViewNumber == 4) {
            stvText4.animateText(left);
        }


    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals("Initialisation")) {
                Log.i("Text", "Received2");
                stvText1.animateText("You may speak now");
            } else if (action.equals("Text")) {

                if (!MyListenerService.isCameraOn) {
                    Log.i("Text", "Received1");

                    String recievedText = intent.getStringExtra("text");
                    setTextOnTextViews(recievedText);

                }
            }
        }
    };


    private void stopping() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);

        if (isMyServiceRunning(FloatingWidgetVoiceService.class)) {

            //Floating widget is running, and listener is also running, so listener has to be closed.
            if (isMyServiceRunning(MyListenerService.class)) {
                stopService(new Intent(MainActivity.this, MyListenerService.class));
            }
        } else {

            //Listener service has to be reopened, since we might be going to camera.
            if (!isMyServiceRunning(MyListenerService.class)) {
                startService(new Intent(MainActivity.this, MyListenerService.class));
            }
        }
    }

    private void starting() {

        if (isMyServiceRunning(FloatingWidgetVoiceService.class)) {
            stopService(new Intent(MainActivity.this, FloatingWidgetVoiceService.class));
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("Initialisation"));
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("Text"));

        if (hasPermissions(this, PERMISSIONS)) {
            if (!isMyServiceRunning(MyListenerService.class)) {
                startService(new Intent(MainActivity.this, MyListenerService.class));
            }
        }

    }


    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private void permissionHandler() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("Write external storage");
        if (!addPermission(permissionsList, Manifest.permission.CAMERA))
            permissionsNeeded.add("Camera");
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
            permissionsNeeded.add("Record Audio");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            }
                        });
                return;

            }
        }

        startServiceAndRegisterReceivers();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
        }
        return true;
    }

    private void startServiceAndRegisterReceivers() {

        startService(new Intent(MainActivity.this, MyListenerService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("Initialisation"));
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("Text"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with results

                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                    // All Permissions Granted

                    //This is done to refresh the speech recogniser, solve the error on first startup of speech recogniser
                    startServiceAndRegisterReceivers();
                    stopService(new Intent(MainActivity.this, MyListenerService.class));
                    startServiceAndRegisterReceivers();


                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}
