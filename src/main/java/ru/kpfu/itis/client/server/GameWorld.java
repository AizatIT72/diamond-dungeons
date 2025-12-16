package ru.kpfu.itis.client.server;

import ru.kpfu.itis.common.PlayerState;
import ru.kpfu.itis.common.TileType;

import java.util.*;

public class GameWorld {

    public static final int SIZE = 20;

    private final TileType[][] map = new TileType[SIZE][SIZE];
    private final Map<Integer, PlayerState> players = new HashMap<>();

    public GameWorld() {
        generateLevel();
    }

    private void generateLevel() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                map[y][x] = TileType.FLOOR;
                if (x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1) {
                    map[y][x] = TileType.WALL;
                }
            }
        }

        map[5][5] = TileType.DIAMOND;
        map[7][7] = TileType.TRAP;
        map[10][10] = TileType.CHEST;
        map[18][18] = TileType.DOOR;
    }

    public synchronized PlayerState spawnPlayer(int id) {
        PlayerState p = new PlayerState(id, "Player-" + id, 1 + id, 1);
        players.put(id, p);
        return p;
    }

    public synchronized Collection<PlayerState> getPlayers() {
        return players.values();
    }

    public TileType getTile(int x, int y) {
        return map[y][x];
    }

    public void removeDiamond(int x, int y) {
        map[y][x] = TileType.FLOOR;
    }
}
