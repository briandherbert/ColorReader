package com.meetme.cameracolors;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import static com.meetme.cameracolors.Constants.*;

import com.meetme.cameracolors.CameraPreviewSurface.BufferUpdateCallback;

/**
 * Shows a camera's preview
 *
 * @author bherbert
 */
public class CameraFragment extends Fragment implements CameraHelper.CameraHelperListener, BufferUpdateCallback {
    public static final String TAG = "CameraFragment";

    boolean showBmp = false;

    private CameraPreviewSurface mPreviewSurface;

    private CameraHelper mCamHelper;

    private CameraFragmentListener mListener;

    FrameLayout mFramePreviewContainer;

    TextView mLblMessage;
    ImageView mImgPreview;

    Button mBtnRecord;
    Button mBtnSnapshot;

    private View mRootView;

    int rotationNeeded = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof CameraFragmentListener)) {
            throw new ClassCastException("Activity must implement the CameraListener interface.");
        }

        mListener = (CameraFragmentListener) context;

        mCamHelper = new CameraHelper((Activity) context);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_camera, container, false);

        mFramePreviewContainer = (FrameLayout) mRootView.findViewById(R.id.preview_container);
        mLblMessage = (TextView) mRootView.findViewById(R.id.lbl_color_buffer);
        mImgPreview = (ImageView) mRootView.findViewById(R.id.img_preview);

        mBtnRecord = (Button) mRootView.findViewById(R.id.btn_record);
        mBtnSnapshot = (Button) mRootView.findViewById(R.id.btn_snapshot);

        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreviewSurface.tryFocus();
                mIsRecording = !mIsRecording;
                mBtnRecord.setText(mIsRecording ? "Stop" : "Start");
            }
        });

        mBtnSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBmp = true;
                mIsRecording = false;
                mBtnRecord.setText(mIsRecording ? "Stop" : "Start");
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

        mCamHelper.resume(-1, this);
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
    boolean mIsRecording = false;

    int height = 0;
    int width = 0;
    int smallerDimen = 0;
    int[] pixels;
    Bitmap copy;
    Bitmap yuvBitmap;

    static final String STATS_TAG = TAG + ":stats";

    @Override
    public void onBufferUpdated(byte[] bytes) {
        frames++;
        if (System.currentTimeMillis() - t0 >= 1000) {
            Log.v(STATS_TAG, "framerate " + frames);
            frames = 0;
            t0 = System.currentTimeMillis();
        }

        if (!mIsRecording && !showBmp) return;

        // Init size dependent vars
        if (height == 0) {
            height = mPreviewSurface.getPreviewWidth();
            width = mPreviewSurface.getPreviewHeight();
            Log.v(STATS_TAG, "Width " + width + " height " + height);

            pixels = new int[width * height];

            smallerDimen = Math.min(height, width);

            minMaxStreak = (int) (smallerDimen * STREAK_MIN_PCT_WIDTH);

            streakStartX = width / 2;
        }

        long t0 = System.nanoTime();
        long t0prime = System.nanoTime();

        // Convert bytes first to YUV image, then to RGB
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, jpegOutput);
        byte[] imageBytes = jpegOutput.toByteArray();
        yuvBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        // Dump image into pixel array
        yuvBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        Log.v(STATS_TAG, "Getting yuv bmp took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));

        if (showBmp) copy = yuvBitmap.copy(Bitmap.Config.RGB_565, true);

        t0 = System.nanoTime();

        if (showBmp) {
            roundAllColors();
            getStreaks();
        } else {
            getStreaks();
        }

        Log.v(STATS_TAG, "roundColorsAndGetStreaks took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0) + " show bmp? " + showBmp);
        t0 = System.nanoTime();

        if (showBmp) mImgPreview.setImageBitmap(copy);

        if (!findMarkerSpans()) {
            showBmp = false;
            return;
        }

        Log.v(STATS_TAG, "findMarkerSpans took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        t0 = System.nanoTime();

        if (!getCorners()) {
            showBmp = false;
            return;
        }

        Log.v(STATS_TAG, "getCorners took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        t0 = System.nanoTime();

        getColorBytes();
        Log.v(STATS_TAG, "getColorBytes took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        t0 = System.nanoTime();

        rotateByteColors();

        getMessage();

        Log.v(TAG, "getMessage took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0));
        Log.v(TAG, "successful message took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0prime) + " show bmp? " + showBmp);

        if (showBmp) mImgPreview.setImageBitmap(copy);
        showBmp = false;


//            int firstColor = yuvBitmap.getPixel(0, 0);
//            String text = "R: " + Color.red(firstColor) + " G: " + Color.green(firstColor) + " B: " + Color.blue(firstColor);
//            lblColorBuffer.setText(text);

    }

    static final String ROUND_COLORS_TAG = TAG + "roundcolors";

    int r, g, b;
    int pxColor;
    int minMaxStreak;

    Point point = new Point();

    Line[] lines = {new Line(), new Line(), new Line()};

    // If r, g, or b is this much less than the max value, it'll be set to 0
    static final int RGB_VAL_DIFF_THRESHHOLD = 40;
    static final int WHITE_MIN_VAL = 100;
    int numFloors = 0;
    int max = 0;


    public byte getByteColorAt(int x, int y) {
        if (!isPointValid(x, y)) return BLACK_BYTE;

        pxColor = pixels[y * width + x];

        r = Color.red(pxColor);
        g = Color.green(pxColor);
        b = Color.blue(pxColor);

        max = Math.max(Math.max(r, g), b);

        numFloors = 0;

        if (max - r > RGB_VAL_DIFF_THRESHHOLD) {
            r = 0;
            numFloors++;
        }

        if (max - g > RGB_VAL_DIFF_THRESHHOLD) {
            g = 0;
            numFloors++;
        }

        if (max - b > RGB_VAL_DIFF_THRESHHOLD) {
            b = 0;
            numFloors++;
        }

        if (numFloors == 2) {
            return r > 0 ? RED_BYTE : g > 0 ? GREEN_BYTE : BLUE_BYTE;
        } else if (NUM_COLORS == 8 && numFloors == 1) {
            pxColor = r == 0 ? Color.CYAN : g == 0 ? Color.MAGENTA : Color.YELLOW;
            return r == 0 ? CYAN_BYTE : g == 0 ? MAGENTA_BYTE : YELLOW_BYTE;
        } else if ((numFloors == 0 || numFloors == 1) && max > WHITE_MIN_VAL) {
            return WHITE_BYTE;
        } else {
            return BLACK_BYTE;
        }
    }

    /**
     * Scan horizontally, looking for horizontal streaks, rounding colors as we go
     */
    public void roundAllColors() {
        Log.v(TAG, "roundColorsAndGetStreaks");

        // Round colors to r, g, b, white, black
        for (int h = 0; h < height; h++) {
            // Go across a row
            for (int w = 0; w < width; w++) {
                // TODO: Remove this, it slows us wayyyy down
                if (showBmp) copy.setPixel(w, h, colorFromByte(getByteColorAt(w, h)));
            }
        }
    }

    static final double STREAK_MIN_PCT_WIDTH = .7;

    public void getStreaks() {
        Log.v(TAG, "getStreaks");

        for (int i = 0; i < lines.length; i++) lines[i].set(0, 0, 0);

        int topY = 0;
        int bottomY = 0;
        boolean hitValidStreak = false;

        // Scan down from the horizontal center looking for colors
        for (topY = 0; topY < height - minMaxStreak; topY++) {
            if (findLongestStreak(topY) >= minMaxStreak) {
                hitValidStreak = true;
            } else if (hitValidStreak) {
                break;
            }
        }

        hitValidStreak = false;

        for (bottomY = height - 1; bottomY > topY && bottomY >= minMaxStreak; bottomY--) {
            findLongestStreak(bottomY);

            drawPoint(streakStartX, bottomY);

            if (findLongestStreak(bottomY) >= minMaxStreak) {
                hitValidStreak = true;
            } else if (hitValidStreak) {
                break;
            }
        }
    }

    int streakStartX = width / 2;
    byte streakColorByte;

    int streakLeftX = streakStartX - 1;
    int streakRightX = streakStartX + 1;

    /**
     * Returns the color of a valid streak if there's also a line of the same color and approx length
     */
    public int findLongestStreak(int y) {
        boolean strike = false;
        byte byteColor = BLACK_BYTE;

        streakColorByte = getByteColorAt(streakStartX, y);
        if (streakColorByte != BLACK_BYTE && streakColorByte != WHITE_BYTE) {
            // To the left
            for (streakLeftX = streakStartX - 1; streakLeftX > 0; streakLeftX--) {
                byteColor = getByteColorAt(streakLeftX, y);
                if (byteColor != streakColorByte) {
                    if (strike) {
                        if (byteColor != WHITE_BYTE) {
                            return 0;
                        }

                        break;
                    } else {
                        strike = true;
                    }
                } else {
                    strike = false;
                }
            }

            byteColor = BLACK_BYTE;
            strike = false;

            // To the right
            for (streakRightX = streakStartX + 1; streakRightX < width; streakRightX++) {
                byteColor = getByteColorAt(streakRightX, y);
                if (byteColor != streakColorByte) {
                    if (strike) {
                        if (byteColor != WHITE_BYTE) {
                            return 0;
                        }

                        break;
                    } else {
                        strike = true;
                    }
                } else {
                    strike = false;
                }
            }

            streakLeftX += 2;
            streakRightX -= 2;

            // Check the length
            if (streakRightX - streakLeftX > lines[streakColorByte].length) {
                lines[streakColorByte].set(streakLeftX, streakRightX, y);
            }

            return streakRightX - streakLeftX;
        }

        return 0;
    }

    static final String MARKER_SPANS_TAG = TAG + "marker";

    //byte nonBlueColorByte = BLACK_BYTE;
    byte topColorByte = BLACK_BYTE;
    byte bottomColorByte = BLACK_BYTE;

    static final double STREAKS_DIFF_PCT = .04;
    double approxSquareSize;
    double halfSquare;

    /**
     * Find the border streaks of color. Clockwise from top is red, blue, blue, green
     *
     * @return
     */
    boolean findMarkerSpans() {
        Log.v(TAG, "findMarkerSpans");
        byte nonBlueColorByte = lines[RED_BYTE].length > lines[GREEN_BYTE].length ? RED_BYTE : GREEN_BYTE;
        topColorByte = lines[nonBlueColorByte].start.y < lines[BLUE_BYTE].start.y ? nonBlueColorByte : BLUE_BYTE;
        bottomColorByte = (topColorByte == BLUE_BYTE) ? nonBlueColorByte : BLUE_BYTE;

        Log.v(MARKER_SPANS_TAG, "max streaks - red: " + lines[RED_BYTE].length + ", green: " + lines[GREEN_BYTE].length + ", blue: " + lines[BLUE_BYTE].length);

        if (!(lines[BLUE_BYTE].length >= minMaxStreak && (lines[RED_BYTE].length >= minMaxStreak || lines[GREEN_BYTE].length >= minMaxStreak))) {
            return false;
        }

        Log.v(MARKER_SPANS_TAG, "max streaks met");

        for (int i = 0; i < 3; i++) {
            Log.v(TAG, "streak for " + colorNameFromByte((byte) i) + " is " + lines[i].toString());
        }

        Log.v(MARKER_SPANS_TAG, "non blue color is " + getColorNameFromColor(colorFromByte(nonBlueColorByte)) + "; top? " + (topColorByte == nonBlueColorByte));

        drawLine(lines[topColorByte]);
        drawLine(lines[bottomColorByte]);

        // Validation steps
        int wiggle = (int) (width * STREAKS_DIFF_PCT);

        boolean streaksMatch = Math.abs(lines[topColorByte].start.x - lines[BLUE_BYTE].start.x) < wiggle &&
                Math.abs(lines[topColorByte].end.x - lines[BLUE_BYTE].end.x) < wiggle;

        Log.v(MARKER_SPANS_TAG, "Streaks match? " + streaksMatch);

        if (!streaksMatch) return false;

        approxSquareSize = lines[topColorByte].length / ((double) NUM_SQUARES_PER_SIDE);

        Log.v(MARKER_SPANS_TAG, "square size: " + approxSquareSize);
        halfSquare = approxSquareSize / 2.0;

        return true;
    }

    static final String CORNERS_TAG = TAG + ":corners";
    Point[] cornerPoints = {new Point(0, 0), new Point(0, 0), new Point(0, 0), new Point(0, 0)};

    float numWhitePoints = 0;
    int ySum = 0;

    /**
     * Find exactly where the marker lines intersect
     *
     * @return
     */
    boolean getCorners() {
        Log.v(TAG, "getCorners");

        int quarterSquare = (int) halfSquare / 2;
        int offsetX;

        Point origin;

        for (Corner corner : Corner.values()) {
            int streakColor = corner.isTop() ? topColorByte : bottomColorByte;
            origin = corner.isLeft() ? lines[streakColor].start : lines[streakColor].end;

            if (!isPointValid(origin)) {
                Log.v(MARKER_SPANS_TAG, "Streak bounds are too close to borders!");
                return false;
            }

            // The end of the streak is 1 to the right
            cornerPoints[corner.ordinal()].set(corner.isLeft() ? origin.x - 1 : origin.x, origin.y);

            offsetX = corner.isLeft() ? origin.x - quarterSquare : origin.x + quarterSquare;
            Point[] yPoints = {new Point(offsetX, origin.y), new Point(offsetX - 1, origin.y), new Point(offsetX + 1, origin.y)};
            Boolean[] yPointsHitWhite = new Boolean[yPoints.length];

            // Move vertically until we hit white, then not white
            for (int i = 0; i < yPoints.length; i++) {
                if (!isPointValid(yPoints[i])) continue;

                for (int j = 0; j < approxSquareSize; j++) {
                    if (getByteColorAt(yPoints[i].x, yPoints[i].y) == WHITE_BYTE) {
                        yPointsHitWhite[i] = Boolean.FALSE;
                    } else if (Boolean.FALSE == yPointsHitWhite[i]) {
                        yPointsHitWhite[i] = true;
                        break;
                    }

                    if (corner.isTop()) {
                        yPoints[i].y++;
                        if (yPoints[i].y >= height) break;
                    } else {
                        yPoints[i].y--;
                        if (yPoints[i].y < 0) break;
                    }
                }
            }

            numWhitePoints = 0;
            ySum = 0;

            // See what our average y val for the vert marker is
            for (int i = 0; i < yPoints.length; i++) {
                if (Boolean.TRUE == yPointsHitWhite[i]) {
                    ySum += yPoints[i].y;
                    numWhitePoints++;
                }
            }

            if (numWhitePoints == 0) {
                Log.v(CORNERS_TAG, "No white points found for " + corner.name());
                // TODO: Interpolate?
                return false;
            }

            cornerPoints[corner.ordinal()].y = Math.round(ySum / numWhitePoints);

            Log.v(CORNERS_TAG, "Corner for " + corner.name() + " is " + cornerPoints[corner.ordinal()]);
            drawPoint(cornerPoints[corner.ordinal()], Color.MAGENTA);
        }

        return true;
    }

    byte[][] colorBytes = new byte[NUM_SQUARES_PER_SIDE][NUM_SQUARES_PER_SIDE];

    /**
     * Make a NUM_SQUARES_PER_SIDE ^ 2 sized grid with just the color byte in each square
     */
    void getColorBytes() {
        Log.v(TAG, "getColorBytes");

        int quadSize = NUM_SQUARES_PER_SIDE / 2;
        int coordX, coordY;
        byte colorByte;

        for (Corner corner : Corner.values()) {
            int xOffSet = (int) (corner.isLeft() ? halfSquare : -halfSquare);
            int yOffSet = (int) (corner.isTop() ? halfSquare : -halfSquare);

            Point origin = new Point(cornerPoints[corner.ordinal()].x + xOffSet, cornerPoints[corner.ordinal()].y + yOffSet);

            for (int y = 0; y < quadSize; y++) {
                coordY = (int) (corner.isTop() ? origin.y + Math.round(y * approxSquareSize) : origin.y - Math.round(y * approxSquareSize));
                for (int x = 0; x < quadSize; x++) {
                    coordX = (int) (corner.isLeft() ? origin.x + Math.round(x * approxSquareSize) : origin.x - Math.round(x * approxSquareSize));
                    drawPoint(coordX, coordY);

                    colorByte = getByteColorAt(coordX, coordY);

                    if (Corner.topLeft == corner) {
                        colorBytes[x][y] = colorByte;
                    } else if (Corner.topRight == corner) {
                        colorBytes[NUM_SQUARES_PER_SIDE - 1 - x][y] = colorByte;
                    } else if (Corner.bottomLeft == corner) {
                        colorBytes[x][NUM_SQUARES_PER_SIDE - 1 - y] = colorByte;
                    } else if (Corner.bottomRight == corner) {
                        colorBytes[NUM_SQUARES_PER_SIDE - 1 - x][NUM_SQUARES_PER_SIDE - 1 - y] = colorByte;
                    }
                }
            }
        }
    }

    void rotateByteColors() {
        if (topColorByte == BLUE_BYTE) {
            if (bottomColorByte == RED_BYTE) {
                rotationNeeded = 180;
            } else {
                rotationNeeded = 90;
            }
        } else if (topColorByte == GREEN_BYTE) {
            rotationNeeded = 270;
        } else {
            rotationNeeded = 0;
        }

        Log.v(TAG, "Rotation needed " + rotationNeeded);
        if (rotationNeeded == 0) return;

        byte[][] colorBytesRotated = new byte[NUM_SQUARES_PER_SIDE][NUM_SQUARES_PER_SIDE];
        if (rotationNeeded == 90) {
            for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
                for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                    colorBytesRotated[NUM_SQUARES_PER_SIDE - 1 - y][x] = colorBytes[x][y];
                }
            }
        } else if (rotationNeeded == 180) {
            for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
                for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                    colorBytesRotated[NUM_SQUARES_PER_SIDE - 1 - x][NUM_SQUARES_PER_SIDE - 1 - y] = colorBytes[x][y];
                }
            }
        } else {    // 270
            for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
                for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                    colorBytesRotated[y][NUM_SQUARES_PER_SIDE - 1 - x] = colorBytes[x][y];
                }
            }
        }

        colorBytes = colorBytesRotated;
    }

    void getMessage() {
        Log.v(TAG, "getMessage");

        int idx = 0;
        int sumInt = 0;
        String message = "";

        // Get the colors on the grid
        for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
            for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                if (idx == NUM_COLORS) {
                    message += ((char) sumInt);

                    sumInt = 0;
                    idx = 0;
                }

                sumInt += colorBytes[x][y] << (idx * 2);
                idx++;
            }
        }

        message += ((char) sumInt);

        Log.v(TAG, "Message: " + message);

        mLblMessage.setText(message);
    }

    enum Corner {
        topLeft, topRight, bottomRight, bottomLeft;

        public boolean isLeft() {
            return this == topLeft || this == bottomLeft;
        }

        public boolean isTop() {
            return this == topLeft || this == topRight;
        }
    }

    public void drawPoint(Point origin, int color) {
        if (!showBmp) return;

        safeSetPixel(origin.x, origin.y, color == Color.MAGENTA ? Color.YELLOW : Color.MAGENTA);
        safeSetPixel(origin.x + 1, origin.y, color);
        safeSetPixel(origin.x - 1, origin.y, color);
        safeSetPixel(origin.x, origin.y + 1, color);
        safeSetPixel(origin.x, origin.y - 1, color);
    }

    void safeSetPixel(int x, int y, int color) {
        if (isPointValid(x, y)) {
            if (copy != null) copy.setPixel(x, y, color);
        }
    }

    boolean isPointValid(int x, int y) {
        return (x >= 0 && x < width && y >= 0 && y < height);
    }

    boolean isPointValid(Point p) {
        return isPointValid(p.x, p.y);
    }

    public void drawPoint(Point origin) {
        drawPoint(origin, Color.MAGENTA);
    }

    public void drawPoint(int x, int y) {
        drawPoint(new Point(x, y), Color.CYAN);
    }

    public void drawLine(Line line) {
        drawPoint(line.start, Color.YELLOW);
        drawPoint(line.end, Color.YELLOW);
    }


    public interface CameraFragmentListener {
        public void onCameraFailed();
    }

    @Override
    public void onCameraInitialized() {
        showCamera();
    }

    @Override
    public void onCameraInitializeFailed() {
        mListener.onCameraFailed();
    }
}
