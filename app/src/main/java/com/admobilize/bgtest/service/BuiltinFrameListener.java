package com.admobilize.bgtest.service;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

import com.admobilize.lib.NativeProcess;

import java.io.IOException;

/**
 * Created by Antonio Vanegas @hpsaturn on 8/11/16.
 */

public class BuiltinFrameListener implements SurfaceHolder.Callback, Camera.PreviewCallback {


    public static final String TAG = BuiltinFrameListener.class.getSimpleName();

    public static final int LEFT_LANDSCAPE = 0;
    private final int mWidth;
    private final int mHeight;

    private final Context ctx;
    private final NativeProcess np;
    private Camera mCamera;
    private int imageFormat;
    private byte[] frameData;
    private boolean bProcessing;
    private int mOrientation = LEFT_LANDSCAPE;

    Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsCameraOrientationActive;

    public BuiltinFrameListener(Context ctx, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        this.ctx = ctx;
        np = new NativeProcess();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (checkCameraHardware(ctx)) {
            mCamera = getCameraInstance();
            if (mCamera != null) {
                try {
                    Log.d(TAG, "[camera] setPreview holder..");
                    mCamera.setPreviewDisplay(holder);
                    Log.d(TAG, "[camera] setPreview callback..");
                    mCamera.setPreviewCallback(this);
                    Log.d(TAG, "[camera] setPreview done.");
                } catch (IOException e) {
                    Log.e(TAG, "[camera] setPreview IOException: "+e.getMessage());
                    mCamera.release();
                    mCamera = null;
                }
            } else {
                Log.e(TAG, "[camera] Get Camera from service failed");
            }
        } else {
            Log.e(TAG, "[camera] There is no camera hardware on device.");
        }
    }

    private Runnable DoImageTracker = new Runnable() {
        public void run() {

            if (mIsCameraOrientationActive && mOrientation != 0) {
                if (frameData != null) {
                    Log.i(TAG,"[camera] frame from native library: "+np.stringFromJNI(frameData));
                }

            } else {
                if (frameData != null) {
                    Log.i(TAG,"[camera] frame from native library: "+np.stringFromJNI(frameData));
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
        mCamera = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21) {
            //We only accept the NV21(YUV420) format.
            if (!bProcessing) {
                frameData = data;
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
                        Log.i(TAG, "[Camera] camIdx:" + camIdx);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "[Camera] failed to open: " + e.getLocalizedMessage());
                    }
                }
            }

            if (c == null) {
                Log.i(TAG, "[Camera] forcing open camera with camIdx 0");
                c = Camera.open(0); // force because FACING_FRONT not found
                Log.i(TAG, "[Camera] open done.");
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

}