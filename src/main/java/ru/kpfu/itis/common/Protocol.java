package ru.kpfu.itis.common;

public class Protocol {
    public static final int CONNECT = 1;           // Подключение к серверу
    public static final int DISCONNECT = 2;        // Отключение
    public static final int PLAYER_MOVE = 3;       // Движение игрока
    public static final int GAME_STATE = 4;        // Состояние игры
    public static final int PLAYER_ACTION = 5;     // Действие (сбор алмаза и т.д.)
    public static final int CHAT_MESSAGE = 6;      // Сообщение в чат

    // Направления движения
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    // Действия игрока
    public static final int ACTION_COLLECT = 1;    // Собрать алмаз
    public static final int ACTION_USE = 2;        // Использовать кнопку
    public static final int ACTION_DOOR = 3;       // Взаимодействие с дверью

    // Разделитель для текстовых сообщений
    public static final String DELIMITER = "|";

    // Максимальное количество игроков
    public static final int MAX_PLAYERS = 3;

    // Размер игрового поля
    public static final int FIELD_WIDTH = 20;
    public static final int FIELD_HEIGHT = 20;

    // Размер клетки в пикселях
    public static final int CELL_SIZE = 30;
}
