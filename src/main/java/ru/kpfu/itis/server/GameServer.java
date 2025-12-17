package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
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

    public GameServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        gameWorld = new GameWorld();

        gameWorld.setBroadcastCallback(this::broadcast);
        System.out.println("🎮 Игровой мир инициализирован");
    }

    public void start() {
        running = true;

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
            System.out.println("👂 Ожидаем подключения на порту " + serverSocket.getLocalPort());

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);

                    // Ограничение на 3 игрока
                    if (clients.size() >= 3) {
                        System.out.println("⚠️  Достигнут лимит игроков (3). Отклоняем подключение.");
                        clientSocket.close();
                        continue;
                    }

                    int playerId = nextPlayerId++;
                    ClientHandler handler = new ClientHandler(clientSocket, playerId);
                    clients.put(playerId, handler);
                    threadPool.execute(handler);

                    System.out.println("🎮 Игрок #" + playerId + " подключился (всего: " + clients.size() + "/3)");

                } catch (IOException e) {
                    if (running) {
                        System.err.println("⚠️  Ошибка приема подключения: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void gameTick() {
        try {
            gameWorld.updateEnemies();
            GameWorld.GameState state = gameWorld.getGameState();
            broadcastGameState(state);

            if (state.levelComplete) {
                broadcastMessage("ACTION|0|Уровень " + state.currentLevel + " пройден!");
                Thread.sleep(2000);
                gameWorld.loadLevel(state.currentLevel + 1);
                broadcastMessage("ACTION|0|Загружен уровень " + (state.currentLevel + 1));
            }
        } catch (Exception e) {
            System.err.println("⚠️  Ошибка в игровом цикле: " + e.getMessage());
        }
    }

    public void registerPlayer(int playerId, String name, String characterType) {
        PlayerState player = gameWorld.addPlayer(playerId, name, characterType);
        broadcastMessage("ACTION|0|👤 " + name + " (" + characterType + ") присоединился к игре");
    }

    public void handlePlayerMove(int playerId, Direction direction) {
        gameWorld.movePlayer(playerId, direction);
    }

    public void handlePlayerAction(int playerId, String action) {
        broadcastMessage("ACTION|" + playerId + "|" + action);
    }

    public void removeClient(int playerId) {
        ClientHandler handler = clients.remove(playerId);
        if (handler != null) {
            gameWorld.removePlayer(playerId);
            broadcastMessage("ACTION|0|Игрок #" + playerId + " отключился");
            System.out.println("👋 Игрок #" + playerId + " отключился (осталось: " + clients.size() + "/3)");
        }
    }

    public void broadcast(Message message) {
        // Преобразуем Message в строку для broadcastMessage
        String messageType;
        switch (message.getType()) {
            case Message.CHAT: messageType = "CHAT"; break;
            case Message.ACTION: messageType = "ACTION"; break;
            case Message.LEVEL_UPDATE: messageType = "LEVEL_UPDATE"; break;
            case Message.GAME_STATE:
                try {
                    broadcastGameState((GameWorld.GameState) message.getData());
                } catch (Exception e) {
                    System.err.println("⚠️  Ошибка broadcast GameState: " + e.getMessage());
                }
                return;
            default: return;
        }

        broadcastMessage(messageType + "|" + message.getPlayerId() + "|" + message.getData());
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                System.err.println("⚠️  Не удалось отправить сообщение игроку");
            }
        }
    }

    private void broadcastGameState(GameWorld.GameState state) {
        try {
            String serializedState = serializeGameState(state);
            String message = "GAME_STATE|0|" + serializedState;
            broadcastMessage(message);
        } catch (IOException e) {
            System.err.println("⚠️  Ошибка сериализации GameState: " + e.getMessage());
        }
    }

    private String serializeGameState(GameWorld.GameState state) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(state);
        oos.flush();
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Игнорируем
        }
        threadPool.shutdown();
        gameLoop.shutdown();
    }

    class ClientHandler implements Runnable {
        private final Socket socket;
        private final int playerId;
        private PrintWriter out;
        private BufferedReader in;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Отправляем ID игрока (формат: CONNECT|playerId|)
                sendMessage("CONNECT|" + playerId + "|");
                System.out.println("Отправлен ID игроку #" + playerId);

                // Чтение сообщений от клиента
                while (connected) {
                    try {
                        String message = in.readLine();
                        if (message == null) {
                            break;
                        }
                        System.out.println("Получено от #" + playerId + ": " + message);
                        handleMessage(message);
                    } catch (SocketException e) {
                        break;
                    } catch (Exception e) {
                        System.err.println("⚠️  Ошибка чтения от клиента " + playerId + ": " + e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("⚠️  Ошибка подключения клиента " + playerId + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void handleMessage(String message) {
            try {
                String[] parts = message.split("\\|", 4);
                if (parts.length < 2) {
                    System.err.println("Некорректное сообщение от #" + playerId + ": " + message);
                    return;
                }

                String messageType = parts[0];

                // Обработка подключения клиента
                if (messageType.equals("CONNECT")) {
                    if (parts.length >= 4) {
                        String username = parts[2];
                        String characterType = parts[3];
                        System.out.println("Регистрируем игрока #" + playerId + ": " + username + " (" + characterType + ")");
                        registerPlayer(playerId, username, characterType);
                    } else {
                        System.err.println("Некорректное сообщение CONNECT от #" + playerId + ": " + message);
                    }
                    return;
                }

                // Для остальных сообщений parts[1] должен быть playerId
                int msgPlayerId;
                try {
                    msgPlayerId = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Некорректный playerId в сообщении от #" + playerId + ": " + parts[1]);
                    return;
                }

                // Проверяем, что playerId в сообщении совпадает с реальным
                if (msgPlayerId != playerId) {
                    System.err.println("Несоответствие playerId: ожидалось " + playerId + ", получено " + msgPlayerId);
                    return;
                }

                String data = parts.length > 2 ? parts[2] : "";

                switch (messageType) {
                    case "MOVE":
                        try {
                            Direction dir = Direction.valueOf(data);
                            handlePlayerMove(playerId, dir);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Некорректное направление: " + data);
                        }
                        break;

                    case "ACTION":
                        handlePlayerAction(playerId, data);
                        break;

                    case "CHAT":
                        broadcastMessage("CHAT|" + playerId + "|" + data);
                        break;

                    default:
                        System.err.println("Неизвестный тип сообщения от #" + playerId + ": " + messageType);
                }
            } catch (Exception e) {
                System.err.println("⚠️  Ошибка обработки сообщения от " + playerId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            if (!connected || out == null) {
                System.err.println("Не могу отправить - клиент #" + playerId + " отключен");
                return;
            }

            try {
                out.println(message);
                out.flush();
            } catch (Exception e) {
                System.err.println("⚠️  Ошибка отправки игроку " + playerId + ": " + e.getMessage());
                disconnect();
            }
        }

        private void disconnect() {
            if (!connected) return;

            connected = false;
            removeClient(playerId);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Игнорируем
            }
        }
    }
}