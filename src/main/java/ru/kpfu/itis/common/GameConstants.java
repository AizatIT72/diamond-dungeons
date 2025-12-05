package ru.kpfu.itis.common;

import java.awt.*;

public class GameConstants {
    // Размеры окна
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 600;

    // Игровое поле
    public static final int GRID_SIZE = 20;
    public static final int CELL_SIZE = 32;

    // Цвета игроков
    public static final Color[] PLAYER_COLORS = {
            new Color(255, 50, 50),    // Красный воин
            new Color(50, 100, 255),   // Синий маг
            new Color(50, 200, 50)     // Зеленый плут
    };

    // Имена персонажей
    public static final String[] CHARACTER_NAMES = {
            "Красный воин", "Синий маг", "Зеленый плут"
    };

    // Характеристики персонажей
    public static final String[] CHARACTER_DESCRIPTIONS = {
            "Прочнее, выдерживает одну ловушку",
            "Видит скрытые ловушки на 2 клетки",
            "Быстрее собирает алмазы"
    };
}
