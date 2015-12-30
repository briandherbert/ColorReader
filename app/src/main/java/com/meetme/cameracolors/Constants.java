package com.meetme.cameracolors;

import android.graphics.Color;

/**
 * Created by bherbert on 12/6/15.
 */
public class Constants {
    // Camera
    static final int CAMERA_MIN_RESOLUTION_SIDE = 160;
    static final int CAMERA_MIN_RESOLUTION = CAMERA_MIN_RESOLUTION_SIDE * CAMERA_MIN_RESOLUTION_SIDE;
    static final int AUTOFOCUS_DELAY_MS = 4000;

    static final int MS_PER_MESSAGE = 500;

    static final int NUM_SQUARES_PER_SIDE = 12;
    static final int NUM_COLORS = 4;

    static final boolean IS_CHECKSUMMING = true;

    static final int COLORS_PER_CHAR = (NUM_COLORS == 4) ? 4 : 3;
    static final int CHARS_PER_CHUNK = ((NUM_SQUARES_PER_SIDE * NUM_SQUARES_PER_SIDE)) / COLORS_PER_CHAR;

    public static byte RED_BYTE = (byte) 0;
    public static byte GREEN_BYTE = (byte) 1;
    public static byte BLUE_BYTE = (byte) 2;
    public static byte WHITE_BYTE = (byte) 3;
    public static byte CYAN_BYTE = (byte) 4;    // #0FF
    public static byte MAGENTA_BYTE = (byte) 5; // #F0F
    public static byte YELLOW_BYTE = (byte) 6;  // #FF0
    public static byte BLACK_BYTE = (byte) 7;

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
        } else if (b == YELLOW_BYTE) {
            return "yellow";
        }

        return "black";
    }


    public static String getColorNameFromColor(int color) {
        switch (color) {
            case Color.RED:
                return "red";
            case Color.GREEN:
                return "green";
            case Color.BLUE:
                return "blue";
            case Color.CYAN:
                return "cyan";
            case Color.MAGENTA:
                return "magenta";
            case Color.YELLOW:
                return "yellow";
            case Color.BLACK:
                return "black";
            case Color.WHITE:
                return "white";
            default:
                return "unknown";
        }
    }

    public static int colorFromByte(byte b) {
        if (b == RED_BYTE) {
            return Color.RED;
        } else if (b == GREEN_BYTE) {
            return Color.GREEN;
        } else if (b == BLUE_BYTE) {
            return Color.BLUE;
        } else if (b == CYAN_BYTE) {
            return Color.CYAN;
        } else if (b == MAGENTA_BYTE) {
            return Color.MAGENTA;
        } else if (b == YELLOW_BYTE) {
            return Color.YELLOW;
        } else if (b == WHITE_BYTE) {
            return Color.WHITE;
        } else if (b == BLACK_BYTE) {
            return Color.BLACK;
        }

        return Color.BLACK;
    }

    static int idxFromColor(int color) {
        switch (color) {
            case Color.RED:
                return RED_BYTE;
            case Color.GREEN:
                return GREEN_BYTE;
            case Color.BLUE:
                return BLUE_BYTE;
            case Color.CYAN:
                return CYAN_BYTE;
            case Color.MAGENTA:
                return MAGENTA_BYTE;
            case Color.YELLOW:
                return YELLOW_BYTE;
            case Color.BLACK:
                return BLACK_BYTE;
            case Color.WHITE:
                return WHITE_BYTE;
            default:
                return -1;
        }
    }

    static int numColorsPerChar() {
        return (NUM_COLORS == 4) ? 4 : 3;
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
