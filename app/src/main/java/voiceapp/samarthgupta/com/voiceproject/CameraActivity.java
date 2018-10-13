package voiceapp.samarthgupta.com.voiceproject;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class CameraActivity extends AppCompatActivity {

    private ImageButton btStartCapture;
    private ImageButton btChangeCamera;
    private TextureView textureView;

    public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";
    int click = 0;

    Timer timer;
    boolean isCamStarted = false;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId = "0";
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Means camera is opened by activity, so the listener service is to remain running
        if (!isMyServiceRunning(FloatingWidgetVoiceService.class)) {
            Log.i("Service", "Service Restarted");
            startService(new Intent(CameraActivity.this, MyListenerService.class));
            MyListenerService.isCameraOn = true;
        } else {
            //Came from widget
            Log.i("Here", "Here hi");
            FloatingWidgetVoiceService.isCameraOn = true;
            startService(new Intent(CameraActivity.this, FloatingWidgetVoiceService.class));
        }

        timer = new Timer();

        Log.i("Text", "Hi Camera");
        textureView = (TextureView) findViewById(R.id.textureView);
        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btStartCapture = (ImageButton) findViewById(R.id.ib_start_capture);
        btChangeCamera = (ImageButton) findViewById(R.id.ib_change_camera);

        LocalBroadcastManager.getInstance(this).registerReceiver(camBroadcastReceiver, new IntentFilter("Cam"));
        LocalBroadcastManager.getInstance(this).registerReceiver(camBroadcastReceiver, new IntentFilter("Recording"));

        btStartCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isCamStarted) {
                    timer = new Timer();
                    isCamStarted = true;
                    timer.schedule(new CameraClick(), 200, 5000);
                    btStartCapture.setImageResource(R.drawable.vidred);
                } else {
                    isCamStarted = false;
                    timer.cancel();
                    timer.purge();
                    btStartCapture.setImageResource(R.drawable.iconcam);
                }

            }
        });


        btChangeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cameraDevice.close();

                String mCameraId = cameraId;
                if (mCameraId.equals(CAMERA_BACK)) {
                    mCameraId = CAMERA_FRONT;
                    cameraId = CAMERA_FRONT;
                    openCamera(mCameraId);
                } else {
                    mCameraId = CAMERA_BACK;
                    cameraId = CAMERA_BACK;
                    openCamera(mCameraId);
                }


            }
        });
    }

    class CameraClick extends TimerTask {
        public void run() {
            takePicture();
        }
    }

    private BroadcastReceiver camBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (!isMyServiceRunning(FloatingWidgetVoiceService.class)) {
                Log.i("Service", "Service Restarted");
                startService(new Intent(CameraActivity.this, MyListenerService.class));
            }

            if (action.equals("Cam")) {

                if ((isMyServiceRunning(FloatingWidgetVoiceService.class) && FloatingWidgetVoiceService.isCameraOn)
                        || (isMyServiceRunning(MyListenerService.class) && MyListenerService.isCameraOn)) {

                    Log.i("Text", "Stopping");
                    timer.cancel();
                    timer.purge();
                    finish();
                }

            } else if (action.equals("Recording")) {

                int startStop = intent.getIntExtra("recording", 0);

                if (isCamStarted) {
                    if (startStop == 0) {
                        isCamStarted = false;
                        timer.cancel();
                        timer.purge();
                        btStartCapture.setImageResource(R.drawable.iconcam);
                    }
                } else {
                    if (startStop == 1) {
                        timer = new Timer();
                        isCamStarted = true;
                        timer.schedule(new CameraClick(), 200, 5000);
                        btStartCapture.setImageResource(R.drawable.vidred);
                    }
                }
            }


        }
    };

    private void takePicture() {
        if (cameraDevice == null)
            return;

        Log.i("Click", (click++) + " ");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            file = new File(Environment.getExternalStorageDirectory() + "/" + UUID.randomUUID().toString() + ".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        {
                            if (image != null)
                                image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    } finally {
                        if (outputStream != null)
                            outputStream.close();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(CameraActivity.this, "Click Saved " + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
//            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera(cameraId);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(camBroadcastReceiver, new IntentFilter("Cam"));
        LocalBroadcastManager.getInstance(this).registerReceiver(camBroadcastReceiver, new IntentFilter("Recording"));


        startBackgroundThread();
        if (textureView.isAvailable())
            openCamera(cameraId);
        else
            textureView.setSurfaceTextureListener(textureListener);

        if (!isMyServiceRunning(FloatingWidgetVoiceService.class)) {
            Log.i("Service", "Service Restarted 1");
            startService(new Intent(CameraActivity.this, MyListenerService.class));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        //Pausing application, unregister
        LocalBroadcastManager.getInstance(this).unregisterReceiver(camBroadcastReceiver);

        stopBackgroundThread();
//        if (isMyServiceRunning(MyListenerService.class)) {
//            Log.i("Service", "Service Restarted 2");
//            startService(new Intent(CameraActivity.this, MyListenerService.class));
//        }

    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    @Override
    protected void onDestroy() {
        Log.i("Text", "Bye Camera");

        if (isMyServiceRunning(FloatingWidgetVoiceService.class)) {
            FloatingWidgetVoiceService.isCameraOn = false;
            startService(new Intent(CameraActivity.this, FloatingWidgetVoiceService.class));
        } else {
            MyListenerService.isCameraOn = false;
            startService(new Intent(CameraActivity.this, MyListenerService.class));
        }

        super.onDestroy();
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
