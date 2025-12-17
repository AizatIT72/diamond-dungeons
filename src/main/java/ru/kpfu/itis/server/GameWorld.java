package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.LevelLoader.GeneratedLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameWorld {
    private TileType[][] map;
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();
    private final List<String> collectedDiamonds = new ArrayList<>();
    private int currentLevel = 1;
    private int totalDiamonds;
    private int collectedDiamondsCount = 0;
    private boolean levelComplete = false;
    private long levelStartTime;
    private java.util.function.Consumer<Message> broadcastCallback;

    public GameWorld() {
        LevelLoader.createDefaultLevels();
        loadLevel(currentLevel);
    }

    public void loadLevel(int level) {
        currentLevel = level;
        GeneratedLevel generated = LevelLoader.loadLevel(level);

        if (generated == null) {
            System.err.println("❌ Не удалось загрузить уровень " + level);
            return;
        }

        this.map = generated.map;
        this.enemies.clear();
        this.enemies.addAll(generated.enemies);
        this.totalDiamonds = generated.totalDiamonds;
        this.collectedDiamondsCount = 0;
        this.levelComplete = false;
        this.levelStartTime = System.currentTimeMillis();
        this.collectedDiamonds.clear();

        // Распределяем игроков по стартовым позициям
        int startIndex = 0;
        List<PlayerState> playerList = new ArrayList<>(players.values());

        for (PlayerState player : playerList) {
            if (startIndex < generated.startPositions.size()) {
                int[] startPos = generated.startPositions.get(startIndex);
                // Проверяем, что позиция свободна
                if (isPositionWalkable(startPos[0], startPos[1])) {
                    player.x = startPos[0];
                    player.y = startPos[1];
                    player.isAlive = true;
                    player.health = player.maxHealth;
                    player.hasKey = false;
                    startIndex++;
                } else {
                    // Ищем любую свободную позицию
                    findFreePosition(player);
                }
            } else {
                // Ищем любую свободную позицию
                findFreePosition(player);
            }
        }

        System.out.println("📊 Уровень " + level + " загружен. Алмазов: " + totalDiamonds);
    }

    private boolean isPositionWalkable(int x, int y) {
        if (x < 0 || x >= map[0].length || y < 0 || y >= map.length) {
            return false;
        }
        return map[y][x].isWalkable() && !isPositionOccupied(x, y);
    }

    private void findFreePosition(PlayerState player) {
        // Сначала ищем рядом со стартовыми позициями
        GeneratedLevel generated = LevelLoader.loadLevel(currentLevel);
        if (generated != null && !generated.startPositions.isEmpty()) {
            for (int[] startPos : generated.startPositions) {
                // Проверяем все клетки в радиусе 2 от стартовой позиции
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        int x = startPos[0] + dx;
                        int y = startPos[1] + dy;
                        if (x >= 0 && x < map[0].length && y >= 0 && y < map.length) {
                            if (isPositionWalkable(x, y)) {
                                player.x = x;
                                player.y = y;
                                return;
                            }
                        }
                    }
                }
            }
        }

        // Если не нашли, ищем любую свободную клетку на карте
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                if (isPositionWalkable(x, y)) {
                    player.x = x;
                    player.y = y;
                    return;
                }
            }
        }

        // Если вообще ничего не нашли, ставим в позицию 1,1
        player.x = 1;
        player.y = 1;
    }

    public synchronized PlayerState addPlayer(int id, String name, String characterType) {
        PlayerState player = new PlayerState(id, name, characterType);

        // Устанавливаем характеристики в зависимости от персонажа
        if (characterType.contains("Красный")) {
            player.maxHealth = 180;
            player.health = 180;
        } else if (characterType.contains("Синий")) {
            player.maxHealth = 120;
            player.health = 120;
        } else if (characterType.contains("Зеленый")) {
            player.maxHealth = 100;
            player.health = 100;
        }

        // Находим свободную позицию
        findFreePosition(player);

        players.put(id, player);

        // Убедимся, что на позиции игрока пол
        if (map[player.y][player.x] != TileType.FLOOR) {
            map[player.y][player.x] = TileType.FLOOR;
        }

        return player;
    }

    private boolean isPositionOccupied(int x, int y) {
        for (PlayerState p : players.values()) {
            if (p.x == x && p.y == y && p.isAlive) return true;
        }
        for (Enemy e : enemies) {
            if (e.x == x && e.y == y && e.isActive) return true;
        }
        return false;
    }

    public synchronized void movePlayer(int playerId, Direction direction) {
        PlayerState player = players.get(playerId);
        if (player == null || !player.canMove() || !player.isAlive) return;

        int newX = player.x + direction.dx;
        int newY = player.y + direction.dy;

        if (isValidMove(newX, newY)) {
            player.x = newX;
            player.y = newY;
            player.lastMoveTime = System.currentTimeMillis();
            checkTileCollisions(player);
            checkEnemyCollisions(player);
        }
    }

    private boolean isValidMove(int x, int y) {
        if (x < 0 || x >= map[0].length || y < 0 || y >= map.length) {
            return false;
        }
        return map[y][x].isWalkable();
    }

    private void checkTileCollisions(PlayerState player) {
        TileType tile = map[player.y][player.x];

        switch (tile) {
            case DIAMOND:
                collectDiamond(player.x, player.y, player);
                break;

            case TRAP:
                int trapDamage = player.characterType.contains("Красный") ? 15 : 25;
                player.takeDamage(trapDamage);
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " попал в ловушку (-" + trapDamage + " HP)"));
                map[player.y][player.x] = TileType.FLOOR;
                break;

            case CHEST:
                player.hasKey = true;
                map[player.y][player.x] = TileType.FLOOR;
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " нашел ключ!"));
                break;

            case DOOR:
                if (player.hasKey && collectedDiamondsCount >= totalDiamonds) {
                    levelComplete = true;
                    broadcast(new Message(Message.ACTION, 0,
                            "Выход открыт! Все алмазы собраны!"));
                }
                break;
        }
    }

    private void collectDiamond(int x, int y, PlayerState player) {
        String diamondKey = x + "," + y;
        if (collectedDiamonds.contains(diamondKey)) return;

        player.addDiamond();
        collectedDiamondsCount++;
        collectedDiamonds.add(diamondKey);
        map[y][x] = TileType.FLOOR;

        // Бонус для зеленого плута
        if (player.characterType.contains("Зеленый")) {
            player.addDiamond(); // Дополнительный алмаз
            broadcast(new Message(Message.ACTION, player.id,
                    player.name + " собрал 2 алмаза благодаря своей ловкости!"));
        } else {
            broadcast(new Message(Message.ACTION, player.id,
                    player.name + " собрал алмаз! (" + collectedDiamondsCount + "/" + totalDiamonds + ")"));
        }

        if (collectedDiamondsCount >= totalDiamonds) {
            broadcast(new Message(Message.ACTION, 0,
                    "Все алмазы собраны! Найдите выход и используйте ключ."));
        }
    }

    private void checkEnemyCollisions(PlayerState player) {
        for (Enemy enemy : enemies) {
            if (enemy.isActive && enemy.x == player.x && enemy.y == player.y) {
                player.takeDamage(enemy.type.damage);
                broadcast(new Message(Message.ACTION, player.id,
                        enemy.type + " атаковал " + player.name + " (-" + enemy.type.damage + " HP)"));

                if (!player.isAlive) {
                    broadcast(new Message(Message.ACTION, player.id,
                            player.name + " погиб от " + enemy.type));

                    // Возрождение через 5 секунд
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            respawnPlayer(player);
                        }
                    }, 5000);
                }
                break;
            }
        }
    }

    private void respawnPlayer(PlayerState player) {
        player.isAlive = true;
        player.health = player.maxHealth;
        findFreePosition(player);
        broadcast(new Message(Message.ACTION, player.id,
                player.name + " возродился!"));
    }

    public synchronized void updateEnemies() {
        List<PlayerState> playerList = new ArrayList<>(players.values());

        for (Enemy enemy : enemies) {
            if (enemy.isActive) {
                enemy.move(map, playerList);
            }
        }
    }

    public synchronized void removePlayer(int playerId) {
        players.remove(playerId);
    }

    public synchronized GameState getGameState() {
        return new GameState(
                new ArrayList<>(players.values()),
                new ArrayList<>(enemies),
                map,
                collectedDiamondsCount,
                totalDiamonds,
                currentLevel,
                levelComplete,
                levelStartTime
        );
    }

    private void broadcast(Message message) {
        if (broadcastCallback != null) {
            broadcastCallback.accept(message);
        }
    }

    public void setBroadcastCallback(java.util.function.Consumer<Message> callback) {
        this.broadcastCallback = callback;
    }

    public static class GameState implements java.io.Serializable {
        // ДОБАВИТЬ ЭТУ СТРОЧКУ
        private static final long serialVersionUID = 1L;

        public final List<PlayerState> players;
        public final List<Enemy> enemies;
        public final TileType[][] map;
        public final int collectedDiamonds;
        public final int totalDiamonds;
        public final int currentLevel;
        public final boolean levelComplete;
        public final long levelStartTime;

        public GameState(List<PlayerState> players, List<Enemy> enemies, TileType[][] map,
                         int collectedDiamonds, int totalDiamonds, int currentLevel,
                         boolean levelComplete, long levelStartTime) {
            this.players = players;
            this.enemies = enemies;
            this.map = map;
            this.collectedDiamonds = collectedDiamonds;
            this.totalDiamonds = totalDiamonds;
            this.currentLevel = currentLevel;
            this.levelComplete = levelComplete;
            this.levelStartTime = levelStartTime;
        }
    }
}