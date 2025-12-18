package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import java.util.*;

public class EnemyAI {

    public interface AIStrategy {
        void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel);
    }

    // Базовая стратегия - случайное движение
    public static class RandomStrategy implements AIStrategy {
        @Override
        public void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel) {
            if (enemy.type.speed == 0 || !enemy.isActive) return;

            long currentTime = System.currentTimeMillis();
            int speed = Math.max(1, enemy.type.speed + (currentLevel / 4));

            if (currentTime - enemy.lastMoveTime < 1000 / speed) return;

            Direction[] directions = Direction.values();
            Direction newDir = directions[(int)(Math.random() * directions.length)];

            int newX = enemy.x + newDir.dx;
            int newY = enemy.y + newDir.dy;

            if (isValidPosition(newX, newY, map)) {
                enemy.x = newX;
                enemy.y = newY;
                enemy.direction = newDir;
            }

            enemy.lastMoveTime = currentTime;
        }
    }

    // Стратегия преследования ближайшего игрока (упрощенная)
    public static class ChaseStrategy implements AIStrategy {
        private static final long ATTACK_COOLDOWN = 1000;  // Кулдаун атаки 1 секунда
        
        @Override
        public void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel) {
            if (enemy.type.speed == 0 || !enemy.isActive) return;

            long currentTime = System.currentTimeMillis();
            int speed = enemy.type.speed + (currentLevel / 3);
            int detectionRange = 5 + (currentLevel / 2);

            if (currentTime - enemy.lastMoveTime < 1000 / speed) return;

            // Находим ближайшего живого игрока
            PlayerState target = null;
            double minDistance = Double.MAX_VALUE;

            for (PlayerState player : players) {
                if (player.lives <= 0) continue;

                double distance = Math.sqrt(
                        Math.pow(player.x - enemy.x, 2) +
                                Math.pow(player.y - enemy.y, 2)
                );

                if (distance < minDistance && distance <= detectionRange) {
                    minDistance = distance;
                    target = player;
                }
            }

            if (target != null) {
                // Преследуем игрока
                Direction moveDir = calculateDirectionToTarget(enemy.x, enemy.y, target.x, target.y);

                if (moveDir != null) {
                    int newX = enemy.x + moveDir.dx;
                    int newY = enemy.y + moveDir.dy;

                    if (isValidPosition(newX, newY, map)) {
                        enemy.x = newX;
                        enemy.y = newY;
                        enemy.direction = moveDir;

                        // Если рядом с игроком - отнимаем жизнь (с кулдауном)
                        if (Math.abs(enemy.x - target.x) <= 1 && Math.abs(enemy.y - target.y) <= 1) {
                            if (currentTime - enemy.lastAttackTime >= ATTACK_COOLDOWN) {
                                enemy.lastAttackTime = currentTime;
                                target.loseLife();
                            }
                        }
                    }
                }
            } else {
                // Патрулируем случайно
                RandomStrategy randomMove = new RandomStrategy();
                randomMove.updateEnemy(enemy, map, players, currentLevel);
            }

            enemy.lastMoveTime = currentTime;
        }

        private Direction calculateDirectionToTarget(int fromX, int fromY, int toX, int toY) {
            int dx = Integer.compare(toX, fromX);
            int dy = Integer.compare(toY, fromY);

            // Предпочитаем движение по оси с большей разницей
            if (Math.abs(toX - fromX) > Math.abs(toY - fromY)) {
                return dx > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            }
        }
    }

    // Стратегия засады - ждет пока игрок подойдет близко (упрощенная)
    public static class AmbushStrategy implements AIStrategy {
        private static final long ATTACK_COOLDOWN = 1000;  // Кулдаун атаки 1 секунда
        private boolean isHidden = true;

        @Override
        public void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel) {
            if (!enemy.isActive) return;

            long currentTime = System.currentTimeMillis();
            int attackRange = 2 + (currentLevel / 4);

            // Проверяем, есть ли игроки в радиусе атаки
            for (PlayerState player : players) {
                if (player.lives <= 0) continue;

                int distance = Math.abs(player.x - enemy.x) + Math.abs(player.y - enemy.y);

                if (distance <= attackRange) {
                    isHidden = false;
                    // Отнимаем жизнь у игрока (с кулдауном)
                    if (currentTime - enemy.lastAttackTime >= ATTACK_COOLDOWN) {
                        enemy.lastAttackTime = currentTime;
                        player.loseLife();
                    }
                    return;
                }
            }

            // Если никто не в радиусе, возможно прячется или медленно движется
            if (!isHidden && Math.random() < 0.2) {
                RandomStrategy randomMove = new RandomStrategy();
                randomMove.updateEnemy(enemy, map, players, currentLevel);
            }
        }
    }

    // Стратегия патрулирования по маршруту (упрощенная)
    public static class PatrolStrategy implements AIStrategy {
        private List<int[]> patrolPoints;
        private int currentPatrolIndex = 0;
        private boolean reverse = false;

        public PatrolStrategy(int startX, int startY) {
            patrolPoints = new ArrayList<>();
            // Создаем квадратный маршрут патрулирования
            patrolPoints.add(new int[]{startX - 3, startY});
            patrolPoints.add(new int[]{startX, startY - 3});
            patrolPoints.add(new int[]{startX + 3, startY});
            patrolPoints.add(new int[]{startX, startY + 3});
        }

        @Override
        public void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel) {
            if (enemy.type.speed == 0 || !enemy.isActive) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - enemy.lastMoveTime < 1000 / enemy.type.speed) return;

            // Движемся к следующей точке патрулирования
            int[] targetPoint = patrolPoints.get(currentPatrolIndex);
            Direction moveDir = calculateDirectionToTarget(enemy.x, enemy.y, targetPoint[0], targetPoint[1]);

            if (moveDir != null) {
                int newX = enemy.x + moveDir.dx;
                int newY = enemy.y + moveDir.dy;

                if (isValidPosition(newX, newY, map)) {
                    enemy.x = newX;
                    enemy.y = newY;
                    enemy.direction = moveDir;
                }
            }

            // Проверяем, достигли ли точки
            if (Math.abs(enemy.x - targetPoint[0]) <= 1 && Math.abs(enemy.y - targetPoint[1]) <= 1) {
                if (reverse) {
                    currentPatrolIndex--;
                    if (currentPatrolIndex < 0) {
                        currentPatrolIndex = 1;
                        reverse = false;
                    }
                } else {
                    currentPatrolIndex++;
                    if (currentPatrolIndex >= patrolPoints.size()) {
                        currentPatrolIndex = patrolPoints.size() - 2;
                        reverse = true;
                    }
                }
            }

            enemy.lastMoveTime = currentTime;
        }

        private Direction calculateDirectionToTarget(int fromX, int fromY, int toX, int toY) {
            int dx = Integer.compare(toX, fromX);
            int dy = Integer.compare(toY, fromY);

            if (dx != 0) {
                return dx > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            }
        }
    }

    // Стратегия для боссов (упрощенная)
    public static class BossStrategy implements AIStrategy {
        private static final long ATTACK_COOLDOWN = 1000;  // Кулдаун атаки 1 секунда
        private int attackCooldownCounter = 0;

        @Override
        public void updateEnemy(Enemy enemy, TileType[][] map, List<PlayerState> players, int currentLevel) {
            if (!enemy.isActive) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - enemy.lastMoveTime < 1000 / 2) return;

            // Ищем всех игроков в большом радиусе
            int detectionRange = 8;
            List<PlayerState> targets = new ArrayList<>();

            for (PlayerState player : players) {
                if (player.lives <= 0) continue;

                int distance = Math.abs(player.x - enemy.x) + Math.abs(player.y - enemy.y);
                if (distance <= detectionRange) {
                    targets.add(player);
                }
            }

            if (!targets.isEmpty()) {
                // Выбираем игрока с наименьшим количеством жизней
                PlayerState target = Collections.min(targets,
                        Comparator.comparingInt(p -> p.lives));

                // Просто двигаемся к цели (без сложного moveTowards)
                Direction moveDir = calculateDirectionToTarget(enemy.x, enemy.y, target.x, target.y);
                if (moveDir != null) {
                    int newX = enemy.x + moveDir.dx;
                    int newY = enemy.y + moveDir.dy;

                    if (isValidPosition(newX, newY, map)) {
                        enemy.x = newX;
                        enemy.y = newY;
                        enemy.direction = moveDir;

                        // Если рядом - отнимаем жизнь (с кулдауном)
                        if (Math.abs(enemy.x - target.x) <= 2 && Math.abs(enemy.y - target.y) <= 2) {
                            if (currentTime - enemy.lastAttackTime >= ATTACK_COOLDOWN) {
                                enemy.lastAttackTime = currentTime;
                                target.loseLife();
                            }
                        }
                    }
                }
            } else {
                // Патрулируем
                RandomStrategy randomMove = new RandomStrategy();
                randomMove.updateEnemy(enemy, map, players, currentLevel);
            }

            enemy.lastMoveTime = currentTime;
        }

        private Direction calculateDirectionToTarget(int fromX, int fromY, int toX, int toY) {
            int dx = Integer.compare(toX, fromX);
            int dy = Integer.compare(toY, fromY);

            if (Math.abs(toX - fromX) > Math.abs(toY - fromY)) {
                return dx > 0 ? Direction.RIGHT : Direction.LEFT;
            } else {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            }
        }
    }

    // Фабрика для создания стратегий
    public static AIStrategy createStrategy(Enemy.EnemyType type, int currentLevel, String aiType) {
        if (aiType != null) {
            switch (aiType) {
                case "RANDOM": return new RandomStrategy();
                case "CHASE": return new ChaseStrategy();
                case "AMBUSH": return new AmbushStrategy();
                case "PATROL": return new PatrolStrategy(0, 0);
                case "BOSS": return new BossStrategy();
            }
        }

        // Автоматический выбор по умолчанию (только для существующих типов)
        switch (type) {
            case BAT:
                if (currentLevel < 3) return new RandomStrategy();
                else if (currentLevel < 6) return new ChaseStrategy();
                else return new AmbushStrategy();

            case SKELETON:
                if (currentLevel < 4) return new ChaseStrategy();
                else return new PatrolStrategy(0, 0);

            case GHOST:
                return new ChaseStrategy();

            case TRAP:
                return new AmbushStrategy();

            default:
                return new RandomStrategy();
        }
    }

    public static AIStrategy createStrategy(Enemy.EnemyType type, int currentLevel) {
        return createStrategy(type, currentLevel, null);
    }

    private static boolean isValidPosition(int x, int y, TileType[][] map) {
        if (x < 0 || x >= map[0].length || y < 0 || y >= map.length) {
            return false;
        }
        return map[y][x].isWalkable();
    }
}