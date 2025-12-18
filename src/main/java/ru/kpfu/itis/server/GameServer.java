package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.protocol.GameMessage;
import ru.kpfu.itis.protocol.GameProtocol;
import ru.kpfu.itis.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);
    private ServerSocket serverSocket;
    private final GameWorld gameWorld;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService gameLoop = Executors.newScheduledThreadPool(1);
    private volatile boolean running = false;
    private int nextPlayerId = 1;
    private int port;
    private ScheduledExecutorService maintenanceExecutor;

    public GameServer(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        serverSocket.setSoTimeout(1000);

        gameWorld = new GameWorld();
        gameWorld.setBroadcastCallback(this::broadcast);
        logger.info("Игровой мир инициализирован");
    }

    public void start() {
        running = true;

        startMaintenance();

        gameLoop.scheduleAtFixedRate(() -> {
            try {
                gameTick();
            } catch (Exception e) {
                logger.error("Ошибка игрового цикла", e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        threadPool.execute(() -> {
            logger.info("Сервер запущен на порту {}", port);
            logger.info("Ожидаем подключения игроков...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setSoTimeout(45000);

                    if (clients.size() >= 3) {
                        logger.warn("Достигнут лимит игроков (3). Отклоняем подключение.");
                        try {
                            OutputStream out = clientSocket.getOutputStream();
                            GameMessage errorMsg = GameProtocol.createErrorMessage(
                                    GameProtocol.ERROR_SERVER_FULL,
                                    "Сервер заполнен (максимум 3 игрока)"
                            );
                            GameProtocol.writeMessage(out, errorMsg);
                            out.flush();
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке сообщения об ошибке клиенту", e);
                        }
                        clientSocket.close();
                        continue;
                    }

                    int playerId = nextPlayerId++;
                    ClientHandler handler = new ClientHandler(clientSocket, playerId);
                    clients.put(playerId, handler);
                    threadPool.execute(handler);

                    logger.info("Игрок #{} подключился (всего: {}/3)", playerId, clients.size());
                    broadcastPlayerList();

                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    if (running) {
                        logger.error("Ошибка приема подключения", e);
                    }
                }
            }
            logger.info("Сервер прекратил прием подключений");
        });
    }

    private void startMaintenance() {
        maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                List<Integer> deadClients = new ArrayList<>();
                long currentTime = System.currentTimeMillis();

                for (ClientHandler client : clients.values()) {
                    if (currentTime - client.getLastActivityTime() > 60000) {
                        logger.warn("Клиент #{} неактивен более 60 секунд", client.playerId);
                        deadClients.add(client.playerId);
                    }
                }

                for (Integer playerId : deadClients) {
                    logger.info("Удаляем неактивного клиента #{}", playerId);
                    removeClient(playerId);
                }

                if (!clients.isEmpty()) {
                    GameMessage heartbeat = GameProtocol.createHeartbeatMessage();
                    for (ClientHandler client : clients.values()) {
                        try {
                            client.sendProtocolMessage(heartbeat);
                        } catch (Exception e) {
                            logger.debug("Ошибка отправки heartbeat клиенту #{}", client.playerId, e);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Ошибка в maintenance", e);
            }
        }, 20000, 20000, TimeUnit.MILLISECONDS);
    }

    private void broadcastPlayerList() {
        if (clients.isEmpty()) return;

        StringBuilder playerList = new StringBuilder("Игроки онлайн (");
        playerList.append(clients.size()).append("/3): ");

        for (ClientHandler client : clients.values()) {
            playerList.append("#").append(client.playerId);
            if (client.getPlayerName() != null) {
                playerList.append("(").append(client.getPlayerName()).append(")");
            }
            playerList.append(" ");
        }

        try {
            GameMessage playerListMsg = GameProtocol.createPlayerListMessage(playerList.toString());
            for (ClientHandler client : clients.values()) {
                client.sendProtocolMessage(playerListMsg);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки списка игроков", e);
        }
    }

    private void gameTick() {
        try {
            gameWorld.updateEnemies();
            GameWorld.GameState state = gameWorld.getGameState();

            broadcastGameState(state);

            if (state.levelComplete) {
                broadcast(new Message(Message.ACTION, 0, "Уровень " + state.currentLevel + " пройден!"));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                gameWorld.loadLevel(state.currentLevel + 1);
                broadcast(new Message(Message.ACTION, 0, "Загружен уровень " + (state.currentLevel + 1)));
            }
        } catch (Exception e) {
            logger.error("Ошибка в игровом цикле", e);
        }
    }

    public void registerPlayer(int playerId, String name, String characterType) {
        PlayerState player = gameWorld.addPlayer(playerId, name, characterType);
        broadcast(new Message(Message.ACTION, 0, "👤 " + name + " (" + characterType + ") присоединился к игре"));
        broadcastPlayerList();
    }

    public void handlePlayerMove(int playerId, Direction direction) {
        gameWorld.movePlayer(playerId, direction);
    }

    public void handlePlayerAction(int playerId, String action) {
        broadcast(new Message(Message.ACTION, playerId, action));
    }

    public void removeClient(int playerId) {
        ClientHandler handler = clients.remove(playerId);
        if (handler != null) {
            gameWorld.removePlayer(playerId);
            String playerName = handler.getPlayerName();
            String disconnectMsg = "Игрок #" + playerId;
            if (playerName != null && !playerName.startsWith("Игрок #")) {
                disconnectMsg += " (" + playerName + ")";
            }
            disconnectMsg += " отключился";

            broadcast(new Message(Message.ACTION, 0, disconnectMsg));
            logger.info("{} (осталось: {}/3)", disconnectMsg, clients.size());
            broadcastPlayerList();
        }
    }

    public void broadcast(Message message) {
        if (clients.isEmpty()) return;

        try {
            GameMessage protocolMsg;

            switch (message.getType()) {
                case Message.CHAT:
                    protocolMsg = GameProtocol.createChatMessage(message.getPlayerId(), (String)message.getData());
                    break;
                case Message.ACTION:
                    protocolMsg = GameProtocol.createActionMessage(message.getPlayerId(), (String)message.getData());
                    break;
                case Message.LEVEL_UPDATE:
                    protocolMsg = GameProtocol.createLevelUpdateMessage(message.getPlayerId(), (String)message.getData());
                    break;
                default:
                    return;
            }

            List<Integer> disconnectedClients = new ArrayList<>();

            for (ClientHandler client : clients.values()) {
                try {
                    client.sendProtocolMessage(protocolMsg);
                } catch (Exception e) {
                    logger.warn("Не удалось отправить сообщение игроку #{}", client.playerId, e);
                    disconnectedClients.add(client.playerId);
                }
            }

            for (Integer playerId : disconnectedClients) {
                removeClient(playerId);
            }

        } catch (Exception e) {
            logger.error("Ошибка broadcast", e);
        }
    }

    private void broadcastGameState(GameWorld.GameState state) {
        if (clients.isEmpty()) return;

        try {
            GameMessage gameStateMsg = GameProtocol.createGameStateMessage(state);

            List<Integer> disconnectedClients = new ArrayList<>();

            for (ClientHandler client : clients.values()) {
                try {
                    client.sendProtocolMessage(gameStateMsg);
                } catch (Exception e) {
                    logger.warn("Не удалось отправить GameState игроку #{}", client.playerId, e);
                    disconnectedClients.add(client.playerId);
                }
            }

            for (Integer playerId : disconnectedClients) {
                removeClient(playerId);
            }

        } catch (IOException e) {
            logger.error("Ошибка сериализации GameState", e);
        }
    }

    public void stop() {
        logger.info("Останавливаем сервер...");
        running = false;

        if (maintenanceExecutor != null) {
            maintenanceExecutor.shutdownNow();
        }

        gameLoop.shutdownNow();

        List<Integer> clientIds = new ArrayList<>(clients.keySet());
        for (Integer playerId : clientIds) {
            removeClient(playerId);
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка при закрытии серверного сокета", e);
        }

        threadPool.shutdown();
        logger.info("Сервер остановлен");
    }

    public int getPlayerCount() {
        return clients.size();
    }

    class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private OutputStream out;
        private InputStream in;
        private volatile boolean connected = true;
        private String playerName;
        private final Object writeLock = new Object();
        private long lastActivityTime = System.currentTimeMillis();

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            this.playerName = "Игрок #" + playerId;
        }

        public long getLastActivityTime() {
            return lastActivityTime;
        }

        public String getPlayerName() {
            return playerName;
        }

        @Override
        public void run() {
            logger.info("Начинаем обработку клиента #{}", playerId);
            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();

                GameMessage connectMsg = new GameMessage(
                        GameProtocol.TYPE_CONNECT,
                        String.valueOf(playerId).getBytes()
                );
                sendProtocolMessage(connectMsg);
                logger.debug("Отправлен ID игроку #{}", playerId);

                while (connected && !socket.isClosed()) {
                    try {
                        GameMessage message = GameProtocol.readMessage(in);
                        if (message == null) {
                            logger.info("Клиент #{} отключился (конец потока)", playerId);
                            break;
                        }

                        lastActivityTime = System.currentTimeMillis();
                        handleProtocolMessage(message);

                    } catch (ProtocolException e) {
                        logger.warn("Протокольная ошибка от #{}: {}", playerId, e.getMessage());
                        if (e.getMessage().contains("Неверный заголовок")) {
                            try {
                                if (in.available() > 0) {
                                    in.skip(1);
                                    continue;
                                }
                            } catch (IOException ex) {
                                logger.debug("Ошибка при пропуске байта", ex);
                            }
                        }

                        try {
                            GameMessage errorMsg = GameProtocol.createErrorMessage(
                                    GameProtocol.ERROR_INVALID_MESSAGE,
                                    "Ошибка протокола: " + e.getMessage()
                            );
                            sendProtocolMessage(errorMsg);
                        } catch (Exception ex) {
                            logger.debug("Ошибка при отправке сообщения об ошибке", ex);
                        }

                        break;
                    } catch (SocketTimeoutException e) {
                        logger.debug("Таймаут при чтении от клиента #{}", playerId);
                        continue;
                    } catch (EOFException e) {
                        logger.info("Клиент #{} отключился (EOF)", playerId);
                        break;
                    } catch (IOException e) {
                        if (e.getMessage() != null && (e.getMessage().contains("closed") ||
                                e.getMessage().contains("reset") || e.getMessage().contains("abort"))) {
                            logger.warn("Соединение с клиентом #{} разорвано", playerId);
                            break;
                        }
                        logger.error("Ошибка чтения от клиента #{}", playerId, e);
                        break;
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка подключения клиента #{}", playerId, e);
            } finally {
                disconnect();
            }
        }

        private void handleProtocolMessage(GameMessage message) {
            try {
                byte type = message.getType();

                switch (type) {
                    case GameProtocol.TYPE_CONNECT:
                        String[] connectData = GameProtocol.parseConnectMessage(message);
                        if (connectData.length >= 2) {
                            String username = connectData[0];
                            String characterType = connectData[1];
                            this.playerName = username;
                            logger.info("Регистрируем игрока #{}: {} ({})", playerId, username, characterType);
                            registerPlayer(playerId, username, characterType);
                        }
                        break;

                    case GameProtocol.TYPE_PLAYER_MOVE:
                        GameProtocol.MoveData moveData = GameProtocol.parseMoveMessage(message);
                        if (moveData.playerId == playerId) {
                            Direction dir = GameProtocol.byteToDirection(moveData.direction);
                            handlePlayerMove(playerId, dir);
                        }
                        break;

                    case GameProtocol.TYPE_ACTION:
                        GameProtocol.MessageData actionData = GameProtocol.parseTextMessage(message);
                        if (actionData.playerId == playerId) {
                            handlePlayerAction(playerId, actionData.text);
                        }
                        break;

                    case GameProtocol.TYPE_CHAT:
                        GameProtocol.MessageData chatData = GameProtocol.parseTextMessage(message);
                        if (chatData.playerId == playerId) {
                            broadcast(new Message(Message.CHAT, playerId, chatData.text));
                        }
                        break;

                    case GameProtocol.TYPE_DISCONNECT:
                        logger.info("Игрок #{} запросил отключение", playerId);
                        disconnect();
                        break;

                    case GameProtocol.TYPE_HEARTBEAT:
                        logger.debug("Получен heartbeat от игрока #{}", playerId);
                        break;

                    default:
                        logger.warn("Неизвестный тип сообщения от #{}: {}", playerId, type);
                }
            } catch (Exception e) {
                logger.error("Ошибка обработки протокольного сообщения от #{}", playerId, e);
            }
        }

        public void sendProtocolMessage(GameMessage message) {
            if (!connected || out == null) {
                logger.warn("Не могу отправить - клиент #{} отключен", playerId);
                return;
            }

            try {
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, message);
                }
            } catch (Exception e) {
                logger.error("Ошибка отправки игроку #{}", playerId, e);
                disconnect();
            }
        }

        private void disconnect() {
            if (!connected) return;

            connected = false;

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.debug("Ошибка при закрытии сокета клиента #{}", playerId, e);
            }

            GameServer.this.removeClient(playerId);
            logger.info("Соединение с игроком #{} ({}) закрыто", playerId, playerName);
        }
    }
}