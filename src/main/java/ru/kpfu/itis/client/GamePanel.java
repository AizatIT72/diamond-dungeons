package ru.kpfu.itis.client;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.GameWorld;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private GameWorld.GameState currentState;
    private int currentPlayerId = -1;

    public GamePanel() {
        setBackground(new Color(20, 20, 30));
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
    }

    public void updateGameState(GameWorld.GameState state, int playerId) {
        this.currentState = state;
        this.currentPlayerId = playerId;
        repaint();
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
                            cellSize);
                }
            }
        }
    }

    private void drawTile(Graphics2D g2d, TileType tile, int x, int y, int size) {
        switch (tile) {
            case WALL:
                g2d.setColor(new Color(100, 70, 40));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(new Color(80, 50, 30));
                g2d.drawRect(x, y, size, size);
                break;
            case DIAMOND:
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(new Color(100, 200, 255));
                Polygon diamond = new Polygon();
                diamond.addPoint(x + size/2, y + size/4);
                diamond.addPoint(x + 3*size/4, y + size/2);
                diamond.addPoint(x + size/2, y + 3*size/4);
                diamond.addPoint(x + size/4, y + size/2);
                g2d.fill(diamond);
                break;
            case TRAP:
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(new Color(255, 50, 50));
                g2d.fillOval(x + size/4, y + size/4, size/2, size/2);
                break;
            case CHEST:
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(new Color(150, 100, 50));
                g2d.fillRect(x + size/4, y + size/4, size/2, size/2);
                g2d.setColor(new Color(200, 150, 100));
                g2d.fillRect(x + size/3, y + size/3, size/3, size/6);
                break;
            case DOOR:
                boolean unlocked = currentState != null && currentState.collectedDiamonds >= currentState.totalDiamonds;
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(unlocked ? new Color(100, 200, 100) : new Color(150, 100, 50));
                g2d.fillRect(x + size/4, y, size/2, size);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x + size/4, y, size/2, size);
                break;
            default:
                g2d.setColor(new Color(60, 60, 60));
                g2d.fillRect(x, y, size, size);
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRect(x, y, size, size);
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
        Color playerColor;
        String character = player.characterType;
        if (character.contains("Красный")) playerColor = Color.RED;
        else if (character.contains("Синий")) playerColor = Color.BLUE;
        else if (character.contains("Зеленый")) playerColor = Color.GREEN;
        else playerColor = Color.GRAY;

        g2d.setColor(playerColor);
        g2d.fillOval(x + 2, y + 2, size - 4, size - 4);

        g2d.setColor(isCurrent ? Color.YELLOW : Color.BLACK);
        g2d.setStroke(new BasicStroke(isCurrent ? 3 : 2));
        g2d.drawOval(x + 2, y + 2, size - 4, size - 4);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, size/4)));
        FontMetrics fm = g2d.getFontMetrics();
        String name = player.name.length() > 8 ? player.name.substring(0, 8) + "..." : player.name;
        int nameWidth = fm.stringWidth(name);
        g2d.drawString(name, x + (size - nameWidth)/2, y - 5);

        if (player.lives > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, Math.max(8, size/5)));
            FontMetrics livesFm = g2d.getFontMetrics();
            String livesText = "❤" + player.lives;
            int livesWidth = livesFm.stringWidth(livesText);

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(x + (size - livesWidth)/2 - 2, y + size + 2, livesWidth + 4, size/5 + 2);

            g2d.setColor(player.lives == 1 ? Color.RED : player.lives == 2 ? Color.YELLOW : Color.GREEN);
            g2d.drawString(livesText, x + (size - livesWidth)/2, y + size + size/5 + 2);
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
            g2d.fillOval(x, y, size, size);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        } else {
            g2d.fillRect(x, y, size, size);
        }

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        if (enemy.type == Enemy.EnemyType.GHOST) {
            g2d.drawOval(x, y, size, size);
        } else {
            g2d.drawRect(x, y, size, size);
        }

        if (enemy.health < enemy.type.health) {
            int healthWidth = (int)(size * (enemy.health / (double)enemy.type.health));
            g2d.setColor(enemy.health > enemy.type.health/2 ? Color.GREEN : Color.RED);
            g2d.fillRect(x, y - 5, healthWidth, 3);
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

        Color zoneColor = trap.attack == TrapAttack.ARROW 
                ? new Color(255, 100, 100, 180)  
                : new Color(255, 150, 0, 200);   

        for (int[] cell : targetCells) {
            int cellX = offsetX + cell[0] * cellSize;
            int cellY = offsetY + cell[1] * cellSize;

            g2d.setColor(zoneColor);
            g2d.fillRect(cellX, cellY, cellSize, cellSize);

            if (trap.attack == TrapAttack.ARROW) {

                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawLine(cellX + cellSize/4, cellY + cellSize/2,
                            cellX + 3*cellSize/4, cellY + cellSize/2);
                g2d.drawLine(cellX + cellSize/2, cellY + cellSize/4,
                            cellX + cellSize/2, cellY + 3*cellSize/4);
            } else if (trap.attack == TrapAttack.FIRE) {

                g2d.setColor(new Color(255, 200, 0));
                for (int i = 0; i < 3; i++) {
                    int flameX = cellX + cellSize/4 + i * cellSize/4;
                    int flameY = cellY + cellSize/3;
                    g2d.fillOval(flameX, flameY, cellSize/4, cellSize/3);
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