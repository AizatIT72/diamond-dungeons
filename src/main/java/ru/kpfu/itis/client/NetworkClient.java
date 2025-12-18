package ru.kpfu.itis.client;

import ru.kpfu.itis.protocol.*;
import ru.kpfu.itis.common.*;
import ru.kpfu.itis.protocol.ProtocolException;
import ru.kpfu.itis.server.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);
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
            socket.setSoTimeout(30000); 

            socket.connect(new InetSocketAddress(host, port), 5000);

            out = socket.getOutputStream();
            in = socket.getInputStream();

            connected = true;
            lastMessageTime = System.currentTimeMillis();

            executor.execute(this::readLoop);

            Thread.sleep(100); 
            sendConnectMessage(username, characterType);

            startHeartbeat();

            logger.info("Успешно подключились к серверу {}:{}", host, port);
            return true;

        } catch (Exception e) {
            logger.error("Ошибка подключения к {}:{}", host, port, e);
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

            try {
                GameMessage heartbeat = GameProtocol.createHeartbeatMessage();
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, heartbeat);
                }
                logger.debug("Отправлен heartbeat серверу");
            } catch (Exception e) {
                logger.error("Не удалось отправить heartbeat", e);
                disconnect();
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS); 
    }

    private void sendConnectMessage(String username, String characterType) {
        try {
            GameMessage connectMsg = GameProtocol.createConnectMessage(username, characterType);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, connectMsg);
            }
            logger.debug("Отправлено сообщение CONNECT с именем: {}", username);
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения CONNECT", e);
        }
    }

    private void readLoop() {
        logger.info("Начинаем чтение сообщений от сервера");
        try {
            while (connected && socket != null && !socket.isClosed() && socket.isConnected()) {
                try {
                    GameMessage message = GameProtocol.readMessage(in);
                    if (message == null) {
                        logger.info("Сервер закрыл соединение (конец потока)");
                        break;
                    }

                    lastMessageTime = System.currentTimeMillis();
                    handleProtocolMessage(message);

                } catch (ProtocolException e) {
                    logger.error("Ошибка протокола: {}", e.getMessage());
                    if (e.getMessage().contains("Неверный заголовок")) {
                        logger.warn("Попытка восстановить синхронизацию протокола...");
                        try {
                            if (in.available() > 0) {
                                in.skip(1);
                                continue;
                            }
                        } catch (IOException ex) {
                            logger.debug("Ошибка при пропуске байта", ex);
                        }
                    }
                    break;
                } catch (SocketTimeoutException e) {
                    logger.debug("Таймаут при чтении, продолжаем ожидание...");
                    continue;
                } catch (EOFException e) {
                    logger.info("Конец потока данных (EOF)");
                    break;
                } catch (IOException e) {
                    if (e.getMessage() != null && (e.getMessage().contains("closed") ||
                            e.getMessage().contains("reset") || e.getMessage().contains("abort"))) {
                        logger.warn("Соединение разорвано: {}", e.getMessage());
                        break;
                    }
                    logger.error("Ошибка чтения", e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Неожиданная ошибка в цикле чтения", e);
        } finally {
            logger.debug("Завершение цикла чтения");
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
                                logger.info("Присвоен ID игрока: {}", playerId);
                                if (onMessageReceived != null) {
                                    onMessageReceived.accept(new Message(
                                            Message.CHAT, 0, "✅ Подключен к серверу как игрок #" + playerId
                                    ));
                                }
                            } else if (this.playerId != newPlayerId) {
                                logger.warn("Несоответствие playerId: было {}, стало {}", playerId, newPlayerId);
                            }
                        } catch (NumberFormatException e) {
                            logger.error("Неверный формат playerId: {}", connectData[0], e);
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
                            logger.warn("Получен неверный тип GameState");
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка десериализации GameState", e);
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
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.CHAT, 0, playerList
                        ));
                    }
                    break;

                case GameProtocol.TYPE_HEARTBEAT:
                    logger.debug("Получен heartbeat от сервера");
                    break;

                case GameProtocol.TYPE_ERROR:
                    GameProtocol.ErrorData errorData = GameProtocol.parseErrorMessage(message);
                    logger.error("Ошибка от сервера [{}]: {}", errorData.errorCode, errorData.errorMessage);
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                Message.CHAT, 0, "❌ Ошибка сервера: " + errorData.errorMessage
                        ));
                    }
                    break;

                default:
                    logger.warn("Неизвестный тип сообщения: {}", type);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки протокольного сообщения", e);
        }
    }

    public void sendMove(Direction direction) {
        if (playerId == -1 || !connected) {
            logger.warn("Не могу отправить MOVE: не подключен или playerId не установлен");
            return;
        }

        try {
            byte dirByte = GameProtocol.directionToByte(direction);
            GameMessage moveMsg = GameProtocol.createMoveMessage(playerId, dirByte);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, moveMsg);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки MOVE", e);
            if (e.getMessage() != null && (e.getMessage().contains("разорвано") ||
                    e.getMessage().contains("closed") || e.getMessage().contains("null"))) {
                disconnect();
            }
        }
    }

    public void sendAction(String action) {
        if (playerId == -1 || !connected) {
            logger.warn("Не могу отправить ACTION: не подключен или playerId не установлен");
            return;
        }

        try {
            GameMessage actionMsg = GameProtocol.createActionMessage(playerId, action);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, actionMsg);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки ACTION", e);
            if (e.getMessage() != null && (e.getMessage().contains("разорвано") ||
                    e.getMessage().contains("closed") || e.getMessage().contains("null"))) {
                disconnect();
            }
        }
    }

    public void sendChat(String text) {
        if (playerId == -1 || !connected) {
            logger.warn("Не могу отправить CHAT: не подключен или playerId не установлен");
            return;
        }

        try {
            GameMessage chatMsg = GameProtocol.createChatMessage(playerId, text);
            synchronized (writeLock) {
                GameProtocol.writeMessage(out, chatMsg);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки CHAT", e);
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

        logger.info("Начинаем отключение от сервера...");
        connected = false;

        if (playerId != -1 && out != null) {
            try {
                GameMessage disconnectMsg = new GameMessage(
                        GameProtocol.TYPE_DISCONNECT,
                        String.valueOf(playerId).getBytes()
                );
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, disconnectMsg);
                }
                logger.debug("Отправлено сообщение DISCONNECT для playerId: {}", playerId);
            } catch (Exception e) {
                logger.debug("Ошибка при отправке DISCONNECT", e);
            }
        }

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }

        executor.shutdownNow();

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка при закрытии сокета", e);
        }

        if (onMessageReceived != null) {
            onMessageReceived.accept(new Message(
                    Message.CHAT, 0, "❌ Отключен от сервера"
            ));
        }

        logger.info("Отключение завершено. PlayerId: {}", playerId);
    }

    public boolean reconnect(String host, int port) {
        if (connected) {
            disconnect();
        }

        try {
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return connect(host, port, username, characterType);
    }
}