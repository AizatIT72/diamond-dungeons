package ru.kpfu.itis.protocol;

import ru.kpfu.itis.common.Direction;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class GameProtocol {
    // Стартовые байты для идентификации протокола
    public static final byte[] PROTOCOL_HEADER = {0x44, 0x44}; // "DD" для Diamond Dungeons
    public static final int HEADER_SIZE = 2;
    public static final int TYPE_SIZE = 1; // 1 байт для типа сообщения
    public static final int LENGTH_SIZE = 4; // 4 байта для длины данных (int)
    public static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10MB максимум

    // Типы сообщений (минимум 4, как требуется)
    public static final byte TYPE_CONNECT = 0x01;        // Подключение клиента
    public static final byte TYPE_GAME_STATE = 0x02;     // Обновление состояния игры
    public static final byte TYPE_PLAYER_MOVE = 0x03;    // Движение игрока
    public static final byte TYPE_CHAT = 0x04;           // Сообщение чата
    public static final byte TYPE_ACTION = 0x05;         // Действие игрока
    public static final byte TYPE_DISCONNECT = 0x06;     // Отключение игрока
    public static final byte TYPE_LEVEL_UPDATE = 0x07;   // Обновление уровня
    public static final byte TYPE_PLAYER_LIST = 0x08;    // Список игроков
    public static final byte TYPE_HEARTBEAT = 0x09;      // Проверка соединения
    public static final byte TYPE_ERROR = 0x0A;          // Ошибка

    // Коды направления для движения
    public static final byte DIRECTION_UP = 0x01;
    public static final byte DIRECTION_DOWN = 0x02;
    public static final byte DIRECTION_LEFT = 0x03;
    public static final byte DIRECTION_RIGHT = 0x04;

    // Коды ошибок
    public static final byte ERROR_INVALID_MESSAGE = 0x01;
    public static final byte ERROR_SERVER_FULL = 0x02;
    public static final byte ERROR_INVALID_PLAYER_ID = 0x03;

    /**
     * Читает сообщение из InputStream с гарантированным чтением всех байтов
     */
    public static GameMessage readMessage(InputStream inputStream) throws IOException, ProtocolException {
        if (inputStream == null) {
            throw new ProtocolException("InputStream равен null");
        }

        try {
            // Читаем заголовок
            byte[] header = readFully(inputStream, HEADER_SIZE);
            if (header == null) {
                return null; // Конец потока
            }

            // Проверяем заголовок протокола
            if (!Arrays.equals(header, PROTOCOL_HEADER)) {
                // Пробуем пропустить 1 байт и попробовать снова (возможно смещение)
                int available = inputStream.available();
                if (available > 0) {
                    inputStream.skip(1);
                    // Попробуем прочитать снова
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

            // Читаем тип сообщения
            byte[] typeBytes = readFully(inputStream, TYPE_SIZE);
            if (typeBytes == null) {
                throw new ProtocolException("Не удалось прочитать тип сообщения");
            }
            byte type = typeBytes[0];

            // Читаем длину данных
            byte[] lengthBytes = readFully(inputStream, LENGTH_SIZE);
            if (lengthBytes == null) {
                throw new ProtocolException("Не удалось прочитать длину сообщения");
            }

            int dataLength = ByteBuffer.wrap(lengthBytes).getInt();

            // Проверяем размер данных
            if (dataLength < 0 || dataLength > MAX_MESSAGE_SIZE) {
                throw new ProtocolException("Недопустимая длина сообщения: " + dataLength);
            }

            // Читаем данные (может быть 0 байт)
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

    /**
     * Гарантированно читает указанное количество байтов
     */
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
                    return null; // Конец потока
                } else {
                    throw new IOException("Неожиданный конец потока");
                }
            }
            totalRead += read;

            // Небольшая задержка, если данные не пришли сразу
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

    /**
     * Записывает сообщение в OutputStream
     */
    public static void writeMessage(OutputStream outputStream, GameMessage message)
            throws IOException, ProtocolException {
        if (outputStream == null) {
            throw new ProtocolException("OutputStream равен null");
        }

        try {
            byte[] data = message.getData();
            int dataLength = data != null ? data.length : 0;

            // Проверяем размер данных
            if (dataLength > MAX_MESSAGE_SIZE) {
                throw new ProtocolException("Сообщение слишком большое");
            }

            // Создаем буфер для всего сообщения
            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + TYPE_SIZE + LENGTH_SIZE + dataLength);

            // Заголовок
            buffer.put(PROTOCOL_HEADER);

            // Тип
            buffer.put(message.getType());

            // Длина данных
            buffer.putInt(dataLength);

            // Данные
            if (dataLength > 0) {
                buffer.put(data);
            }

            // Записываем все сразу
            byte[] fullMessage = buffer.array();
            outputStream.write(fullMessage);
            outputStream.flush();

        } catch (IOException e) {
            throw new ProtocolException("Ошибка записи сообщения: " + e.getMessage(), e);
        }
    }

    /**
     * Создает сообщение CONNECT
     */
    public static GameMessage createConnectMessage(String username, String characterType) {
        String data = username + "|" + characterType;
        return new GameMessage(TYPE_CONNECT, data.getBytes());
    }

    /**
     * Создает сообщение PLAYER_MOVE
     */
    public static GameMessage createMoveMessage(int playerId, byte direction) {
        ByteBuffer buffer = ByteBuffer.allocate(5); // playerId(4) + direction(1)
        buffer.putInt(playerId);
        buffer.put(direction);
        return new GameMessage(TYPE_PLAYER_MOVE, buffer.array());
    }

    /**
     * Создает сообщение CHAT
     */
    public static GameMessage createChatMessage(int playerId, String text) {
        String data = playerId + "|" + text;
        return new GameMessage(TYPE_CHAT, data.getBytes());
    }

    /**
     * Создает сообщение ACTION
     */
    public static GameMessage createActionMessage(int playerId, String action) {
        String data = playerId + "|" + action;
        return new GameMessage(TYPE_ACTION, data.getBytes());
    }

    /**
     * Создает сообщение LEVEL_UPDATE
     */
    public static GameMessage createLevelUpdateMessage(int playerId, String text) {
        String data = playerId + "|" + text;
        return new GameMessage(TYPE_LEVEL_UPDATE, data.getBytes());
    }

    /**
     * Создает сообщение GAME_STATE
     */
    public static GameMessage createGameStateMessage(Object gameState) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(gameState);
        oos.flush();
        return new GameMessage(TYPE_GAME_STATE, baos.toByteArray());
    }

    /**
     * Создает сообщение со списком игроков
     */
    public static GameMessage createPlayerListMessage(String playerListInfo) {
        return new GameMessage(TYPE_PLAYER_LIST, playerListInfo.getBytes());
    }

    /**
     * Создает heartbeat сообщение
     */
    public static GameMessage createHeartbeatMessage() {
        return new GameMessage(TYPE_HEARTBEAT, new byte[0]);
    }

    /**
     * Создает сообщение об ошибке
     */
    public static GameMessage createErrorMessage(byte errorCode, String errorMessage) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + errorMessage.getBytes().length);
        buffer.put(errorCode);
        buffer.put(errorMessage.getBytes());
        return new GameMessage(TYPE_ERROR, buffer.array());
    }

    /**
     * Разбирает сообщение CONNECT
     */
    public static String[] parseConnectMessage(GameMessage message) {
        String data = new String(message.getData());
        return data.split("\\|", 2);
    }

    /**
     * Разбирает сообщение PLAYER_MOVE
     */
    public static MoveData parseMoveMessage(GameMessage message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getData());
        int playerId = buffer.getInt();
        byte direction = buffer.get();
        return new MoveData(playerId, direction);
    }

    /**
     * Разбирает сообщение CHAT, ACTION или LEVEL_UPDATE
     */
    public static MessageData parseTextMessage(GameMessage message) {
        String data = new String(message.getData());
        String[] parts = data.split("\\|", 2);
        int playerId = Integer.parseInt(parts[0]);
        String text = parts.length > 1 ? parts[1] : "";
        return new MessageData(playerId, text);
    }

    /**
     * Разбирает сообщение GAME_STATE
     */
    public static Object parseGameStateMessage(GameMessage message) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(message.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    /**
     * Разбирает сообщение об ошибке
     */
    public static ErrorData parseErrorMessage(GameMessage message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getData());
        byte errorCode = buffer.get();
        byte[] errorBytes = new byte[buffer.remaining()];
        buffer.get(errorBytes);
        String errorMessage = new String(errorBytes);
        return new ErrorData(errorCode, errorMessage);
    }

    /**
     * Преобразует Direction в byte
     */
    public static byte directionToByte(Direction direction) {
        switch (direction) {
            case UP: return DIRECTION_UP;
            case DOWN: return DIRECTION_DOWN;
            case LEFT: return DIRECTION_LEFT;
            case RIGHT: return DIRECTION_RIGHT;
            default: return DIRECTION_UP;
        }
    }

    /**
     * Преобразует byte в Direction
     */
    public static Direction byteToDirection(byte dirByte) {
        switch (dirByte) {
            case DIRECTION_UP: return Direction.UP;
            case DIRECTION_DOWN: return Direction.DOWN;
            case DIRECTION_LEFT: return Direction.LEFT;
            case DIRECTION_RIGHT: return Direction.RIGHT;
            default: return Direction.UP;
        }
    }

    // Вспомогательные классы для данных
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