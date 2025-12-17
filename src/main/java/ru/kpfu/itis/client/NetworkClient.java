package ru.kpfu.itis.client;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.GameWorld;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;
    private int playerId = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Consumer<GameWorld.GameState> onGameStateUpdate;
    private Consumer<Message> onMessageReceived;

    public boolean connect(String host, int port, String username, String characterType) {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;

            // Запускаем поток чтения
            executor.execute(this::readLoop);

            // Отправляем данные подключения
            Thread.sleep(100); // Небольшая задержка
            sendConnectMessage(username, characterType);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Ошибка подключения: " + e.getMessage());
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    private void sendConnectMessage(String username, String characterType) {
        // Формат: CONNECT|0|username|characterType
        sendRawMessage("CONNECT|0|" + username + "|" + characterType);
    }

    private void readLoop() {
        try {
            while (connected && !socket.isClosed()) {
                String message = in.readLine();
                if (message == null) {
                    break;
                }
                handleRawMessage(message);
            }
        } catch (IOException e) {
            if (!"Socket closed".equals(e.getMessage())) {
                System.err.println("❌ Ошибка чтения: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            disconnect();
        }
    }

    private void handleRawMessage(String rawMessage) {
        try {
            // Убрали лишний вывод в консоль
            String[] parts = rawMessage.split("\\|", 4);
            if (parts.length < 2) return;

            String messageType = parts[0];

            if (messageType.equals("CONNECT")) {
                // Формат: CONNECT|playerId|
                playerId = Integer.parseInt(parts[1]);
                return;
            }

            int msgPlayerId = Integer.parseInt(parts[1]);
            String data = parts.length > 2 ? parts[2] : "";

            switch (messageType) {
                case "GAME_STATE":
                    if (data != null && !data.isEmpty()) {
                        try {
                            GameWorld.GameState state = deserializeGameState(data);
                            if (onGameStateUpdate != null) {
                                onGameStateUpdate.accept(state);
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Ошибка десериализации состояния: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;

                case "CHAT":
                case "ACTION":
                case "LEVEL_UPDATE":
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(new Message(
                                messageType.equals("CHAT") ? Message.CHAT :
                                        messageType.equals("ACTION") ? Message.ACTION : Message.LEVEL_UPDATE,
                                msgPlayerId, data
                        ));
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMove(Direction direction) {
        // Формат: MOVE|playerId|direction
        if (playerId != -1) {
            sendRawMessage("MOVE|" + playerId + "|" + direction.name());
        }
    }

    public void sendAction(String action) {
        // Формат: ACTION|playerId|action
        if (playerId != -1) {
            sendRawMessage("ACTION|" + playerId + "|" + action);
        }
    }

    public void sendChat(String text) {
        // Формат: CHAT|playerId|text
        if (playerId != -1) {
            sendRawMessage("CHAT|" + playerId + "|" + text);
        }
    }

    private void sendRawMessage(String message) {
        if (!connected || out == null) {
            System.err.println("❌ Не могу отправить - нет соединения: " + message);
            return;
        }

        try {
            // Убрали лишний вывод в консоль
            out.println(message);
            out.flush();
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки: " + e.getMessage());
            disconnect();
        }
    }

    private GameWorld.GameState deserializeGameState(String data) throws IOException, ClassNotFoundException {
        if (data == null || data.isEmpty()) {
            throw new IOException("Пустые данные для десериализации");
        }
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (GameWorld.GameState) ois.readObject();
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
        return connected;
    }

    public void disconnect() {
        connected = false;
        executor.shutdownNow();

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Игнорируем
        }
    }
}