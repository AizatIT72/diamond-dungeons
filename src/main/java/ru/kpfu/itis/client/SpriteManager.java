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
    // Множественные текстуры для разнообразия
    private java.util.List<BufferedImage> wallTextures;
    private java.util.List<BufferedImage> floorTextures;
    
    // Спрайты для персонажей (3 типа, 4 направления, несколько кадров анимации)
    private Map<String, BufferedImage[][]> playerSprites; // [characterType][direction][frame]
    
    // Спрайты для мобов
    private Map<Enemy.EnemyType, BufferedImage[][]> enemySprites; // [direction][frame]
    private BufferedImage[][][] patrolEnemySprites; // [axis][direction][frame]
    
    // Спрайты для ловушек
    private Map<TrapAttack, Map<Direction, BufferedImage>> trapSprites;
    // Анимированные спрайты для ловушек (огонь и стрелы)
    private Map<TrapAttack, Map<Direction, BufferedImage[]>> animatedTrapSprites;
    
    // Размер спрайта
    private static final int SPRITE_SIZE = 32;
    private static final int ANIMATION_FRAMES = 4;
    
    private SpriteManager() {
        tileSprites = new HashMap<>();
        playerSprites = new HashMap<>();
        enemySprites = new HashMap<>();
        trapSprites = new HashMap<>();
        animatedTrapSprites = new HashMap<>();
        patrolEnemySprites = new BufferedImage[2][2][ANIMATION_FRAMES];
        wallTextures = new java.util.ArrayList<>();
        floorTextures = new java.util.ArrayList<>();
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
        // Загружаем все текстуры стен из папки walls/
        loadWallTextures();
        
        // Загружаем все текстуры полов из папки floors/
        loadFloorTextures();
        
        // Устанавливаем дефолтные спрайты (для обратной совместимости)
        if (!wallTextures.isEmpty()) {
            tileSprites.put(TileType.WALL, wallTextures.get(0));
        } else {
            tileSprites.put(TileType.WALL, createWallSprite());
        }
        
        if (!floorTextures.isEmpty()) {
            tileSprites.put(TileType.FLOOR, floorTextures.get(0));
        } else {
            tileSprites.put(TileType.FLOOR, createFloorSprite());
        }
        
        // Алмаз - используем программно созданный синий ромбик
        tileSprites.put(TileType.DIAMOND, createDiamondSprite());
        
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
        // Маппинг: Зеленый лучник -> папка 1, Серебряный рыцарь -> папка 2, Темный маг -> папка 3
        String[] characterTypes = {"Зеленый лучник", "Серебряный рыцарь", "Темный маг"};
        int[] folderNumbers = {1, 2, 3};
        
        Direction[] directions = Direction.values();
        // Правильный маппинг: UP(0), DOWN(1), LEFT(2), RIGHT(3)
        // U_ для UP, D_ для DOWN, S_ для LEFT, S_ (отраженный) для RIGHT
        
        for (int i = 0; i < characterTypes.length; i++) {
            String charType = characterTypes[i];
            int folderNum = folderNumbers[i];
            
            BufferedImage[][] sprites = new BufferedImage[directions.length][ANIMATION_FRAMES];
            boolean loadedFromFile = false;
            
            // Пытаемся загрузить из файлов
            for (int dir = 0; dir < directions.length; dir++) {
                Direction direction = directions[dir];
                String prefix;
                boolean needFlip = false;
                
                // Правильный маппинг направлений
                switch (direction) {
                    case UP:
                        prefix = "U_";
                        break;
                    case DOWN:
                        prefix = "D_";
                        break;
                    case LEFT:
                        prefix = "S_";
                        break;
                    case RIGHT:
                        prefix = "S_";
                        needFlip = true; // Отражаем для RIGHT
                        break;
                    default:
                        prefix = "D_";
                }
                
                String walkPath = String.format("sprites/players/%d/%sWalk.png", folderNum, prefix);
                
                BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
                if (walkFrames != null && walkFrames.length > 0) {
                    loadedFromFile = true;
                    // Используем кадры ходьбы для анимации
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        BufferedImage frameImg = walkFrames[frame];
                        // Для направления RIGHT отражаем спрайт S_ по горизонтали
                        if (needFlip) {
                            frameImg = flipImageHorizontally(frameImg);
                        }
                        sprites[dir][frame] = frameImg;
                    }
                    // Если кадров меньше, дублируем последний
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            BufferedImage lastFrame = walkFrames[walkFrames.length - 1];
                            if (needFlip) {
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
        // Правильный маппинг: UP(0), DOWN(1), LEFT(2), RIGHT(3)
        
        for (int i = 0; i < types.length; i++) {
            Enemy.EnemyType type = types[i];
            if (type == Enemy.EnemyType.TRAP) continue;
            
            int folderNum = folderNumbers[i];
            BufferedImage[][] sprites = new BufferedImage[directions.length][ANIMATION_FRAMES];
            boolean loadedFromFile = false;
            
            // Пытаемся загрузить из файлов
            for (int dir = 0; dir < directions.length; dir++) {
                Direction direction = directions[dir];
                String prefix;
                boolean needFlip = false;
                
                // Правильный маппинг направлений
                switch (direction) {
                    case UP:
                        prefix = "U_";
                        break;
                    case DOWN:
                        prefix = "D_";
                        break;
                    case LEFT:
                        prefix = "S_";
                        break;
                    case RIGHT:
                        prefix = "S_";
                        needFlip = true; // Отражаем для RIGHT
                        break;
                    default:
                        prefix = "D_";
                }
                
                String walkPath = String.format("sprites/enemies/%d/%sWalk.png", folderNum, prefix);
                
                BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
                if (walkFrames != null && walkFrames.length > 0) {
                    loadedFromFile = true;
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        BufferedImage frameImg = walkFrames[frame];
                        // Для направления RIGHT отражаем спрайт S_ по горизонтали
                        if (needFlip) {
                            frameImg = flipImageHorizontally(frameImg);
                        }
                        sprites[dir][frame] = frameImg;
                    }
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            BufferedImage lastFrame = walkFrames[walkFrames.length - 1];
                            if (needFlip) {
                                lastFrame = flipImageHorizontally(lastFrame);
                            }
                            sprites[dir][frame] = lastFrame;
                        }
                    }
                } else {
                    // Fallback на программную генерацию
                    System.err.println("Не удалось загрузить спрайты для " + type + " направление " + direction + " из " + walkPath);
                    for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                        sprites[dir][frame] = createEnemySprite(type, directions[dir], frame);
                    }
                }
            }
            
            if (loadedFromFile) {
                System.out.println("Загружены спрайты врага: " + type + " (все направления)");
            } else {
                System.err.println("ВНИМАНИЕ: Спрайты для врага " + type + " не загружены из файлов, используется программная генерация");
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
        // HORIZONTAL + POSITIVE = вправо (S_ отраженный)
        // HORIZONTAL + NEGATIVE = влево (S_)
        // VERTICAL + POSITIVE = вниз (D_)
        // VERTICAL + NEGATIVE = вверх (U_)
        
        for (int axis = 0; axis < 2; axis++) {
            for (int dir = 0; dir < 2; dir++) {
                String prefix;
                boolean needFlip = false;
                
                if (axis == 0) { // HORIZONTAL
                    prefix = "S_";
                    if (dir == 0) { // POSITIVE (вправо)
                        needFlip = true;
                    } // NEGATIVE (влево) - без отражения
                } else { // VERTICAL
                    if (dir == 0) { // POSITIVE (вниз)
                        prefix = "D_";
                    } else { // NEGATIVE (вверх)
                        prefix = "U_";
                    }
                }
                
                String walkPath = String.format("sprites/enemies/4/%sWalk.png", prefix);
                BufferedImage[] walkFrames = SpriteSheetLoader.loadAndSplitAuto(walkPath);
                boolean loadedFromFile = false;
                
                if (walkFrames != null && walkFrames.length > 0) {
                    loadedFromFile = true;
                    for (int frame = 0; frame < Math.min(ANIMATION_FRAMES, walkFrames.length); frame++) {
                        BufferedImage frameImg = walkFrames[frame];
                        if (needFlip) {
                            frameImg = flipImageHorizontally(frameImg);
                        }
                        patrolEnemySprites[axis][dir][frame] = frameImg;
                    }
                    if (walkFrames.length < ANIMATION_FRAMES) {
                        for (int frame = walkFrames.length; frame < ANIMATION_FRAMES; frame++) {
                            BufferedImage lastFrame = walkFrames[walkFrames.length - 1];
                            if (needFlip) {
                                lastFrame = flipImageHorizontally(lastFrame);
                            }
                            patrolEnemySprites[axis][dir][frame] = lastFrame;
                        }
                    }
                } else {
                    // Fallback на программную генерацию
                    for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                        patrolEnemySprites[axis][dir][frame] = createPatrolEnemySprite(axis == 0, dir == 0, frame);
                    }
                }
            }
        }
    }
    
    private void loadTrapSprites() {
        // Загружаем анимированные спрайты для стрел
        Map<Direction, BufferedImage[]> arrowAnimations = new HashMap<>();
        Map<Direction, String> arrowFiles = new HashMap<>();
        arrowFiles.put(Direction.UP, "sprites/traps/arrow/arrow_from_down_to_up.png");
        arrowFiles.put(Direction.DOWN, "sprites/traps/arrow/arrow_from_up_to_down.png");
        arrowFiles.put(Direction.LEFT, "sprites/traps/arrow/arrow_from_right_to_left.png");
        arrowFiles.put(Direction.RIGHT, "sprites/traps/arrow/arrow_from_left_to_right.png");
        
        for (Direction dir : Direction.values()) {
            String path = arrowFiles.get(dir);
            BufferedImage[] frames = SpriteSheetLoader.loadAndSplitAuto(path);
            if (frames != null && frames.length > 0) {
                arrowAnimations.put(dir, frames);
            } else {
                // Fallback - создаем один кадр программно
                BufferedImage[] fallback = {createArrowTrapSprite(dir)};
                arrowAnimations.put(dir, fallback);
            }
        }
        animatedTrapSprites.put(TrapAttack.ARROW, arrowAnimations);
        
        // Для обратной совместимости - статичные спрайты
        Map<Direction, BufferedImage> arrowSprites = new HashMap<>();
        for (Direction dir : Direction.values()) {
            BufferedImage[] frames = arrowAnimations.get(dir);
            if (frames != null && frames.length > 0) {
                arrowSprites.put(dir, frames[0]); // Первый кадр для статичного отображения
            } else {
                arrowSprites.put(dir, createArrowTrapSprite(dir));
            }
        }
        trapSprites.put(TrapAttack.ARROW, arrowSprites);
        
        // Загружаем анимированные спрайты для огня
        Map<Direction, BufferedImage[]> fireAnimations = new HashMap<>();
        Map<Direction, String> fireFiles = new HashMap<>();
        fireFiles.put(Direction.UP, "sprites/traps/fire/fire_from_down_to_up.png");
        fireFiles.put(Direction.DOWN, "sprites/traps/fire/fire_frome_up_to_down.png");
        fireFiles.put(Direction.LEFT, "sprites/traps/fire/fire_frome_left_to_righ.png");
        fireFiles.put(Direction.RIGHT, "sprites/traps/fire/fire_frome_left_to_right.png");
        
        for (Direction dir : Direction.values()) {
            String path = fireFiles.get(dir);
            BufferedImage[] frames = SpriteSheetLoader.loadAndSplitAuto(path);
            if (frames != null && frames.length > 0) {
                fireAnimations.put(dir, frames);
            } else {
                // Fallback - создаем один кадр программно
                BufferedImage[] fallback = {createFireTrapSprite(dir)};
                fireAnimations.put(dir, fallback);
            }
        }
        animatedTrapSprites.put(TrapAttack.FIRE, fireAnimations);
        
        // Для обратной совместимости - статичные спрайты
        Map<Direction, BufferedImage> fireSprites = new HashMap<>();
        for (Direction dir : Direction.values()) {
            BufferedImage[] frames = fireAnimations.get(dir);
            if (frames != null && frames.length > 0) {
                fireSprites.put(dir, frames[0]); // Первый кадр для статичного отображения
            } else {
                fireSprites.put(dir, createFireTrapSprite(dir));
            }
        }
        trapSprites.put(TrapAttack.FIRE, fireSprites);
        
        System.out.println("Загружены анимированные спрайты для ловушек");
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
        if (characterType.contains("Зеленый")) {
            playerColor = new Color(50, 200, 50);
        } else if (characterType.contains("Серебряный")) {
            playerColor = new Color(192, 192, 192);
        } else if (characterType.contains("Темный")) {
            playerColor = new Color(64, 64, 64);
        } else {
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
    
    private void loadWallTextures() {
        // Используем только Tile_33.png для стены
        BufferedImage img = SpriteSheetLoader.loadImage("sprites/tiles/walls/Tile_33.png");
        if (img != null) {
            // Делаем тайл светлее для стены
            img = adjustImageBrightness(img, 1.2f);
            wallTextures.add(img);
        }
        
        System.out.println("Загружено текстур стен: " + wallTextures.size());
    }
    
    private void loadFloorTextures() {
        // Используем Tile_39.png для пола
        BufferedImage img = SpriteSheetLoader.loadImage("sprites/tiles/floors/Tile_39.png");
        if (img != null) {
            // Делаем тайл темнее для пола
            img = adjustImageBrightness(img, 0.7f);
            floorTextures.add(img);
        }
        
        System.out.println("Загружено текстур полов: " + floorTextures.size());
    }
    
    // Геттеры для спрайтов
    public BufferedImage getTileSprite(TileType tile) {
        // Для обратной совместимости - возвращаем первую доступную текстуру
        if (tile == TileType.WALL && !wallTextures.isEmpty()) {
            return wallTextures.get(0);
        }
        if (tile == TileType.FLOOR && !floorTextures.isEmpty()) {
            return floorTextures.get(0);
        }
        return tileSprites.getOrDefault(tile, createFloorSprite());
    }
    
    /**
     * Получить спрайт тайла на основе позиции (для детерминированного выбора)
     * Это позволяет использовать разные текстуры для разных позиций, создавая разнообразие
     */
    public BufferedImage getTileSprite(TileType tile, int x, int y) {
        // Для стен и полов выбираем текстуру на основе позиции (детерминированно)
        // Это создает разнообразие, но каждый тайл всегда будет иметь одну и ту же текстуру
        if (tile == TileType.WALL && !wallTextures.isEmpty()) {
            int index = Math.abs((x * 31 + y * 17) % wallTextures.size());
            return wallTextures.get(index);
        }
        if (tile == TileType.FLOOR && !floorTextures.isEmpty()) {
            int index = Math.abs((x * 31 + y * 17) % floorTextures.size());
            return floorTextures.get(index);
        }
        // Для остальных тайлов используем стандартный метод
        return getTileSprite(tile);
    }
    
    public BufferedImage getPlayerSprite(String characterType, Direction direction, int frame) {
        if (characterType == null || direction == null) {
            return null;
        }
        
        String key = characterType.contains("Зеленый лучник") ? "Зеленый лучник" :
                    characterType.contains("Серебряный рыцарь") ? "Серебряный рыцарь" :
                    characterType.contains("Темный маг") ? "Темный маг" : "Зеленый лучник";
        
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
    
    /**
     * Получить анимированный спрайт ловушки (для активных ловушек)
     */
    public BufferedImage getAnimatedTrapSprite(TrapAttack attack, Direction direction, int frame) {
        if (attack == null || direction == null) {
            return null;
        }
        
        Map<Direction, BufferedImage[]> animations = animatedTrapSprites.get(attack);
        if (animations == null) {
            // Fallback на статичный спрайт
            return getTrapSprite(attack, direction);
        }
        
        BufferedImage[] frames = animations.get(direction);
        if (frames == null || frames.length == 0) {
            return getTrapSprite(attack, direction);
        }
        
        if (frame < 0 || frame >= frames.length) {
            frame = 0;
        }
        
        return frames[frame];
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

