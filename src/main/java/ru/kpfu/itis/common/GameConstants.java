package ru.kpfu.itis.common;

import java.awt.*;

public class GameConstants {

    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 600;

    public static final int GRID_SIZE = 20;
    public static final int CELL_SIZE = 32;

    public static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    public static final Color PRIMARY_COLOR = new Color(255, 215, 0);
    public static final Color SECONDARY_COLOR = new Color(100, 200, 255);

    public static final String[] CHARACTER_NAMES = {
            "⚔️ Красный воин",
            "🔮 Синий маг",
            "🏹 Зеленый плут"
    };

    public static final int[] PLAYER_HEALTH = {180, 120, 100};
    public static final int[] PLAYER_SPEED = {1, 1, 2};
    public static final boolean[] CAN_SEE_TRAPS = {false, true, false};
    public static final int[] DIAMOND_BONUS = {0, 0, 2};

    public static final int SERVER_PORT = 7777;
    public static final int MAX_PLAYERS = 3;
    public static final int GAME_TICK_MS = 100;

    public static final int MAX_LEVELS = 10;
    public static final int[] LEVEL_DIAMONDS = {5, 8, 12, 15, 18, 20, 22, 25, 28, 30};
    public static final int[] LEVEL_ENEMIES = {2, 3, 4, 5, 6, 7, 8, 9, 10, 12};
}