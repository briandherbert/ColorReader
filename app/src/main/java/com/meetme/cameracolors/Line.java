package com.meetme.cameracolors;

import android.graphics.Point;
import android.util.Log;

/**
 * Created by bherbert on 12/21/15.
 */
public class Line {
    String TAG = "Line";
    public Point start = new Point(0, 0);
    public Point end = new Point(0, 0);

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

    @Override
    public String toString() {
        return "Line{" +
                "TAG='" + TAG + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", length=" + length +
                ", slope=" + slope +
                '}';
    }
}