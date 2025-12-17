package ru.kpfu.itis.protocol;

public class GameMessage {
    private final byte type;
    private final byte[] data;

    public GameMessage(byte type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public byte getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
