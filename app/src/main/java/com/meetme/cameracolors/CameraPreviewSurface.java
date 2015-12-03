package com.meetme.cameracolors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
public class CameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private Camera mCamera;

    private boolean mIsCameraSideways;

    BufferUpdateCallback listener;

    public interface BufferUpdateCallback {
        public void onBufferUpdated(byte[] bytes);
    }

    public CameraPreviewSurface(Context context, Camera camera, boolean isCameraSideways, BufferUpdateCallback listener) {
        super(context);

        this.listener = listener;

        setWillNotDraw(false);

        mCamera = camera;

        mIsCameraSideways = isCameraSideways;

        // Install a SurfaceHolder.Callback so we get notified when the underlying surface is
        // created and destroyed.
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    int previewWidth, previewHeight = 0;

    public int getPreviewWidth() {
        return previewWidth;
    }
    public int getPreviewHeight() {
        return previewHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCamera == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        Size previewSize = mCamera.getParameters().getPreviewSize();

        Log.v("blarg", "bits / px " + ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()));

        // If the camera is sideways, swap width and height
        final float pw = mIsCameraSideways ? previewSize.height : previewSize.width;
        final float ph = mIsCameraSideways ? previewSize.width : previewSize.height;

        final float sw = MeasureSpec.getSize(widthMeasureSpec);
        final float sh = MeasureSpec.getSize(heightMeasureSpec);

        // We want to scale so that the entire preview fits in our bounds
        float scale = (sw / sh > pw / ph) ? sh / ph : sw / pw;

        Log.v("blarg", "size " + pw + ", " + ph);

        previewWidth = (int) pw;
        previewHeight = (int) ph;

        setMeasuredDimension((int) (scale * pw), (int) (scale * ph));
    }

    long t0 = System.currentTimeMillis();
    int frames = 0;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created, setting will not draw");

        if (mCamera == null) return;

        try {
            mCamera.setPreviewDisplay(holder);

            mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Log.v("blarg", "first preview frame, buffer size " + data.length);

                    //mCamera.autoFocus(null);

                    mCamera.addCallbackBuffer(data);
                    mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            listener.onBufferUpdated(data);

                            mCamera.addCallbackBuffer(data);
                        }
                    });
                }
            });


            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Surface destroyed");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            holder.getSurface().release();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
}