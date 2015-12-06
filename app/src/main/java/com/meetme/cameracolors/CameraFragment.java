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
 */
public class CameraFragment extends Fragment implements CameraHelper.CameraHelperListener, BufferUpdateCallback {
    public static final String TAG = "CameraFragment";

    public static final String EXTRA_SIDEWAYS = "sideways";

    public static final int PHOTO_DELAY = 1500;


    static final double GRID_SIZE = 12;
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

    /**
     * This is the time when we requested the photo to be taken
     */
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
        imgPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeNextPreview = true;
            }
        });

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
    boolean analyzeNextPreview = false;


    @Override
    public void onBufferUpdated(byte[] bytes) {
        frames++;
        if (System.currentTimeMillis() - t0 >= 500) {
            //Log.v("blarg", "framerate " + frames);
            frames = 0;
            t0 = System.currentTimeMillis();

        } else {
            return;
        }


        height = mPreviewSurface.getPreviewWidth();
        width = mPreviewSurface.getPreviewHeight();

        if (!doJpegConvert) {
            // !!! THIS NEXT LINE MAKES THE JPEG PREVIEW GRAYSCALE !!!
            int[] rgb = CameraUtils.YUVtoRGB(bytes, mPreviewSurface.getPreviewWidth(), mPreviewSurface.getPreviewHeight(),
                    0, 0);

            String text = "R: " + rgb[0] + " G: " + rgb[1] + " B: " + rgb[2];
            lblColorBuffer.setText(text);
        } else {
            if (!analyzeNextPreview) return;

            Log.v(scantag, "Width " + width + " height " + height);


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

            Log.v(scantag, "round colors took " + (System.currentTimeMillis() - t0));

            imgPreview.setImageBitmap(copy);

            analyzeNextPreview = false;

            int firstColor = yuvBitmap.getPixel(0, 0);
//            String text = "R: " + Color.red(firstColor) + " G: " + Color.green(firstColor) + " B: " + Color.blue(firstColor);
//            lblColorBuffer.setText(text);

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

    public static byte RED_BYTE = (byte) 0;
    public static byte GREEN_BYTE = (byte) 1;
    public static byte BLUE_BYTE = (byte) 2;
    public static byte WHITE_BYTE = (byte) 3;
    static byte BLACK_BYTE = (byte) 4;
    static byte MAGENTA_BYTE = (byte) 5;

    int RED = 0;
    int GREEN = 1;
    int BLUE = 2;

    Bitmap copy;

    static String scantag = "scanning";

    int streak = 0;
    int maxStreak = 0;
    Boolean onStreak = null;

    Point point = new Point();

    int streakColor = Color.BLACK;

    int minMaxStreak;
    double minMaxStreakPctOfWidth = .5;

    Line[] lines = {new Line(), new Line(), new Line()};

    public class Line {
        String TAG = "Line";
        public Point start = new Point();
        public Point end = new Point();

        int length = 0;
        double slope = 0;

        public Line() {
        }

        public void set(int x0, int x1, int y) {
            set(new Point(x0, y), new Point(x1, y));
        }

        public void set(Point start, Point end) {
            if (start.x > end.x) {
                int tempX = start.x;
                start.x = end.x;
                end.x = tempX;
            }

            this.start.x = start.x;
            this.start.y = start.y;

            this.end.x = end.x;
            this.end.y = end.y;

            length = end.x - start.x;

            slope = (start.y - end.y) / (double) (start.x - end.x);

            Log.v(TAG, "Created line " + start + ", " + end + " length " + length + " slope " + slope);
        }
    }

    public void roundColors(Bitmap bmp) {
        minMaxStreak = (int) (width * minMaxStreakPctOfWidth);
        maxStreak = 0;

        r = g = b = 0;

        for (int i = 0; i < lines.length; i++) lines[i].set(0, 0, 0);

        // Get a byte array of colors
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        // Round colors to r, g, b, white, black
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

                    if (streakColor == Color.BLACK && (color == Color.RED || color == Color.GREEN || color == Color.BLUE)) {
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
                    onStreak = false;
                } else if (onStreak != null) {
                    onStreak = null;

                    if (streak > 0) {   // ended streak
                        if (streak > lines[idxFromColor(streakColor)].length) {
                            lines[idxFromColor(streakColor)].set(point.x, w - 1, point.y);
                        }

                        streak = 0;
                    }

                    streakColor = Color.BLACK;
                }

                // TODO: Remove this, it slows us wayyyy down
                copy.setPixel(w, h, color);
            }
        }

        Log.v(scantag, "max streaks - red: " + lines[0].length + ", green: " + lines[1].length + ", blue: " + lines[2].length);

        if (lines[2].length >= minMaxStreak && (lines[0].length >= minMaxStreak || lines[1].length >= minMaxStreak)) {
            Log.v(scantag, "max streaks met");

            drawLine(lines[BLUE]);

            int nonBlueColor = lines[0].start.y < lines[1].start.y ? Color.RED : Color.GREEN;

            Log.v(scantag, "non blue color is " + getColorName(nonBlueColor));
            Point origin = new Point();
            byte colorByte;

            double approxSquareSize;
            double halfSquare;
            // Orientations
            if (nonBlueColor == Color.RED) {

                if (lines[RED].start.y < lines[BLUE].start.y) {
                    drawLine(lines[RED]);

                    approxSquareSize = lines[RED].length / GRID_SIZE;
                    halfSquare = approxSquareSize / 2.0;

                    Log.v(scantag, "rotated 0");
                    // 0 degrees
                    origin.x = (int) Math.round(lines[RED].start.x + halfSquare - 1);
                    origin.y = (int) (lines[RED].start.y + halfSquare + (halfSquare * (.66)));

                    // Mark origin and streak
                    drawPoint(origin, Color.MAGENTA);

                    int fails = 0;

                    int coordX, coordY;

                    int idx = 0;
                    int sumInt = 0;
                    String message = "";

                    for (int y = 0; y < GRID_SIZE; y++) {
                        for (int x = 0; x < GRID_SIZE; x++) {
                            if (idx == 4) {
                                message += ((char) sumInt);

                                sumInt = 0;
                                idx = 0;
                            }

                            coordX = (int) Math.round(origin.x + (x * approxSquareSize));
                            coordY = (int) Math.round(origin.y + (y * approxSquareSize));
                            colorByte = bmpColors[coordX][coordY];

                            drawPoint(coordX, coordY);
                            Log.v(scantag, "Color at (" + x + ", " + y + ") is " + colorNameFromByte(colorByte) + " right? " +
                                    (colorByte == ANSWER[y][x]) + " answer " + colorNameFromByte(ANSWER[y][x]));

                            if (colorByte != ANSWER[y][x]) fails++;

                            sumInt += colorByte << (idx * 2);
                            idx++;
                        }
                    }

                    Log.v(scantag, "Finished, fails: " + fails);
                    lblColorBuffer.setText((message));

                } else {

                }
            } else { // Nonblue is green

            }
        }
    }

    public void drawPoint(Point origin, int color) {
        copy.setPixel(origin.x, origin.y, Color.MAGENTA);
        copy.setPixel(origin.x + 1, origin.y, color);
        copy.setPixel(origin.x - 1, origin.y, color);
        copy.setPixel(origin.x, origin.y + 1, color);
        copy.setPixel(origin.x, origin.y - 1, color);
    }

    public void drawPoint(Point origin) {
        drawPoint(origin, Color.CYAN);
    }

    public void drawPoint(int x, int y) {
        drawPoint(new Point(x, y), Color.CYAN);
    }

    public void drawLine(Line line) {
        drawPoint(line.start, Color.YELLOW);
        drawPoint(line.end, Color.YELLOW);
    }

    static int idxFromColor(int color) {
        if (color == Color.RED) {
            return 0;
        } else if (color == Color.GREEN) {
            return 1;
        } else if (color == Color.BLUE) {
            return 2;
        }

        return -1;
    }

    public static int colorFromByte(byte b) {
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

    public static boolean isMagenta(int color) {
        return isMagenta(Color.red(color), Color.green(color), Color.blue(color));
    }

    public static boolean isMagenta(int red, int green, int blue) {

        return red > 50 && blue > 60 && green < 10;
    }


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


    static byte[][] ANSWER = {
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE},
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE},
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE},
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE},
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE, WHITE_BYTE, RED_BYTE},
            {RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE, RED_BYTE, GREEN_BYTE, BLUE_BYTE},
            {GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE, GREEN_BYTE, GREEN_BYTE, WHITE_BYTE, WHITE_BYTE}
    };
}
