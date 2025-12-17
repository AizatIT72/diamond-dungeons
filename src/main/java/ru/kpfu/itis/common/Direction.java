package ru.kpfu.itis.common;

import java.io.Serializable;

public enum Direction implements Serializable {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Direction fromString(String s) {
        try {
            return Direction.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UP;
        }
    }
}
