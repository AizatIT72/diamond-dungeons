//package ru.kpfu.itis.client.ui;
//
//import ru.kpfu.itis.common.GameConstants;
//
//import javax.swing.*;
//import java.awt.*;
//
//public class LoginPanel extends JPanel {
//    private MainWindow mainWindow;
//    private JTextField usernameField;
//    private JComboBox<String> characterCombo;
//    private JTextField serverAddressField;
//    private JButton loginButton;
//    private JButton serverModeButton;
//    private JButton singlePlayerButton; // Новая кнопка для одиночной игры
//
//    public LoginPanel(MainWindow mainWindow) {
//        this.mainWindow = mainWindow;
//        setLayout(new BorderLayout());
//        setBackground(new Color(30, 30, 40));
//        initUI();
//    }
//
//    private void initUI() {
//        JPanel headerPanel = createHeaderPanel();
//        JPanel formPanel = createFormPanel();
//        JPanel buttonPanel = createButtonPanel();
//
//        add(headerPanel, BorderLayout.NORTH);
//        add(formPanel, BorderLayout.CENTER);
//        add(buttonPanel, BorderLayout.SOUTH);
//    }
//
//    private JPanel createHeaderPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBackground(new Color(40, 40, 50));
//        panel.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
//
//        JLabel titleLabel = new JLabel("DIAMOND RUSH", SwingConstants.CENTER);
//        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
//        titleLabel.setForeground(new Color(255, 215, 0));
//
//        JLabel subtitleLabel = new JLabel("Кооперативная игра (1-3 игрока)", SwingConstants.CENTER);
//        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//        subtitleLabel.setForeground(Color.WHITE);
//
//        panel.add(titleLabel, BorderLayout.CENTER);
//        panel.add(subtitleLabel, BorderLayout.SOUTH);
//
//        return panel;
//    }
//
//    private JPanel createFormPanel() {
//        JPanel panel = new JPanel(new GridBagLayout());
//        panel.setOpaque(false);
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(10, 20, 10, 20);
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//
//        // Имя пользователя
//        gbc.gridx = 0; gbc.gridy = 0;
//        JLabel nameLabel = new JLabel("Имя игрока:");
//        nameLabel.setForeground(Color.WHITE);
//        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
//        panel.add(nameLabel, gbc);
//
//        gbc.gridx = 1; gbc.gridwidth = 2;
//        usernameField = new JTextField("Игрок", 20);
//        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
//        panel.add(usernameField, gbc);
//
//        // Персонаж
//        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
//        JLabel charLabel = new JLabel("Персонаж:");
//        charLabel.setForeground(Color.WHITE);
//        charLabel.setFont(new Font("Arial", Font.BOLD, 14));
//        panel.add(charLabel, gbc);
//
//        gbc.gridx = 1; gbc.gridwidth = 2;
//        characterCombo = new JComboBox<>(GameConstants.CHARACTER_NAMES);
//        characterCombo.setFont(new Font("Arial", Font.PLAIN, 14));
//        panel.add(characterCombo, gbc);
//
//        // Режим игры
//        gbc.gridx = 0; gbc.gridy = 2;
//        JLabel modeLabel = new JLabel("Режим:");
//        modeLabel.setForeground(Color.WHITE);
//        modeLabel.setFont(new Font("Arial", Font.BOLD, 14));
//        panel.add(modeLabel, gbc);
//
//        gbc.gridx = 1; gbc.gridwidth = 2;
//        JComboBox<String> gameModeCombo = new JComboBox<>(new String[]{
//                "Одиночная игра (демо)",
//                "Сетевая игра (требует сервер)"
//        });
//        gameModeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
//        panel.add(gameModeCombo, gbc);
//
//        // Адрес сервера (только для сетевого режима)
//        gbc.gridx = 0; gbc.gridy = 3;
//        JLabel serverLabel = new JLabel("Сервер:");
//        serverLabel.setForeground(Color.WHITE);
//        serverLabel.setFont(new Font("Arial", Font.BOLD, 14));
//        panel.add(serverLabel, gbc);
//
//        gbc.gridx = 1; gbc.gridwidth = 2;
//        serverAddressField = new JTextField("localhost:5555", 20);
//        serverAddressField.setFont(new Font("Arial", Font.PLAIN, 14));
//        panel.add(serverAddressField, gbc);
//
//        return panel;
//    }
//
//    private JPanel createButtonPanel() {
//        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
//        panel.setOpaque(false);
//
//        singlePlayerButton = createStyledButton("Одиночная игра", new Color(156, 39, 176));
//        singlePlayerButton.addActionListener(e -> startSinglePlayer());
//
//        loginButton = createStyledButton("Сетевая игра", new Color(76, 175, 80));
//        loginButton.addActionListener(e -> login());
//
//        serverModeButton = createStyledButton("Запустить сервер", new Color(33, 150, 243));
//        serverModeButton.addActionListener(e -> mainWindow.showServerPanel());
//
//        panel.add(singlePlayerButton);
//        panel.add(loginButton);
//        panel.add(serverModeButton);
//
//        return panel;
//    }
//
//    private JButton createStyledButton(String text, Color color) {
//        JButton button = new JButton(text) {
//            @Override
//            protected void paintComponent(Graphics g) {
//                Graphics2D g2 = (Graphics2D) g.create();
//                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//                g2.setColor(color);
//                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
//
//                g2.setColor(Color.WHITE);
//                g2.setFont(getFont().deriveFont(Font.BOLD, 14));
//                FontMetrics fm = g2.getFontMetrics();
//                int x = (getWidth() - fm.stringWidth(getText())) / 2;
//                int y = (getHeight() + fm.getAscent()) / 2 - 2;
//                g2.drawString(getText(), x, y);
//
//                g2.dispose();
//            }
//        };
//
//        button.setPreferredSize(new Dimension(180, 40));
//        button.setBorder(BorderFactory.createEmptyBorder());
//        button.setContentAreaFilled(false);
//        button.setFocusPainted(false);
//
//        return button;
//    }
//
//    private void startSinglePlayer() {
//        String username = usernameField.getText().trim();
//        if (username.isEmpty()) {
//            username = "Игрок";
//        }
//
//        // Сохраняем данные для одиночной игры
//        GameSession.getInstance().setUsername(username);
//        GameSession.getInstance().setCharacterType((String) characterCombo.getSelectedItem());
//        GameSession.getInstance().setSinglePlayer(true);
//
//        JOptionPane.showMessageDialog(this,
//                "Одиночная игра запущена!\n\n" +
//                        "Вы управляете всеми тремя персонажами\n" +
//                        "для тестирования механик игры.\n\n" +
//                        "Управление: WASD/стрелки",
//                "Одиночная игра",
//                JOptionPane.INFORMATION_MESSAGE);
//
//        mainWindow.showGamePanel();
//    }
//
//    private void login() {
//        String username = usernameField.getText().trim();
//        if (username.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Введите имя игрока!", "Ошибка", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        GameSession.getInstance().setUsername(username);
//        GameSession.getInstance().setCharacterType((String) characterCombo.getSelectedItem());
//        GameSession.getInstance().setSinglePlayer(false);
//
//        JOptionPane.showMessageDialog(this,
//                "Попытка подключения к серверу...\n\n" +
//                        "Для сетевой игры необходимо:\n" +
//                        "1. Запустить сервер\n" +
//                        "2. Подождать подключения 3 игроков\n" +
//                        "3. Начать игру\n\n" +
//                        "Пока включен демо-режим",
//                "Сетевая игра",
//                JOptionPane.INFORMATION_MESSAGE);
//
//        mainWindow.showGamePanel();
//    }
//}
