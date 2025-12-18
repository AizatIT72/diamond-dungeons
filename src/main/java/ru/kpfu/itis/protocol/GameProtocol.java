package ru.kpfu.itis.protocol;

import ru.kpfu.itis.common.Direction;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class GameProtocol {

    public static final byte[] PROTOCOL_HEADER = {0x44, 0x44}; 
    public static final int HEADER_SIZE = 2;
    public static final int TYPE_SIZE = 1; 
    public static final int LENGTH_SIZE = 4; 
    public static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; 

    public static final byte TYPE_CONNECT = 0x01;        
    public static final byte TYPE_GAME_STATE = 0x02;     
    public static final byte TYPE_PLAYER_MOVE = 0x03;    
    public static final byte TYPE_CHAT = 0x04;           
    public static final byte TYPE_ACTION = 0x05;         
    public static final byte TYPE_DISCONNECT = 0x06;     
    public static final byte TYPE_LEVEL_UPDATE = 0x07;   
    public static final byte TYPE_PLAYER_LIST = 0x08;    
    public static final byte TYPE_HEARTBEAT = 0x09;      
    public static final byte TYPE_ERROR = 0x0A;          

    public static final byte DIRECTION_UP = 0x01;
    public static final byte DIRECTION_DOWN = 0x02;
    public static final byte DIRECTION_LEFT = 0x03;
    public static final byte DIRECTION_RIGHT = 0x04;

    public static final byte ERROR_INVALID_MESSAGE = 0x01;
    public static final byte ERROR_SERVER_FULL = 0x02;
    public static final byte ERROR_INVALID_PLAYER_ID = 0x03;

    public static GameMessage readMessage(InputStream inputStream) throws IOException, ProtocolException {
        if (inputStream == null) {
            throw new ProtocolException("InputStream равен null");
        }

        try {

            byte[] header = readFully(inputStream, HEADER_SIZE);
            if (header == null) {
                return null; 
            }

            if (!Arrays.equals(header, PROTOCOL_HEADER)) {

                int available = inputStream.available();
                if (available > 0) {
                    inputStream.skip(1);

                    byte[] retryHeader = readFully(inputStream, HEADER_SIZE);
                    if (retryHeader != null && Arrays.equals(retryHeader, PROTOCOL_HEADER)) {
                        header = retryHeader;
                    } else {
                        throw new ProtocolException("Неверный заголовок протокола");
                    }
                } else {
                    throw new ProtocolException("Неверный заголовок протокола");
                }
            }

            byte[] typeBytes = readFully(inputStream, TYPE_SIZE);
            if (typeBytes == null) {
                throw new ProtocolException("Не удалось прочитать тип сообщения");
            }
            byte type = typeBytes[0];

            byte[] lengthBytes = readFully(inputStream, LENGTH_SIZE);
            if (lengthBytes == null) {
                throw new ProtocolException("Не удалось прочитать длину сообщения");
            }

            int dataLength = ByteBuffer.wrap(lengthBytes).getInt();

            if (dataLength < 0 || dataLength > MAX_MESSAGE_SIZE) {
                throw new ProtocolException("Недопустимая длина сообщения: " + dataLength);
            }

            byte[] data;
            if (dataLength == 0) {
                data = new byte[0];
            } else {
                data = readFully(inputStream, dataLength);
                if (data == null) {
                    throw new ProtocolException("Не удалось прочитать данные сообщения");
                }
            }

            return new GameMessage(type, data);

        } catch (IOException e) {
            throw new ProtocolException("Ошибка чтения сообщения: " + e.getMessage(), e);
        }
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        if (length <= 0) {
            return new byte[0];
        }

        byte[] buffer = new byte[length];
        int totalRead = 0;
        int read;

        while (totalRead < length) {
            read = in.read(buffer, totalRead, length - totalRead);
            if (read == -1) {
                if (totalRead == 0) {
                    return null; 
                } else {
                    throw new IOException("Неожиданный конец потока");
                }
            }
            totalRead += read;

            if (totalRead < length && in.available() == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Поток прерван во время чтения", e);
                }
            }
        }

        return buffer;
    }

    public static void writeMessage(OutputStream outputStream, GameMessage message)
            throws IOException, ProtocolException {
        if (outputStream == null) {
            throw new ProtocolException("OutputStream равен null");
        }

        try {
            byte[] data = message.getData();
            int dataLength = data != null ? data.length : 0;

            if (dataLength > MAX_MESSAGE_SIZE) {
                throw new ProtocolException("Сообщение слишком большое");
            }

            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + TYPE_SIZE + LENGTH_SIZE + dataLength);

            buffer.put(PROTOCOL_HEADER);

            buffer.put(message.getType());

            buffer.putInt(dataLength);

            if (dataLength > 0) {
                buffer.put(data);
            }

            byte[] fullMessage = buffer.array();
            outputStream.write(fullMessage);
            outputStream.flush();

        } catch (IOException e) {
            throw new ProtocolException("Ошибка записи сообщения: " + e.getMessage(), e);
        }
    }

    public static GameMessage createConnectMessage(String username, String characterType) {
        String data = username + "|" + characterType;
        return new GameMessage(TYPE_CONNECT, data.getBytes());
    }

    public static GameMessage createMoveMessage(int playerId, byte direction) {
        ByteBuffer buffer = ByteBuffer.allocate(5); 
        buffer.putInt(playerId);
        buffer.put(direction);
        return new GameMessage(TYPE_PLAYER_MOVE, buffer.array());
    }

    public static GameMessage createChatMessage(int playerId, String text) {
        String data = playerId + "|" + text;
        return new GameMessage(TYPE_CHAT, data.getBytes());
    }

    public static GameMessage createActionMessage(int playerId, String action) {
        String data = playerId + "|" + action;
        return new GameMessage(TYPE_ACTION, data.getBytes());
    }

    public static GameMessage createLevelUpdateMessage(int playerId, String text) {
        String data = playerId + "|" + text;
        return new GameMessage(TYPE_LEVEL_UPDATE, data.getBytes());
    }

    public static GameMessage createGameStateMessage(Object gameState) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(gameState);
        oos.flush();
        return new GameMessage(TYPE_GAME_STATE, baos.toByteArray());
    }

    public static GameMessage createPlayerListMessage(String playerListInfo) {
        return new GameMessage(TYPE_PLAYER_LIST, playerListInfo.getBytes());
    }

    public static GameMessage createHeartbeatMessage() {
        return new GameMessage(TYPE_HEARTBEAT, new byte[0]);
    }

    public static GameMessage createErrorMessage(byte errorCode, String errorMessage) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + errorMessage.getBytes().length);
        buffer.put(errorCode);
        buffer.put(errorMessage.getBytes());
        return new GameMessage(TYPE_ERROR, buffer.array());
    }

    public static String[] parseConnectMessage(GameMessage message) {
        String data = new String(message.getData());
        return data.split("\\|", 2);
    }

    public static MoveData parseMoveMessage(GameMessage message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getData());
        int playerId = buffer.getInt();
        byte direction = buffer.get();
        return new MoveData(playerId, direction);
    }

    public static MessageData parseTextMessage(GameMessage message) {
        String data = new String(message.getData());
        String[] parts = data.split("\\|", 2);
        int playerId = Integer.parseInt(parts[0]);
        String text = parts.length > 1 ? parts[1] : "";
        return new MessageData(playerId, text);
    }

    public static Object parseGameStateMessage(GameMessage message) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(message.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    public static ErrorData parseErrorMessage(GameMessage message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getData());
        byte errorCode = buffer.get();
        byte[] errorBytes = new byte[buffer.remaining()];
        buffer.get(errorBytes);
        String errorMessage = new String(errorBytes);
        return new ErrorData(errorCode, errorMessage);
    }

    public static byte directionToByte(Direction direction) {
        switch (direction) {
            case UP: return DIRECTION_UP;
            case DOWN: return DIRECTION_DOWN;
            case LEFT: return DIRECTION_LEFT;
            case RIGHT: return DIRECTION_RIGHT;
            default: return DIRECTION_UP;
        }
    }

    public static Direction byteToDirection(byte dirByte) {
        switch (dirByte) {
            case DIRECTION_UP: return Direction.UP;
            case DIRECTION_DOWN: return Direction.DOWN;
            case DIRECTION_LEFT: return Direction.LEFT;
            case DIRECTION_RIGHT: return Direction.RIGHT;
            default: return Direction.UP;
        }
    }

    public static class MoveData {
        public final int playerId;
        public final byte direction;

        public MoveData(int playerId, byte direction) {
            this.playerId = playerId;
            this.direction = direction;
        }
    }

    public static class MessageData {
        public final int playerId;
        public final String text;

        public MessageData(int playerId, String text) {
            this.playerId = playerId;
            this.text = text;
        }
    }

    public static class ErrorData {
        public final byte errorCode;
        public final String errorMessage;

        public ErrorData(byte errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}