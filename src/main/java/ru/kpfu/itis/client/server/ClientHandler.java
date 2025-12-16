package ru.kpfu.itis.client.server;

import ru.kpfu.itis.common.Direction;
import ru.kpfu.itis.common.PlayerState;
import ru.kpfu.itis.common.TileType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PlayerState player;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, PlayerState player) {
        this.socket = socket;
        this.player = player;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            sendWorldState();

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Direction) {
                    handleMove((Direction) obj);
                }
            }
        } catch (Exception e) {
            System.out.println("Player disconnected: " + player.id);
        }
    }

    private void handleMove(Direction dir) {
        int nx = player.x + dir.dx;
        int ny = player.y + dir.dy;

        TileType tile = ServerMain.world.getTile(nx, ny);
        if (tile == TileType.WALL) return;

        player.x = nx;
        player.y = ny;

        if (tile == TileType.DIAMOND) {
            ServerMain.world.removeDiamond(nx, ny);
        }

        if (tile == TileType.TRAP) {
            player.hp -= 25;
            if (player.hp <= 0) {
                player.hp = player.maxHp;
                player.x = 1;
                player.y = 1;
            }
        }

        if (tile == TileType.CHEST) {
            player.hasKey = true;
        }

        if (tile == TileType.DOOR) {
            player.atDoor = true;
        }

        sendWorldState();
    }

    private void sendWorldState() {
        try {
            out.reset();
            out.writeObject(ServerMain.world.getPlayers());
            out.flush();
        } catch (Exception ignored) {}
    }
}
