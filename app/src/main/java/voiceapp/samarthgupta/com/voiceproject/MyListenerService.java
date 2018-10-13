package voiceapp.samarthgupta.com.voiceproject;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;

public class MyListenerService extends Service {

    private SpeechRecognizerManager mSpeechManager;
    public static Boolean isCameraOn = false;

    public MyListenerService() {

    }


    @Override
    public void onCreate() {
        super.onCreate();
        setSpeechListener();
        sendInitialisationBroadcast();
        Log.i("Text", "Created");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (mSpeechManager == null) {
            setSpeechListener();
            sendInitialisationBroadcast();
        }


        Log.i("Text", "Started");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        if (mSpeechManager == null) {
            setSpeechListener();
            sendInitialisationBroadcast();
        }

        return null;
    }

    private void setSpeechListener() {

        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {


                if (results != null && results.size() > 0) {

                    if (results.size() == 1) {
                        mSpeechManager.destroy();
                        mSpeechManager = null;
                        setSpeechListener();

                        Log.i("Text", results.get(0));
                        sendDataToBackend(results.get(0));

                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (results.size() > 5) {
                            results = (ArrayList<String>) results.subList(0, 5);
                        }
                        for (String result : results) {
                            sb.append(result).append("\n");
                        }
                        Log.i("Text", results.get(0));
                        sendDataToBackend(results.get(0));
                    }
                } else
                    Log.i("Text", "No results");

            }
        });
    }

    private void sendDataToBackend(String data) {
        if (data.length() >= 3) {
            sendNewTextConversionBroadcast(data);

            //Control cam
            if (data.contains("camera") || data.contains("Camera")) {

                if (data.contains("start") || data.contains("open") || data.contains("Open") || data.contains("Start")) {

                    //If camera is not on
                    if (!isCameraOn) {
                        //Start camera
                        isCameraOn = true;
                        Log.i("Text", "Camera started");

//                        Many instances of camera is opening
                        startActivity(new Intent(MyListenerService.this, CameraActivity.class));
                    }

                } else if (data.contains("Stop") || data.contains("stop") || data.contains("Close") || data.contains("close")) {

                    if (isCameraOn) {
                        //Camera is on
                        //Ask camera to stop recording and close
                        Log.i("Text", "Camera broadcast sent");
                        sendCameraBroadcast();
                    } else {
                        Log.i("Text", "Camera is NOT ON");
                    }

                }

            }

            if (data.contains("recording") || data.contains("Recording")) {

                if (data.contains("start") || data.contains("open") || data.contains("Open") || data.contains("Start")) {
                    sendStartStopRecordingBroadcast(1);
                } else if (data.contains("Stop") || data.contains("stop") || data.contains("Close") || data.contains("close")) {
                    sendStartStopRecordingBroadcast(0);
                }

            }

            if(data.contains("running") || data.contains("Running") || data.contains("run") || data.contains("Run")){

                if (data.contains("start") || data.contains("open") || data.contains("Open") || data.contains("Start")) {
                    startService(new Intent(MyListenerService.this, MyRunningService.class));
                } else if (data.contains("Stop") || data.contains("stop") || data.contains("Close") || data.contains("close")) {
                    stopRunningServiceBroadcast();
                }


            }


        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSpeechManager.destroy();
        Log.i("Text", "Service destroyed");
    }

    private void sendInitialisationBroadcast() {
        Intent intent = new Intent("Initialisation"); //put the same message as in the filter you used in the activity when registering the receiver
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendNewTextConversionBroadcast(String text) {
        Intent intent = new Intent("Text");
        intent.putExtra("text", text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCameraBroadcast() {
        Intent intent = new Intent("Cam");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStartStopRecordingBroadcast(int startStop) {
        Intent intent = new Intent("Recording");
        //0 is stop
        //1 is start
        intent.putExtra("recording", startStop);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //Broadcast for stopping running service
    private void stopRunningServiceBroadcast() {
        Intent intent = new Intent("Run"); //put the same message as in the filter you used in the activity when registering the receiver
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
