package com.meetme.cameracolors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static com.meetme.cameracolors.Constants.*;

/**
 * TODO: document your custom view class.
 */
public class ColorsView extends View {
    static final String TAG = ColorsView.class.getSimpleName();

    int width = 0;
    int height = 0;

    int squareSize;
    int halfSquare;
    int gridSize;

    String message = "";

    // [chunkIdx][bytes]
    byte[][] colorBytes;

    long mDrawStartTime = 0;
    long mLastChunkUpdateTime = 0;
    int mCurrentChunkIdx = 0;

    Paint paint = new Paint();
    Paint redPaint = new Paint();
    Paint greenPaint = new Paint();
    Paint bluePaint = new Paint();
    Paint whitePaint = new Paint();

    int[] counts = {0, 0, 0, 0};

    public ColorsView(Context context) {
        super(context);
        init();
    }

    public ColorsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        colorBytes = new byte[1][CHARS_PER_CHUNK * COLORS_PER_CHAR];

        for (int i = 0; i < colorBytes[0].length; i++) {
            colorBytes[0][i] = BLACK_BYTE;
        }

        redPaint.setColor(Color.RED);
        greenPaint.setColor(Color.GREEN);
        bluePaint.setColor(Color.BLUE);
    }

    public void setMessage(String msg) {
        mCurrentChunkIdx = 0;

        message = msg;

        convertStringToColorBytes(message);

        Log.v(TAG, "message " + message + "\ncolorBytes size " + colorBytes.length);
    }

    public void convertStringToColorBytes(String str) {
        byte[] bytes = stringToBytesASCII(str);

        int adjustedCharsPerChunk = CHARS_PER_CHUNK - (IS_CHECKSUMMING ? 1 : 0);
        int numChunks = (int) Math.ceil(bytes.length / (double) adjustedCharsPerChunk);

        Log.v(TAG, "num chunks : " + numChunks +  " byte length " + bytes.length + " chars per chunk " + CHARS_PER_CHUNK);

        colorBytes = new byte[numChunks][CHARS_PER_CHUNK * COLORS_PER_CHAR];

        for (int c = 0; c < colorBytes.length; c++) {
            for (int i = 0; i < colorBytes[c].length; i ++) {
                colorBytes[c][i] = BLACK_BYTE;
            }
        }

        byte b;

        byte colorByte;
        int idx;
        int chunkIdx = 0;

        for (int i = 0; i < bytes.length; i++) {
            b = bytes[i];
            chunkIdx = i / adjustedCharsPerChunk;
            for (int j = 0; j < COLORS_PER_CHAR; j++) {
                idx = ((i % adjustedCharsPerChunk) * COLORS_PER_CHAR) + j;
                colorByte = (byte) ((b >> (j * 2)) & 0b11);
                // TODO: This will only work for 4 colors!!
                //Log.v(TAG, "Assigning byte at " + idx + " to " + colorByte);

                counts[colorByte]++;
                colorBytes[chunkIdx][idx] = colorByte;
            }
        }

        invalidate();
    }

    public static byte[] stringToBytesASCII(String str) {
        char[] buffer = str.toCharArray();
        byte[] b = new byte[buffer.length];

        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) buffer[i];
        }

        return b;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        width = xNew;
        height = yNew;

        if (width > 0) {
            int shortSide = Math.min(width, height);

            squareSize = shortSide / (NUM_SQUARES_PER_SIDE + 1);
            halfSquare = squareSize / 2;
            gridSize = NUM_SQUARES_PER_SIDE * squareSize;
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(halfSquare, 0, halfSquare + gridSize, halfSquare, redPaint);
        canvas.drawRect(0, halfSquare, halfSquare, halfSquare + gridSize, greenPaint);

        canvas.drawRect(halfSquare, gridSize + halfSquare, halfSquare + gridSize, gridSize + squareSize, bluePaint);
        canvas.drawRect(gridSize + halfSquare, halfSquare, gridSize + squareSize, halfSquare + gridSize, bluePaint);

        int idx;
        byte colorByte;

        for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
            for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                idx = Math.min(y * NUM_SQUARES_PER_SIDE + x, colorBytes[mCurrentChunkIdx].length - 1);
                colorByte = colorBytes[mCurrentChunkIdx][idx];

                //Log.v(TAG, "Drawing byte at " + idx + " to " + colorByte);

                paint.setColor(colorFromByte(colorByte));

                canvas.drawRect(halfSquare + x * squareSize,
                        halfSquare + y * squareSize,
                        halfSquare + x * squareSize + squareSize,
                        halfSquare + y * squareSize + squareSize,
                        paint);
            }
        }

        if (mDrawStartTime == 0) {
            mDrawStartTime = System.currentTimeMillis();
        } else {
            elapsed = (int) (System.currentTimeMillis() - mDrawStartTime);
            newChunkIdx = (elapsed / MS_PER_MESSAGE) % colorBytes.length;

            if (newChunkIdx != mCurrentChunkIdx) {
                //Log.v(TAG, "drawing chunk " + mCurrentChunkIdx + " after " + elapsed);
                mCurrentChunkIdx = newChunkIdx;
            }
        }

        if (!message.isEmpty()) invalidate();

//        removeCallbacks(updateChunkRunnable);
//        postDelayed(updateChunkRunnable, MS_PER_MESSAGE / 2);
    }

    int elapsed = 0;
    int newChunkIdx = 0;

    Handler handler = new Handler();

    final Runnable updateChunkRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };
}
