package com.meetme.cameracolors;

import android.graphics.Color;

/**
 * Created by bherbert on 12/6/15.
 */
public class Constants {
    static final int NUM_SQUARES_PER_SIDE = 12;
    static final int NUM_COLORS = 4;

    public static byte RED_BYTE = (byte) 0;
    public static byte GREEN_BYTE = (byte) 1;
    public static byte BLUE_BYTE = (byte) 2;
    public static byte CYAN_BYTE = (byte) 7;
    public static byte MAGENTA_BYTE = (byte) 4;
    public static byte YELLOW_BYTE = (byte) 5;
    public static byte BLACK_BYTE = (byte) 6;
    public static byte WHITE_BYTE = (byte) 3;

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
}
