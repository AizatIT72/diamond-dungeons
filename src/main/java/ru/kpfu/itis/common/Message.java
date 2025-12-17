package ru.kpfu.itis.common;

import java.io.Serializable;

public class Message implements Serializable {
    // ДОБАВИТЬ ЭТУ СТРОЧКУ
    private static final long serialVersionUID = 1L;

    // Типы сообщений
    public static final int CONNECT = 1;
    public static final int DISCONNECT = 2;
    public static final int MOVE = 3;
    public static final int ACTION = 4;
    public static final int GAME_STATE = 5;
    public static final int CHAT = 6;
    public static final int PLAYER_UPDATE = 7;
    public static final int ENEMY_UPDATE = 8;
    public static final int LEVEL_UPDATE = 9;
    public static final int PLAYER_DAMAGE = 10;

    private int type;
    private int playerId;
    private Object data;
    private long timestamp;

    public Message(int type, int playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(int type, Object data) {
        this(type, 0, data);
    }

    // Геттеры
    public int getType() { return type; }
    public int getPlayerId() { return playerId; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }

    public boolean isType(int type) {
        return this.type == type;
    }
}