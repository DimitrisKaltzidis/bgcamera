package com.admobilize.bgapi2.service;

import android.content.Context;
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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.admobilize.bgapi2.streamer.MJpegHttpStreamer;
import com.admobilize.bgapi2.streamer.MemoryOutputStream;
import com.admobilize.bgapi2.streamer.MovingAverage;
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

    private final int mWidth;
    private final int mHeight;
    private final Context ctx;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private ImageReader mImageReader;
    private boolean mIsCameraOrientationActive;
    private byte[] frameData;
    private boolean cameraRunning;

    private MemoryOutputStream mJpegOutputStream;
    private int mPreviewBufferSize;
    private MJpegHttpStreamer streamer;


    public FrameListener(Context ctx, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        initStreamer();
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


        @Override
        public void onReady(CameraCaptureSession session) {
            mSession = session;
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


    /**
     * Starts a builtin camera with api camera 2
     */
    private void startCamera() {
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 4 /* images buffered */);
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private Handler mHandler;
    private boolean isProcessing = false;

    private long mNumFrames = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;
    private final MovingAverage mAverageSpf = new MovingAverage(50 /* numValues */);


    /**
     * Listens for frames and send them to  be processed
     */
    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                if (!isProcessing) {
                    // Calcalute the timestamp
                    final Long timestamp = SystemClock.elapsedRealtime();
                    final long MILLI_PER_SECOND = 1000L;
                    final long timestampSeconds = timestamp / MILLI_PER_SECOND;
                    // Update and log the frame rate
                    final long LOGS_PER_FRAME = 10L;
                    mNumFrames++;
                    if (mLastTimestamp != Long.MIN_VALUE)
                    {
                        mAverageSpf.update(timestampSeconds - mLastTimestamp);
                        if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
                        {
                            Log.d(TAG, "FPS: " + 1.0 / mAverageSpf.getAverage());
                        }
                    }
                    // process image
                    image = reader.acquireLatestImage();
                    frameData = ImageUtils.imageToByteArray(image);
//                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                    frameData = new byte[buffer.capacity()];
//                    buffer.get(frameData);
                    if(streamer!=null)streamer.streamJpeg(frameData, frameData.length, timestamp);
                    // Clean up
                    mJpegOutputStream.seek(0);
                    mHandler.post(doImageTracker);
                    mLastTimestamp = timestampSeconds;

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

    private Runnable doImageTracker = new Runnable() {
        public void run() {
            isProcessing = true;
            if (frameData != null){
                np.stringFromJNI(frameData);
            }
            isProcessing = false;
        }
    };

    private void initStreamer() {
        // streamer
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / BITS_PER_BYTE;
        mPreviewBufferSize = mWidth * mHeight * bytesPerPixel * 3 / 2 + 1;
        mJpegOutputStream = new MemoryOutputStream(mPreviewBufferSize);
        streamer = new MJpegHttpStreamer(8080, mPreviewBufferSize);
        streamer.start();
    }

}
