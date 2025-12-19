package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.LevelLoader.GeneratedLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameWorld {
    private static final Logger logger = LoggerFactory.getLogger(GameWorld.class);
    private TileType[][] map;
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();
    private final List<PatrolEnemy> patrolEnemies = new CopyOnWriteArrayList<>();
    private final List<Trap> traps = new CopyOnWriteArrayList<>();
    private final List<String> collectedDiamonds = new ArrayList<>();
    private int currentLevel = 1;
    private int totalDiamonds;
    private int collectedDiamondsCount = 0;
    private boolean levelComplete = false;
    private long levelStartTime;
    private boolean isRestarting = false;
    private java.util.function.Consumer<Message> broadcastCallback;

    // Новые поля для перехода между уровнями
    private boolean isLevelTransitioning = false;
    private long levelTransitionStartTime = 0;
    private static final long LEVEL_TRANSITION_DURATION = 2000; // 2 секунды на переход

    private static final long PATROL_ENEMY_MOVE_DELAY = 600;
    private static final long ENEMY_ATTACK_COOLDOWN = 1000;

    public GameWorld() {
        LevelLoader.createDefaultLevels();
        loadLevel(currentLevel);
    }

    public void loadLevel(int level) {
        isRestarting = false;
        isLevelTransitioning = false; // Сбрасываем флаг перехода
        currentLevel = level;
        GeneratedLevel generated = LevelLoader.loadLevel(level);

        if (generated == null) {
            logger.error("Не удалось загрузить уровень {}", level);
            return;
        }

        this.map = generated.map;
        this.enemies.clear();
        this.enemies.addAll(generated.enemies);
        this.patrolEnemies.clear();

        if (generated.patrolEnemies != null && !generated.patrolEnemies.isEmpty()) {
            this.patrolEnemies.addAll(generated.patrolEnemies);
        } else {
            this.initPatrolEnemies();
        }
        this.traps.clear();

        if (generated.traps != null && !generated.traps.isEmpty()) {
            this.traps.addAll(generated.traps);
        } else {
            this.initTraps();
        }
        this.totalDiamonds = generated.totalDiamonds;
        this.collectedDiamondsCount = 0;
        this.levelComplete = false;
        this.levelStartTime = System.currentTimeMillis();
        this.collectedDiamonds.clear();

        int startIndex = 0;
        List<PlayerState> playerList = new ArrayList<>(players.values());

        for (PlayerState player : playerList) {
            player.diamonds = 0;
            player.hasKey = false; // Сбрасываем ключ при загрузке нового уровня

            if (startIndex < generated.startPositions.size()) {
                int[] startPos = generated.startPositions.get(startIndex);

                if (isPositionWalkable(startPos[0], startPos[1])) {
                    player.x = startPos[0];
                    player.y = startPos[1];
                    player.isAlive = true;
                    player.lives = 3;
                    startIndex++;
                } else {
                    findFreePosition(player);
                }
            } else {
                findFreePosition(player);
            }
        }

        logger.info("Уровень {} загружен. Алмазов: {}", level, totalDiamonds);
    }

    // Новый метод: проверка перехода на следующий уровень
    private void checkLevelTransition() {
        if (isLevelTransitioning || levelComplete) return;

        // Проверяем, собраны ли все алмазы
        if (collectedDiamondsCount < totalDiamonds) {
            return;
        }

        // Проверяем, стоят ли все живые игроки на дверях (TileType.DOOR)
        boolean allPlayersOnDoor = true;
        int alivePlayers = 0;

        for (PlayerState player : players.values()) {
            if (player.lives > 0) { // Только живые игроки
                alivePlayers++;

                // Проверяем, стоит ли игрок на двери
                TileType currentTile = map[player.y][player.x];
                if (currentTile != TileType.DOOR) {
                    allPlayersOnDoor = false;
                    break;
                }
            }
        }

        // Если есть живые игроки и все они на дверях, то начинаем переход
        if (alivePlayers > 0 && allPlayersOnDoor && !isRestarting) {
            startLevelTransition();
        }
    }

    // Новый метод: начало перехода на следующий уровень
    private void startLevelTransition() {
        isLevelTransitioning = true;
        levelTransitionStartTime = System.currentTimeMillis();
        levelComplete = true;

        broadcast(new Message(Message.ACTION, 0,
                "🎉 Все игроки в дверях! Переход на следующий уровень через 2 секунды..."));

        // Запускаем таймер для перехода
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                loadLevel(currentLevel + 1);
                isLevelTransitioning = false;
                broadcast(new Message(Message.ACTION, 0,
                        "🌌 Уровень " + currentLevel + " загружен!"));
            }
        }, LEVEL_TRANSITION_DURATION);
    }

    private boolean isPositionWalkable(int x, int y) {
        if (x < 0 || x >= map[0].length || y < 0 || y >= map.length) {
            return false;
        }
        return map[y][x].isWalkable() && !isPositionOccupied(x, y);
    }

    private void findFreePosition(PlayerState player) {
        GeneratedLevel generated = LevelLoader.loadLevel(currentLevel);
        if (generated != null && !generated.startPositions.isEmpty()) {
            for (int[] startPos : generated.startPositions) {
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

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                if (isPositionWalkable(x, y)) {
                    player.x = x;
                    player.y = y;
                    return;
                }
            }
        }

        player.x = 1;
        player.y = 1;
    }

    public synchronized PlayerState addPlayer(int id, String name, String characterType) {
        PlayerState player = new PlayerState(id, name, characterType);
        player.lives = 3;
        findFreePosition(player);
        players.put(id, player);

        if (map[player.y][player.x] != TileType.FLOOR) {
            map[player.y][player.x] = TileType.FLOOR;
        }

        return player;
    }

    private boolean isPositionOccupied(int x, int y) {
        for (PlayerState p : players.values()) {
            if (p.x == x && p.y == y && p.lives > 0) return true;
        }
        for (Enemy e : enemies) {
            if (e.x == x && e.y == y && e.isActive) return true;
        }
        for (PatrolEnemy pe : patrolEnemies) {
            if (pe.x == x && pe.y == y) return true;
        }
        return false;
    }

    public synchronized void movePlayer(int playerId, Direction direction) {
        PlayerState player = players.get(playerId);

        if (player == null || !player.canMove() || player.lives <= 0) return;

        int newX = player.x + direction.dx;
        int newY = player.y + direction.dy;

        if (isValidMove(newX, newY)) {
            player.x = newX;
            player.y = newY;
            player.direction = direction; // Обновляем направление
            player.lastMoveTime = System.currentTimeMillis();
            checkTileCollisions(player);
            checkEnemyCollisions(player);
            checkPatrolEnemyCollisions(player);
            checkTrapCollisions(player);
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
                player.loseLife();
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " попал в ловушку! Осталось жизней: " + player.lives));

                if (player.lives <= 0) {
                    broadcast(new Message(Message.ACTION, player.id,
                            player.name + " погиб! Уровень будет перезапущен через 3 секунды..."));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            restartLevel();
                        }
                    }, 3000);
                }
                map[player.y][player.x] = TileType.FLOOR;
                break;

            case CHEST:
                // Убираем логику с ключом, так как он не нужен
                map[player.y][player.x] = TileType.FLOOR;
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " нашел сундук!"));
                break;

            case DOOR:
                // Просто проверяем переход, ключ не нужен
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " у двери!"));
                checkLevelTransition(); // Проверяем, готовы ли все к переходу
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

        if (player.characterType.contains("Зеленый")) {
            player.addDiamond();
            broadcast(new Message(Message.ACTION, player.id,
                    player.name + " собрал 2 алмаза благодаря своей ловкости!"));
        } else {
            broadcast(new Message(Message.ACTION, player.id,
                    player.name + " собрал алмаз! (" + collectedDiamondsCount + "/" + totalDiamonds + ")"));
        }

        if (collectedDiamondsCount >= totalDiamonds) {
            broadcast(new Message(Message.ACTION, 0,
                    "💎 Все алмазы собраны! Идите к дверям!"));
            // Теперь проверяем, может быть игроки уже на дверях
            checkLevelTransition();
        }
    }

    private void checkEnemyCollisions(PlayerState player) {
        long now = System.currentTimeMillis();

        for (Enemy enemy : enemies) {
            if (enemy.isActive && enemy.x == player.x && enemy.y == player.y) {
                if (now - enemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                    enemy.lastAttackTime = now;

                    player.loseLife();
                    broadcast(new Message(Message.ACTION, player.id,
                            enemy.type + " атаковал " + player.name + "! Осталось жизней: " + player.lives));

                    if (player.lives <= 0) {
                        broadcast(new Message(Message.ACTION, player.id,
                                player.name + " погиб! Уровень будет перезапущен через 3 секунды..."));
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                restartLevel();
                            }
                        }, 3000);
                    }
                }
                break;
            }
        }
    }

    private void checkPatrolEnemyCollisions(PlayerState player) {
        long now = System.currentTimeMillis();

        for (PatrolEnemy patrolEnemy : patrolEnemies) {
            if (patrolEnemy.x == player.x && patrolEnemy.y == player.y) {
                if (now - patrolEnemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                    patrolEnemy.lastAttackTime = now;

                    player.loseLife();
                    broadcast(new Message(Message.ACTION, player.id,
                            player.name + " столкнулся с патрульным мобом! Осталось жизней: " + player.lives));

                    if (player.lives <= 0) {
                        broadcast(new Message(Message.ACTION, player.id,
                                player.name + " погиб от патрульного моба! Уровень будет перезапущен через 3 секунды..."));
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                restartLevel();
                            }
                        }, 3000);
                    }
                }
                break;
            }
        }
    }

    private void initTraps() {
        if (map == null || map.length == 0 || map[0].length == 0) return;

        int trapX = map[0].length - 1;
        int trapY = map.length / 2;
        if (trapX >= 0 && trapX < map[0].length && trapY >= 0 && trapY < map.length) {
            Trap arrowTrap = new Trap(trapX, trapY, TrapType.PRESSURE, TrapAttack.ARROW, Direction.LEFT);
            traps.add(arrowTrap);
        }

        int fireX = map[0].length / 2;
        int fireY = 0;
        if (fireX >= 0 && fireX < map[0].length && fireY >= 0 && fireY < map.length) {
            Trap fireTrap = new Trap(fireX, fireY, TrapType.TIMER, TrapAttack.FIRE, Direction.DOWN);
            traps.add(fireTrap);
        }
    }

    private List<int[]> getTargetCells(Trap trap) {
        List<int[]> cells = new ArrayList<>();

        for (int i = 1; i <= trap.range; i++) {
            int targetX = trap.x;
            int targetY = trap.y;

            switch (trap.direction) {
                case LEFT:
                    targetX = trap.x - i;
                    break;
                case RIGHT:
                    targetX = trap.x + i;
                    break;
                case UP:
                    targetY = trap.y - i;
                    break;
                case DOWN:
                    targetY = trap.y + i;
                    break;
            }

            if (targetX >= 0 && targetX < map[0].length &&
                    targetY >= 0 && targetY < map.length) {
                cells.add(new int[]{targetX, targetY});
            }
        }

        return cells;
    }

    private void updateTraps() {
        long now = System.currentTimeMillis();

        for (Trap trap : traps) {
            if (trap.active) {
                trap.deactivate(now);
            }

            if (trap.type == TrapType.TIMER && trap.shouldActivate(now)) {
                trap.activate(now);
            }
        }
    }

    private void checkTrapCollisions(PlayerState player) {
        if (player.lives <= 0) return;

        long now = System.currentTimeMillis();

        for (Trap trap : traps) {
            if (trap.type == TrapType.PRESSURE && !trap.active) {
                List<int[]> targetCells = getTargetCells(trap);
                for (int[] cell : targetCells) {
                    if (cell[0] == player.x && cell[1] == player.y) {
                        trap.activate(now);
                        break;
                    }
                }
            }

            if (trap.active) {
                List<int[]> targetCells = getTargetCells(trap);
                for (int[] cell : targetCells) {
                    if (cell[0] == player.x && cell[1] == player.y) {
                        if (now - trap.lastDamageTime >= Trap.DAMAGE_COOLDOWN) {
                            trap.lastDamageTime = now;

                            player.loseLife();
                            broadcast(new Message(Message.ACTION, player.id,
                                    player.name + " попал в ловушку (" + trap.attack + ")! Осталось жизней: " + player.lives));

                            if (player.lives <= 0) {
                                broadcast(new Message(Message.ACTION, player.id,
                                        player.name + " погиб от ловушки! Уровень будет перезапущен через 3 секунды..."));
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        restartLevel();
                                    }
                                }, 3000);
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    private synchronized void restartLevel() {
        if (isRestarting) {
            return;
        }

        isRestarting = true;
        broadcast(new Message(Message.ACTION, 0,
                "Уровень перезапущен! Все игроки начинают заново."));

        loadLevel(currentLevel);
        isRestarting = false;
    }

    public synchronized void updateEnemies() {
        List<PlayerState> playerList = new ArrayList<>(players.values());

        for (Enemy enemy : enemies) {
            if (enemy.isActive) {
                enemy.move(map, playerList);
            }
        }

        updatePatrolEnemies();
        updateTraps();
        checkTrapsAfterUpdate();

        // Также проверяем переход после обновления врагов (на случай, если игроки уже на дверях)
        checkLevelTransition();
    }

    private void checkTrapsAfterUpdate() {
        for (PlayerState player : players.values()) {
            if (player.lives > 0) {
                checkTrapCollisions(player);
            }
        }
    }

    private void initPatrolEnemies() {
        if (map != null && map.length > 0 && map[0].length > 0) {
            int centerX = map[0].length / 2;
            int centerY = map.length / 2;

            for (int y = centerY - 2; y <= centerY + 2; y++) {
                for (int x = centerX - 2; x <= centerX + 2; x++) {
                    if (x >= 0 && x < map[0].length && y >= 0 && y < map.length) {
                        if (isValidMove(x, y) && !isPositionOccupied(x, y)) {
                            PatrolEnemy patrolEnemy = new PatrolEnemy();
                            patrolEnemy.x = x;
                            patrolEnemy.y = y;
                            patrolEnemy.axis = PatrolAxis.HORIZONTAL;
                            patrolEnemy.direction = PatrolDirection.POSITIVE;
                            patrolEnemy.lastMoveTime = System.currentTimeMillis();
                            patrolEnemies.add(patrolEnemy);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updatePatrolEnemies() {
        long now = System.currentTimeMillis();

        for (PatrolEnemy patrolEnemy : patrolEnemies) {
            if (now - patrolEnemy.lastMoveTime < PATROL_ENEMY_MOVE_DELAY) {
                continue;
            }

            movePatrolEnemy(patrolEnemy);
            patrolEnemy.lastMoveTime = now;
        }

        checkPatrolEnemyCollisionsAfterMove();
    }

    private void movePatrolEnemy(PatrolEnemy patrolEnemy) {
        int dx = 0;
        int dy = 0;

        if (patrolEnemy.axis == PatrolAxis.HORIZONTAL) {
            dx = patrolEnemy.direction == PatrolDirection.POSITIVE ? 1 : -1;
        } else {
            dy = patrolEnemy.direction == PatrolDirection.POSITIVE ? 1 : -1;
        }

        int nextX = patrolEnemy.x + dx;
        int nextY = patrolEnemy.y + dy;

        if (!isValidMove(nextX, nextY)) {
            patrolEnemy.direction = patrolEnemy.direction == PatrolDirection.POSITIVE
                    ? PatrolDirection.NEGATIVE
                    : PatrolDirection.POSITIVE;
            return;
        }

        patrolEnemy.x = nextX;
        patrolEnemy.y = nextY;
    }

    private void checkPatrolEnemyCollisionsAfterMove() {
        long now = System.currentTimeMillis();

        for (PatrolEnemy patrolEnemy : patrolEnemies) {
            for (PlayerState player : players.values()) {
                if (player.lives <= 0) continue;

                if (player.x == patrolEnemy.x && player.y == patrolEnemy.y) {
                    if (now - patrolEnemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                        patrolEnemy.lastAttackTime = now;

                        player.loseLife();
                        broadcast(new Message(Message.ACTION, player.id,
                                player.name + " столкнулся с патрульным мобом! Осталось жизней: " + player.lives));

                        if (player.lives <= 0) {
                            broadcast(new Message(Message.ACTION, player.id,
                                    player.name + " погиб от патрульного моба! Уровень будет перезапущен через 3 секунды..."));
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    restartLevel();
                                }
                            }, 3000);
                        }
                    }
                }
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
                new ArrayList<>(patrolEnemies),
                new ArrayList<>(traps),
                map,
                collectedDiamondsCount,
                totalDiamonds,
                currentLevel,
                levelComplete,
                levelStartTime,
                isLevelTransitioning,
                levelTransitionStartTime
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
        private static final long serialVersionUID = 1L;

        public final List<PlayerState> players;
        public final List<Enemy> enemies;
        public final List<PatrolEnemy> patrolEnemies;
        public final List<Trap> traps;
        public final TileType[][] map;
        public final int collectedDiamonds;
        public final int totalDiamonds;
        public final int currentLevel;
        public final boolean levelComplete;
        public final long levelStartTime;
        public final boolean isLevelTransitioning;
        public final long levelTransitionStartTime;

        public GameState(List<PlayerState> players, List<Enemy> enemies, List<PatrolEnemy> patrolEnemies,
                         List<Trap> traps, TileType[][] map,
                         int collectedDiamonds, int totalDiamonds, int currentLevel,
                         boolean levelComplete, long levelStartTime,
                         boolean isLevelTransitioning, long levelTransitionStartTime) {
            this.players = players;
            this.enemies = enemies;
            this.patrolEnemies = patrolEnemies;
            this.traps = traps;
            this.map = map;
            this.collectedDiamonds = collectedDiamonds;
            this.totalDiamonds = totalDiamonds;
            this.currentLevel = currentLevel;
            this.levelComplete = levelComplete;
            this.levelStartTime = levelStartTime;
            this.isLevelTransitioning = isLevelTransitioning;
            this.levelTransitionStartTime = levelTransitionStartTime;
        }
    }
}