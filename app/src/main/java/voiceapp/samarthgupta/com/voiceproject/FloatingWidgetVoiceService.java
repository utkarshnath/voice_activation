package voiceapp.samarthgupta.com.voiceproject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.hanks.htextview.scale.ScaleTextView;

import java.util.ArrayList;


//Service handles the floating widget and the audio recording functions
public class FloatingWidgetVoiceService extends Service implements View.OnClickListener {

    private WindowManager mWindowManager;
    private View mFloatingWidgetView, collapsedView, expandedView;
    private Point szWindow = new Point();
    private View minimise;
    long timeStart = 0, timeEnd = 0;

    private SpeechRecognizerManager mSpeechManager;

    //Views
    ScaleTextView stvText1, stvText2, stvText3, stvText4;

    //Camera control
    public static Boolean isCameraOn = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //Widget opened - close the service
        if(isMyServiceRunning(MyListenerService.class)){
            stopService(new Intent(FloatingWidgetVoiceService.this, MyListenerService.class));
        }

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getWindowManagerDefaultDisplay();

        //Init LayoutInflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        if (inflater != null) {
            addFloatingWidgetView(inflater);
            implementClickListeners();
            implementTouchListenerToFloatingWidgetView();
        }
    }

    public FloatingWidgetVoiceService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_floating_view:
                //close the service and remove the from from the window

                //Close service
                stopSelf();
                break;

            case R.id.imageView:
                //Cross sign - close expanded view and show floating widget
                updateExpandedView(false);
                break;

            case R.id.collapse_view:
                updateExpandedView(true);
                break;

        }
    }

    private void updateExpandedView(boolean isVisible) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();
        params.width = isVisible ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = isVisible ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        params.flags = isVisible ? WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL :
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowManager.updateViewLayout(mFloatingWidgetView, params);
        collapsedView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        expandedView.setVisibility(isVisible ? View.VISIBLE : View.GONE);

    }

    private void addFloatingWidgetView(LayoutInflater inflater) {

        //Inflate the floating view layout we created
        FrameLayout frameLayout = new FrameLayout(this) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                    minimise.performClick();
                }
                return super.dispatchKeyEvent(event);
            }
        };

        mFloatingWidgetView = inflater.inflate(R.layout.floating_widget_layout, frameLayout);

        //Add the view to the window.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.CENTER;

        //Initially view will be added to top-left corner, you change x-y coordinates according to your need
        params.x = 0;
        params.y = 0;

        //Add the view to the window
        mWindowManager.addView(mFloatingWidgetView, params);

        //find id of collapsed view layout
        collapsedView = mFloatingWidgetView.findViewById(R.id.collapse_view);

        //find id of the expanded view layout
        expandedView = mFloatingWidgetView.findViewById(R.id.expanded_container);

        //TODO : Add voice code here

        stvText1 = (ScaleTextView) expandedView.findViewById(R.id.tv_text_1);
        stvText2 = (ScaleTextView) expandedView.findViewById(R.id.tv_text_2);
        stvText3 = (ScaleTextView) expandedView.findViewById(R.id.tv_text_3);
        stvText4 = (ScaleTextView) expandedView.findViewById(R.id.tv_text_4);
//        fabRun = (FloatingActionButton) expandedView.findViewById(R.id.fab_run);
//        fabRun.setOnClickListener(this);

        //Starting speech recognition
        setSpeechListener();

        //Set timer for the recogniser to start otherwise stop service

//        long TIME_OUT = 5000;
//        long start = System.currentTimeMillis();
//
//
//        boolean flag = true;
//        while (System.currentTimeMillis() - start < TIME_OUT) {
//            if (mSpeechManager != null) {
//                flag = false;
//                break;
//            }
//        }
//
//        //Meaning that speech recogniser remained null even after 5 seconds.
//        if (flag) {
//            stopSelf();
//        } else {
//            stvText1.animateText("You may speak now.");
//        }

    }


    private void getWindowManagerDefaultDisplay() {
        mWindowManager.getDefaultDisplay().getSize(szWindow);
    }

    /*  Implement Touch Listener to Floating Widget Root View  */
    private void implementTouchListenerToFloatingWidgetView() {
        //Drag and move floating view using user's touch action.
        View collapsedView = mFloatingWidgetView.findViewById(R.id.collapse_view);
        collapsedView.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();
                float x_cord = event.getRawX();
                float y_cord = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timeStart = System.currentTimeMillis();
                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        timeEnd = System.currentTimeMillis();
                        float x_diff = x_cord - initialTouchX;
                        float y_diff = y_cord - initialTouchY;
                        if (Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5) {

                            //Also check the difference between start time and end time should be less than 300ms
                            if ((timeEnd - timeStart) < 300)
                                updateExpandedView(true);

                        }
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingWidgetView, params);
                        lastAction = event.getAction();
                        return true;
                }
                return false;
            }
        });
    }

    private void implementClickListeners() {
        mFloatingWidgetView.findViewById(R.id.close_floating_view).setOnClickListener(this);
        mFloatingWidgetView.findViewById(R.id.collapse_view).setOnClickListener(this);
        minimise = mFloatingWidgetView.findViewById(R.id.imageView);
        minimise.setOnClickListener(this);
    }


    /*  Reset position of Floating Widget view on dragging  */
    private void resetPosition(int x_cord_now) {
        if (x_cord_now <= szWindow.x / 2) {
            moveToLeft(x_cord_now);
        } else {
            moveToRight(x_cord_now);
        }
    }

    /*  Method to move the Floating widget view to Left  */
    private void moveToLeft(final int current_x_cord) {

        new CountDownTimer(500, 5) {
            //get params of Floating Widget view
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();

            public void onTick(long t) {
                long step = (500 - t) / 5;

                mParams.x = 0 - (int) (current_x_cord * current_x_cord * step);

                //If you want bounce effect uncomment below line and comment above line
//                 mParams.x = 0 - (int) (double) bounceValue(step, x);

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mFloatingWidgetView, mParams);
            }

            public void onFinish() {
                mParams.x = 0;

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mFloatingWidgetView, mParams);
            }
        }.start();
    }

    /*  Method to move the Floating widget view to Right  */
    private void moveToRight(final int current_x_cord) {

        new CountDownTimer(500, 5) {
            //get params of Floating Widget view
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();

            public void onTick(long t) {
                long step = (500 - t) / 5;

                mParams.x = (int) (szWindow.x + (current_x_cord * current_x_cord * step) - mFloatingWidgetView.getWidth());

                //If you want bounce effect uncomment below line and comment above line
//                  mParams.x = szWindow.x + (int) (double) bounceValue(step, x_cord_now) - mFloatingWidgetView.getWidth();

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mFloatingWidgetView, mParams);
            }

            public void onFinish() {
                mParams.x = szWindow.x - mFloatingWidgetView.getWidth();

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mFloatingWidgetView, mParams);
            }
        }.start();
    }

    /*  return status bar height on basis of device display metrics  */
    private int getStatusBarHeight() {
        return (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
    }

    /*  Update Floating Widget view coordinates on Configuration change  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        getWindowManagerDefaultDisplay();

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (layoutParams.y + (mFloatingWidgetView.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (mFloatingWidgetView.getHeight() + getStatusBarHeight());
                mWindowManager.updateViewLayout(mFloatingWidgetView, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingWidgetView != null)
            mWindowManager.removeView(mFloatingWidgetView);

        mSpeechManager.destroy();
    }

    private void setSpeechListener() {

        Log.i("Text", "Speech Listener");
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {

                if (results != null && results.size() > 0) {

                    if (results.size() == 1) {
                        mSpeechManager.destroy();
                        mSpeechManager = null;

                        Log.i("Text", results.get(0));
                        processSpeechData(results.get(0));

                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (results.size() > 5) {
                            results = (ArrayList<String>) results.subList(0, 5);
                        }
                        for (String result : results) {
                            sb.append(result).append("\n");
                        }
                        Log.i("Text", results.get(0));
                        processSpeechData(results.get(0));
                    }
                } else
                    Log.i("Text", "No results");

            }
        });
    }

    private void processSpeechData(String data) {
        if (data.length() >= 3) {

            setTextOnTextViews(data);

            //Control cam
            if (data.contains("camera") || data.contains("Camera")) {

                if (data.contains("start") || data.contains("open") || data.contains("Open") || data.contains("Start")) {

                    //If camera is not on
                    if (!isCameraOn) {
                        //Start camera
                        isCameraOn = true;
                        Log.i("Text", "Camera started");
                        updateExpandedView(false);
                        startActivity(new Intent(FloatingWidgetVoiceService.this, CameraActivity.class));

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

            if (data.contains("running") || data.contains("Running") || data.contains("run") || data.contains("Run")) {

                if (data.contains("start") || data.contains("open") || data.contains("Open") || data.contains("Start")) {
                    startService(new Intent(FloatingWidgetVoiceService.this, MyRunningService.class));
                } else if (data.contains("Stop") || data.contains("stop") || data.contains("Close") || data.contains("close")) {
                    stopRunningServiceBroadcast();
                }


            }
        }
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


    private void setTextOnTextViews(String recievedText) {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < recievedText.length(); i++) {
            builder.append(recievedText.charAt(i));

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
        int textViewNumber = (recievedText.length() / 30) + 1;
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
}
