package ru.kpfu.itis.client;

import ru.kpfu.itis.protocol.*;
import ru.kpfu.itis.common.*;
import ru.kpfu.itis.protocol.ProtocolException;
import ru.kpfu.itis.server.GameWorld;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private volatile boolean connected = false;
    private int playerId = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Consumer<GameWorld.GameState> onGameStateUpdate;
    private Consumer<Message> onMessageReceived;
    private long lastMessageTime = System.currentTimeMillis();
    private final Object writeLock = new Object();
    private String username;
    private String characterType;
    private ScheduledExecutorService heartbeatExecutor;

    public boolean connect(String host, int port, String username, String characterType) {
        this.username = username;
        this.characterType = characterType;

        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(30000); // Увеличиваем таймаут до 30 секунд

            // Подключаемся с таймаутом
            socket.connect(new InetSocketAddress(host, port), 5000);

            out = socket.getOutputStream();
            in = socket.getInputStream();

            connected = true;
            lastMessageTime = System.currentTimeMillis();

            // Запускаем поток чтения с использованием протокола
            executor.execute(this::readLoop);

            // Отправляем сообщение подключения через протокол
            Thread.sleep(100); // Небольшая задержка для установки соединения
            sendConnectMessage(username, characterType);

            // Запускаем heartbeat
            startHeartbeat();

            System.out.println("✅ Успешно подключились к серверу " + host + ":" + port);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Ошибка подключения к " + host + ":" + port + ": " + e.getMessage());
            disconnect();
            return false;
        }
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (!connected) {
                heartbeatExecutor.shutdown();
                return;
            }

            // Отправляем heartbeat каждые 10 секунд независимо от активности
            try {
                GameMessage heartbeat = GameProtocol.createHeartbeatMessage();
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, heartbeat);
                }
                System.out.println("❤️  Отправлен heartbeat серверу");
            } catch (Exception e) {
                System.err.println("❌ Не удалось отправить heartbeat: " + e.getMessage());
                disconnect();
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS); // Каждые 10 секунд
    }

    private void sendConnectMessage(String username, String characterType) {
        try {
            GameMessage connectMsg = GameProtocol.createConnectMessage(username, characterType);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, connectMsg);
            }
            System.out.println("📤 Отправлено сообщение CONNECT с именем: " + username);
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки сообщения CONNECT: " + e.getMessage());
        }
    }

    private void readLoop() {
        System.out.println("📥 Начинаем чтение сообщений от сервера");

        try {
            while (connected && socket != null && !socket.isClosed() && socket.isConnected()) {
                try {
                    GameMessage message = GameProtocol.readMessage(in);
                    if (message == null) {
                        System.out.println("📭 Сервер закрыл соединение (конец потока)");
                        break;
                    }

                    lastMessageTime = System.currentTimeMillis();
                    handleProtocolMessage(message);

                } catch (ProtocolException e) {
                    System.err.println("❌ Ошибка протокола: " + e.getMessage());

                    // Пробуем восстановить соединение
                    if (e.getMessage().contains("Неверный заголовок")) {
                        System.err.println("⚠️  Попытка восстановить синхронизацию протокола...");
                        try {
                            if (in.available() > 0) {
                                in.skip(1);
                                continue;
                            }
                        } catch (IOException ex) {
                            // Игнорируем
                        }
                    }

                    // Если это не ошибка заголовка, разрываем соединение
                    break;
                } catch (SocketTimeoutException e) {
                    // Таймаут - это нормально, продолжаем ждать
                    System.out.println("⏱️  Таймаут при чтении, продолжаем ожидание...");
                    continue;
                } catch (EOFException e) {
                    System.out.println("📭 Конец потока данных (EOF)");
                    break;
                } catch (IOException e) {
                    if (e.getMessage() != null && (e.getMessage().contains("closed") ||
                            e.getMessage().contains("reset") || e.getMessage().contains("abort"))) {
                        System.err.println("❌ Соединение разорвано: " + e.getMessage());
                        break;
                    }
                    System.err.println("❌ Ошибка чтения: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Неожиданная ошибка в цикле чтения: " + e.getMessage());
        } finally {
            System.out.println("📤 Завершение цикла чтения");
            disconnect();
        }
    }

    private void handleProtocolMessage(GameMessage message) {
        try {
            byte type = message.getType();

            switch (type) {
                case GameProtocol.TYPE_CONNECT:
                    String[] connectData = GameProtocol.parseConnectMessage(message);
                    if (connectData.length >= 1) {
                        try {
                            int newPlayerId = Integer.parseInt(connectData[0]);
                            if (this.playerId == -1) {
                                this.playerId = newPlayerId;
                                System.out.println("✅ Присвоен ID игрока: " + playerId);
                                // Уведомляем GUI о подключении
                                if (onMessageReceived != null) {
                                    onMessageReceived.accept(new Message(
                                            Message.CHAT, 0, "✅ Подключен к серверу как игрок #" + playerId
                                    ));
                                }
                            } else if (this.playerId != newPlayerId) {
                                System.err.println("⚠️  Несоответствие playerId: было " +
                                        playerId + ", стало " + newPlayerId);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("❌ Неверный формат playerId: " + connectData[0]);
                        }
                    }
                    break;

                case GameProtocol.TYPE_GAME_STATE:
                    try {
                        Object stateObj = GameProtocol.parseGameStateMessage(message);
                        if (stateObj instanceof GameWorld.GameState) {
                            GameWorld.GameState state = (GameWorld.GameState) stateObj;
                            if (onGameStateUpdate != null) {
                                onGameStateUpdate.accept(state);
                            }
                        } else {
                            System.err.println("❌ Получен неверный тип GameState");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Ошибка десериализации GameState: " + e.getMessage());
                    }
                    break;

                case GameProtocol.TYPE_CHAT:
                    GameProtocol.MessageData chatData = GameProtocol.parseTextMessage(message);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.CHAT,
                                chatData.playerId,
                                chatData.text
                        ));
                    }
                    break;

                case GameProtocol.TYPE_ACTION:
                    GameProtocol.MessageData actionData = GameProtocol.parseTextMessage(message);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.ACTION,
                                actionData.playerId,
                                actionData.text
                        ));
                    }
                    break;

                case GameProtocol.TYPE_LEVEL_UPDATE:
                    GameProtocol.MessageData levelData = GameProtocol.parseTextMessage(message);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.LEVEL_UPDATE,
                                levelData.playerId,
                                levelData.text
                        ));
                    }
                    break;

                case GameProtocol.TYPE_PLAYER_LIST:
                    String playerList = new String(message.getData());
                    System.out.println("👥 Список игроков от сервера: " + playerList);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.CHAT, 0, playerList
                        ));
                    }
                    break;

                case GameProtocol.TYPE_HEARTBEAT:
                    // Просто подтверждаем получение heartbeat
                    System.out.println("❤️  Получен heartbeat от сервера");
                    break;

                case GameProtocol.TYPE_ERROR:
                    GameProtocol.ErrorData errorData = GameProtocol.parseErrorMessage(message);
                    System.err.println("❌ Ошибка от сервера [" + errorData.errorCode + "]: " + errorData.errorMessage);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.CHAT, 0, "❌ Ошибка сервера: " + errorData.errorMessage
                        ));
                    }
                    break;

                default:
                    System.err.println("⚠️  Неизвестный тип сообщения: " + type);
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки протокольного сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMove(Direction direction) {
        if (playerId == -1 || !connected) {
            System.err.println("⚠️  Не могу отправить MOVE: не подключен или playerId не установлен");
            return;
        }

        try {
            byte dirByte = GameProtocol.directionToByte(direction);
            GameMessage moveMsg = GameProtocol.createMoveMessage(playerId, dirByte);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, moveMsg);
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки MOVE: " + e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("разорвано") ||
                    e.getMessage().contains("closed") || e.getMessage().contains("null"))) {
                disconnect();
            }
        }
    }

    public void sendAction(String action) {
        if (playerId == -1 || !connected) {
            System.err.println("⚠️  Не могу отправить ACTION: не подключен или playerId не установлен");
            return;
        }

        try {
            GameMessage actionMsg = GameProtocol.createActionMessage(playerId, action);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, actionMsg);
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки ACTION: " + e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("разорвано") ||
                    e.getMessage().contains("closed") || e.getMessage().contains("null"))) {
                disconnect();
            }
        }
    }

    public void sendChat(String text) {
        if (playerId == -1 || !connected) {
            System.err.println("⚠️  Не могу отправить CHAT: не подключен или playerId не установлен");
            return;
        }

        try {
            GameMessage chatMsg = GameProtocol.createChatMessage(playerId, text);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, chatMsg);
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки CHAT: " + e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("разорвано") ||
                    e.getMessage().contains("closed") || e.getMessage().contains("null"))) {
                disconnect();
            }
        }
    }

    public void setOnGameStateUpdate(Consumer<GameWorld.GameState> callback) {
        this.onGameStateUpdate = callback;
    }

    public void setOnMessageReceived(Consumer<Message> callback) {
        this.onMessageReceived = callback;
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public synchronized void disconnect() {
        if (!connected) return;

        System.out.println("📤 Начинаем отключение от сервера...");
        connected = false;

        // Отправляем сообщение о дисконнекте, если можем
        if (playerId != -1 && out != null) {
            try {
                GameMessage disconnectMsg = new GameMessage(
                        GameProtocol.TYPE_DISCONNECT,
                        String.valueOf(playerId).getBytes()
                );
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, disconnectMsg);
                }
                System.out.println("📤 Отправлено сообщение DISCONNECT для playerId: " + playerId);
            } catch (Exception e) {
                // Игнорируем, т.к. мы уже отключаемся
            }
        }

        // Останавливаем heartbeat
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }

        // Останавливаем executor
        executor.shutdownNow();

        // Закрываем сокет
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Игнорируем
        }

        // Уведомляем GUI об отключении
        if (onMessageReceived != null) {
            onMessageReceived.accept(new Message(
                    Message.CHAT, 0, "❌ Отключен от сервера"
            ));
        }

        System.out.println("📤 Отключение завершено. PlayerId: " + playerId);
    }

    /**
     * Пытается переподключиться к серверу
     */
    public boolean reconnect(String host, int port) {
        if (connected) {
            disconnect();
        }

        try {
            Thread.sleep(1000); // Ждем секунду перед переподключением
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return connect(host, port, username, characterType);
    }
}