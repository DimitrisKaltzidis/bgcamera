package com.admobilize.bgapi2.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.admobilize.lib.NativeProcess;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Antonio Vanegas @hpsaturn on 2017.11.19.
 */

public class FrameListener {

    protected static final String TAG = FrameListener.class.getSimpleName();
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;

    public static final int PORTRAIT = 270;
    private final NativeProcess np;

    private int mOrientation = PORTRAIT;
    private final int mWidth;
    private final int mHeight;
    private final Context ctx;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private ImageReader mImageReader;
    private boolean mIsCameraOrientationActive;
    private byte[] frameData;
    private boolean cameraRunning;

    public FrameListener(Context ctx, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        this.ctx = ctx;
        mHandler = new Handler();
        np = new  NativeProcess();
        startCamera();
    }

    /**
     * Listens for camera state changes
     */
    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened");
            mCameraDevice = camera;
            actOnReadyCameraDevice();
            cameraRunning=true;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    /**
     * Send request for camera feed
     */
    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        public CameraCaptureSession session;

        @Override
        public void onReady(CameraCaptureSession session) {
            this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    private Handler mHandler;
    private boolean isProcessing = false;

    private int count;
    private int sample = 100;
    /**
     * Listens for frames and send them to  be processed
     */
    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
//                Log.d(TAG, " new frame....");
                if (!isProcessing) {

                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    frameData = new byte[buffer.capacity()];
                    buffer.get(frameData);

                    mHandler.post(doImageTracker);

                    if(count++==sample){
                        Log.i(TAG,""+sample+" frames reached");
                        count=0;
                    }
                    image.close();
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());

            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    /**
     * Starts a builtin camera with api camera 2
     */
    private void startCamera() {
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 2 /* images buffered */);
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Gets a camera available
     * @param manager
     * @return
     */
    public String getCamera(CameraManager manager) {

        String cameraIndex = "0";
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Log.d(TAG, "cameraId " + cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation != CAMERACHOICE) {
                    cameraIndex = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraIndex;
    }


    public void actOnReadyCameraDevice() {
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    /**
     * Create capture request to get camera frames
     * @return
     */
    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(mImageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public void onStop() {
        try {

            if (mSession != null) {
                mSession.abortCaptures();
                mSession.close();
            }

            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if(mImageReader != null){
                mImageReader.close();
                mImageReader = null;
            }

            cameraRunning=false;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


    public boolean isCameraRunning() {
        return cameraRunning;
    }

    public void setOrientation(int orientation) {
        Log.d(TAG, "new orientation " + orientation);
        mOrientation = orientation;
    }

    public void allowCameraOrientation(boolean isAllowed) {
        mIsCameraOrientationActive = isAllowed;
    }

    public boolean isCameraOrientationChangeAllowed() {
        return mIsCameraOrientationActive;
    }

    public interface FrameCallback {
        void onFrame(byte[] frame);
    }

    private Runnable doImageTracker = new Runnable() {
        public void run() {
            isProcessing = true;
            if (frameData != null) np.stringFromJNI(frameData);
            isProcessing = false;
        }
    };

}
