package ru.kpfu.itis.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerPanel extends JPanel {
    private MainWindow mainWindow;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JButton clearButton;
    private JLabel statusLabel;
    private boolean serverRunning = false;

    public ServerPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 40));
        initUI();
    }

    private void initUI() {
        // Панель заголовка
        JPanel headerPanel = createHeaderPanel();

        // Центральная панель с логами
        JPanel logPanel = createLogPanel();

        // Панель управления
        JPanel controlPanel = createControlPanel();

        // Панель настроек
        JPanel settingsPanel = createSettingsPanel();

        add(headerPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        add(settingsPanel, BorderLayout.WEST);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("УПРАВЛЕНИЕ СЕРВЕРОМ", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 215, 0));

        statusLabel = new JLabel("Статус: Остановлен");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);

        JButton backButton = new JButton("Назад");
        backButton.addActionListener(e -> mainWindow.showLoginPanel());

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(backButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel logTitle = new JLabel("Лог сервера:");
        logTitle.setForeground(Color.WHITE);
        logTitle.setFont(new Font("Arial", Font.BOLD, 14));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 30));
        logArea.setForeground(new Color(200, 220, 200));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        // Заполняем тестовыми логами
        logArea.append("=== Diamond Rush Server ===\n");
        logArea.append("Готов к запуску.\n");
        logArea.append("Для начала игры необходимо 3 игрока.\n");
        logArea.append("Порт по умолчанию: 5555\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));

        panel.add(logTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        startButton = createStyledButton("Запустить сервер", new Color(76, 175, 80));
        stopButton = createStyledButton("Остановить сервер", new Color(244, 67, 54));
        clearButton = createStyledButton("Очистить логи", new Color(33, 150, 243));

        stopButton.setEnabled(false);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText("");
                log("Логи очищены.");
            }
        });

        panel.add(startButton);
        panel.add(stopButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setPreferredSize(new Dimension(250, 0));

        JLabel title = new JLabel("Настройки сервера");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        // Порт сервера
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.setOpaque(false);
        JLabel portLabel = new JLabel("Порт:");
        portLabel.setForeground(Color.WHITE);
        JTextField portField = new JTextField("5555", 8);
        portPanel.add(portLabel);
        portPanel.add(portField);

        // Максимальное количество игроков
        JPanel maxPlayersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxPlayersPanel.setOpaque(false);
        JLabel maxPlayersLabel = new JLabel("Макс. игроков:");
        maxPlayersLabel.setForeground(Color.WHITE);
        JComboBox<Integer> maxPlayersCombo = new JComboBox<>(new Integer[]{2, 3, 4});
        maxPlayersCombo.setSelectedItem(3);
        maxPlayersPanel.add(maxPlayersLabel);
        maxPlayersPanel.add(maxPlayersCombo);

        // Уровень сложности
        JPanel difficultyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        difficultyPanel.setOpaque(false);
        JLabel difficultyLabel = new JLabel("Сложность:");
        difficultyLabel.setForeground(Color.WHITE);
        JComboBox<String> difficultyCombo = new JComboBox<>(new String[]{"Легкая", "Средняя", "Сложная"});
        difficultyCombo.setSelectedItem("Средняя");
        difficultyPanel.add(difficultyLabel);
        difficultyPanel.add(difficultyCombo);

        // Количество уровней
        JPanel levelsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        levelsPanel.setOpaque(false);
        JLabel levelsLabel = new JLabel("Уровней:");
        levelsLabel.setForeground(Color.WHITE);
        JComboBox<Integer> levelsCombo = new JComboBox<>(new Integer[]{1, 3, 5, 10});
        levelsCombo.setSelectedItem(3);
        levelsPanel.add(levelsLabel);
        levelsPanel.add(levelsCombo);

        panel.add(portPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(maxPlayersPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(difficultyPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(levelsPanel);
        panel.add(Box.createVerticalStrut(20));

        // Кнопка применения настроек
        JButton applyButton = new JButton("Применить настройки");
        applyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        applyButton.setMaximumSize(new Dimension(200, 30));
        applyButton.addActionListener(e -> {
            int port = Integer.parseInt(portField.getText());
            int maxPlayers = (Integer) maxPlayersCombo.getSelectedItem();
            String difficulty = (String) difficultyCombo.getSelectedItem();
            int levels = (Integer) levelsCombo.getSelectedItem();

            log("Настройки применены:");
            log("  Порт: " + port);
            log("  Макс. игроков: " + maxPlayers);
            log("  Сложность: " + difficulty);
            log("  Уровней: " + levels);
        });

        panel.add(applyButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Рисуем скругленную кнопку
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                // Текст
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        button.setPreferredSize(new Dimension(150, 35));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);

        return button;
    }

    private void startServer() {
        if (!serverRunning) {
            serverRunning = true;
            statusLabel.setText("Статус: Запущен");
            statusLabel.setForeground(Color.GREEN);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            log("=== Сервер запущен ===");
            log("Ожидание подключения игроков...");
            log("Порт: 5555");
            log("Максимум игроков: 3");
            log("=========================");

            // В реальном приложении здесь будет запуск сервера в отдельном потоке
        }
    }

    private void stopServer() {
        if (serverRunning) {
            serverRunning = false;
            statusLabel.setText("Статус: Остановлен");
            statusLabel.setForeground(Color.RED);

            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            log("Сервер остановлен.");
            log("Все соединения закрыты.");
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[LOG] " + message + "\n");
            // Прокручиваем вниз
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
