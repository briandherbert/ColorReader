package com.meetme.cameracolors;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.Display;

/**
 * Handles the setup/teardown of hardware camera(s). Be sure to call {@link #pause()} and {@link #resume(int, CameraHelperListener)} in the
 * corresponding lifecycle callbacks of your implementing fragment or activity!
 *
 * @author bherbert
 *
 */
public class CameraHelper {
    private static final String TAG = "CameraHelper";

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenRotation;

    private int mNumCameras = 1;

    private Camera mCamera;
    private int mCameraIdx = 0;

    private boolean mIsFrontFacing = false;
    private boolean mIsCameraSideways = false;
    private int mDegreesToRotate = 0;
    private boolean mHasFlashOnAnyCamera = false;
    private boolean mIsFlashOn = false;

    CameraHelperListener mListener;

    public interface CameraHelperListener {
        /**
         * Called when the camera is successfully initialized; hasFlash indicates whether this specific camera has a flash
         */
        public void onCameraInitialized(boolean hasFlash);

        public void onCameraInitializeFailed();
    }

    public CameraHelper(Activity activity) {
        // Get screen dimensions
        Display display = activity.getWindowManager().getDefaultDisplay();
        mScreenWidth = display.getWidth();
        mScreenHeight = display.getHeight();
        Log.v(TAG, "cam helper says height is " + mScreenHeight);
        mScreenRotation = display.getRotation();

        mNumCameras = Camera.getNumberOfCameras();
        mHasFlashOnAnyCamera = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void resume(int camIdx, CameraHelperListener listener) {
        mListener = listener;
        initCamera(camIdx);
    }

    public void pause() {
        if (mCamera == null) return;

        mCamera.stopPreview();
        mCamera.release();
        mListener = null;
        mCamera = null;
    }

    public void setParams(Parameters params) {
        if (mCamera == null) return;
        mCamera.setParameters(params);
    }

    public void setFlash(boolean on) {
        if (mCamera == null || !mHasFlashOnAnyCamera) return;

        Parameters params = mCamera.getParameters();
        params.setFlashMode(on ? Parameters.FLASH_MODE_ON : Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);

        mIsFlashOn = on;
    }

    /**
     * Get a new camera instance based on {@link #mCameraIdx}, releasing any existing ones, and set {@link #mIsCameraSideways}
     *
     * @return
     */
    private void initCamera(int camIdx) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        if (camIdx >= 0 && camIdx < mNumCameras) {
            mCameraIdx = camIdx;
        } else {
            mCameraIdx = getBackFacingCamIdx();
        }

        mCamera = CameraUtils.getNextCameraInstance(mCameraIdx);

        if (mCamera == null) {
            if (mListener != null) {
                mListener.onCameraInitializeFailed();
            }
            return;
        }

        // Rotate the camera if necessary
        mIsCameraSideways = CameraUtils.getIsCameraSideways(mCamera.getParameters().getSupportedPreviewSizes(), mScreenWidth, mScreenHeight);

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mCameraIdx, info);
        mIsFrontFacing = (info.facing == CameraInfo.CAMERA_FACING_FRONT);

        mDegreesToRotate = CameraUtils.getRotationDegrees(mScreenRotation, info);

        mCamera.setDisplayOrientation(mDegreesToRotate);

        int maxDimen = 1000;

        // Get and set params
        Parameters params = CameraUtils.getCameraParams(mCamera.getParameters(), mScreenWidth, mScreenHeight, mIsCameraSideways, maxDimen, maxDimen);

        params.setJpegQuality(70);

        if (params != null) {
            // The flash needs to get reset after every picture
            params.setFlashMode(mHasFlashOnAnyCamera && mIsFlashOn ? Parameters.FLASH_MODE_ON : Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);

        }

        if (mListener != null) {
            mListener.onCameraInitialized(mHasFlashOnAnyCamera && !mIsFrontFacing);
        }
    }

    /**
     * Iterate to the next camera
     */
    public void switchCameras() {
        if (mNumCameras > 1) {
            mCameraIdx = (mCameraIdx + 1) % mNumCameras;
            initCamera(mCameraIdx);
        }
    }

    /**
     * Get the index of the first available front-facing camera; defaults to 0
     */
    public int getFrontFacingCamIdx() {
        CameraInfo info = new CameraInfo();

        for (int i = 0; i < mNumCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }

        return 0;
    }

    public int getBackFacingCamIdx() {
        CameraInfo info = new CameraInfo();

        for (int i = 0; i < mNumCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }

        return 0;
    }

    public void takePicture(PictureCallback callback) {
        Log.v(TAG, "Taking picture, width=" + mCamera.getParameters().getPictureSize().width);
        mCamera.takePicture(null, null, callback);
    }

    public boolean isFrontFacing() {
        return mIsFrontFacing;
    }

    public boolean isCameraSideways() {
        return mIsCameraSideways;
    }

    public int getDegreesToRotatePhoto() {
        if (mIsFrontFacing) {
            if (mDegreesToRotate == 270) {
                return 90;
            } else if (mDegreesToRotate == 90) {
                return 270;
            }
        }

        return mDegreesToRotate;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public int getCameraIdx() {
        return mCameraIdx;
    }

    public boolean hasFlashOnAnyCamera() {
        return mHasFlashOnAnyCamera;
    }

    public int getNumCameras() {
        return mNumCameras;
    }
}