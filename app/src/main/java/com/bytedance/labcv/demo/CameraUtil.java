package com.bytedance.labcv.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class CameraUtil extends CameraDevice.StateCallback implements ImageReader.OnImageAvailableListener {

    private GLSurfaceView glView;
    private SurfaceTexture surfaceTexture;
    private String cameraId;
    private CameraManager camManager;
    private Context context;
    private HandlerThread camBackgroundHandlerThread;
    private Handler camBackgroundHandler;
    private CameraDevice.StateCallback cameraStateCallback;
    private Size previewSize;
    private int cameraRotation;
    private ImageReader imageReader;
    final int CAMERA_REQUEST_CODE = 100;
    private boolean cameraOpened = false;

    /**
     * Camera capture session's callback to handle raw frame data
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            // process camera preview stream raw frame data

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }
    };


    /**
     * open the Camera.
     * Note: permission check is suppressed here, since we'll only call the function after
     * permission is granted in CameraFragment's activityResultLauncher (Android X).
     */
    @SuppressLint("MissingPermission")
    public void openCamera() {
        startBackgroundThread();
        try {
            camManager.openCamera(cameraId, cameraStateCallback, camBackgroundHandler);

            } catch (CameraAccessException e) {
                System.out.println("Failed when trying to open the camera!");
                e.printStackTrace();
            }

    }

    /**
     * get the open/close status of the current camera
     * true: camera is already open
     * false: camera is off
     */
    public boolean getCameraStatus(){
        if(cameraOpened)
            return true;
        else
            return false;
    }


    /**
     * release camera
     */
    public void releaseCamera(){

    }

    /**
     * setupFrontCamera
     */
    public void setupFrontCamera(GLSurfaceView glTextureView) {
        glView = glTextureView;
        // create camera manager
        camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] camIds = camManager.getCameraIdList();
            for (Integer index = 0; index < camIds.length; index++) {
                CameraCharacteristics camCharacter = camManager.getCameraCharacteristics(camIds[index]);
                // select front camera
                if (camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camIds[index];
                    cameraRotation = camCharacter.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    // get a list of camera preview sizes
                    Size[] previewSizes = camCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                    // match the preview size to the UI TextureView size for display, with highest quality
                    for (index = 0; index < previewSizes.length; index++) {
                        Size currentPreviewSize = previewSizes[index];

                        // get camera preview aspect ration
                        Float previewAspectRatio = ((float) currentPreviewSize.getHeight()) / currentPreviewSize.getWidth();
                        // check camera orientation and update aspect ratio calculation if needed
                        if(cameraRotation == 90 || cameraRotation == 270){
                            previewAspectRatio = ((float) currentPreviewSize.getWidth()) / currentPreviewSize.getHeight();
                        }

                        Float textureViewAspectRatio = ((float) glView.getHeight()) / glView.getWidth();
                        Float epsilon = (float) 0.001;
                        // when camera preview and textureView has the same aspect ratio
                        if (Math.abs(previewAspectRatio - textureViewAspectRatio) < epsilon){
                            // take this previewSize for display
                            previewSize = currentPreviewSize;
                            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
                            imageReader.setOnImageAvailableListener(this, camBackgroundHandler);
                            return;
                        }
                    }
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * setup TextureView for displaying camera preview
     */
    public void setSurfaceTexture(SurfaceTexture texture){
        if(texture == null){
            System.out.println("surface texture for camera preview must NOT be empty");
        }
        surfaceTexture = texture;
    }


    /**
     * get camera preview size, with width and height
     */
    public Size getPreviewSize(){
        return previewSize;
    }

    /**
     * get camera rotation angle - 0, 90, 180, 270
     */
    public int getCameraRotation(){ return cameraRotation; }

    /**
     * setup app context
     */
    public void setCameraContext(Context context){
        this.context = context;
    }

    /**
     * Initialize Camera Util before using it
     */
    public void initCameraStateCallback(){

        // create camera device state callback
        cameraStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {

                System.out.println("Camera Open successful");
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                // TODO: adopt GL surface view instead of surface view
                Surface previewSurface = new Surface(surfaceTexture);
                try {
                    CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    captureRequestBuilder.addTarget(previewSurface);

                    cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    try {
                                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                                                null, camBackgroundHandler);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                }
                            }, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                System.out.println("Camera disconnected");
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                System.out.println("Camera state callback error");
            }
        };
    }

    /**
     * Background Camera Thread Handler
     */
    public void startBackgroundThread(){
        if(camBackgroundHandlerThread == null || camBackgroundHandler == null){
            camBackgroundHandlerThread = new HandlerThread("Camera Preview Thread");
            camBackgroundHandlerThread.start();
            camBackgroundHandler = new Handler(camBackgroundHandlerThread.getLooper());
        }
    }

    public void stopBackgroundThread(){
        if(camBackgroundHandlerThread != null){
            camBackgroundHandlerThread.quitSafely();
            try{
                camBackgroundHandlerThread.join();
                camBackgroundHandlerThread = null;
                camBackgroundHandler = null;
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }

    }


    /**
     * Image Available Listener
     * @param imageReader
     */
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();

    }

    /**
     * camera onOpened callback
     * @param cameraDevice
     */
    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        cameraOpened = true;
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {

    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) {

    }

    @Override
    public void onClosed(@NonNull CameraDevice camera) {
        cameraOpened = false;
    }
}
