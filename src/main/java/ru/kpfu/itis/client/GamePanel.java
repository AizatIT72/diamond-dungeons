package ru.kpfu.itis.client;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.GameWorld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;

public class GamePanel extends JPanel {
    private GameWorld.GameState currentState;
    private int currentPlayerId = -1;
    private Timer transitionTimer;
    private Timer animationTimer;
    private SpriteManager spriteManager;
    private long animationStartTime;

    public GamePanel() {
        setBackground(new Color(20, 20, 30));
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        
        spriteManager = SpriteManager.getInstance();
        animationStartTime = System.currentTimeMillis();
        
        // Таймер для анимации
        animationTimer = new Timer(100, e -> repaint());
        animationTimer.start();
    }

    public void updateGameState(GameWorld.GameState state, int playerId) {
        this.currentState = state;
        this.currentPlayerId = playerId;

        // Если идет переход, перерисовываем чаще для анимации
        if (state != null && state.isLevelTransitioning) {
            repaint();
            // Запускаем быструю перерисовку для анимации
            if (transitionTimer != null) {
                transitionTimer.stop();
            }
            transitionTimer = new Timer(50, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    repaint();
                }
            });
            transitionTimer.start();
        } else {
            if (transitionTimer != null) {
                transitionTimer.stop();
                transitionTimer = null;
            }
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == null || currentState.map == null) {
            drawLoadingScreen(g2d);
            return;
        }

        drawGameWorld(g2d);
        drawPlayers(g2d);
        drawEnemies(g2d);
        drawPatrolEnemies(g2d);
        drawTraps(g2d);
        drawUI(g2d);

        // Добавляем затемнение при переходе между уровнями
        if (currentState.isLevelTransitioning) {
            drawLevelTransition(g2d);
        }
    }

    private void drawLoadingScreen(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String text = "⏳ Загрузка...";
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = getHeight() / 2;
        g2d.drawString(text, x, y);
    }

    // Новый метод: отрисовка перехода между уровнями
    private void drawLevelTransition(Graphics2D g2d) {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - currentState.levelTransitionStartTime;
        float progress = Math.min(1.0f, elapsed / 2000.0f); // 2 секунды

        // Плавное затемнение и осветление
        float alpha;
        if (progress < 0.5f) {
            // Затемнение (0 -> 0.8)
            alpha = progress * 1.6f;
        } else {
            // Осветление (0.8 -> 0)
            alpha = (1.0f - progress) * 1.6f;
        }

        g2d.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Отображаем сообщение о переходе
        if (progress < 0.5f) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String text = "🌌 Переход на уровень " + (currentState.currentLevel + 1) + "...";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(text, x, y);
        }
    }

    private void drawGameWorld(Graphics2D g2d) {
        if (currentState.map.length == 0) return;

        int cellSize = calculateCellSize();
        int offsetX = (getWidth() - currentState.map[0].length * cellSize) / 2;
        int offsetY = (getHeight() - currentState.map.length * cellSize) / 2;

        for (int y = 0; y < currentState.map.length; y++) {
            for (int x = 0; x < currentState.map[y].length; x++) {
                TileType tile = currentState.map[y][x];
                if (tile == null) {
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fillRect(offsetX + x * cellSize, offsetY + y * cellSize, cellSize, cellSize);
                } else {
                    drawTile(g2d, tile,
                            offsetX + x * cellSize,
                            offsetY + y * cellSize,
                            cellSize, x, y);
                }
            }
        }
    }

    private void drawTile(Graphics2D g2d, TileType tile, int x, int y, int size, int tileX, int tileY) {
        // Используем детерминированный выбор текстуры на основе позиции
        BufferedImage sprite = spriteManager.getTileSprite(tile, tileX, tileY);
        
        if (sprite != null) {
            // Масштабируем спрайт под размер ячейки
            g2d.drawImage(sprite, x, y, size, size, null);
        } else {
            // Fallback на старый способ отрисовки
            switch (tile) {
                case WALL:
                    g2d.setColor(new Color(100, 70, 40));
                    g2d.fillRect(x, y, size, size);
                    g2d.setColor(new Color(80, 50, 30));
                    g2d.drawRect(x, y, size, size);
                    break;
                case DIAMOND:
                    BufferedImage diamondSprite = spriteManager.getTileSprite(TileType.DIAMOND);
                    if (diamondSprite != null) {
                        g2d.drawImage(diamondSprite, x, y, size, size, null);
                    } else {
                        g2d.setColor(new Color(60, 60, 60));
                        g2d.fillRect(x, y, size, size);
                        g2d.setColor(new Color(100, 200, 255));
                        Polygon diamond = new Polygon();
                        diamond.addPoint(x + size/2, y + size/4);
                        diamond.addPoint(x + 3*size/4, y + size/2);
                        diamond.addPoint(x + size/2, y + 3*size/4);
                        diamond.addPoint(x + size/4, y + size/2);
                        g2d.fill(diamond);
                    }
                    break;
                case DOOR:
                    boolean unlocked = currentState != null && currentState.collectedDiamonds >= currentState.totalDiamonds;
                    BufferedImage doorSprite = spriteManager.getTileSprite(TileType.DOOR);
                    if (doorSprite != null) {
                        g2d.drawImage(doorSprite, x, y, size, size, null);
                        if (unlocked) {
                            g2d.setColor(new Color(100, 255, 100, 150));
                            g2d.fillRect(x, y, size, size);
                        }
                    } else {
                        g2d.setColor(new Color(60, 60, 60));
                        g2d.fillRect(x, y, size, size);
                        g2d.setColor(unlocked ? new Color(100, 200, 100) : new Color(150, 100, 50));
                        g2d.fillRect(x + size/4, y, size/2, size);
                    }
                    break;
                default:
                    BufferedImage floorSprite = spriteManager.getTileSprite(TileType.FLOOR, tileX, tileY);
                    if (floorSprite != null) {
                        g2d.drawImage(floorSprite, x, y, size, size, null);
                    } else {
                        g2d.setColor(new Color(60, 60, 60));
                        g2d.fillRect(x, y, size, size);
                        g2d.setColor(new Color(80, 80, 80));
                        g2d.drawRect(x, y, size, size);
                    }
            }
        }
    }

    private void drawPlayers(Graphics2D g2d) {
        if (currentState == null || currentState.players.isEmpty()) return;

        int cellSize = calculateCellSize();
        int offsetX = (getWidth() - currentState.map[0].length * cellSize) / 2;
        int offsetY = (getHeight() - currentState.map.length * cellSize) / 2;

        for (PlayerState player : currentState.players) {
            int x = offsetX + player.x * cellSize;
            int y = offsetY + player.y * cellSize;
            drawPlayer(g2d, player, x, y, cellSize, player.id == currentPlayerId);
        }
    }

    private void drawPlayer(Graphics2D g2d, PlayerState player, int x, int y, int size, boolean isCurrent) {
        // Определяем направление персонажа (по умолчанию DOWN)
        Direction direction = player.direction != null ? player.direction : Direction.DOWN;
        
        // Получаем кадр анимации на основе времени
        long currentTime = System.currentTimeMillis();
        int frame = spriteManager.getAnimationFrame(currentTime - animationStartTime, 4);
        
        // Получаем спрайт персонажа
        BufferedImage sprite = spriteManager.getPlayerSprite(player.characterType, direction, frame);
        
        // Увеличиваем размер персонажа в 1.75 раза
        int playerSize = (int)(size * 1.75);
        // Центрируем персонажа в ячейке
        int playerX = x - (playerSize - size) / 2;
        int playerY = y - (playerSize - size) / 2;
        
        if (sprite != null) {
            // Рисуем спрайт увеличенным в 1.75 раза
            g2d.drawImage(sprite, playerX, playerY, playerSize, playerSize, null);
            
            // Желтая обводка убрана по запросу
        } else {
            // Fallback на старый способ
            Color playerColor;
            String character = player.characterType;
            if (character.contains("Зеленый")) playerColor = Color.GREEN;
            else if (character.contains("Серебряный")) playerColor = Color.LIGHT_GRAY;
            else if (character.contains("Темный")) playerColor = Color.DARK_GRAY;
            else playerColor = Color.GRAY;

            g2d.setColor(playerColor);
            g2d.fillOval(playerX + 2, playerY + 2, playerSize - 4, playerSize - 4);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(playerX + 2, playerY + 2, playerSize - 4, playerSize - 4);
        }

        // Имя игрока (позиционируем относительно увеличенного персонажа)
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, size/4)));
        FontMetrics fm = g2d.getFontMetrics();
        String name = player.name.length() > 8 ? player.name.substring(0, 8) + "..." : player.name;
        int nameWidth = fm.stringWidth(name);
        g2d.drawString(name, x + (size - nameWidth)/2, playerY - 5);

        // Жизни (позиционируем относительно увеличенного персонажа)
        if (player.lives > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, size/5)));
            FontMetrics livesFm = g2d.getFontMetrics();
            String livesText = "❤" + player.lives;
            int livesWidth = livesFm.stringWidth(livesText);

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(x + (size - livesWidth)/2 - 2, playerY + playerSize + 2, livesWidth + 4, size/5 + 2);

            g2d.setColor(player.lives == 1 ? Color.RED : player.lives == 2 ? Color.YELLOW : Color.GREEN);
            g2d.drawString(livesText, x + (size - livesWidth)/2, playerY + playerSize + size/5 + 2);
        }
    }

    private void drawEnemies(Graphics2D g2d) {
        if (currentState == null || currentState.enemies.isEmpty()) return;

        int cellSize = calculateCellSize();
        int offsetX = (getWidth() - currentState.map[0].length * cellSize) / 2;
        int offsetY = (getHeight() - currentState.map.length * cellSize) / 2;

        for (Enemy enemy : currentState.enemies) {
            if (!enemy.isActive) continue;

            int x = offsetX + enemy.x * cellSize;
            int y = offsetY + enemy.y * cellSize;
            drawEnemy(g2d, enemy, x, y, cellSize);
        }
    }

    private void drawEnemy(Graphics2D g2d, Enemy enemy, int x, int y, int size) {
        // Получаем кадр анимации
        long currentTime = System.currentTimeMillis();
        int frame = spriteManager.getAnimationFrame(currentTime - animationStartTime, enemy.type.speed);
        
        // Получаем направление врага
        Direction direction = enemy.direction != null ? enemy.direction : Direction.DOWN;
        
        // Увеличиваем размер моба в 2 раза (как у персонажей)
        int enemySize = (int)(size * 2);
        // Центрируем моба в ячейке
        int enemyX = x - (enemySize - size) / 2;
        int enemyY = y - (enemySize - size) / 2;
        
        // Получаем спрайт врага
        BufferedImage sprite = spriteManager.getEnemySprite(enemy.type, direction, frame);
        
        if (sprite != null) {
            // Рисуем спрайт увеличенным в 2 раза
            g2d.drawImage(sprite, enemyX, enemyY, enemySize, enemySize, null);
        } else {
            // Fallback на старый способ
            Color enemyColor;
            switch (enemy.type) {
                case BAT: enemyColor = new Color(100, 50, 150); break;
                case SKELETON: enemyColor = new Color(200, 200, 200); break;
                case GHOST: enemyColor = new Color(100, 200, 200, 150); break;
                default: enemyColor = Color.GRAY;
            }

            g2d.setColor(enemyColor);
            if (enemy.type == Enemy.EnemyType.GHOST) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2d.fillOval(enemyX, enemyY, enemySize, enemySize);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g2d.fillRect(enemyX, enemyY, enemySize, enemySize);
            }

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            if (enemy.type == Enemy.EnemyType.GHOST) {
                g2d.drawOval(enemyX, enemyY, enemySize, enemySize);
            } else {
                g2d.drawRect(enemyX, enemyY, enemySize, enemySize);
            }
        }

        // Полоса здоровья
        if (enemy.health < enemy.type.health) {
            int healthWidth = (int)(enemySize * (enemy.health / (double)enemy.type.health));
            g2d.setColor(enemy.health > enemy.type.health/2 ? Color.GREEN : Color.RED);
            g2d.fillRect(enemyX, enemyY - 5, healthWidth, 3);
        }
    }

    private void drawPatrolEnemies(Graphics2D g2d) {
        if (currentState == null || currentState.patrolEnemies == null || currentState.patrolEnemies.isEmpty()) {
            return;
        }

        int cellSize = calculateCellSize();
        int offsetX = (getWidth() - currentState.map[0].length * cellSize) / 2;
        int offsetY = (getHeight() - currentState.map.length * cellSize) / 2;

        for (PatrolEnemy patrolEnemy : currentState.patrolEnemies) {
            int x = offsetX + patrolEnemy.x * cellSize;
            int y = offsetY + patrolEnemy.y * cellSize;

            // Увеличиваем размер патрульного моба в 1.75 раза (как у персонажей)
            int patrolSize = (int)(cellSize * 2);
            // Центрируем моба в ячейке
            int patrolX = x - (patrolSize - cellSize) / 2;
            int patrolY = y - (patrolSize - cellSize) / 2;

            // Получаем кадр анимации
            long currentTime = System.currentTimeMillis();
            int frame = spriteManager.getAnimationFrame(currentTime - animationStartTime, 2);
            
            // Получаем спрайт патрульного моба
            BufferedImage sprite = spriteManager.getPatrolEnemySprite(
                patrolEnemy.axis, 
                patrolEnemy.direction, 
                frame
            );
            
            if (sprite != null) {
                // Рисуем спрайт увеличенным в 1.75 раза
                g2d.drawImage(sprite, patrolX, patrolY, patrolSize, patrolSize, null);
            } else {
                // Fallback на старый способ
                double pulse = Math.sin(System.currentTimeMillis() * 0.005) * 0.1 + 0.9;
                int size = (int) (cellSize * pulse);
                int offset = (cellSize - size) / 2;

                g2d.setColor(Color.RED);
                g2d.fillOval(x + offset, y + offset, size, size);

                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x + offset, y + offset, size, size);

                int centerX = x + cellSize / 2;
                int centerY = y + cellSize / 2;
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2));

                if (patrolEnemy.axis == PatrolAxis.HORIZONTAL) {
                    if (patrolEnemy.direction == PatrolDirection.POSITIVE) {
                        g2d.drawLine(centerX, centerY, centerX + size / 4, centerY);
                        g2d.drawLine(centerX + size / 4, centerY, centerX + size / 6, centerY - size / 8);
                        g2d.drawLine(centerX + size / 4, centerY, centerX + size / 6, centerY + size / 8);
                    } else {
                        g2d.drawLine(centerX, centerY, centerX - size / 4, centerY);
                        g2d.drawLine(centerX - size / 4, centerY, centerX - size / 6, centerY - size / 8);
                        g2d.drawLine(centerX - size / 4, centerY, centerX - size / 6, centerY + size / 8);
                    }
                } else {
                    if (patrolEnemy.direction == PatrolDirection.POSITIVE) {
                        g2d.drawLine(centerX, centerY, centerX, centerY + size / 4);
                        g2d.drawLine(centerX, centerY + size / 4, centerX - size / 8, centerY + size / 6);
                        g2d.drawLine(centerX, centerY + size / 4, centerX + size / 8, centerY + size / 6);
                    } else {
                        g2d.drawLine(centerX, centerY, centerX, centerY - size / 4);
                        g2d.drawLine(centerX, centerY - size / 4, centerX - size / 8, centerY - size / 6);
                        g2d.drawLine(centerX, centerY - size / 4, centerX + size / 8, centerY - size / 6);
                    }
                }
            }
        }
    }

    private void drawTraps(Graphics2D g2d) {
        if (currentState == null || currentState.traps == null || currentState.traps.isEmpty()) {
            return;
        }

        int cellSize = calculateCellSize();
        int offsetX = (getWidth() - currentState.map[0].length * cellSize) / 2;
        int offsetY = (getHeight() - currentState.map.length * cellSize) / 2;

        for (Trap trap : currentState.traps) {
            int trapX = offsetX + trap.x * cellSize;
            int trapY = offsetY + trap.y * cellSize;

            drawTrap(g2d, trap, trapX, trapY, cellSize);

            if (trap.active) {
                drawTrapZone(g2d, trap, offsetX, offsetY, cellSize);
            }
        }
    }

    private void drawTrap(Graphics2D g2d, Trap trap, int x, int y, int cellSize) {
        // Получаем кадр анимации для активных ловушек
        long currentTime = System.currentTimeMillis();
        int frame = 0;
        if (trap.active) {
            frame = spriteManager.getAnimationFrame(currentTime - animationStartTime, 8); // Быстрая анимация
        }
        
        // Получаем спрайт ловушки (анимированный если активна, иначе статичный)
        BufferedImage trapSprite;
        if (trap.active) {
            trapSprite = spriteManager.getAnimatedTrapSprite(trap.attack, trap.direction, frame);
        } else {
            trapSprite = spriteManager.getTrapSprite(trap.attack, trap.direction);
        }
        
        if (trapSprite != null) {
            // Рисуем спрайт ловушки на стене
            g2d.drawImage(trapSprite, x, y, cellSize, cellSize, null);
        } else {
            // Fallback на старый способ
            int trapDrawX = x;
            int trapDrawY = y;
            int trapWidth = cellSize / 4;
            int trapHeight = cellSize / 4;

            switch (trap.direction) {
                case LEFT:
                    trapDrawX = x + cellSize - trapWidth - 2;
                    trapDrawY = y + (cellSize - trapHeight) / 2;
                    break;
                case RIGHT:
                    trapDrawX = x + 2;
                    trapDrawY = y + (cellSize - trapHeight) / 2;
                    break;
                case UP:
                    trapDrawX = x + (cellSize - trapWidth) / 2;
                    trapDrawY = y + cellSize - trapHeight - 2;
                    break;
                case DOWN:
                    trapDrawX = x + (cellSize - trapWidth) / 2;
                    trapDrawY = y + 2;
                    break;
            }

            if (trap.attack == TrapAttack.ARROW) {
                g2d.setColor(new Color(80, 80, 80));
                g2d.fillRect(trapDrawX, trapDrawY, trapWidth, trapHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(trapDrawX, trapDrawY, trapWidth, trapHeight);

                if (trap.active) {
                    g2d.setColor(Color.RED);
                    int centerX = trapDrawX + trapWidth / 2;
                    int centerY = trapDrawY + trapHeight / 2;
                    g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
                }
            } else if (trap.attack == TrapAttack.FIRE) {
                if (trap.active) {
                    g2d.setColor(new Color(255, 100, 0));
                } else {
                    double pulse = Math.sin(System.currentTimeMillis() * 0.01) * 0.3 + 0.7;
                    int alpha = (int) (255 * pulse);
                    g2d.setColor(new Color(255, 150, 0, alpha));
                }
                g2d.fillOval(trapDrawX, trapDrawY, trapWidth, trapHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(trapDrawX, trapDrawY, trapWidth, trapHeight);
            }
        }
    }

    private void drawTrapZone(Graphics2D g2d, Trap trap, int offsetX, int offsetY, int cellSize) {
        java.util.List<int[]> targetCells = new java.util.ArrayList<>();
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

            if (targetX >= 0 && targetX < currentState.map[0].length &&
                    targetY >= 0 && targetY < currentState.map.length) {
                targetCells.add(new int[]{targetX, targetY});
            }
        }

        // Получаем кадр анимации для эффекта
        long currentTime = System.currentTimeMillis();
        int frame = spriteManager.getAnimationFrame(currentTime - animationStartTime, 8);

        for (int[] cell : targetCells) {
            int cellX = offsetX + cell[0] * cellSize;
            int cellY = offsetY + cell[1] * cellSize;

            // Для активных ловушек рисуем анимированные эффекты
            if (trap.attack == TrapAttack.ARROW) {
                // Анимированная стрела в зоне поражения
                BufferedImage arrowSprite = spriteManager.getAnimatedTrapSprite(trap.attack, trap.direction, frame);
                if (arrowSprite != null) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2d.drawImage(arrowSprite, cellX, cellY, cellSize, cellSize, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    // Fallback
                    g2d.setColor(new Color(255, 100, 100, 180));
                    g2d.fillRect(cellX, cellY, cellSize, cellSize);
                }
            } else if (trap.attack == TrapAttack.FIRE) {
                // Анимированное пламя в зоне поражения
                BufferedImage fireSprite = spriteManager.getAnimatedTrapSprite(trap.attack, trap.direction, frame);
                if (fireSprite != null) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                    g2d.drawImage(fireSprite, cellX, cellY, cellSize, cellSize, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    // Fallback
                    g2d.setColor(new Color(255, 150, 0, 200));
                    g2d.fillRect(cellX, cellY, cellSize, cellSize);
                }
            }
        }
    }

    private void drawUI(Graphics2D g2d) {
        if (currentPlayerId != -1) {
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            String controls = "WASD/Стрелки - Движение | Пробел - Действие";
            int textWidth = g2d.getFontMetrics().stringWidth(controls);
            g2d.drawString(controls, getWidth()/2 - textWidth/2, getHeight() - 20);
        }
    }

    private int calculateCellSize() {
        if (currentState == null || currentState.map.length == 0) return 32;

        int mapWidth = currentState.map[0].length;
        int mapHeight = currentState.map.length;

        int maxCellWidth = getWidth() / mapWidth;
        int maxCellHeight = getHeight() / mapHeight;

        return Math.min(maxCellWidth, maxCellHeight) - 2;
    }
}