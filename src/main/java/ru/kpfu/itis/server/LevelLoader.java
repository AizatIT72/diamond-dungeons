package ru.kpfu.itis.server;

import ru.kpfu.itis.common.TileType;
import ru.kpfu.itis.common.Enemy;

import java.io.*;
import java.util.*;

public class LevelLoader {

    public static class GeneratedLevel {
        public TileType[][] map;
        public List<int[]> startPositions;
        public List<Enemy> enemies;
        public int totalDiamonds;

        public GeneratedLevel(int width, int height) {
            map = new TileType[height][width];
            startPositions = new ArrayList<>();
            enemies = new ArrayList<>();
            totalDiamonds = 0;

            // Инициализируем всю карту полом по умолчанию
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
                    System.out.println("Загружаем уровень из: " + file.getAbsolutePath());
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    reader.close();
                    GeneratedLevel level = parseLevel(lines);
                    if (level != null) {
                        System.out.println("Уровень " + levelNum + " успешно загружен. Размер: " +
                                level.map[0].length + "x" + level.map.length);
                        return level;
                    }
                } catch (IOException e) {
                    System.err.println("Ошибка чтения файла " + path + ": " + e.getMessage());
                }
            }
        }

        // Если файл не найден, создаем простой уровень
        System.out.println("Уровень " + levelNum + " не найден, создаем простой уровень...");
        return createSimpleLevel();
    }

    private static GeneratedLevel parseLevel(List<String> lines) {
        if (lines.isEmpty()) {
            System.err.println("Файл уровня пуст");
            return null;
        }

        // Удаляем пустые строки
        lines.removeIf(String::isEmpty);

        if (lines.isEmpty()) {
            System.err.println("Нет данных в файле уровня");
            return null;
        }

        int height = lines.size();
        int width = 0;

        // Находим максимальную ширину
        for (String line : lines) {
            if (line.length() > width) {
                width = line.length();
            }
        }

        if (width == 0 || height == 0) {
            System.err.println("Неверные размеры уровня: " + width + "x" + height);
            return null;
        }

        System.out.println("Размер уровня: " + width + "x" + height);

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
                        System.out.println("Стартовая позиция @: " + x + "," + y);
                        break;
                    case '$':
                        level.map[y][x] = TileType.DIAMOND;
                        level.totalDiamonds++;
                        break;
                    case 'D':
                        level.map[y][x] = TileType.DOOR;
                        System.out.println("Дверь D: " + x + "," + y);
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
                        System.out.println("Враг E (Скелет): " + x + "," + y);
                        break;
                    case 'G':
                        level.map[y][x] = TileType.FLOOR;
                        level.enemies.add(new Enemy(enemyId++, Enemy.EnemyType.GHOST, x, y));
                        System.out.println("Враг G (Призрак): " + x + "," + y);
                        break;
                    case 'F':
                        level.map[y][x] = TileType.FLOOR;
                        level.enemies.add(new Enemy(enemyId++, Enemy.EnemyType.BAT, x, y));
                        System.out.println("Враг F (Летучая мышь): " + x + "," + y);
                        break;
                    case ' ':
                    case '.':
                    default:
                        level.map[y][x] = TileType.FLOOR;
                }
            }
        }

        System.out.println("Уровень содержит: " + level.totalDiamonds + " алмазов, " +
                level.enemies.size() + " врагов, " +
                level.startPositions.size() + " стартовых позиций");

        return level;
    }

    private static GeneratedLevel createSimpleLevel() {
        int width = 20;
        int height = 20;

        GeneratedLevel level = new GeneratedLevel(width, height);

        // Границы
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    level.map[y][x] = TileType.WALL;
                }
            }
        }

        // Стартовые позиции
        level.startPositions.add(new int[]{1, 1});
        level.startPositions.add(new int[]{width - 2, 1});
        level.startPositions.add(new int[]{1, height - 2});

        // Алмазы
        for (int x = 5; x < 15; x += 3) {
            for (int y = 5; y < 15; y += 3) {
                if (level.map[y][x] == TileType.FLOOR) {
                    level.map[y][x] = TileType.DIAMOND;
                    level.totalDiamonds++;
                }
            }
        }

        // Выходы
        level.map[height - 2][width - 2] = TileType.DOOR;
        level.map[1][width - 2] = TileType.DOOR;

        // Несколько врагов
        level.enemies.add(new Enemy(1, Enemy.EnemyType.BAT, 5, 5));
        level.enemies.add(new Enemy(2, Enemy.EnemyType.SKELETON, width - 5, height - 5));

        return level;
    }

    // Убираем автоматическое создание файлов
    public static void createDefaultLevels() {
        // Ничего не делаем
    }
}