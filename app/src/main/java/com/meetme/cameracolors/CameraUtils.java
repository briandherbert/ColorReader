package com.meetme.cameracolors;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.util.List;

/**
 * Utilities to help with setting up a camera
 *
 * @author bherbert
 *
 */
public class CameraUtils {
    public static final String TAG = "CameraUtils";

    /**
     * Get an instance of the next available camera. If multiple cameras are available, this will rotate through them. If the SDK version is below 9,
     * the cameraIdx param is ignored.
     */
    public static Camera getNextCameraInstance(int cameraIdx) {
        Camera camera = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            try {
                camera = Camera.open(cameraIdx);
            } catch (RuntimeException e) {
                Log.w("blarg", "error opening camera", e);
            }
        } else {
            camera = Camera.open();
        }

        return camera;
    }

    /**
     * Set camera parameters such as preview size, picture size, focus mode, etc.
     *
     * @param params
     * @param deviceWidth
     * @param deviceHeight
     * @param isCameraSideways
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Parameters getCameraParams(Parameters params, int deviceWidth, int deviceHeight, boolean isCameraSideways, int maxWidth,
                                             int maxHeight) {
        if (params == null) return null;

        // Preview size (best size is the one that matches device dimensions)
        Size mBestPreviewSize = getBestSize(params.getSupportedPreviewSizes(), deviceWidth, deviceHeight, isCameraSideways);
        params.setPreviewSize(mBestPreviewSize.width, mBestPreviewSize.height);

        // Picture size (best size is the one closest to our max photo dimensions!
        Size mBestPicSize = getBestSize(params.getSupportedPictureSizes(), deviceWidth, deviceHeight, isCameraSideways);
        params.setPictureSize(mBestPicSize.width, mBestPicSize.height);

        params.setPreviewFormat(ImageFormat.NV21);

        // Focus mode
        String focusMode = getBestFocusMode(params);

        if (focusMode != null) {
            params.setFocusMode(focusMode);
        }

        return params;
    }

    @SuppressLint("InlinedApi")
    public static String getBestFocusMode(Parameters params) {
        if (params == null) return null;

        String focusMode = null;

        List<String> focusModes = params.getSupportedFocusModes();

        if (focusModes.contains(Parameters.FOCUS_MODE_FIXED)) {
            focusMode = Parameters.FOCUS_MODE_FIXED;
        } else if (focusModes.contains(Parameters.FOCUS_MODE_MACRO)) {
            focusMode = Parameters.FOCUS_MODE_MACRO;
        } else if (focusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            focusMode = Parameters.FOCUS_MODE_AUTO;
        }

        return focusMode;
    }

    /**
     * There's no sure-fire way to always tell if the camera is sideways without taking a picture, but we'll take an educated guess based on whether
     * the camera's preview sizes are mostly the same as our current orientation.
     *
     * @param cameraPreviewSizes
     * @param deviceWidth
     * @param deviceHeight
     * @return
     */
    public static boolean getIsCameraSideways(List<Size> cameraPreviewSizes, int deviceWidth, int deviceHeight) {
        boolean isPortrait = (deviceHeight >= deviceWidth);
        int numSidewaysOrientations = 0;

        for (Size size : cameraPreviewSizes) {
            if (isPortrait == (size.height <= size.width)) {
                numSidewaysOrientations++;
            }
        }

        return (numSidewaysOrientations > cameraPreviewSizes.size() / 2);
    }

    /**
     * Given a range of sizes, find the one that is closest to the given width and height.
     */
    public static Size getBestSize(List<Size> sizes, int width, int height, boolean isCameraSideways) {
        // TODO: Figure out what "best" means. Right now it's just the least difference between the
        // sum of width and height, but should we factor in size and aspect ratio?

        if (sizes == null || width <= 0 || height <= 0) {
            return null;
        }

        // Swap width and height
        if (isCameraSideways) {
            width = width ^ height;
            height = width ^ height;
            width = width ^ height;
        }

        Size bestSize = null;

        // Lower is better
        int bestScore = -1;

        String sizesStr = "supported preview sizes: \n";

        for (Size size : sizes) {
            sizesStr += size.width + ", " + size.height + "\n";
        }

        Log.v(TAG, sizesStr);

        for (Size size : sizes) {
            //int score = Math.abs(width - size.width) + Math.abs(height - size.height);

            // Get a low res, but not lower than lowLimit
            int score = size.width * size.height;
            int lowLimit = 150 * 150;

            if (bestScore == -1 || (score < bestScore & score > lowLimit)) {
                bestScore = score;
                bestSize = size;
            }
        }

        Log.v(TAG, "Found best size to match " + width + "x" + height + ": " + bestSize.width + "x" + bestSize.height);

        return bestSize;
    }

    /**
     * Given a screen orientation and a camera (index), find the degrees required to rotate the camera to align with the current orientation.
     */
    @TargetApi(9)
    public static int getRotationDegrees(int screenOrientation, CameraInfo info) {
        // The camera's orientation, in ordinal direction degrees
        int cameraOrientation = info.orientation;

        // Translate rotation constants into degrees
        if (screenOrientation == Surface.ROTATION_0) {
            screenOrientation = 0;
        } else if (screenOrientation == Surface.ROTATION_90) {
            screenOrientation = 90;
        } else if (screenOrientation == Surface.ROTATION_180) {
            screenOrientation = 180;
        } else {
            screenOrientation = 270;
        }

        // If the camera is front-facing, its rotation is in the counter direction to the back
        // camera, which all our calculations are based on. Practically, we just need to swap 90 and
        // 270.
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            if (cameraOrientation == 270) {
                cameraOrientation = 90;
            } else if (cameraOrientation == 90) {
                cameraOrientation = 270;
            }
        }

        // Tare the rotation and get the result in positive degrees. Parens and order of ops are for
        // clarity and to prevent modulo of a negative.
        int degreesToRotate = ((cameraOrientation - screenOrientation) + 360) % 360;

        return degreesToRotate;
    }

    public static int[] YUVtoRGB(byte[] data, int width, int height, int x, int y) {
        // Get the Y value, stored in the first block of data
        // The logical "AND 0xff" is needed to deal with the signed issue
        int Y = data[y*width + x] & 0xff;

        // Get U and V values, stored after Y values, one per 2x2 block
        // of pixels, interleaved. Prepare them as floats with correct range
        // ready for calculation later.
        int xby2 = x/2;
        int yby2 = y/2;

        int numPixels = width * height;

        // make this V for NV12/420SP
        float U = (float)(data[numPixels + 2*xby2 + yby2*width] & 0xff) - 128.0f;

        // make this U for NV12/420SP
        float V = (float)(data[numPixels + 2*xby2 + 1 + yby2*width] & 0xff) - 128.0f;

        for (int i = numPixels; i < data.length; i++) {
            data[i] = Byte.MAX_VALUE;
        }

        // Do the YUV -> RGB conversion
        float Yf = 1.164f*((float)Y) - 16.0f;
        int R = (int)(Yf + 1.596f*V);
        int G = (int)(Yf - 0.813f*V - 0.391f*U);
        int B = (int)(Yf            + 2.018f*U);

        // Clip rgb values to 0-255
        R = R < 0 ? 0 : R > 255 ? 255 : R;
        G = G < 0 ? 0 : G > 255 ? 255 : G;
        B = B < 0 ? 0 : B > 255 ? 255 : B;

        return new int[]{R, G, B};
    }
}