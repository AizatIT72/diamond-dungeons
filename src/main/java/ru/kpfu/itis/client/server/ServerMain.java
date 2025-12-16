package ru.kpfu.itis.client.server;

import ru.kpfu.itis.common.PlayerState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMain {

    public static final int PORT = 7777;

    private static final AtomicInteger ID_GEN = new AtomicInteger(1);
    public static final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    public static final GameWorld world = new GameWorld();

    public static void main(String[] args) throws IOException {

        System.out.println("SERVER STARTED ON PORT " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);

        new Thread(new TickLoop()).start();

        while (true) {
            Socket socket = serverSocket.accept();
            int playerId = ID_GEN.getAndIncrement();

            PlayerState player = world.spawnPlayer(playerId);

            ClientHandler handler = new ClientHandler(socket, player);
            clients.put(playerId, handler);

            new Thread(handler).start();
        }
    }
}
