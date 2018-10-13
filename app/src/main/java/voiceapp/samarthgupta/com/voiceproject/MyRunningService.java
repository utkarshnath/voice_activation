package voiceapp.samarthgupta.com.voiceproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class MyRunningService extends Service {

    private SensorManager mSensorManager;
    Sensor stepDetectorSensor;
    private long steps = 0;

    NotificationManager manager;
    long startTime = 0;
    long stopTime = 0;
    long timeElapsed = 0;

    Handler handler = new Handler(Looper.getMainLooper());

    public MyRunningService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        addNotification(this, 0);
        LocalBroadcastManager.getInstance(this).registerReceiver(stopBroadcastReceiver, new IntentFilter("Run"));

        startTime = System.currentTimeMillis();

        if (mSensorManager != null) {
            stepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mSensorManager.registerListener(stepDetectorListener, stepDetectorSensor, 5000);
        }


    }

    SensorEventListener stepDetectorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            steps++;
//            sendRunningUpdateBroadcast(steps, getDistanceRun(steps));
//            manager.cancel(0);
            addNotification(MyRunningService.this, steps);
            Log.i("Run", steps + " ");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };


    public float getDistanceRun(long steps) {
        float distance = (float) (steps * 78) / (float) 100000;
        return distance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LocalBroadcastManager.getInstance(this).registerReceiver(stopBroadcastReceiver, new IntentFilter("Run"));
        if (intent != null && intent.getAction() != null) {

            if (intent.getAction().equals("Stop")) {

                stopSelf();
                stopTime = System.currentTimeMillis();
                timeElapsed = stopTime - startTime;

                //TODO : Handle time
                return START_NOT_STICKY;

            } else if (intent.getAction().equals("Toast")) {
                toastData();
            }


        }

        return START_STICKY;

    }

    private void toastData() {

        handler.post(new Runnable() {

            @Override
            public void run() {

                String dist = "Distance = " + getDistanceRun(steps) + " km";
                String st = "Steps = " + steps;

                timeElapsed = System.currentTimeMillis() - startTime;

                int minutes = (int) ((timeElapsed / (1000 * 60)) % 60);
                int hours = (int) ((timeElapsed / (1000 * 60 * 60)) % 24);

                String time = "Time Elapsed : " + hours + " hrs " + minutes + " minutes";
                String toast = dist + "\n" + st + "\n" + time;

                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
            }
        });

    }


    private void addNotification(Context context, long steps) {

        Intent reopenActivityIntent = new Intent(context, MainActivity.class);
        reopenActivityIntent.setAction("X");
        reopenActivityIntent.putExtra("steps", steps);
        reopenActivityIntent.putExtra("distance", getDistanceRun(steps));

        //TODO: Change view stats activity
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, reopenActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        //For stop run button
        Intent intent = new Intent(context, MyRunningService.class);
        intent.setAction("Stop");
        PendingIntent stopIntent = PendingIntent.getService(context, 0, intent, 0);

        //For stop show stats button
        Intent showIntent = new Intent(context, MyRunningService.class);
        showIntent.setAction("Toast");
        PendingIntent showPendingIntent = PendingIntent.getService(context, 0, showIntent, 0);

        String dist = "Distance = " + getDistanceRun(steps) + " km";
        String st = "Steps = " + steps;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "Channel")
                        .setContentTitle("Running Mode")
                        .setContentText(dist)
                        .setSubText(st)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.runsmall)
                        .addAction(R.drawable.run, "Stop Run", stopIntent)
                        .addAction(R.drawable.run, "View Stats", showPendingIntent);


        builder.setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Channel",
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());


    }

    private void sendRunningUpdateBroadcast(long steps, float distance) {
        Intent intent = new Intent("RunUpdate"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("steps", steps);
        intent.putExtra("distance", distance);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        toastData();
        mSensorManager.unregisterListener(stepDetectorListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopBroadcastReceiver);
        manager.cancel(0);
    }

    private BroadcastReceiver stopBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals("Run")) {
//                Intent intentRunning = new Intent(MyRunningService.this, MainActivity.class);
//                startActivity(intentRunning);

                //TODO : Handle time
                stopTime = System.currentTimeMillis();
                timeElapsed = stopTime - startTime;
                stopSelf();
            }

        }
    };


}
