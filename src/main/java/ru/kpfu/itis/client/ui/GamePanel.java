package ru.kpfu.itis.client.ui;

import ru.kpfu.itis.client.ui.GameSession;
import ru.kpfu.itis.client.ui.MainWindow;
import ru.kpfu.itis.common.GameConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel {
    private MainWindow mainWindow;
    private Timer gameTimer;

    private int[][] gameMap;
    private PlayerData[] players;
    private int collectedDiamonds = 0;
    private int totalDiamonds = 15;
    private int currentPlayerIndex = 0;
    private boolean singlePlayerMode = false;

    class PlayerData {
        int x, y;
        Color color;
        String name;
        boolean active = true;
    }

    public GamePanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        initGame();
        initUI();
        initInput();
    }

    private void initGame() {
        singlePlayerMode = GameSession.getInstance().isSinglePlayer();

        // Создаем тестовую карту
        createTestMap();

        // Создаем игроков
        players = new PlayerData[3];
        for (int i = 0; i < 3; i++) {
            players[i] = new PlayerData();
            players[i].x = 1 + i * 5;
            players[i].y = 1 + i * 3;
            players[i].color = GameConstants.PLAYER_COLORS[i];

            // Устанавливаем имя с проверкой
            String username = GameSession.getInstance().getUsername();
            if (username == null || username.trim().isEmpty()) {
                username = "Игрок";
            }

            players[i].name = singlePlayerMode ?
                    "Игрок " + (i + 1) :
                    (i == 0 ? username : "Игрок " + (i + 1));
            players[i].active = true;
        }

        // Таймер для обновления игры
        gameTimer = new Timer(1000 / 60, e -> {
            updateGame();
            repaint();
        });
        gameTimer.start();
    }

    private void createTestMap() {
        gameMap = new int[GameConstants.GRID_SIZE][GameConstants.GRID_SIZE];

        // Стены по краям
        for (int i = 0; i < GameConstants.GRID_SIZE; i++) {
            for (int j = 0; j < GameConstants.GRID_SIZE; j++) {
                if (i == 0 || i == GameConstants.GRID_SIZE - 1 ||
                        j == 0 || j == GameConstants.GRID_SIZE - 1) {
                    gameMap[i][j] = 1;
                }
            }
        }

        // Алмазы
        int[][] diamondPositions = {{5,5}, {5,10}, {10,5}, {10,10}, {15,15}};
        for (int[] pos : diamondPositions) {
            gameMap[pos[1]][pos[0]] = 2;
        }

        // Двери
        gameMap[5][15] = 3;
        gameMap[15][5] = 3;

        // Ловушки
        gameMap[7][7] = 4;
        gameMap[12][12] = 4;

        // Кнопки
        gameMap[3][3] = 5;
        gameMap[3][16] = 5;

        totalDiamonds = diamondPositions.length;
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(40, 40, 50));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel modeLabel = new JLabel(singlePlayerMode ? "Режим: Одиночная игра" : "Режим: Сетевая игра");
        modeLabel.setForeground(singlePlayerMode ? Color.MAGENTA : Color.CYAN);
        modeLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel diamondLabel = new JLabel("Алмазы: " + collectedDiamonds + "/" + totalDiamonds);
        diamondLabel.setForeground(new Color(100, 200, 255));
        diamondLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel levelLabel = new JLabel("Уровень: " + GameSession.getInstance().getCurrentLevel());
        levelLabel.setForeground(Color.YELLOW);
        levelLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton exitButton = new JButton("Выход");
        exitButton.addActionListener(e -> {
            gameTimer.stop();
            mainWindow.showLoginPanel();
        });

        topPanel.add(modeLabel, BorderLayout.WEST);
        topPanel.add(diamondLabel, BorderLayout.CENTER);
        topPanel.add(levelLabel, BorderLayout.EAST);

        JPanel sidePanel = createSidePanel();
        add(topPanel, BorderLayout.NORTH);
        add(sidePanel, BorderLayout.EAST);
        add(new DrawingPanel(), BorderLayout.CENTER);
    }

    private JPanel createSidePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(50, 50, 60));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(200, 0));

        JLabel title = new JLabel("ИГРОКИ");
        title.setForeground(Color.YELLOW);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                panel.add(createPlayerInfoPanel(i));
                if (i < players.length - 1) {
                    panel.add(Box.createVerticalStrut(10));
                }
            }
        }

        panel.add(Box.createVerticalStrut(20));

        if (singlePlayerMode) {
            JLabel controlLabel = new JLabel("<html><center>Управление:<br>" +
                    "1-3 - выбор игрока<br>" +
                    "WASD/стрелки - движение</center></html>");
            controlLabel.setForeground(Color.WHITE);
            controlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(controlLabel);
        }

        panel.add(Box.createVerticalStrut(20));

        JButton nextLevelButton = new JButton("Следующий уровень");
        nextLevelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextLevelButton.setMaximumSize(new Dimension(150, 30));
        nextLevelButton.addActionListener(e -> nextLevel());

        JButton menuButton = new JButton("Главное меню");
        menuButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuButton.setMaximumSize(new Dimension(150, 30));
        menuButton.addActionListener(e -> {
            gameTimer.stop();
            mainWindow.showLoginPanel();
        });

        panel.add(nextLevelButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(menuButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createPlayerInfoPanel(int playerIndex) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(70, 70, 80));

        if (players[playerIndex] != null) {
            panel.setBorder(BorderFactory.createLineBorder(
                    singlePlayerMode && playerIndex == currentPlayerIndex ?
                            Color.YELLOW : players[playerIndex].color, 2));

            String playerName = players[playerIndex].name != null ? players[playerIndex].name : "Игрок " + (playerIndex + 1);

            JLabel nameLabel = new JLabel(playerName);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            String status = players[playerIndex].active ? "Активен" : "Неактивен";
            JLabel statusLabel = new JLabel(status);
            statusLabel.setForeground(players[playerIndex].active ? Color.GREEN : Color.RED);
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (singlePlayerMode) {
                JLabel controlLabel = new JLabel(playerIndex == currentPlayerIndex ? "✓ Управляется" : "");
                controlLabel.setForeground(Color.YELLOW);
                controlLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                controlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(controlLabel);
            }

            panel.add(Box.createVerticalStrut(5));
            panel.add(nameLabel);
            panel.add(statusLabel);
            panel.add(Box.createVerticalStrut(5));
        }

        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return panel;
    }

    private void initInput() {
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
    }

    private void handleKeyPress(KeyEvent e) {
        int key = e.getKeyCode();

        // В одиночной игре: переключение между игроками цифрами 1-3
        if (singlePlayerMode) {
            if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) {
                currentPlayerIndex = key - KeyEvent.VK_1;
                if (currentPlayerIndex < 0 || currentPlayerIndex >= players.length) {
                    currentPlayerIndex = 0;
                }
                repaint();
                return;
            }
        }

        // Движение текущего игрока
        PlayerData currentPlayer = singlePlayerMode ?
                players[currentPlayerIndex] : players[0];

        if (currentPlayer == null || !currentPlayer.active) return;

        int dx = 0, dy = 0;

        switch (key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                dy = -1;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                dy = 1;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                dx = -1;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                dx = 1;
                break;
        }

        if (dx != 0 || dy != 0) {
            movePlayer(currentPlayer, dx, dy);
        }
    }

    private void movePlayer(PlayerData player, int dx, int dy) {
        int newX = player.x + dx;
        int newY = player.y + dy;

        if (isValidMove(newX, newY)) {
            player.x = newX;
            player.y = newY;

            // Проверка сбора алмазов
            if (gameMap[newY][newX] == 2) {
                gameMap[newY][newX] = 0;
                collectedDiamonds++;

                // Проверка победы
                if (collectedDiamonds >= totalDiamonds) {
                    gameTimer.stop();
                    JOptionPane.showMessageDialog(this,
                            "Поздравляем! Все алмазы собраны!\n\n" +
                                    "Собрано: " + collectedDiamonds + "/" + totalDiamonds + "\n" +
                                    "Уровень пройден!",
                            "Победа!",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }

            // Проверка ловушки
            if (gameMap[newY][newX] == 4) {
                player.active = false;
                JOptionPane.showMessageDialog(this,
                        "Игрок " + (player.name != null ? player.name : "Неизвестный") + " попал в ловушку!",
                        "Ловушка!",
                        JOptionPane.WARNING_MESSAGE);
            }

            repaint();
        }
    }

    private boolean isValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.GRID_SIZE ||
                y < 0 || y >= GameConstants.GRID_SIZE) {
            return false;
        }

        return gameMap[y][x] != 1;
    }

    private void updateGame() {
        // Обновление игровой логики
    }

    private void nextLevel() {
        gameTimer.stop();
        GameSession.getInstance().nextLevel();
        initGame();
        revalidate();
        repaint();

        JOptionPane.showMessageDialog(this,
                "Уровень " + GameSession.getInstance().getCurrentLevel() + " загружен!\n" +
                        "Начинайте собирать алмазы.",
                "Новый уровень",
                JOptionPane.INFORMATION_MESSAGE);
    }

    class DrawingPanel extends JPanel {
        private long animationTime = 0;

        public DrawingPanel() {
            setBackground(new Color(20, 20, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            try {
                drawGameField(g2d);
                drawPlayers(g2d);
                drawUI(g2d);
            } catch (Exception e) {
                // Ловим любые исключения при отрисовке
                e.printStackTrace();
                g2d.setColor(Color.RED);
                g2d.drawString("Ошибка отрисовки: " + e.getMessage(), 20, 20);
            }

            animationTime++;
        }

        private void drawGameField(Graphics2D g2d) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            int cellSize = Math.min(
                    panelWidth / GameConstants.GRID_SIZE,
                    panelHeight / GameConstants.GRID_SIZE
            );

            if (cellSize <= 0) return; // Проверка на корректный размер

            int offsetX = (panelWidth - cellSize * GameConstants.GRID_SIZE) / 2;
            int offsetY = (panelHeight - cellSize * GameConstants.GRID_SIZE) / 2;

            // Сетка
            g2d.setColor(new Color(60, 60, 70));
            for (int i = 0; i <= GameConstants.GRID_SIZE; i++) {
                g2d.drawLine(offsetX + i * cellSize, offsetY,
                        offsetX + i * cellSize, offsetY + GameConstants.GRID_SIZE * cellSize);
                g2d.drawLine(offsetX, offsetY + i * cellSize,
                        offsetX + GameConstants.GRID_SIZE * cellSize, offsetY + i * cellSize);
            }

            // Объекты
            for (int y = 0; y < GameConstants.GRID_SIZE; y++) {
                for (int x = 0; x < GameConstants.GRID_SIZE; x++) {
                    int cellX = offsetX + x * cellSize;
                    int cellY = offsetY + y * cellSize;

                    switch (gameMap[y][x]) {
                        case 1: // Стена
                            drawWall(g2d, cellX, cellY, cellSize);
                            break;
                        case 2: // Алмаз
                            drawDiamond(g2d, cellX, cellY, cellSize);
                            break;
                        case 3: // Дверь
                            drawDoor(g2d, cellX, cellY, cellSize);
                            break;
                        case 4: // Ловушка
                            drawTrap(g2d, cellX, cellY, cellSize);
                            break;
                        case 5: // Кнопка
                            drawButton(g2d, cellX, cellY, cellSize);
                            break;
                    }
                }
            }
        }

        private void drawWall(Graphics2D g2d, int x, int y, int size) {
            g2d.setColor(new Color(100, 70, 40));
            g2d.fillRect(x, y, size, size);
            g2d.setColor(new Color(80, 50, 30));
            g2d.drawRect(x + 2, y + 2, size - 4, size - 4);
        }

        private void drawDiamond(Graphics2D g2d, int x, int y, int size) {
            double pulse = Math.sin(animationTime * 0.1) * 0.2 + 0.8;
            int diamondSize = (int)(size * 0.7 * pulse);
            int offset = (size - diamondSize) / 2;

            Polygon diamond = new Polygon();
            diamond.addPoint(x + size/2, y + offset);
            diamond.addPoint(x + size - offset, y + size/2);
            diamond.addPoint(x + size/2, y + size - offset);
            diamond.addPoint(x + offset, y + size/2);

            GradientPaint gradient = new GradientPaint(
                    x, y, new Color(100, 200, 255, 200),
                    x + size, y + size, new Color(50, 150, 255, 150)
            );
            g2d.setPaint(gradient);
            g2d.fill(diamond);

            g2d.setColor(new Color(200, 240, 255));
            g2d.draw(diamond);

            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.drawLine(x + size/2, y + offset + 2, x + size - offset - 2, y + size/2);
            g2d.drawLine(x + size/2, y + offset + 2, x + offset + 2, y + size/2);
        }

        private void drawDoor(Graphics2D g2d, int x, int y, int size) {
            g2d.setColor(new Color(150, 100, 50));
            g2d.fillRect(x + 2, y + 2, size - 4, size - 4);
            g2d.setColor(collectedDiamonds >= totalDiamonds ? Color.GREEN : Color.YELLOW);
            g2d.drawRect(x + 4, y + 4, size - 8, size - 8);
            g2d.drawString("D", x + size/2 - 3, y + size/2 + 4);
        }

        private void drawTrap(Graphics2D g2d, int x, int y, int size) {
            double pulse = Math.sin(animationTime * 0.2) * 0.3 + 0.7;
            int trapSize = (int)(size * 0.6 * pulse);
            int offset = (size - trapSize) / 2;

            g2d.setColor(new Color(200, 50, 50, 150));
            g2d.fillOval(x + offset, y + offset, trapSize, trapSize);
            g2d.setColor(Color.RED);
            g2d.drawString("!", x + size/2 - 3, y + size/2 + 4);
        }

        private void drawButton(Graphics2D g2d, int x, int y, int size) {
            g2d.setColor(new Color(100, 200, 100));
            g2d.fillRect(x + size/4, y + size/4, size/2, size/2);
            g2d.setColor(Color.WHITE);
            g2d.drawString("B", x + size/2 - 3, y + size/2 + 4);
        }

        private void drawPlayers(Graphics2D g2d) {
            if (players == null) return;

            int panelWidth = getWidth();
            int panelHeight = getHeight();

            int cellSize = Math.min(
                    panelWidth / GameConstants.GRID_SIZE,
                    panelHeight / GameConstants.GRID_SIZE
            );

            if (cellSize <= 0) return;

            int offsetX = (panelWidth - cellSize * GameConstants.GRID_SIZE) / 2;
            int offsetY = (panelHeight - cellSize * GameConstants.GRID_SIZE) / 2;

            for (int i = 0; i < players.length; i++) {
                if (players[i] == null) continue;
                if (!players[i].active) continue;

                int x = offsetX + players[i].x * cellSize;
                int y = offsetY + players[i].y * cellSize;

                // Проверка корректности координат
                if (x < 0 || y < 0 || x + cellSize > panelWidth || y + cellSize > panelHeight) {
                    continue;
                }

                // Тень
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillOval(x + 3, y + 3, cellSize - 6, cellSize - 6);

                // Тело
                if (players[i].color != null) {
                    GradientPaint playerGradient = new GradientPaint(
                            x, y, players[i].color.brighter(),
                            x + cellSize, y + cellSize, players[i].color.darker()
                    );
                    g2d.setPaint(playerGradient);
                } else {
                    g2d.setColor(Color.GRAY);
                }
                g2d.fillOval(x + 2, y + 2, cellSize - 4, cellSize - 4);

                // Контур
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x + 2, y + 2, cellSize - 4, cellSize - 4);

                // Если одиночная игра и текущий игрок
                if (singlePlayerMode && i == currentPlayerIndex) {
                    g2d.setColor(new Color(255, 255, 100, 150));
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawOval(x, y, cellSize, cellSize);
                }

                // Имя (с проверкой на null)
                if (players[i].name != null) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    int nameWidth = fm.stringWidth(players[i].name);
                    g2d.drawString(players[i].name, x + (cellSize - nameWidth)/2, y - 5);
                }

                // Номер игрока (для одиночной игры)
                if (singlePlayerMode) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    String number = String.valueOf(i + 1);
                    FontMetrics fm = g2d.getFontMetrics();
                    int numWidth = fm.stringWidth(number);
                    g2d.drawString(number,
                            x + cellSize/2 - numWidth/2,
                            y + cellSize/2 + 4);
                }
            }
        }

        private void drawUI(Graphics2D g2d) {
            // Отображаем, каким игроком управляем
            if (singlePlayerMode) {
                String controlText = "Управление: Игрок " + (currentPlayerIndex + 1);
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString(controlText, 20, 30);
            }

            // Счетчик алмазов
            String diamondText = "Алмазы: " + collectedDiamonds + "/" + totalDiamonds;
            g2d.setColor(collectedDiamonds >= totalDiamonds ? Color.GREEN : new Color(100, 200, 255));
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString(diamondText, 20, getHeight() - 20);
        }
    }
}