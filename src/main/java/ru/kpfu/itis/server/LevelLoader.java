package ru.kpfu.itis.server;

import ru.kpfu.itis.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class LevelLoader {
    private static final Logger logger = LoggerFactory.getLogger(LevelLoader.class);

    public static class GeneratedLevel {
        public TileType[][] map;
        public List<int[]> startPositions;
        public List<Enemy> enemies;
        public List<PatrolEnemy> patrolEnemies;
        public List<Trap> traps;
        public int totalDiamonds;

        public GeneratedLevel(int width, int height) {
            map = new TileType[height][width];
            startPositions = new ArrayList<>();
            enemies = new ArrayList<>();
            patrolEnemies = new ArrayList<>();
            traps = new ArrayList<>();
            totalDiamonds = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    map[y][x] = TileType.FLOOR;
                }
            }
        }
    }

    public static GeneratedLevel loadLevel(int levelNum) {
        String[] possiblePaths = {
                "levels/level" + levelNum + ".txt",
                "src/main/resources/levels/level" + levelNum + ".txt",
                "resources/levels/level" + levelNum + ".txt"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    reader.close();
                    GeneratedLevel level = parseLevel(lines);
                    if (level != null) {
                        logger.info("Уровень {} успешно загружен. Размер: {}x{}", levelNum, level.map[0].length, level.map.length);
                        return level;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка чтения файла {}", path, e);
                }
            }
        }

        logger.warn("Уровень {} не найден, создаем простой уровень...", levelNum);
        return createSimpleLevel();
    }

    private static GeneratedLevel parseLevel(List<String> lines) {
        if (lines.isEmpty()) {
            logger.error("Файл уровня пуст");
            return null;
        }

        lines.removeIf(String::isEmpty);

        if (lines.isEmpty()) {
            logger.error("Нет данных в файле уровня");
            return null;
        }

        int height = lines.size();
        int width = 0;

        for (String line : lines) {
            if (line.length() > width) {
                width = line.length();
            }
        }

        if (width == 0 || height == 0) {
            logger.error("Неверные размеры уровня: {}x{}", width, height);
            return null;
        }

        logger.debug("Размер уровня: {}x{}", width, height);
        GeneratedLevel level = new GeneratedLevel(width, height);
        int enemyId = 1;

        for (int y = 0; y < height; y++) {
            String line = lines.get(y);
            for (int x = 0; x < width; x++) {
                char c = (x < line.length()) ? line.charAt(x) : ' ';

                switch (c) {
                    case '#':
                        level.map[y][x] = TileType.WALL;
                        break;
                    case '@':
                        level.map[y][x] = TileType.FLOOR;
                        level.startPositions.add(new int[]{x, y});
                        logger.debug("Стартовая позиция @: {},{}", x, y);
                        break;
                    case '$':
                        level.map[y][x] = TileType.DIAMOND;
                        level.totalDiamonds++;
                        break;
                    case 'D':
                        level.map[y][x] = TileType.DOOR;
                        logger.debug("Дверь D: {},{}", x, y);
                        break;
                    case 'T':
                        level.map[y][x] = TileType.TRAP;
                        break;
                    case 'C':
                        level.map[y][x] = TileType.CHEST;
                        break;
                    case 'B':
                        level.map[y][x] = TileType.BUTTON;
                        break;
                    case 'E':
                        level.map[y][x] = TileType.FLOOR;
                        level.enemies.add(new Enemy(enemyId++, Enemy.EnemyType.SKELETON, x, y));
                        logger.debug("Враг E (Скелет): {},{}", x, y);
                        break;
                    case 'G':
                        level.map[y][x] = TileType.FLOOR;
                        level.enemies.add(new Enemy(enemyId++, Enemy.EnemyType.GHOST, x, y));
                        logger.debug("Враг G (Призрак): {},{}", x, y);
                        break;
                    case 'F':
                        level.map[y][x] = TileType.FLOOR;
                        level.enemies.add(new Enemy(enemyId++, Enemy.EnemyType.BAT, x, y));
                        logger.debug("Враг F (Летучая мышь): {},{}", x, y);
                        break;

                    case 'P':
                        level.map[y][x] = TileType.FLOOR;
                        PatrolEnemy patrolH = new PatrolEnemy();
                        patrolH.x = x;
                        patrolH.y = y;
                        patrolH.axis = PatrolAxis.HORIZONTAL;
                        patrolH.direction = PatrolDirection.POSITIVE;
                        patrolH.lastMoveTime = System.currentTimeMillis();
                        level.patrolEnemies.add(patrolH);
                        logger.debug("Патрульный моб P (горизонтально): {},{}", x, y);
                        break;
                    case 'p':
                        level.map[y][x] = TileType.FLOOR;
                        PatrolEnemy patrolV = new PatrolEnemy();
                        patrolV.x = x;
                        patrolV.y = y;
                        patrolV.axis = PatrolAxis.VERTICAL;
                        patrolV.direction = PatrolDirection.POSITIVE;
                        patrolV.lastMoveTime = System.currentTimeMillis();
                        level.patrolEnemies.add(patrolV);
                        logger.debug("Патрульный моб p (вертикально): {},{}", x, y);
                        break;

                    case '<':
                        level.map[y][x] = TileType.WALL;  
                        level.traps.add(new Trap(x, y, TrapType.PRESSURE, TrapAttack.ARROW, Direction.LEFT));
                        logger.debug("Ловушка < (стрела влево): {},{}", x, y);
                        break;
                    case '>':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.PRESSURE, TrapAttack.ARROW, Direction.RIGHT));
                        logger.debug("Ловушка > (стрела вправо): {},{}", x, y);
                        break;
                    case '^':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.PRESSURE, TrapAttack.ARROW, Direction.UP));
                        logger.debug("Ловушка ^ (стрела вверх): {},{}", x, y);
                        break;
                    case 'v':
                    case 'V':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.PRESSURE, TrapAttack.ARROW, Direction.DOWN));
                        logger.debug("Ловушка v (стрела вниз): {},{}", x, y);
                        break;

                    case '[':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.TIMER, TrapAttack.FIRE, Direction.LEFT));
                        logger.debug("Ловушка [ (пламя влево): {},{}", x, y);
                        break;
                    case ']':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.TIMER, TrapAttack.FIRE, Direction.RIGHT));
                        logger.debug("Ловушка ] (пламя вправо): {},{}", x, y);
                        break;
                    case '{':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.TIMER, TrapAttack.FIRE, Direction.UP));
                        logger.debug("Ловушка {{ (пламя вверх): {},{}", x, y);
                        break;
                    case '}':
                        level.map[y][x] = TileType.WALL;
                        level.traps.add(new Trap(x, y, TrapType.TIMER, TrapAttack.FIRE, Direction.DOWN));
                        logger.debug("Ловушка }} (пламя вниз): {},{}", x, y);
                        break;
                    case ' ':
                    case '.':
                    default:
                        level.map[y][x] = TileType.FLOOR;
                }
            }
        }

        logger.info("Уровень содержит: {} алмазов, {} врагов, {} патрульных мобов, {} ловушек, {} стартовых позиций",
                level.totalDiamonds, level.enemies.size(), level.patrolEnemies.size(),
                level.traps.size(), level.startPositions.size());

        return level;
    }

    private static GeneratedLevel createSimpleLevel() {
        int width = 20;
        int height = 20;

        GeneratedLevel level = new GeneratedLevel(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    level.map[y][x] = TileType.WALL;
                }
            }
        }

        level.startPositions.add(new int[]{1, 1});
        level.startPositions.add(new int[]{width - 2, 1});
        level.startPositions.add(new int[]{1, height - 2});

        for (int x = 5; x < 15; x += 3) {
            for (int y = 5; y < 15; y += 3) {
                if (level.map[y][x] == TileType.FLOOR) {
                    level.map[y][x] = TileType.DIAMOND;
                    level.totalDiamonds++;
                }
            }
        }

        level.map[height - 2][width - 2] = TileType.DOOR;
        level.map[1][width - 2] = TileType.DOOR;

        level.enemies.add(new Enemy(1, Enemy.EnemyType.BAT, 5, 5));
        level.enemies.add(new Enemy(2, Enemy.EnemyType.SKELETON, width - 5, height - 5));

        return level;
    }

    public static void createDefaultLevels() {

    }
}