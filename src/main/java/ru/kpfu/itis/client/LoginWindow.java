package ru.kpfu.itis.client;

import ru.kpfu.itis.common.GameConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JTextField hostField;
    private JTextField portField;
    private JComboBox<String> characterCombo;
    private JButton connectButton;
    private JButton startServerButton;
    private JButton singlePlayerButton;
    private JLabel statusLabel;

    public LoginWindow() {
        setTitle("Diamond Dungeons - Вход");
        setSize(600, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        initUI();
    }

    private void initUI() {
        // Главная панель
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(40, 44, 52));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Заголовок
        JLabel titleLabel = new JLabel("💎 DIAMOND DUNGEONS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 215, 0));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Разделитель
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(100, 100, 120));
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 20, 0);
        mainPanel.add(separator, gbc);

        gbc.insets = new Insets(8, 10, 8, 10);

        // Имя игрока
        JLabel nameLabel = createStyledLabel("👤 Имя игрока:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        mainPanel.add(nameLabel, gbc);

        usernameField = createStyledTextField("Игрок");
        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);

        // Персонаж
        JLabel charLabel = createStyledLabel("🎭 Персонаж:");
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(charLabel, gbc);

        characterCombo = new JComboBox<>(GameConstants.CHARACTER_NAMES);
        styleComboBox(characterCombo);
        gbc.gridx = 1;
        mainPanel.add(characterCombo, gbc);

        // Хост
        JLabel hostLabel = createStyledLabel("🌐 Хост:");
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(hostLabel, gbc);

        hostField = createStyledTextField("localhost");
        gbc.gridx = 1;
        mainPanel.add(hostField, gbc);

        // Порт
        JLabel portLabel = createStyledLabel("🔌 Порт:");
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(portLabel, gbc);

        portField = createStyledTextField("7777");
        gbc.gridx = 1;
        mainPanel.add(portField, gbc);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        startServerButton = createStyledButton("🚀 Запустить сервер", new Color(33, 150, 243));
        startServerButton.addActionListener(e -> startServer());
        buttonPanel.add(startServerButton);

        connectButton = createStyledButton("🔗 Подключиться", new Color(76, 175, 80));
        connectButton.addActionListener(e -> connectToServer());
        buttonPanel.add(connectButton);

        singlePlayerButton = createStyledButton("🎮 Одиночная игра", new Color(156, 39, 176));
        singlePlayerButton.addActionListener(e -> startSinglePlayer());
        buttonPanel.add(singlePlayerButton);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        // Статус
        statusLabel = new JLabel("✅ Готов к подключению", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(100, 255, 100));
        gbc.gridy = 7; gbc.insets = new Insets(20, 0, 0, 0);
        mainPanel.add(statusLabel, gbc);

        // Информация
        JLabel infoLabel = new JLabel(
                "<html><div style='text-align: center; color: #aaaaaa;'>" +
                        "Для игры: запустите сервер, затем подключитесь<br>" +
                        "Максимум 3 игрока, минимально 2 игрока" +
                        "</div></html>",
                SwingConstants.CENTER
        );
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 8;
        mainPanel.add(infoLabel, gbc);

        add(mainPanel);

        // Обработчик изменения размера
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateFontSizes();
            }
        });

        getRootPane().setDefaultButton(connectButton);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text, 15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(new Color(60, 64, 72));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 120), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(new Color(60, 64, 72));
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 2),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void updateFontSizes() {
        int width = getWidth();
        int fontSize = Math.max(12, Math.min(16, width / 40));

        Font fieldFont = new Font("Segoe UI", Font.PLAIN, fontSize);
        Font labelFont = new Font("Segoe UI", Font.BOLD, fontSize);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, fontSize);

        usernameField.setFont(fieldFont);
        hostField.setFont(fieldFont);
        portField.setFont(fieldFont);
        characterCombo.setFont(fieldFont);
        connectButton.setFont(buttonFont);
        startServerButton.setFont(buttonFont);
        singlePlayerButton.setFont(buttonFont);
    }

    private void startServer() {
        String portText = portField.getText().trim();

        if (portText.isEmpty()) {
            showError("Введите порт для сервера");
            return;
        }

        try {
            int port = Integer.parseInt(portText);

            statusLabel.setText("🚀 Запуск сервера...");
            statusLabel.setForeground(Color.YELLOW);

            new Thread(() -> {
                try {
                    ru.kpfu.itis.server.ServerMain.main(new String[]{portText});
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("❌ Ошибка запуска");
                        statusLabel.setForeground(Color.RED);
                        showError("Ошибка: " + e.getMessage());
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showError("Неверный формат порта");
        }
    }

    private void connectToServer() {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        String characterType = (String) characterCombo.getSelectedItem();

        // Проверка
        if (username.isEmpty() || username.length() < 2 || username.length() > 15) {
            showError("Имя должно быть 2-15 символов");
            return;
        }

        if (host.isEmpty()) {
            showError("Введите хост сервера");
            return;
        }

        if (portText.isEmpty()) {
            showError("Введите порт сервера");
            return;
        }

        try {
            int port = Integer.parseInt(portText);

            statusLabel.setText("🔗 Подключение...");
            statusLabel.setForeground(Color.YELLOW);

            new Thread(() -> {
                try (Socket testSocket = new Socket()) {
                    testSocket.connect(new InetSocketAddress(host, port), 3000);

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("✅ Подключено");
                        statusLabel.setForeground(Color.GREEN);

                        dispose();
                        new GameClient(host, port, username, characterType).setVisible(true);
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("❌ Не удалось подключиться");
                        statusLabel.setForeground(Color.RED);

                        int choice = JOptionPane.showConfirmDialog(
                                LoginWindow.this,
                                "Сервер недоступен. Запустить сервер на порту " + port + "?",
                                "Сервер недоступен",
                                JOptionPane.YES_NO_OPTION
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            startServer();
                        }
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showError("Неверный формат порта");
        }
    }

    private void startSinglePlayer() {
        String username = usernameField.getText().trim();
        String characterType = (String) characterCombo.getSelectedItem();

        if (username.isEmpty() || username.length() < 2) {
            showError("Введите имя игрока");
            return;
        }

        int port = 7778;
        hostField.setText("localhost");
        portField.setText(String.valueOf(port));

        JOptionPane.showMessageDialog(this,
                "🎮 Одиночная игра\n\n" +
                        "Запускается локальный сервер на порту " + port + "\n" +
                        "Вы подключитесь как первый игрок\n" +
                        "Можно пригласить друзей подключиться",
                "Одиночная игра",
                JOptionPane.INFORMATION_MESSAGE);

        new Thread(() -> {
            try {
                ru.kpfu.itis.server.ServerMain.main(new String[]{String.valueOf(port)});
            } catch (Exception e) {
                // Игнорируем в этом потоке
            }
        }).start();

        Timer timer = new Timer(2000, e -> connectToServer());
        timer.setRepeats(false);
        timer.start();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            LoginWindow window = new LoginWindow();
            window.setVisible(true);

            JOptionPane.showMessageDialog(window,
                    "Добро пожаловать в Diamond Dungeons!\n\n" +
                            "🎮 Как начать:\n" +
                            "1. Запустите сервер (кнопка выше)\n" +
                            "2. Подключитесь к нему\n" +
                            "3. Пригласите друзей подключиться к вашему серверу\n\n" +
                            "⚡ Управление:\n" +
                            "• WASD/Стрелки - движение\n" +
                            "• Пробел - действие\n" +
                            "• Enter - чат",
                    "Справка",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
}