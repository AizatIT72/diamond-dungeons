package ru.kpfu.itis.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class SpriteSheetLoader {
    
    /**
     * Загружает изображение из ресурсов с поддержкой прозрачности
     */
    public static BufferedImage loadImage(String path) {
        try {
            InputStream is = SpriteSheetLoader.class.getClassLoader().getResourceAsStream(path);
            if (is == null) {
                System.err.println("Не найден ресурс: " + path);
                // Пробуем альтернативный способ загрузки
                is = ClassLoader.getSystemResourceAsStream(path);
                if (is == null) {
                    System.err.println("Альтернативный способ загрузки также не сработал: " + path);
                    return null;
                }
            }
            BufferedImage img = ImageIO.read(is);
            is.close();
            if (img != null) {
                // Конвертируем изображение в ARGB формат для правильной обработки прозрачности
                if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage converted = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = converted.createGraphics();
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    img = converted;
                }
                System.out.println("✓ Изображение загружено: " + path + " (размер: " + img.getWidth() + "x" + img.getHeight() + ", тип: ARGB)");
            }
            return img;
        } catch (IOException e) {
            System.err.println("Ошибка загрузки изображения " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Разделяет спрайт-лист на отдельные кадры
     * @param spriteSheet спрайт-лист
     * @param frameWidth ширина одного кадра
     * @param frameHeight высота одного кадра
     * @param frames количество кадров (если 0, то определяется автоматически)
     * @return массив кадров
     */
    public static BufferedImage[] splitSpriteSheet(BufferedImage spriteSheet, int frameWidth, int frameHeight, int frames) {
        if (spriteSheet == null) {
            return new BufferedImage[0];
        }
        
        int sheetWidth = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();
        
        // Определяем количество кадров автоматически, если не указано
        if (frames == 0) {
            frames = sheetWidth / frameWidth;
        }
        
        BufferedImage[] result = new BufferedImage[frames];
        
        for (int i = 0; i < frames; i++) {
            int x = i * frameWidth;
            if (x + frameWidth <= sheetWidth) {
                BufferedImage subImage = spriteSheet.getSubimage(x, 0, frameWidth, frameHeight);
                // Конвертируем в ARGB для сохранения прозрачности
                if (subImage.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage converted = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = converted.createGraphics();
                    g.drawImage(subImage, 0, 0, null);
                    g.dispose();
                    result[i] = converted;
                } else {
                    result[i] = subImage;
                }
            } else {
                // Если кадр не помещается, создаем пустой кадр
                result[i] = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
            }
        }
        
        return result;
    }
    
    /**
     * Разделяет спрайт-лист на кадры, автоматически определяя размер кадра
     * Предполагается, что все кадры одинакового размера и расположены горизонтально
     */
    public static BufferedImage[] splitSpriteSheetAuto(BufferedImage spriteSheet) {
        if (spriteSheet == null) {
            return new BufferedImage[0];
        }
        
        int sheetWidth = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();
        
        // Пытаемся определить размер кадра по высоте (обычно кадры квадратные или близки к квадрату)
        // Проверяем стандартные размеры: 16, 24, 32, 48, 64
        int[] possibleSizes = {16, 24, 32, 48, 64};
        
        for (int size : possibleSizes) {
            if (sheetWidth % size == 0 && sheetHeight == size) {
                int frames = sheetWidth / size;
                return splitSpriteSheet(spriteSheet, size, size, frames);
            }
        }
        
        // Если не нашли стандартный размер, пытаемся определить по количеству кадров
        // Обычно в спрайт-листах 4-8 кадров
        for (int frames = 4; frames <= 8; frames++) {
            if (sheetWidth % frames == 0) {
                int frameWidth = sheetWidth / frames;
                if (frameWidth == sheetHeight) {
                    return splitSpriteSheet(spriteSheet, frameWidth, sheetHeight, frames);
                }
            }
        }
        
        // Если ничего не подошло, возвращаем весь спрайт как один кадр
        return new BufferedImage[]{spriteSheet};
    }
    
    /**
     * Загружает и разделяет спрайт-лист на кадры
     */
    public static BufferedImage[] loadAndSplit(String path, int frameWidth, int frameHeight, int frames) {
        BufferedImage sheet = loadImage(path);
        if (sheet == null) {
            return new BufferedImage[0];
        }
        return splitSpriteSheet(sheet, frameWidth, frameHeight, frames);
    }
    
    /**
     * Загружает и автоматически разделяет спрайт-лист
     */
    public static BufferedImage[] loadAndSplitAuto(String path) {
        BufferedImage sheet = loadImage(path);
        if (sheet == null) {
            return new BufferedImage[0];
        }
        return splitSpriteSheetAuto(sheet);
    }
}

