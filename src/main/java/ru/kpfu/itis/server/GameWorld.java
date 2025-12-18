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
    private final List<PatrolEnemy> patrolEnemies = new CopyOnWriteArrayList<>();
    private final List<Trap> traps = new CopyOnWriteArrayList<>();
    private final List<String> collectedDiamonds = new ArrayList<>();
    private int currentLevel = 1;
    private int totalDiamonds;
    private int collectedDiamondsCount = 0;
    private boolean levelComplete = false;
    private long levelStartTime;
    private boolean isRestarting = false;  // Флаг для предотвращения множественных перезапусков
    private java.util.function.Consumer<Message> broadcastCallback;

    // Задержка движения патрульного моба (мс) - медленнее игрока
    private static final long PATROL_ENEMY_MOVE_DELAY = 600;
    // Кулдаун атаки мобов (мс) - моб может атаковать только раз в секунду
    private static final long ENEMY_ATTACK_COOLDOWN = 1000;

    public GameWorld() {
        LevelLoader.createDefaultLevels();
        loadLevel(currentLevel);
    }

    public void loadLevel(int level) {
        isRestarting = false;  // Сбрасываем флаг перезапуска
        currentLevel = level;
        GeneratedLevel generated = LevelLoader.loadLevel(level);

        if (generated == null) {
            System.err.println("❌ Не удалось загрузить уровень " + level);
            return;
        }

        this.map = generated.map;
        this.enemies.clear();
        this.enemies.addAll(generated.enemies);
        this.patrolEnemies.clear();
        // Загружаем патрульных мобов из уровня (если есть), иначе создаем по умолчанию
        if (generated.patrolEnemies != null && !generated.patrolEnemies.isEmpty()) {
            this.patrolEnemies.addAll(generated.patrolEnemies);
        } else {
            this.initPatrolEnemies();  // Fallback для совместимости
        }
        this.traps.clear();
        // Загружаем ловушки из уровня (если есть), иначе создаем по умолчанию
        if (generated.traps != null && !generated.traps.isEmpty()) {
            this.traps.addAll(generated.traps);
        } else {
            this.initTraps();  // Fallback для совместимости
        }
        this.totalDiamonds = generated.totalDiamonds;
        this.collectedDiamondsCount = 0;
        this.levelComplete = false;
        this.levelStartTime = System.currentTimeMillis();
        this.collectedDiamonds.clear();

        // Распределяем игроков по стартовым позициям
        int startIndex = 0;
        List<PlayerState> playerList = new ArrayList<>(players.values());

        for (PlayerState player : playerList) {
            // Обнуляем алмазы при перезагрузке уровня
            player.diamonds = 0;
            
            if (startIndex < generated.startPositions.size()) {
                int[] startPos = generated.startPositions.get(startIndex);
                // Проверяем, что позиция свободна
                if (isPositionWalkable(startPos[0], startPos[1])) {
                    player.x = startPos[0];
                    player.y = startPos[1];
                    player.isAlive = true;
                    player.lives = 3;  // Восстанавливаем жизни при загрузке уровня
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

        // Все игроки начинают с 3 жизнями (независимо от персонажа)
        player.lives = 3;

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
            // Позиция занята, если у игрока есть жизни (даже если isAlive = false при lives = 0)
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
        // Игрок может двигаться, пока у него есть жизни (> 0)
        if (player == null || !player.canMove() || player.lives <= 0) return;

        int newX = player.x + direction.dx;
        int newY = player.y + direction.dy;

        if (isValidMove(newX, newY)) {
            player.x = newX;
            player.y = newY;
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
                // Ловушка отнимает 1 жизнь
                player.loseLife();
                broadcast(new Message(Message.ACTION, player.id,
                        player.name + " попал в ловушку! Осталось жизней: " + player.lives));
                
                // Если жизни закончились - перезапуск уровня через 3 секунды
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
        long now = System.currentTimeMillis();
        
        for (Enemy enemy : enemies) {
            if (enemy.isActive && enemy.x == player.x && enemy.y == player.y) {
                // Проверяем кулдаун атаки - моб может атаковать только раз в ENEMY_ATTACK_COOLDOWN мс
                if (now - enemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                    enemy.lastAttackTime = now;
                    
                    // Враг отнимает 1 жизнь
                    player.loseLife();
                    broadcast(new Message(Message.ACTION, player.id,
                            enemy.type + " атаковал " + player.name + "! Осталось жизней: " + player.lives));

                    // Если жизни закончились - перезапуск уровня через 3 секунды
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
                // Проверяем кулдаун атаки - патрульный моб может атаковать только раз в ENEMY_ATTACK_COOLDOWN мс
                if (now - patrolEnemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                    patrolEnemy.lastAttackTime = now;
                    
                    // Патрульный моб отнимает 1 жизнь
                    player.loseLife();
                    broadcast(new Message(Message.ACTION, player.id,
                            player.name + " столкнулся с патрульным мобом! Осталось жизней: " + player.lives));

                    // Если жизни закончились - перезапуск уровня через 3 секунды
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

    /**
     * Инициализация ловушек для текущего уровня.
     * Создаем несколько примеров ловушек.
     */
    private void initTraps() {
        if (map == null || map.length == 0 || map[0].length == 0) return;

        // Пример: стрела на стене справа, стреляет влево (контактная)
        int trapX = map[0].length - 1;
        int trapY = map.length / 2;
        if (trapX >= 0 && trapX < map[0].length && trapY >= 0 && trapY < map.length) {
            Trap arrowTrap = new Trap(trapX, trapY, TrapType.PRESSURE, TrapAttack.ARROW, Direction.LEFT);
            traps.add(arrowTrap);
        }

        // Пример: огненная ловушка на стене сверху, стреляет вниз (таймерная)
        int fireX = map[0].length / 2;
        int fireY = 0;
        if (fireX >= 0 && fireX < map[0].length && fireY >= 0 && fireY < map.length) {
            Trap fireTrap = new Trap(fireX, fireY, TrapType.TIMER, TrapAttack.FIRE, Direction.DOWN);
            traps.add(fireTrap);
        }
    }

    /**
     * Возвращает список клеток, которые поражает ловушка (зона поражения).
     */
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

            // Проверяем, что клетка в пределах карты
            if (targetX >= 0 && targetX < map[0].length && 
                targetY >= 0 && targetY < map.length) {
                cells.add(new int[]{targetX, targetY});
            }
        }

        return cells;
    }

    /**
     * Обновление ловушек - активация таймерных и деактивация после выстрела.
     */
    private void updateTraps() {
        long now = System.currentTimeMillis();

        for (Trap trap : traps) {
            // Деактивация после выстрела
            if (trap.active) {
                trap.deactivate(now);
            }

            // Активация таймерных ловушек
            if (trap.type == TrapType.TIMER && trap.shouldActivate(now)) {
                trap.activate(now);
            }
        }
    }

    /**
     * Проверка столкновений с ловушками.
     */
    private void checkTrapCollisions(PlayerState player) {
        if (player.lives <= 0) return;

        long now = System.currentTimeMillis();

        for (Trap trap : traps) {
            // Для контактных ловушек - проверяем, находится ли игрок в зоне поражения
            if (trap.type == TrapType.PRESSURE && !trap.active) {
                List<int[]> targetCells = getTargetCells(trap);
                for (int[] cell : targetCells) {
                    if (cell[0] == player.x && cell[1] == player.y) {
                        // Игрок наступил на триггерную клетку - активируем ловушку
                        trap.activate(now);
                        break;
                    }
                }
            }

            // Проверяем попадание в активную ловушку
            if (trap.active) {
                List<int[]> targetCells = getTargetCells(trap);
                for (int[] cell : targetCells) {
                    if (cell[0] == player.x && cell[1] == player.y) {
                        // Проверяем кулдаун урона - ловушка может нанести урон только раз в секунду
                        if (now - trap.lastDamageTime >= Trap.DAMAGE_COOLDOWN) {
                            trap.lastDamageTime = now;
                            
                            // Игрок попал в зону поражения
                            player.loseLife();
                            broadcast(new Message(Message.ACTION, player.id,
                                    player.name + " попал в ловушку (" + trap.attack + ")! Осталось жизней: " + player.lives));

                            // Если жизни закончились - перезапуск уровня через 3 секунды
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
                        return;  // Одна ловушка может убить за раз
                    }
                }
            }
        }
    }

    /**
     * Перезапускает текущий уровень для всех игроков.
     * Вызывается когда у любого игрока заканчиваются жизни.
     */
    private synchronized void restartLevel() {
        // Предотвращаем множественные перезапуски
        if (isRestarting) {
            return;
        }
        
        isRestarting = true;
        broadcast(new Message(Message.ACTION, 0,
                "Уровень перезапущен! Все игроки начинают заново."));
        
        // Перезагружаем текущий уровень - это сбросит карту, врагов, алмазы и позиции игроков
        loadLevel(currentLevel);
        
        // Сбрасываем флаг после завершения
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
        
        // Проверяем попадание в ловушки после их обновления
        checkTrapsAfterUpdate();
    }

    /**
     * Проверка попадания в ловушки после обновления (для таймерных).
     */
    private void checkTrapsAfterUpdate() {
        for (PlayerState player : players.values()) {
            if (player.lives > 0) {
                checkTrapCollisions(player);
            }
        }
    }

    /**
     * Инициализация патрульных мобов для текущего уровня.
     * Создаем одного патрульного моба для начала.
     */
    private void initPatrolEnemies() {
        // Создаем патрульного моба в центре карты (пример)
        // В реальной игре можно загружать из уровня или генерировать умнее
        if (map != null && map.length > 0 && map[0].length > 0) {
            int centerX = map[0].length / 2;
            int centerY = map.length / 2;

            // Ищем свободную позицию
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

    /**
     * Обновление патрульных мобов - движение по прямой линии с таймером.
     */
    private void updatePatrolEnemies() {
        long now = System.currentTimeMillis();

        for (PatrolEnemy patrolEnemy : patrolEnemies) {
            if (now - patrolEnemy.lastMoveTime < PATROL_ENEMY_MOVE_DELAY) {
                continue;
            }

            movePatrolEnemy(patrolEnemy);
            patrolEnemy.lastMoveTime = now;
        }

        // Проверяем столкновения с игроками после всех движений
        checkPatrolEnemyCollisionsAfterMove();
    }

    /**
     * Движение патрульного моба по прямой линии.
     */
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
            // Столкновение со стеной - разворачиваемся
            patrolEnemy.direction = patrolEnemy.direction == PatrolDirection.POSITIVE
                    ? PatrolDirection.NEGATIVE
                    : PatrolDirection.POSITIVE;
            return;
        }

        // Двигаемся
        patrolEnemy.x = nextX;
        patrolEnemy.y = nextY;
    }

    /**
     * Проверка столкновений патрульных мобов с игроками после движения.
     */
    private void checkPatrolEnemyCollisionsAfterMove() {
        long now = System.currentTimeMillis();
        
        for (PatrolEnemy patrolEnemy : patrolEnemies) {
            for (PlayerState player : players.values()) {
                // Проверяем только живых игроков (с жизнями > 0)
                if (player.lives <= 0) continue;

                if (player.x == patrolEnemy.x && player.y == patrolEnemy.y) {
                    // Проверяем кулдаун атаки - патрульный моб может атаковать только раз в ENEMY_ATTACK_COOLDOWN мс
                    if (now - patrolEnemy.lastAttackTime >= ENEMY_ATTACK_COOLDOWN) {
                        patrolEnemy.lastAttackTime = now;
                        
                        // Патрульный моб отнимает 1 жизнь
                        player.loseLife();
                        broadcast(new Message(Message.ACTION, player.id,
                                player.name + " столкнулся с патрульным мобом! Осталось жизней: " + player.lives));

                        // Если жизни закончились - перезапуск уровня через 3 секунды
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
        public final List<PatrolEnemy> patrolEnemies;
        public final List<Trap> traps;
        public final TileType[][] map;
        public final int collectedDiamonds;
        public final int totalDiamonds;
        public final int currentLevel;
        public final boolean levelComplete;
        public final long levelStartTime;

        public GameState(List<PlayerState> players, List<Enemy> enemies, List<PatrolEnemy> patrolEnemies, 
                         List<Trap> traps, TileType[][] map,
                         int collectedDiamonds, int totalDiamonds, int currentLevel,
                         boolean levelComplete, long levelStartTime) {
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
        }
    }
}