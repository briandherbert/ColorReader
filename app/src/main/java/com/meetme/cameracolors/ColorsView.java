package com.meetme.cameracolors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class ColorsView extends View {
    static final String TAG = ColorsView.class.getSimpleName();

    static final int NUM_SQUARES_PER_SIDE = 12;

    int width = 0;
    int height = 0;

    int squareSize;
    int halfSquare;
    int gridSize;

    String message = "";

    byte[] colorBytes;

    static byte RED_BYTE = (byte) 0;
    static byte GREEN_BYTE = (byte) 1;
    static byte BLUE_BYTE = (byte) 2;
    static byte WHITE_BYTE = (byte) 3;

    static final int NUM_COLORS = 4;

    Paint paint = new Paint();
    Paint redPaint = new Paint();
    Paint greenPaint = new Paint();
    Paint bluePaint = new Paint();
    Paint whitePaint = new Paint();


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
        colorBytes = new byte[NUM_SQUARES_PER_SIDE * NUM_SQUARES_PER_SIDE];

        for (int i = 0; i < colorBytes.length; i++) {
            colorBytes[i] = WHITE_BYTE;
        }

        redPaint.setColor(Color.RED);
        greenPaint.setColor(Color.GREEN);
        bluePaint.setColor(Color.BLUE);
    }

    public void setMessage(String msg) {
        message = msg;

        convertStringToColorBytes(message);

        Log.v(TAG, "message " + message + "\ncolorBytes size " + colorBytes.length);
    }

    public void convertStringToColorBytes(String str) {
        byte[] bytes = stringToBytesASCII(str);

        colorBytes = new byte[bytes.length * 4];

        byte b;

        for (int i = 0; i < bytes.length; i++) {
            b = bytes[i];
            for (int j = 0; j < NUM_COLORS; j++) {
                colorBytes[(i * NUM_COLORS) + j] = (byte)((b >> (j * 2)) & 0b11);
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

        for (int y = 0; y < NUM_SQUARES_PER_SIDE; y++) {
            for (int x = 0; x < NUM_SQUARES_PER_SIDE; x++) {
                paint.setColor(
                        CameraFragment.colorFromByte(
                                colorBytes[Math.min(y * NUM_SQUARES_PER_SIDE + x, colorBytes.length - 1)]));

                canvas.drawRect(halfSquare + x * squareSize,
                        halfSquare + y * squareSize,
                        halfSquare + x * squareSize + squareSize,
                        halfSquare + y * squareSize + squareSize,
                        paint);
            }
        }
    }
}
