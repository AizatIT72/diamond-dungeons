package ru.kpfu.itis.client;

import ru.kpfu.itis.common.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class SpriteManager {
    private static SpriteManager instance;
    
    // Спрайты для тайлов
    private Map<TileType, BufferedImage> tileSprites;
    
    // Спрайты для персонажей (3 типа, 4 направления, несколько кадров анимации)
    private Map<String, BufferedImage[][]> playerSprites; // [characterType][direction][frame]
    
    // Спрайты для мобов
    private Map<Enemy.EnemyType, BufferedImage[][]> enemySprites; // [direction][frame]
    private BufferedImage[][][] patrolEnemySprites; // [axis][direction][frame]
    
    // Спрайты для ловушек
    private Map<TrapAttack, Map<Direction, BufferedImage>> trapSprites;
    
    // Размер спрайта
    private static final int SPRITE_SIZE = 32;
    private static final int ANIMATION_FRAMES = 4;
    
    private SpriteManager() {
        tileSprites = new HashMap<>();
        playerSprites = new HashMap<>();
        enemySprites = new HashMap<>();
        trapSprites = new HashMap<>();
        patrolEnemySprites = new BufferedImage[2][2][ANIMATION_FRAMES];
        loadAllSprites();
    }
    
    public static SpriteManager getInstance() {
        if (instance == null) {
            instance = new SpriteManager();
        }
        return instance;
    }
    
    private void loadAllSprites() {
        try {
            loadTileSprites();
            loadPlayerSprites();
            loadEnemySprites();
            loadPatrolEnemySprites();
            loadTrapSprites();
        } catch (Exception e) {
            System.err.println("Ошибка загрузки спрайтов: " + e.getMessage());
            e.printStackTrace();
            // Инициализируем пустые спрайты, чтобы избежать NullPointerException
            if (tileSprites == null) tileSprites = new HashMap<>();
            if (playerSprites == null) playerSprites = new HashMap<>();
            if (enemySprites == null) enemySprites = new HashMap<>();
            if (trapSprites == null) trapSprites = new HashMap<>();
            if (patrolEnemySprites == null) patrolEnemySprites = new BufferedImage[2][2][ANIMATION_FRAMES];
        }
    }
    
    private void loadTileSprites() {
        // Пытаемся загрузить из файлов, если не получается - используем программную генерацию
        // Пробуем разные тайлы для стены (светлые)
        BufferedImage wall = null;
        String[] wallTiles = {"sprites/tiles/Tile_03.png", "sprites/tiles/Tile_05.png", 
                              "sprites/tiles/Tile_07.png", "sprites/tiles/Tile_11.png"};
        for (String path : wallTiles) {
            wall = SpriteSheetLoader.loadImage(path);
            if (wall != null) {
                // Делаем тайл светлее, если он темный
                wall = adjustImageBrightness(wall, 1.5f);
                break;
            }
        }
        if (wall != null) {
            tileSprites.put(TileType.WALL, wall);
        } else {
            tileSprites.put(TileType.WALL, createWallSprite());
        }
        
        // Пробуем разные тайлы для пола (темные)
        BufferedImage floor = null;
        String[] floorTiles = {"sprites/tiles/Tile_04.png", "sprites/tiles/Tile_06.png", 
                               "sprites/tiles/Tile_09.png", "sprites/tiles/Tile_13.png"};
        for (String path : floorTiles) {
            floor = SpriteSheetLoader.loadImage(path);
            if (floor != null) {
                // Делаем тайл темнее, если он светлый
                floor = adjustImageBrightness(floor, 0.6f);
                break;
            }
        }
        if (floor != null) {
            tileSprites.put(TileType.FLOOR, floor);
        } else {
            tileSprites.put(TileType.FLOOR, createFloorSprite());
        }
        
        // Алмаз - используем объект из папки objects или создаем программно
        BufferedImage diamond = SpriteSheetLoader.loadImage("sprites/objects/Other/1.png");
        if (diamond != null) {
            tileSprites.put(TileType.DIAMOND, diamond);
        } else {
            tileSprites.put(TileType.DIAMOND, createDiamondSprite());
        }
        
        // Дверь
        BufferedImage door = SpriteSheetLoader.loadImage("sprites/objects/Doors/1.png");
        if (door != null) {
            tileSprites.put(TileType.DOOR, door);
        } else {
            tileSprites.put(TileType.DOOR, createDoorSprite());
        }
        
        // Старт
        tileSprites.put(TileType.START, createStartSprite());
    }
    
    private void loadPlayerSprites() {
        // Маппинг: Красный -> папка 1, Синий -> папка 2, Зеленый -> папка 3
        String[] characterTypes = {"Красный", "Синий", "Зеленый"};
        int[] folderNumbers = {1, 2, 3};
        
        Direction[] directions = Direction.values();
        String[] directionPrefixes = {"D_", "S_", "U_", "S_"}; // DOWN, LEFT, UP, RIGHT (S используется для LEFT и RIGHT)
        
        for (int i = 0; i < characterTypes.length; i++) {
            String charType = characterTypes[i];
            int folderNum = folderNumbers[i];
            
            BufferedImage[][] sprites = new BufferedImage[directions.length][ANIMATION_FRAMES];
            boolean loadedFromFile = false;
            
            // Пытаемся загрузить из файлов
            for (int dir = 0; dir < directions.length; dir++) {
                String prefix = directionPrefixes[dir];
                String walkPath = String.format("sprites/players/%d/%sWalk.png", folderNum, prefix);
                
                BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
                if (walkFrames != null && walkFrames.length > 0) {
                    loadedFromFile = true;
                    // Используем кадры ходьбы для анимации
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        BufferedImage frameImg = walkFrames[frame];
                        // Для направления RIGHT отражаем спрайт S_ по горизонтали
                        if (dir == 3 && prefix.equals("S_")) { // RIGHT
                            frameImg = flipImageHorizontally(frameImg);
                        }
                        sprites[dir][frame] = frameImg;
                    }
                    // Если кадров меньше, дублируем последний
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            BufferedImage lastFrame = walkFrames[walkFrames.length - 1];
                            if (dir == 3 && prefix.equals("S_")) {
                                lastFrame = flipImageHorizontally(lastFrame);
                            }
                            sprites[dir][frame] = lastFrame;
                        }
                    }
                } else {
                    // Fallback на программную генерацию
                    for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                        sprites[dir][frame] = createPlayerSprite(charType, directions[dir], frame);
                    }
                }
            }
            
            if (loadedFromFile) {
                System.out.println("Загружены спрайты персонажа: " + charType);
            }
            
            playerSprites.put(charType, sprites);
        }
    }
    
    private void loadEnemySprites() {
        // Маппинг: SKELETON -> папка 1, BAT -> папка 2, GHOST -> папка 3
        Enemy.EnemyType[] types = {Enemy.EnemyType.SKELETON, Enemy.EnemyType.BAT, Enemy.EnemyType.GHOST};
        int[] folderNumbers = {1, 2, 3};
        
        Direction[] directions = Direction.values();
        String[] directionPrefixes = {"D_", "S_", "U_", "S_"}; // DOWN, LEFT, UP, RIGHT
        
        for (int i = 0; i < types.length; i++) {
            Enemy.EnemyType type = types[i];
            if (type == Enemy.EnemyType.TRAP) continue;
            
            int folderNum = folderNumbers[i];
            BufferedImage[][] sprites = new BufferedImage[directions.length][ANIMATION_FRAMES];
            boolean loadedFromFile = false;
            
            // Пытаемся загрузить из файлов
            for (int dir = 0; dir < directions.length; dir++) {
                String prefix = directionPrefixes[dir];
                String walkPath = String.format("sprites/enemies/%d/%sWalk.png", folderNum, prefix);
                
                BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
                if (walkFrames != null && walkFrames.length > 0) {
                    loadedFromFile = true;
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        BufferedImage frameImg = walkFrames[frame];
                        // Для направления RIGHT отражаем спрайт S_ по горизонтали
                        if (dir == 3 && prefix.equals("S_")) { // RIGHT
                            frameImg = flipImageHorizontally(frameImg);
                        }
                        sprites[dir][frame] = frameImg;
                    }
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            BufferedImage lastFrame = walkFrames[walkFrames.length - 1];
                            if (dir == 3 && prefix.equals("S_")) {
                                lastFrame = flipImageHorizontally(lastFrame);
                            }
                            sprites[dir][frame] = lastFrame;
                        }
                    }
                } else {
                    // Fallback на программную генерацию
                    for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                        sprites[dir][frame] = createEnemySprite(type, directions[dir], frame);
                    }
                }
            }
            
            if (loadedFromFile) {
                System.out.println("Загружены спрайты врага: " + type);
            }
            
            enemySprites.put(type, sprites);
        }
    }
    
    private void loadPatrolEnemySprites() {
        // Патрульные мобы: [axis][direction][frame]
        // axis: 0=horizontal, 1=vertical
        // direction: 0=positive, 1=negative
        patrolEnemySprites = new BufferedImage[2][2][ANIMATION_FRAMES];
        
        // Используем врага из папки 4 для патрульных мобов
        String[] axisDirs = {"S_", "D_"}; // S для горизонтального, D для вертикального
        
        for (int axis = 0; axis < 2; axis++) {
            String prefix = axisDirs[axis];
            String walkPath = String.format("sprites/enemies/4/%sWalk.png", prefix);
            
            BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
            boolean loadedFromFile = false;
            
            if (walkFrames != null && walkFrames.length > 0) {
                loadedFromFile = true;
                // Для обоих направлений используем те же кадры
                for (int dir = 0; dir < 2; dir++) {
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        patrolEnemySprites[axis][dir][frame] = walkFrames[frame];
                    }
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            patrolEnemySprites[axis][dir][frame] = walkFrames[walkFrames.length - 1];
                        }
                    }
                }
            }
            
            if (!loadedFromFile) {
                // Fallback на программную генерацию
                for (int dir = 0; dir < 2; dir++) {
                    for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                        patrolEnemySprites[axis][dir][frame] = createPatrolEnemySprite(axis == 0, dir == 0, frame);
                    }
                }
            }
        }
    }
    
    private void loadTrapSprites() {
        // Стрелы
        Map<Direction, BufferedImage> arrowSprites = new HashMap<>();
        for (Direction dir : Direction.values()) {
            arrowSprites.put(dir, createArrowTrapSprite(dir));
        }
        trapSprites.put(TrapAttack.ARROW, arrowSprites);
        
        // Пламя
        Map<Direction, BufferedImage> fireSprites = new HashMap<>();
        for (Direction dir : Direction.values()) {
            fireSprites.put(dir, createFireTrapSprite(dir));
        }
        trapSprites.put(TrapAttack.FIRE, fireSprites);
    }
    
    // Создание спрайтов программно
    private BufferedImage createWallSprite() {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Светлая стена (белый/светло-серый)
            g.setColor(new Color(240, 240, 240)); // Светло-серый, почти белый
            g.fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE);
            
            // Текстура для объема
            g.setColor(new Color(255, 255, 255)); // Белый для бликов
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if ((i + j) % 2 == 0) {
                        g.fillRect(i * 8, j * 8, 8, 8);
                    }
                }
            }
            
            // Тени для глубины
            g.setColor(new Color(220, 220, 220)); // Немного темнее для теней
            g.fillRect(0, 0, SPRITE_SIZE, 2); // Верхняя тень
            g.fillRect(0, 0, 2, SPRITE_SIZE); // Левая тень
            
            // Обводка
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(1));
            g.drawRect(0, 0, SPRITE_SIZE - 1, SPRITE_SIZE - 1);
            
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта стены: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createFloorSprite() {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Темный пол
            g.setColor(new Color(50, 50, 50)); // Темно-серый
            g.fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE);
            
            // Текстура для детализации
            g.setColor(new Color(40, 40, 40)); // Еще темнее для вариации
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if ((i + j) % 2 == 0) {
                        g.fillRect(i * 8, j * 8, 8, 8);
                    }
                }
            }
            
            // Легкие блики для текстуры
            g.setColor(new Color(60, 60, 60)); // Немного светлее для бликов
            for (int i = 0; i < 8; i += 2) {
                for (int j = 0; j < 8; j += 2) {
                    if ((i + j) % 4 == 0) {
                        g.fillRect(i * 4, j * 4, 2, 2);
                    }
                }
            }
            
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта пола: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createDiamondSprite() {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Алмаз
        int[] xPoints = {SPRITE_SIZE/2, SPRITE_SIZE*3/4, SPRITE_SIZE/2, SPRITE_SIZE/4};
        int[] yPoints = {SPRITE_SIZE/4, SPRITE_SIZE/2, SPRITE_SIZE*3/4, SPRITE_SIZE/2};
        
        // Градиент для алмаза
        GradientPaint gradient = new GradientPaint(
            SPRITE_SIZE/4, SPRITE_SIZE/4, new Color(100, 200, 255),
            SPRITE_SIZE*3/4, SPRITE_SIZE*3/4, new Color(50, 150, 255)
        );
        g.setPaint(gradient);
        g.fillPolygon(xPoints, yPoints, 4);
        
        // Блик
        g.setColor(new Color(255, 255, 255, 200));
        g.fillPolygon(new int[]{SPRITE_SIZE/2, SPRITE_SIZE*5/8, SPRITE_SIZE/2, SPRITE_SIZE*3/8},
                     new int[]{SPRITE_SIZE/3, SPRITE_SIZE/2, SPRITE_SIZE*2/3, SPRITE_SIZE/2}, 4);
        
        // Обводка
        g.setColor(new Color(0, 100, 200));
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(xPoints, yPoints, 4);
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта алмаза: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createDoorSprite() {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Дверь
        g.setColor(new Color(100, 70, 40));
        g.fillRect(SPRITE_SIZE/4, 0, SPRITE_SIZE/2, SPRITE_SIZE);
        
        // Детали двери
        g.setColor(new Color(80, 50, 30));
        g.setStroke(new BasicStroke(2));
        g.drawRect(SPRITE_SIZE/4, 0, SPRITE_SIZE/2, SPRITE_SIZE);
        g.drawLine(SPRITE_SIZE/2, 0, SPRITE_SIZE/2, SPRITE_SIZE);
        
        // Ручка
        g.setColor(new Color(200, 150, 100));
        g.fillOval(SPRITE_SIZE*3/4 - 4, SPRITE_SIZE/2 - 4, 8, 8);
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта двери: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createStartSprite() {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Стартовая точка (зеленый круг)
        g.setColor(new Color(100, 255, 100, 150));
        g.fillOval(SPRITE_SIZE/4, SPRITE_SIZE/4, SPRITE_SIZE/2, SPRITE_SIZE/2);
        
        g.setColor(new Color(50, 200, 50));
        g.setStroke(new BasicStroke(2));
        g.drawOval(SPRITE_SIZE/4, SPRITE_SIZE/4, SPRITE_SIZE/2, SPRITE_SIZE/2);
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта старта: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createPlayerSprite(String characterType, Direction direction, int frame) {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color playerColor;
        switch (characterType) {
            case "Красный":
                playerColor = new Color(200, 50, 50);
                break;
            case "Синий":
                playerColor = new Color(50, 100, 200);
                break;
            case "Зеленый":
                playerColor = new Color(50, 200, 50);
                break;
            default:
                playerColor = Color.GRAY;
        }
        
        // Тело персонажа
        int bodyY = SPRITE_SIZE/2;
        int bodySize = SPRITE_SIZE*3/4;
        
        // Анимация ходьбы (смещение по Y)
        int walkOffset = (int)(Math.sin(frame * Math.PI / 2) * 2);
        
        // Голова
        g.setColor(playerColor);
        g.fillOval(SPRITE_SIZE/4, SPRITE_SIZE/4 + walkOffset, SPRITE_SIZE/2, SPRITE_SIZE/2);
        
        // Тело
        g.fillRect(SPRITE_SIZE/3, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/3, SPRITE_SIZE/3);
        
        // Руки (в зависимости от направления)
        int armOffset = frame % 2 == 0 ? 2 : -2;
        switch (direction) {
            case LEFT:
                g.fillRect(SPRITE_SIZE/4, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                g.fillRect(SPRITE_SIZE*2/3, SPRITE_SIZE/2 + walkOffset + armOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                break;
            case RIGHT:
                g.fillRect(SPRITE_SIZE*2/3, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                g.fillRect(SPRITE_SIZE/4, SPRITE_SIZE/2 + walkOffset + armOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                break;
            case UP:
                g.fillRect(SPRITE_SIZE/4, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                g.fillRect(SPRITE_SIZE*2/3, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                break;
            case DOWN:
                g.fillRect(SPRITE_SIZE/4, SPRITE_SIZE*2/3 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                g.fillRect(SPRITE_SIZE*2/3, SPRITE_SIZE*2/3 + walkOffset, SPRITE_SIZE/6, SPRITE_SIZE/4);
                break;
        }
        
        // Ноги (анимация)
        g.setColor(new Color(playerColor.getRed()/2, playerColor.getGreen()/2, playerColor.getBlue()/2));
        if (frame % 2 == 0) {
            g.fillRect(SPRITE_SIZE/3, SPRITE_SIZE*5/6 + walkOffset, SPRITE_SIZE/8, SPRITE_SIZE/6);
            g.fillRect(SPRITE_SIZE*5/8, SPRITE_SIZE*5/6 + walkOffset, SPRITE_SIZE/8, SPRITE_SIZE/6);
        } else {
            g.fillRect(SPRITE_SIZE/3, SPRITE_SIZE*5/6 + walkOffset + 2, SPRITE_SIZE/8, SPRITE_SIZE/6);
            g.fillRect(SPRITE_SIZE*5/8, SPRITE_SIZE*5/6 + walkOffset - 2, SPRITE_SIZE/8, SPRITE_SIZE/6);
        }
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта персонажа: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createEnemySprite(Enemy.EnemyType type, Direction direction, int frame) {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color enemyColor;
        switch (type) {
            case BAT:
                enemyColor = new Color(100, 50, 150);
                break;
            case SKELETON:
                enemyColor = new Color(200, 200, 200);
                break;
            case GHOST:
                enemyColor = new Color(100, 200, 200);
                break;
            default:
                enemyColor = Color.GRAY;
        }
        
        int walkOffset = (int)(Math.sin(frame * Math.PI / 2) * 2);
        
        if (type == Enemy.EnemyType.GHOST) {
            // Призрак - полупрозрачный овал
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.setColor(enemyColor);
            g.fillOval(SPRITE_SIZE/4, SPRITE_SIZE/4 + walkOffset, SPRITE_SIZE/2, SPRITE_SIZE/2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else if (type == Enemy.EnemyType.BAT) {
            // Летучая мышь
            g.setColor(enemyColor);
            // Тело
            g.fillOval(SPRITE_SIZE/3, SPRITE_SIZE/3 + walkOffset, SPRITE_SIZE/3, SPRITE_SIZE/3);
            // Крылья (анимация)
            int wingOffset = (int)(Math.sin(frame * Math.PI) * 4);
            g.fillOval(SPRITE_SIZE/6, SPRITE_SIZE/3 + walkOffset, SPRITE_SIZE/4, SPRITE_SIZE/3);
            g.fillOval(SPRITE_SIZE*2/3, SPRITE_SIZE/3 + walkOffset, SPRITE_SIZE/4, SPRITE_SIZE/3);
        } else {
            // Скелет
            g.setColor(enemyColor);
            // Череп
            g.fillOval(SPRITE_SIZE/4, SPRITE_SIZE/4 + walkOffset, SPRITE_SIZE/2, SPRITE_SIZE/2);
            // Тело
            g.fillRect(SPRITE_SIZE/3, SPRITE_SIZE/2 + walkOffset, SPRITE_SIZE/3, SPRITE_SIZE/3);
            // Кости
            g.setColor(new Color(220, 220, 220));
            g.fillRect(SPRITE_SIZE/4, SPRITE_SIZE*2/3 + walkOffset, SPRITE_SIZE/8, SPRITE_SIZE/4);
            g.fillRect(SPRITE_SIZE*5/8, SPRITE_SIZE*2/3 + walkOffset, SPRITE_SIZE/8, SPRITE_SIZE/4);
        }
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта врага: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createPatrolEnemySprite(boolean horizontal, boolean positive, int frame) {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int walkOffset = (int)(Math.sin(frame * Math.PI / 2) * 2);
        
        // Патрульный моб - красный круг
        g.setColor(new Color(200, 50, 50));
        g.fillOval(SPRITE_SIZE/4, SPRITE_SIZE/4 + walkOffset, SPRITE_SIZE/2, SPRITE_SIZE/2);
        
        // Стрелка направления
        g.setColor(Color.YELLOW);
        g.setStroke(new BasicStroke(2));
        int centerX = SPRITE_SIZE/2;
        int centerY = SPRITE_SIZE/2 + walkOffset;
        
        if (horizontal) {
            if (positive) {
                g.drawLine(centerX, centerY, centerX + SPRITE_SIZE/4, centerY);
                g.drawLine(centerX + SPRITE_SIZE/4, centerY, centerX + SPRITE_SIZE/6, centerY - SPRITE_SIZE/8);
                g.drawLine(centerX + SPRITE_SIZE/4, centerY, centerX + SPRITE_SIZE/6, centerY + SPRITE_SIZE/8);
            } else {
                g.drawLine(centerX, centerY, centerX - SPRITE_SIZE/4, centerY);
                g.drawLine(centerX - SPRITE_SIZE/4, centerY, centerX - SPRITE_SIZE/6, centerY - SPRITE_SIZE/8);
                g.drawLine(centerX - SPRITE_SIZE/4, centerY, centerX - SPRITE_SIZE/6, centerY + SPRITE_SIZE/8);
            }
        } else {
            if (positive) {
                g.drawLine(centerX, centerY, centerX, centerY + SPRITE_SIZE/4);
                g.drawLine(centerX, centerY + SPRITE_SIZE/4, centerX - SPRITE_SIZE/8, centerY + SPRITE_SIZE/6);
                g.drawLine(centerX, centerY + SPRITE_SIZE/4, centerX + SPRITE_SIZE/8, centerY + SPRITE_SIZE/6);
            } else {
                g.drawLine(centerX, centerY, centerX, centerY - SPRITE_SIZE/4);
                g.drawLine(centerX, centerY - SPRITE_SIZE/4, centerX - SPRITE_SIZE/8, centerY - SPRITE_SIZE/6);
                g.drawLine(centerX, centerY - SPRITE_SIZE/4, centerX + SPRITE_SIZE/8, centerY - SPRITE_SIZE/6);
            }
        }
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта патрульного моба: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createArrowTrapSprite(Direction direction) {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Стена с ловушкой
        g.setColor(new Color(80, 70, 60));
        g.fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE);
        
        // Стрела
        g.setColor(new Color(150, 150, 150));
        int centerX = SPRITE_SIZE/2;
        int centerY = SPRITE_SIZE/2;
        
        switch (direction) {
            case LEFT:
                g.fillRect(SPRITE_SIZE*3/4, centerY - 2, SPRITE_SIZE/4, 4);
                g.fillPolygon(
                    new int[]{SPRITE_SIZE*3/4, SPRITE_SIZE*3/4 - 4, SPRITE_SIZE*3/4},
                    new int[]{centerY - 4, centerY, centerY + 4}, 3
                );
                break;
            case RIGHT:
                g.fillRect(0, centerY - 2, SPRITE_SIZE/4, 4);
                g.fillPolygon(
                    new int[]{SPRITE_SIZE/4, SPRITE_SIZE/4 + 4, SPRITE_SIZE/4},
                    new int[]{centerY - 4, centerY, centerY + 4}, 3
                );
                break;
            case UP:
                g.fillRect(centerX - 2, SPRITE_SIZE*3/4, 4, SPRITE_SIZE/4);
                g.fillPolygon(
                    new int[]{centerX - 4, centerX, centerX + 4},
                    new int[]{SPRITE_SIZE*3/4, SPRITE_SIZE*3/4 - 4, SPRITE_SIZE*3/4}, 3
                );
                break;
            case DOWN:
                g.fillRect(centerX - 2, 0, 4, SPRITE_SIZE/4);
                g.fillPolygon(
                    new int[]{centerX - 4, centerX, centerX + 4},
                    new int[]{SPRITE_SIZE/4, SPRITE_SIZE/4 + 4, SPRITE_SIZE/4}, 3
                );
                break;
        }
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта ловушки-стрелы: " + e.getMessage());
            return null;
        }
    }
    
    private BufferedImage createFireTrapSprite(Direction direction) {
        try {
            BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            if (g == null) return null;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Стена с ловушкой
        g.setColor(new Color(80, 70, 60));
        g.fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE);
        
        // Пламя
        int centerX = SPRITE_SIZE/2;
        int centerY = SPRITE_SIZE/2;
        
        switch (direction) {
            case LEFT:
                g.setColor(new Color(255, 150, 0));
                g.fillOval(SPRITE_SIZE*3/4, centerY - 4, 8, 8);
                break;
            case RIGHT:
                g.setColor(new Color(255, 150, 0));
                g.fillOval(0, centerY - 4, 8, 8);
                break;
            case UP:
                g.setColor(new Color(255, 150, 0));
                g.fillOval(centerX - 4, SPRITE_SIZE*3/4, 8, 8);
                break;
            case DOWN:
                g.setColor(new Color(255, 150, 0));
                g.fillOval(centerX - 4, 0, 8, 8);
                break;
        }
        
            g.dispose();
            return img;
        } catch (Exception e) {
            System.err.println("Ошибка создания спрайта ловушки-пламени: " + e.getMessage());
            return null;
        }
    }
    
    // Геттеры для спрайтов
    public BufferedImage getTileSprite(TileType tile) {
        return tileSprites.getOrDefault(tile, createFloorSprite());
    }
    
    public BufferedImage getPlayerSprite(String characterType, Direction direction, int frame) {
        if (characterType == null || direction == null) {
            return null;
        }
        
        String key = characterType.contains("Красный") ? "Красный" :
                    characterType.contains("Синий") ? "Синий" :
                    characterType.contains("Зеленый") ? "Зеленый" : "Красный";
        
        BufferedImage[][] sprites = playerSprites.get(key);
        if (sprites == null || sprites.length == 0) {
            return null;
        }
        
        int dirIndex = direction.ordinal();
        if (dirIndex < 0 || dirIndex >= sprites.length) {
            dirIndex = 0;
        }
        if (frame < 0 || frame >= sprites[dirIndex].length) {
            frame = 0;
        }
        
        return sprites[dirIndex][frame];
    }
    
    public BufferedImage getEnemySprite(Enemy.EnemyType type, Direction direction, int frame) {
        if (type == null || direction == null) {
            return null;
        }
        
        BufferedImage[][] sprites = enemySprites.get(type);
        if (sprites == null || sprites.length == 0) {
            return null;
        }
        
        int dirIndex = direction.ordinal();
        if (dirIndex < 0 || dirIndex >= sprites.length) {
            dirIndex = 0;
        }
        if (frame < 0 || frame >= sprites[dirIndex].length) {
            frame = 0;
        }
        
        return sprites[dirIndex][frame];
    }
    
    public BufferedImage getPatrolEnemySprite(PatrolAxis axis, PatrolDirection direction, int frame) {
        if (patrolEnemySprites == null) {
            return null;
        }
        
        int axisIndex = axis == PatrolAxis.HORIZONTAL ? 0 : 1;
        int dirIndex = direction == PatrolDirection.POSITIVE ? 0 : 1;
        
        if (axisIndex >= patrolEnemySprites.length || 
            dirIndex >= patrolEnemySprites[axisIndex].length ||
            frame >= patrolEnemySprites[axisIndex][dirIndex].length) {
            frame = 0;
        }
        
        if (axisIndex < patrolEnemySprites.length && 
            dirIndex < patrolEnemySprites[axisIndex].length &&
            frame < patrolEnemySprites[axisIndex][dirIndex].length) {
            return patrolEnemySprites[axisIndex][dirIndex][frame];
        }
        
        return null;
    }
    
    public BufferedImage getTrapSprite(TrapAttack attack, Direction direction) {
        if (attack == null || direction == null) {
            return null;
        }
        
        Map<Direction, BufferedImage> sprites = trapSprites.get(attack);
        if (sprites == null) {
            return null;
        }
        
        return sprites.get(direction);
    }
    
    public int getAnimationFrame(long time, int speed) {
        if (speed <= 0) {
            speed = 1; // Защита от деления на ноль
        }
        int frame = (int)((time / (1000L / speed)) % ANIMATION_FRAMES);
        return Math.max(0, Math.min(frame, ANIMATION_FRAMES - 1));
    }
    
    /**
     * Отражает изображение по горизонтали
     */
    private BufferedImage flipImageHorizontally(BufferedImage img) {
        if (img == null) return null;
        
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(img, width, 0, -width, height, null);
        g.dispose();
        return flipped;
    }
    
    /**
     * Изменяет яркость изображения
     * @param img исходное изображение
     * @param brightnessFactor множитель яркости (1.0 = без изменений, >1.0 = светлее, <1.0 = темнее)
     */
    private BufferedImage adjustImageBrightness(BufferedImage img, float brightnessFactor) {
        if (img == null) return null;
        
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage adjusted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                
                // Если пиксель прозрачный, пропускаем
                if (alpha == 0) {
                    adjusted.setRGB(x, y, rgb);
                    continue;
                }
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Применяем множитель яркости
                r = Math.min(255, Math.max(0, (int)(r * brightnessFactor)));
                g = Math.min(255, Math.max(0, (int)(g * brightnessFactor)));
                b = Math.min(255, Math.max(0, (int)(b * brightnessFactor)));
                
                int newRgb = (alpha << 24) | (r << 16) | (g << 8) | b;
                adjusted.setRGB(x, y, newRgb);
            }
        }
        
        return adjusted;
    }
}

