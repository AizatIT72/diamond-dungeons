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
                    // Если tile null, рисуем пол
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

        if (player.health < player.maxHealth) {
            int healthWidth = (int)((size - 4) * (player.health / (double)player.maxHealth));
            g2d.setColor(player.health > 50 ? Color.GREEN :
                    player.health > 25 ? Color.YELLOW : Color.RED);
            g2d.fillRect(x + 2, y + size, healthWidth, 4);
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

    private void drawUI(Graphics2D g2d) {
        if (currentState == null) return;

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));

        String stats = String.format(
                "Уровень: %d | Алмазы: %d/%d | Игроков: %d/%d",
                currentState.currentLevel,
                currentState.collectedDiamonds,
                currentState.totalDiamonds,
                currentState.players.size(),
                3
        );

        g2d.drawString(stats, 20, 30);

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