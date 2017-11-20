package com.admobilize.bgtest.service;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by Antonio Vanegas @hpsaturn on 8/11/16.
 */

public class BuiltinFrameListener implements SurfaceHolder.Callback, Camera.PreviewCallback {


    public static final String TAG = BuiltinFrameListener.class.getSimpleName();

    public static final int LEFT_LANDSCAPE = 0;
    private static int mCameraIdx;
    private final int mWidth;
    private final int mHeight;

    private final Context ctx;
    private Camera mCamera;
    private int imageFormat;
    private byte[] frameData;
    private boolean bProcessing = false;
    private int mOrientation = LEFT_LANDSCAPE;

    Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean cameraRunning;
    private byte[] previewFrame;
    private boolean mIsCameraOrientationActive;
    private FrameCallbck mFrameCallback;
    private int cameraId = 0;

    public BuiltinFrameListener(Context ctx, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        this.ctx = ctx;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (checkCameraHardware(ctx)) {
            mCamera = getCameraInstance();
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.setPreviewCallback(this);
                } catch (IOException e) {
                    mCamera.release();
                    mCamera = null;
                }
            } else {
                Log.e(TAG, "Get Camera from service failed");
            }
        } else {
            Log.e(TAG, "There is no camera hardware on device.");
        }
    }

    private Runnable DoImageTracker = new Runnable() {
        public void run() {

            if (mIsCameraOrientationActive && mOrientation != 0) {
                if (frameData != null) {
                    Log.i(TAG,"frame from native library: "+((BuiltinCameraService)(ctx)).stringFromJNI(frameData));
                }

            } else {
                if (frameData != null) {
                    Log.i(TAG,"frame from native library: "+((BuiltinCameraService)(ctx)).stringFromJNI(frameData));
                }
            }
            bProcessing = false;
        }
    };


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters;
        if (mCamera != null) {
            try {
                parameters = mCamera.getParameters();
                // Set the camera preview size
                parameters.setPreviewSize(mWidth, mHeight);
                parameters.setColorEffect("none");
                parameters.setAutoExposureLock(false);
//                parameters.setExposureCompensation(parameters.getMinExposureCompensation()); // TODO: set this via UI
                Log.i(TAG, "[Camera] Supported Exposure Modes:" + parameters.get("exposure-mode-values"));
                Log.i(TAG, "[Camera] Supported White Balance Modes:" + parameters.get("whitebalance-values"));
                Log.i(TAG, "[Camera] Exposure Compensation:" + parameters.getExposureCompensation());
                imageFormat = parameters.getPreviewFormat();
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                cameraRunning = true;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.e(TAG, "[Camera] !!!Camera is null , camera not found");
        }

    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        previewFrame = null;
        mCamera = null;
        cameraRunning = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21) {
            //We only accept the NV21(YUV420) format.
            if (!bProcessing) {
                frameData = data;
                if (mFrameCallback != null) mFrameCallback.onFrame();
                bProcessing = true;
                mHandler.post(DoImageTracker);
            }
        }
    }

    public void onStop() {
        Log.d(TAG, "--> onStop");
        mHandler.removeCallbacks(DoImageTracker);
        mHandler.postDelayed(DoImageTracker, 0);
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
        previewFrame = null;
        mCamera = null;
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                Log.i(TAG, "[Camera] try to open camera camIdx:" + camIdx);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        c = Camera.open(camIdx);
                        mCameraIdx = camIdx;
                        Log.i(TAG, "[Camera] camIdx:" + camIdx);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "[Camera] failed to open: " + e.getLocalizedMessage());
                    }
                }
            }

            if (c == null) {
                Log.i(TAG, "[Camera] forcing open camera with camIdx 0");
                c = Camera.open(0); // force because FACING_FRONT not found
                mCameraIdx = 0;
            }
        } catch (Exception e) {
            Log.e("TAG", "[Camera] Open camera failed: " + e);
        }
        return c;
    }

    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }


    public void allowCameraOrientation(boolean isAllowed) {
        Log.d(TAG, "set rotation status:" + isAllowed);
        mIsCameraOrientationActive = isAllowed;
    }

    public boolean isCameraOrientationChangeAllowed() {
        Log.d(TAG, "is rotation allowed:" + mIsCameraOrientationActive);
        return mIsCameraOrientationActive;
    }

    public interface FrameCallbck {
        void onFrame();
    }

    public void setFrameCallback(FrameCallbck frameCallback) {
        mFrameCallback = frameCallback;
    }
}