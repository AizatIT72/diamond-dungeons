package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.protocol.GameMessage;
import ru.kpfu.itis.protocol.GameProtocol;
import ru.kpfu.itis.protocol.ProtocolException;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
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
        serverSocket.setSoTimeout(1000); // Таймаут на accept для graceful shutdown

        gameWorld = new GameWorld();
        gameWorld.setBroadcastCallback(this::broadcast);

        System.out.println("🎮 Игровой мир инициализирован");
    }

    public void start() {
        running = true;

        // Запускаем обслуживание
        startMaintenance();

        // Игровой цикл
        gameLoop.scheduleAtFixedRate(() -> {
            try {
                gameTick();
            } catch (Exception e) {
                System.err.println("⚠️  Ошибка игрового цикла: " + e.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Прием подключений
        threadPool.execute(() -> {
            System.out.println("👂 Сервер запущен на порту " + port);
            System.out.println("👂 Ожидаем подключения игроков...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setSoTimeout(45000); // Таймаут 45 секунд на операции

                    // Ограничение на 3 игрока
                    if (clients.size() >= 3) {
                        System.out.println("⚠️  Достигнут лимит игроков (3). Отклоняем подключение.");
                        try {
                            // Отправляем сообщение об ошибке
                            OutputStream out = clientSocket.getOutputStream();
                            GameMessage errorMsg = GameProtocol.createErrorMessage(
                                    GameProtocol.ERROR_SERVER_FULL,
                                    "Сервер заполнен (максимум 3 игрока)"
                            );
                            GameProtocol.writeMessage(out, errorMsg);
                            out.flush();
                        } catch (Exception e) {
                            // Игнорируем
                        }
                        clientSocket.close();
                        continue;
                    }

                    int playerId = nextPlayerId++;
                    ClientHandler handler = new ClientHandler(clientSocket, playerId);
                    clients.put(playerId, handler);
                    threadPool.execute(handler);

                    System.out.println("🎮 Игрок #" + playerId + " подключился (всего: " + clients.size() + "/3)");

                    // Отправляем обновленный список игроков всем
                    broadcastPlayerList();

                } catch (SocketTimeoutException e) {
                    // Таймаут на accept - это нормально, продолжаем цикл
                    continue;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("⚠️  Ошибка приема подключения: " + e.getMessage());
                    }
                }
            }

            System.out.println("🛑 Сервер прекратил прием подключений");
        });
    }

    private void startMaintenance() {
        maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                // Проверяем "мертвые" соединения
                List<Integer> deadClients = new ArrayList<>();
                long currentTime = System.currentTimeMillis();

                for (ClientHandler client : clients.values()) {
                    if (currentTime - client.getLastActivityTime() > 60000) { // 60 секунд без активности
                        System.err.println("⚠️  Клиент #" + client.playerId + " неактивен более 60 секунд");
                        deadClients.add(client.playerId);
                    }
                }

                // Удаляем мертвых клиентов
                for (Integer playerId : deadClients) {
                    System.err.println("🗑️  Удаляем неактивного клиента #" + playerId);
                    removeClient(playerId);
                }

                // Отправляем heartbeat всем клиентам каждые 20 секунд
                if (!clients.isEmpty()) {
                    GameMessage heartbeat = GameProtocol.createHeartbeatMessage();
                    for (ClientHandler client : clients.values()) {
                        try {
                            client.sendProtocolMessage(heartbeat);
                        } catch (Exception e) {
                            // Игнорируем
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("⚠️  Ошибка в maintenance: " + e.getMessage());
            }
        }, 20000, 20000, TimeUnit.MILLISECONDS); // Каждые 20 секунд
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
            System.err.println("⚠️  Ошибка отправки списка игроков: " + e.getMessage());
        }
    }

    private void gameTick() {
        try {
            gameWorld.updateEnemies();
            GameWorld.GameState state = gameWorld.getGameState();

            // Отправляем состояние игры только если есть изменения или прошло время
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
            System.err.println("⚠️  Ошибка в игровом цикле: " + e.getMessage());
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
            System.out.println("👋 " + disconnectMsg + " (осталось: " + clients.size() + "/3)");
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

            // Отправляем всем клиентам
            List<Integer> disconnectedClients = new ArrayList<>();

            for (ClientHandler client : clients.values()) {
                try {
                    client.sendProtocolMessage(protocolMsg);
                } catch (Exception e) {
                    System.err.println("⚠️  Не удалось отправить сообщение игроку #" +
                            client.playerId + ": " + e.getMessage());
                    disconnectedClients.add(client.playerId);
                }
            }

            // Удаляем отключившихся клиентов
            for (Integer playerId : disconnectedClients) {
                removeClient(playerId);
            }

        } catch (Exception e) {
            System.err.println("⚠️  Ошибка broadcast: " + e.getMessage());
        }
    }

    private void broadcastGameState(GameWorld.GameState state) {
        if (clients.isEmpty()) return;

        try {
            GameMessage gameStateMsg = GameProtocol.createGameStateMessage(state);

            // Отправляем состояние игры всем подключенным клиентам
            List<Integer> disconnectedClients = new ArrayList<>();

            for (ClientHandler client : clients.values()) {
                try {
                    client.sendProtocolMessage(gameStateMsg);
                } catch (Exception e) {
                    System.err.println("⚠️  Не удалось отправить GameState игроку #" +
                            client.playerId + ": " + e.getMessage());
                    disconnectedClients.add(client.playerId);
                }
            }

            // Удаляем отключившихся клиентов
            for (Integer playerId : disconnectedClients) {
                removeClient(playerId);
            }

        } catch (IOException e) {
            System.err.println("⚠️  Ошибка сериализации GameState: " + e.getMessage());
        }
    }

    public void stop() {
        System.out.println("🛑 Останавливаем сервер...");
        running = false;

        // Останавливаем обслуживание
        if (maintenanceExecutor != null) {
            maintenanceExecutor.shutdownNow();
        }

        // Останавливаем игровой цикл
        gameLoop.shutdownNow();

        // Закрываем все клиентские соединения
        List<Integer> clientIds = new ArrayList<>(clients.keySet());
        for (Integer playerId : clientIds) {
            removeClient(playerId);
        }

        // Закрываем серверный сокет
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Игнорируем
        }

        // Останавливаем пул потоков
        threadPool.shutdown();

        System.out.println("🛑 Сервер остановлен");
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
            System.out.println("👤 Начинаем обработку клиента #" + playerId);

            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();

                // Отправляем подтверждение подключения с playerId
                GameMessage connectMsg = new GameMessage(
                        GameProtocol.TYPE_CONNECT,
                        String.valueOf(playerId).getBytes()
                );
                sendProtocolMessage(connectMsg);
                System.out.println("📤 Отправлен ID игроку #" + playerId);

                // Чтение сообщений от клиента
                while (connected && !socket.isClosed()) {
                    try {
                        GameMessage message = GameProtocol.readMessage(in);
                        if (message == null) {
                            System.out.println("📭 Клиент #" + playerId + " отключился (конец потока)");
                            break;
                        }

                        lastActivityTime = System.currentTimeMillis();
                        handleProtocolMessage(message);

                    } catch (ProtocolException e) {
                        System.err.println("Протокольная ошибка от #" + playerId + ": " + e.getMessage());

                        // Пробуем восстановить синхронизацию
                        if (e.getMessage().contains("Неверный заголовок")) {
                            try {
                                if (in.available() > 0) {
                                    in.skip(1);
                                    continue;
                                }
                            } catch (IOException ex) {
                                // Игнорируем
                            }
                        }

                        // Отправляем сообщение об ошибке клиенту
                        try {
                            GameMessage errorMsg = GameProtocol.createErrorMessage(
                                    GameProtocol.ERROR_INVALID_MESSAGE,
                                    "Ошибка протокола: " + e.getMessage()
                            );
                            sendProtocolMessage(errorMsg);
                        } catch (Exception ex) {
                            // Игнорируем
                        }

                        break;
                    } catch (SocketTimeoutException e) {
                        // Таймаут - продолжаем ждать
                        System.out.println("⏱️  Таймаут при чтении от клиента #" + playerId + ", продолжаем...");
                        continue;
                    } catch (EOFException e) {
                        System.out.println("📭 Клиент #" + playerId + " отключился (EOF)");
                        break;
                    } catch (IOException e) {
                        if (e.getMessage() != null && (e.getMessage().contains("closed") ||
                                e.getMessage().contains("reset") || e.getMessage().contains("abort"))) {
                            System.err.println("❌ Соединение с клиентом #" + playerId + " разорвано");
                            break;
                        }
                        System.err.println("Ошибка чтения от клиента " + playerId + ": " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка подключения клиента " + playerId + ": " + e.getMessage());
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
                            this.playerName = username;  // Обновляем имя
                            System.out.println("Регистрируем игрока #" + playerId + ": " +
                                    username + " (" + characterType + ")");
                            registerPlayer(playerId, username, characterType);
                        }
                        break;

                    case GameProtocol.TYPE_PLAYER_MOVE:
                        GameProtocol.MoveData moveData = GameProtocol.parseMoveMessage(message);
                        if (moveData.playerId == playerId) {
                            Direction dir = GameProtocol.byteToDirection(moveData.direction);
                            handlePlayerMove(playerId, dir);
                        } else {
                            System.err.println("Несоответствие playerId в MOVE от #" + playerId +
                                    ": ожидалось " + playerId + ", получено " + moveData.playerId);
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
                            // Рассылаем сообщение всем
                            broadcast(new Message(Message.CHAT, playerId, chatData.text));
                        }
                        break;

                    case GameProtocol.TYPE_DISCONNECT:
                        System.out.println("👋 Игрок #" + playerId + " запросил отключение");
                        disconnect();
                        break;

                    case GameProtocol.TYPE_HEARTBEAT:
                        // Просто обновляем время активности
                        System.out.println("❤️  Получен heartbeat от игрока #" + playerId);
                        break;

                    default:
                        System.err.println("Неизвестный тип сообщения от #" + playerId + ": " + type);
                }
            } catch (Exception e) {
                System.err.println("Ошибка обработки протокольного сообщения от #" + playerId + ": " + e.getMessage());
            }
        }

        public void sendProtocolMessage(GameMessage message) {
            if (!connected || out == null) {
                System.err.println("Не могу отправить - клиент #" + playerId + " отключен");
                return;
            }

            try {
                synchronized (writeLock) {
                    GameProtocol.writeMessage(out, message);
                }
            } catch (Exception e) {
                System.err.println("Ошибка отправки игроку #" + playerId + ": " + e.getMessage());
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
                // Игнорируем
            }

            // Удаляем из списка клиентов сервера
            GameServer.this.removeClient(playerId);

            System.out.println("📤 Соединение с игроком #" + playerId + " (" + playerName + ") закрыто");
        }
    }
}