package ru.kpfu.itis.client;

import javax.swing.*;
import java.awt.*;

public class GameClient extends JFrame {
    private LoginWindow loginWindow;
    private String username;
    private String characterType;

    public GameClient() {
        setTitle("Diamond Dungeons");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        showLoginWindow();
    }

    private void showLoginWindow() {
        loginWindow = new LoginWindow();
        loginWindow.setVisible(true);

        new Thread(() -> {
            while (!loginWindow.isLoggedIn()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            SwingUtilities.invokeLater(() -> {
                username = loginWindow.getUsername();
                characterType = loginWindow.getCharacterType();
                showMainWindow();
            });
        }).start();
    }

    private void showMainWindow() {
        getContentPane().removeAll();

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel profilePanel = createProfilePanel();
        tabbedPane.addTab("Профиль", profilePanel);

        JPanel gamePanel = new JPanel();
        gamePanel.add(new JLabel("Игровая панель - подключение к серверу..."));
        tabbedPane.addTab("Игра", gamePanel);

        JPanel statsPanel = new JPanel();
        statsPanel.add(new JLabel("Статистика будет здесь"));
        tabbedPane.addTab("Статистика", statsPanel);

        add(tabbedPane);

        revalidate();
        repaint();
        setVisible(true);
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        JLabel titleLabel = new JLabel("Профиль игрока", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel nameLabel = new JLabel("Имя: " + username);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel characterLabel = new JLabel("Персонаж: " + characterType);
        characterLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel statusLabel = new JLabel("Статус: Не в сети");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusLabel.setForeground(Color.RED);

        infoPanel.add(titleLabel);
        infoPanel.add(nameLabel);
        infoPanel.add(characterLabel);
        infoPanel.add(statusLabel);

        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawAvatar(g);
            }

            private void drawAvatar(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int size = Math.min(getWidth(), getHeight()) - 40;

                Color playerColor;
                switch (characterType) {
                    case "Красный воин":
                        playerColor = Color.RED;
                        break;
                    case "Синий маг":
                        playerColor = Color.BLUE;
                        break;
                    case "Зеленый плут":
                        playerColor = Color.GREEN;
                        break;
                    default:
                        playerColor = Color.GRAY;
                }

                g2d.setColor(playerColor);
                g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);

                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(centerX - size / 2, centerY - size / 2, size, size);

                g2d.setColor(Color.WHITE);
                g2d.fillOval(centerX - size / 4, centerY - size / 6, size / 6, size / 6);
                g2d.fillOval(centerX + size / 4 - size / 6, centerY - size / 6, size / 6, size / 6);

                g2d.setColor(Color.BLACK);
                g2d.fillOval(centerX - size / 8, centerY - size / 12, size / 12, size / 12);
                g2d.fillOval(centerX + size / 8 - size / 12, centerY - size / 12, size / 12, size / 12);

                g2d.setStroke(new BasicStroke(2));
                g2d.drawArc(centerX - size / 4, centerY, size / 2, size / 3, 0, -180);

                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(username);
                g2d.drawString(username, centerX - textWidth / 2, centerY + size / 2 + 20);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(200, 250));
        avatarPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        // Нижняя часть - кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton connectButton = new JButton("Подключиться к серверу");
        JButton settingsButton = new JButton("Настройки");

        connectButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Подключение к серверу...\n(реализуем позже)",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        buttonPanel.add(connectButton);
        buttonPanel.add(settingsButton);

        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(avatarPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            client.setVisible(true);
        });
    }
}
