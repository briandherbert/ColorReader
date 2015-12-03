package com.meetme.cameracolors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Random;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.meetme.cameracolors.CameraPreviewSurface.BufferUpdateCallback;

/**
 * Shows a camera's preview
 *
 * @author bherbert
 *
 */
public class CameraFragment extends Fragment implements CameraHelper.CameraHelperListener, BufferUpdateCallback {
    public static final String TAG = "CameraFragment";

    public static final String EXTRA_SIDEWAYS = "sideways";

    public static final int PHOTO_DELAY = 1500;


    static final int GRID_SIZE = 12;
    /*
     * TODO: This shouldn't be necessary; we should be able to infer if the
     * camera is sideways, but there is no "get" counterpart to
     * Camera.setDisplayOrientation
     */

    private CameraPreviewSurface mPreviewSurface;

    private CameraHelper mCamHelper;

    private CameraFragmentListener mListener;

    FrameLayout mFramePreviewContainer;

    TextView lblColorBuffer;

    ImageView imgPreview;

    /** This is the time when we requested the photo to be taken */
    private long mStartedReactionPhotoTime = 0;

    File screenshotFile = null;

    Random rand = new Random();

    boolean isFirstLoad = true;

    private View mRootView;

    private boolean mShowCameraPreview = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof CameraFragmentListener)) {
            throw new ClassCastException("Activity must implement the CameraListener interface.");
        }

        mListener = (CameraFragmentListener) activity;

        mCamHelper = new CameraHelper(activity);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_camera, container, false);

        mFramePreviewContainer = (FrameLayout) mRootView.findViewById(R.id.preview_container);
        lblColorBuffer = (TextView) mRootView.findViewById(R.id.lbl_color_buffer);

        imgPreview = (ImageView) mRootView.findViewById(R.id.img_preview);

        return mRootView;
    }

    @Override
    public void onPause() {
        mCamHelper.pause();
        mFramePreviewContainer.removeAllViews();

        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onresume");
        super.onResume();

        if (isFirstLoad) {
            int camIdx = -1;

            mCamHelper.resume(camIdx, this);
            isFirstLoad = false;
        }
    }

    /**
     * Initialize the camera, pass it to the SurfaceView, and add the
     * SurfaceView to our fragment's view hierarchy. This sets up everything
     * from scratch, so any existing cameras and views will be released and
     * removed.
     */
    public void showCamera() {

        // Create our Preview view and set it as the content of our activity.
        mPreviewSurface = new CameraPreviewSurface(getActivity(), mCamHelper.getCamera(), mCamHelper.isCameraSideways(), this);

        Log.v(TAG, "degrees to rotate " + mCamHelper.getDegreesToRotatePhoto());

        // Create LP with wrap_content and gravity=center
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;

        // Now add it to the container
        mFramePreviewContainer.removeAllViews();
        mFramePreviewContainer.addView(mPreviewSurface, lp);
    }

    long t0 = 0;
    int frames = 0;
    boolean doJpegConvert = true;


    @Override
    public void onBufferUpdated(byte[] bytes) {
        frames++;
        if (System.currentTimeMillis() - t0 >= 5000) {
            //Log.v("blarg", "framerate " + frames);
            frames = 0;
            t0 = System.currentTimeMillis();

        } else {
            return;
        }


        height = mPreviewSurface.getPreviewWidth();
        width = mPreviewSurface.getPreviewHeight();

        Log.v(TAG, "Width " + width + " height " + height);

        if (!doJpegConvert) {
            // !!! THIS NEXT LINE MAKES THE JPEG PREVIEW GRAYSCALE !!!
            int[] rgb = CameraUtils.YUVtoRGB(bytes, mPreviewSurface.getPreviewWidth(), mPreviewSurface.getPreviewHeight(),
                    0, 0);

            String text = "R: " + rgb[0] + " G: " + rgb[1] + " B: " + rgb[2];
            lblColorBuffer.setText(text);
        } else {
            if (bmpColors == null) {
                bmpColors = new byte[width][height];
                pixels = new int[width * height];
            }

            YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);

            ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, jpegOutput);
            byte[] imageBytes = jpegOutput.toByteArray();

            Bitmap yuvBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            copy = yuvBitmap.copy(Bitmap.Config.RGB_565, true);

            long t0 = System.currentTimeMillis();

            roundColors(yuvBitmap);

            Log.v(TAG, "round colors took " + (System.currentTimeMillis() - t0));

            imgPreview.setImageBitmap(copy);

            Log.v("scanner", "scanning");
        }
    }

    int r, g, b;
    int color;
    int threshholdDiff = 65;
    byte[][] bmpColors;
    int numFloors = 0;
    int max = 0;

    int height, width;

    int[] pixels;

    static byte RED_BYTE = (byte) 0;
    static byte GREEN_BYTE = (byte) 1;
    static byte BLUE_BYTE = (byte) 2;
    static byte MAGENTA_BYTE = (byte) 3;
    static byte BLACK_BYTE = (byte) 4;
    static byte WHITE_BYTE = (byte) 5;

    Bitmap copy;

    static String scantag = "scanning";

    int streak = 0;
    int maxStreak = 0;
    Boolean onStreak = null;

    Point point = new Point();
    Point streakStart = new Point();
    Point streakEnd = new Point();

    int streakColor = Color.BLACK;
    int maxStreakColor = Color.BLACK;


    public void roundColors(Bitmap bmp) {
        maxStreak = 0;

        r = g = b = 0;

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int h = 0; h < height; h++) {
            streak = 0;
            onStreak = null;
            streakColor = Color.BLACK;

            for (int w = 0; w < width; w++) {
                color = pixels[h * width + w];

                r = Color.red(color);
                g = Color.green(color);
                b = Color.blue(color);

                max = Math.max(Math.max(r, g), b);

                numFloors = 0;

                if (max - r > threshholdDiff) {
                    r = 0;
                    numFloors++;
                }

                if (max - g > threshholdDiff) {
                    g = 0;
                    numFloors++;
                }

                if (max - b > threshholdDiff) {
                    b = 0;
                    numFloors++;
                }

                if (numFloors == 2) {
                    color = r > 0 ? Color.RED : g > 0 ? Color.GREEN : Color.BLUE;
                    bmpColors[w][h] = r > 0 ? RED_BYTE : g > 0 ? GREEN_BYTE : BLUE_BYTE;

                    if (streakColor == Color.BLACK && (color == Color.RED || color == Color.GREEN)) {
                        streakColor = color;
                    }
                } else if (numFloors == 0 && r > 100) {
                    color = Color.WHITE;
                    bmpColors[w][h] = WHITE_BYTE;
                } else {
                    color = Color.BLACK;
                    bmpColors[w][h] = BLACK_BYTE;
                }

                if (color != Color.BLACK && color == streakColor) {
                    if (streak == 0) point.set(w, h);

                    streak++;
                    onStreak = true;
                } else if (onStreak != null && onStreak) {
                    //Log.v("allcolors", "missed one");
                    onStreak = false;
                } else if (onStreak != null) {
                    onStreak = null;

                    if (streak > 0) {   // ended streak
                        if (streak > maxStreak) {
                            maxStreak = streak;
                            maxStreakColor = streakColor;
                            streakStart.set(point.x, point.y);
                            streakEnd.set(w, h);
                        }

                        streak = 0;
                    }

                    streakColor = Color.BLACK;
                }

                // TODO: Remove this, it slows us wayyyy down
                copy.setPixel(w, h, color);
            }
        }

        if (maxStreak > 0) {
            Log.v(scantag, "max streak color " +
                    (getColorName(maxStreakColor)) +
                    " length " + maxStreak + " start " + streakStart + " end " + streakEnd);



            int streakLength = Math.abs(streakStart.x - streakEnd.x);
            if (streakLength > width / 2) {
                Log.v(scantag, "streak is long enough: " + streakLength + " color: " + getColorName(maxStreakColor));

                // Order the points LTR for convenience
                if (streakStart.x > streakEnd.x) {
                    int tempX = streakStart.x;

                    streakStart.x = streakEnd.x;
                    streakEnd.x = streakStart.x;
                }

                int approxSquareSize = streakLength / (GRID_SIZE - 1);
                int halfSquare = approxSquareSize / 2;

                int xLeft = Math.max(0, streakStart.x - halfSquare);
                int xRight = Math.min(width - 1, streakEnd.x + halfSquare);

                int colorLeft = colorFromByte(bmpColors[xLeft][streakStart.y]);
                int colorRight = colorFromByte(bmpColors[xRight][streakEnd.y]);

                Log.v(scantag, "color left: " + getColorName(colorLeft) + ", right: " + getColorName(colorRight));

                if (colorLeft != Color.RED && colorLeft != Color.GREEN && colorRight != Color.RED && colorRight != Color.GREEN) {
                    Log.v(scantag, "no origin found");
                    return;
                }

                Point origin = new Point();
                // Orientations
                if (maxStreakColor == Color.RED) {
                    if (colorLeft == Color.GREEN) {
                        Log.v(scantag, "rotated 0");
                        // 0 degrees
                        origin.x = xLeft;
                        origin.y = streakStart.y + halfSquare + (halfSquare / 2);

                        for (int y = 0; y < GRID_SIZE; y++) {
                            for (int x = 0; x < GRID_SIZE; x++) {

                                Log.v(scantag, "Color at (" + x + ", " + y + ") is " +
                                        colorNameFromByte(bmpColors[origin.x + (x * approxSquareSize)][origin.y + (y * approxSquareSize)]));
                            }
                        }

                    } else if (colorRight == Color.GREEN) {

                    }
                } else if (maxStreakColor == Color.GREEN) {
                    if (colorLeft == Color.RED) {

                    } else if (colorRight == Color.RED) {
                        Log.v(scantag, "Rotated 90");
                        // 90 degrees
                        origin.x = xRight;
                        origin.y = streakStart.y + halfSquare;

                        int realColorIdx = 0;
                        for (int x = 11; x >= 0; x--) {
                            for (int y = 0; y < 12; y++) {

                                Log.v(scantag, "Color at " + (realColorIdx % GRID_SIZE) + ", " + (realColorIdx / GRID_SIZE) + " is " +
                                        colorNameFromByte(bmpColors[x * approxSquareSize][y *approxSquareSize]));
                                realColorIdx++;
                            }
                        }
                    }
                } else {
                    Log.v(scantag, "no origin found");
                }

            }
        }
    }

    static int colorFromByte(byte b) {
        if (b == RED_BYTE) {
            return Color.RED;
        } else if (b == GREEN_BYTE) {
            return Color.GREEN;
        } else if (b == BLUE_BYTE) {
            return Color.BLUE;
        } else if (b == MAGENTA_BYTE) {
            return Color.MAGENTA;
        } else if (b == WHITE_BYTE) {
            return Color.WHITE;
        }

        return Color.BLACK;
    }

    static String colorNameFromByte(byte b) {
        if (b == RED_BYTE) {
            return "red";
        } else if (b == GREEN_BYTE) {
            return "green";
        } else if (b == BLUE_BYTE) {
            return "blue";
        } else if (b == MAGENTA_BYTE) {
            return "magenta";
        } else if (b == WHITE_BYTE) {
            return "white";
        }

        return "black";
    }

    public static String getColorName(int color) {
        switch (color) {
            case Color.RED:
                return "red";
            case Color.GREEN:
                return "green";
            case Color.BLUE:
                return "blue";
            case Color.BLACK:
                return "black";
            case Color.WHITE:
                return "white";
            default:
                return "unknown";
        }
    }

    public void findFirstColor(Bitmap bmp) {
        // First find the #f0f magenta border

        int xOrigin, yOrigin = 0;

        int borderWidth = 0;

        int y = 0;
        int x = bmp.getWidth() / 2;

        for (y = 0; y < bmp.getHeight(); y++) {
            int color = bmp.getPixel(x, y);

            if (isMagenta(color)) {
                borderWidth++;
                y++;
                Log.v("scanner", "found magenta!");
                // We have the start of a magenta border. Figure the width of it
                while (y < bmp.getHeight() && isMagenta(bmp.getPixel(x, y++))) borderWidth++;

                Log.v("scanner", "magenta border width " + borderWidth);
                break;
            }
        }


    }

    public static boolean isMagenta(int color) {
        return isMagenta(Color.red(color), Color.green(color), Color.blue(color));
    }

    public static boolean isMagenta(int red, int green, int blue) {

        return red > 50 && blue > 60 && green < 10;
    }

    //ByteArrayOutputStream jpegOutput=new ByteArrayOutputStream();


    public interface CameraFragmentListener {
        public void onCameraFailed();

    }

    @Override
    public void onCameraInitialized(boolean hasFlash) {
        showCamera();
    }

    @Override
    public void onCameraInitializeFailed() {
        mListener.onCameraFailed();
    }
}
