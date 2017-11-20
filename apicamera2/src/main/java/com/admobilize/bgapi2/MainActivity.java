package com.admobilize.bgapi2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.admobilize.bgapi2.receivers.ServiceScheduler;
import com.admobilize.bgapi2.service.CameraService;

import java.nio.ByteBuffer;


/**
 * Created by Antonio Vanegas @hpsaturn on 2017.11.19.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    /**
     * A {@link Handler} for running Cloud tasks in the background.
     */
    private Handler mCloudHandler;

    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;

    private Api2Camera mCamera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return;
        }
        // Example of a call to a native method
        Intent newIntent = new Intent(this, CameraService.class);
        startService(newIntent);
        ServiceScheduler.startScheduleService(this, 5*1000);

//        mCameraThread = new HandlerThread("CameraBackground");
//        mCameraThread.start();
//        mCameraHandler = new Handler(mCameraThread.getLooper());
//
//         Camera code is complicated, so we've shoved it all in this closet class for you.
//        mCamera = Api2Camera.getInstance();
//        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
//


    }

    private int count;
    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            // get image bytes
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            imageBuf.get(imageBytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if(count++==5){
                Log.i(TAG,"5 frames reached");
                count=0;
            }
//            frameData = FileTools.getNV21(bitmap.getWidth(), bitmap.getHeight(), bitmap);
            image.close();
//            onPictureTaken(imageBytes);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();
        mCameraThread.quitSafely();
    }
}
