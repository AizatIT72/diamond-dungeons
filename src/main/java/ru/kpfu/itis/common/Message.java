package ru.kpfu.itis.common;

public class Message {
    private int type;
    private int playerId;
    private String data;
    private long timestamp;

    public Message(int type, int playerId, String data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(int type, String data) {
        this(type, 0, data);
    }

    // Конструктор из строки (для сетевой передачи)
    public Message(String messageString) {
        String[] parts = messageString.split("\\" + Protocol.DELIMITER, 3);
        if (parts.length >= 1) this.type = Integer.parseInt(parts[0]);
        if (parts.length >= 2) this.playerId = Integer.parseInt(parts[1]);
        if (parts.length >= 3) this.data = parts[2];
        this.timestamp = System.currentTimeMillis();
    }

    // Преобразование в строку для отправки
    public String toString() {
        return type + Protocol.DELIMITER + playerId + Protocol.DELIMITER +
                (data != null ? data : "");
    }

    // Геттеры и сеттеры
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public long getTimestamp() { return timestamp; }

    // Вспомогательные методы
    public boolean isType(int type) {
        return this.type == type;
    }
}
