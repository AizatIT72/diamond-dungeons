//package ru.kpfu.itis.client.ui;
//
//import javax.swing.*;
//import java.awt.*;
//
//public class ProfilePanel extends JPanel {
//    private MainWindow mainWindow;
//    private String username = "Гость";
//    private String characterType = "Не выбран";
//
//    public ProfilePanel(MainWindow mainWindow) {
//        this.mainWindow = mainWindow;
//        setLayout(new BorderLayout());
//        setBackground(new Color(30, 30, 40));
//        initUI();
//    }
//
//    private void initUI() {
//        JPanel headerPanel = createHeaderPanel();
//
//        JPanel profilePanel = createProfileContentPanel();
//
//        JPanel statsPanel = createStatsPanel();
//
//        JPanel buttonPanel = createButtonPanel();
//
//        add(headerPanel, BorderLayout.NORTH);
//        add(profilePanel, BorderLayout.CENTER);
//        add(statsPanel, BorderLayout.EAST);
//        add(buttonPanel, BorderLayout.SOUTH);
//    }
//
//    private JPanel createHeaderPanel() {
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBackground(new Color(40, 40, 50));
//        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
//
//        JLabel titleLabel = new JLabel("ПРОФИЛЬ ИГРОКА", SwingConstants.CENTER);
//        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
//        titleLabel.setForeground(new Color(255, 215, 0));
//
//        panel.add(titleLabel, BorderLayout.CENTER);
//
//        JButton backButton = new JButton("Назад");
//        backButton.addActionListener(e -> mainWindow.showLoginPanel());
//        panel.add(backButton, BorderLayout.EAST);
//
//        return panel;
//    }
//
//    private JPanel createProfileContentPanel() {
//        JPanel panel = new JPanel(new GridBagLayout());
//        panel.setOpaque(false);
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(20, 20, 20, 20);
//
//        AvatarPanel avatarPanel = new AvatarPanel();
//        avatarPanel.setPreferredSize(new Dimension(200, 250));
//
//        JPanel infoPanel = new JPanel();
//        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
//        infoPanel.setOpaque(false);
//
//        JLabel nameLabel = new JLabel("Имя: " + username);
//        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
//        nameLabel.setForeground(Color.WHITE);
//
//        JLabel characterLabel = new JLabel("Персонаж: " + characterType);
//        characterLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//        characterLabel.setForeground(Color.WHITE);
//
//        JLabel levelLabel = new JLabel("Уровень: 1");
//        levelLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//        levelLabel.setForeground(Color.WHITE);
//
//        JLabel expLabel = new JLabel("Опыт: 0/100");
//        expLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//        expLabel.setForeground(Color.WHITE);
//
//        JProgressBar expBar = new JProgressBar(0, 100);
//        expBar.setValue(0);
//        expBar.setStringPainted(true);
//        expBar.setForeground(new Color(50, 200, 50));
//        expBar.setBackground(new Color(50, 50, 50));
//        expBar.setPreferredSize(new Dimension(200, 20));
//
//        infoPanel.add(nameLabel);
//        infoPanel.add(Box.createVerticalStrut(10));
//        infoPanel.add(characterLabel);
//        infoPanel.add(Box.createVerticalStrut(10));
//        infoPanel.add(levelLabel);
//        infoPanel.add(Box.createVerticalStrut(10));
//        infoPanel.add(expLabel);
//        infoPanel.add(Box.createVerticalStrut(10));
//        infoPanel.add(expBar);
//
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.anchor = GridBagConstraints.CENTER;
//        panel.add(avatarPanel, gbc);
//
//        gbc.gridx = 1;
//        gbc.anchor = GridBagConstraints.WEST;
//        panel.add(infoPanel, gbc);
//
//        return panel;
//    }
//
//    private JPanel createStatsPanel() {
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        panel.setBackground(new Color(40, 40, 50));
//        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//        panel.setPreferredSize(new Dimension(250, 0));
//
//        JLabel title = new JLabel("СТАТИСТИКА");
//        title.setFont(new Font("Arial", Font.BOLD, 20));
//        title.setForeground(Color.YELLOW);
//        title.setAlignmentX(Component.CENTER_ALIGNMENT);
//
//        panel.add(title);
//        panel.add(Box.createVerticalStrut(20));
//
//        String[][] stats = {
//                {"Пройдено уровней", "0"},
//                {"Собрано алмазов", "0"},
//                {"Смертей", "0"},
//                {"Время в игре", "0:00"},
//                {"Рекорд уровня", "N/A"},
//                {"Игр с друзьями", "0"}
//        };
//
//        for (String[] stat : stats) {
//            JPanel statPanel = new JPanel(new BorderLayout());
//            statPanel.setOpaque(false);
//            statPanel.setMaximumSize(new Dimension(200, 40));
//
//            JLabel keyLabel = new JLabel(stat[0] + ":");
//            keyLabel.setForeground(new Color(200, 200, 200));
//            keyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
//
//            JLabel valueLabel = new JLabel(stat[1]);
//            valueLabel.setForeground(Color.WHITE);
//            valueLabel.setFont(new Font("Arial", Font.BOLD, 14));
//            valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
//
//            statPanel.add(keyLabel, BorderLayout.WEST);
//            statPanel.add(valueLabel, BorderLayout.EAST);
//
//            panel.add(statPanel);
//            panel.add(Box.createVerticalStrut(5));
//        }
//
//        panel.add(Box.createVerticalGlue());
//
//        return panel;
//    }
//
//    private JPanel createButtonPanel() {
//        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
//        panel.setOpaque(false);
//
//        JButton editButton = createStyledButton("Редактировать профиль", new Color(33, 150, 243));
//        JButton achievementsButton = createStyledButton("Достижения", new Color(156, 39, 176));
//        JButton historyButton = createStyledButton("История игр", new Color(255, 87, 34));
//
//        editButton.addActionListener(e -> showEditDialog());
//
//        panel.add(editButton);
//        panel.add(achievementsButton);
//        panel.add(historyButton);
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
//        button.setPreferredSize(new Dimension(200, 40));
//        button.setBorder(BorderFactory.createEmptyBorder());
//        button.setContentAreaFilled(false);
//        button.setFocusPainted(false);
//
//        return button;
//    }
//
//    private void showEditDialog() {
//        JDialog editDialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Редактирование профиля", true);
//        editDialog.setSize(400, 300);
//        editDialog.setLocationRelativeTo(this);
//
//        JPanel panel = new JPanel(new GridBagLayout());
//        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(10, 10, 10, 10);
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//
//        JLabel nameLabel = new JLabel("Новое имя:");
//        gbc.gridx = 0; gbc.gridy = 0;
//        panel.add(nameLabel, gbc);
//
//        JTextField nameField = new JTextField(username, 15);
//        gbc.gridx = 1; gbc.gridwidth = 2;
//        panel.add(nameField, gbc);
//
//        JButton saveButton = new JButton("Сохранить");
//        JButton cancelButton = new JButton("Отмена");
//
//        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1;
//        panel.add(saveButton, gbc);
//
//        gbc.gridx = 2;
//        panel.add(cancelButton, gbc);
//
//        saveButton.addActionListener(e -> {
//            String newName = nameField.getText().trim();
//            if (!newName.isEmpty()) {
//                username = newName;
//                editDialog.dispose();
//                mainWindow.showProfilePanel();
//            }
//        });
//
//        cancelButton.addActionListener(e -> editDialog.dispose());
//
//        editDialog.add(panel);
//        editDialog.setVisible(true);
//    }
//
//    class AvatarPanel extends JPanel {
//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2d = (Graphics2D) g;
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            int width = getWidth();
//            int height = getHeight();
//
//            GradientPaint bgGradient = new GradientPaint(
//                    0, 0, new Color(60, 60, 80),
//                    width, height, new Color(40, 40, 60)
//            );
//            g2d.setPaint(bgGradient);
//            g2d.fillRoundRect(0, 0, width, height, 20, 20);
//
//            Color playerColor;
//            switch (characterType) {
//                case "Красный воин":
//                    playerColor = new Color(255, 100, 100);
//                    break;
//                case "Синий маг":
//                    playerColor = new Color(100, 150, 255);
//                    break;
//                case "Зеленый плут":
//                    playerColor = new Color(100, 255, 100);
//                    break;
//                default:
//                    playerColor = Color.GRAY;
//            }
//
//            int centerX = width / 2;
//            int centerY = height / 2;
//            int size = Math.min(width, height) - 40;
//
//            g2d.setColor(playerColor);
//            g2d.fillOval(centerX - size/2, centerY - size/2, size, size);
//
//            g2d.setColor(playerColor.darker());
//            if (characterType.equals("Красный воин")) {
//                g2d.fillRect(centerX + size/4, centerY - size/6, size/6, size/2);
//                g2d.fillRect(centerX - size/3, centerY - size/6, size/8, size/2);
//            } else if (characterType.equals("Синий маг")) {
//                g2d.setColor(new Color(200, 200, 255, 150));
//                g2d.fillOval(centerX - size/3, centerY - size/3, size/3, size/3);
//            } else if (characterType.equals("Зеленый плут")) {
//                int[] xPoints = {centerX, centerX + size/3, centerX - size/3};
//                int[] yPoints = {centerY - size/2, centerY, centerY};
//                g2d.fillPolygon(xPoints, yPoints, 3);
//            }
//
//            g2d.setColor(Color.WHITE);
//            g2d.setStroke(new BasicStroke(3));
//            g2d.drawRoundRect(0, 0, width, height, 20, 20);
//
//            g2d.setFont(new Font("Arial", Font.BOLD, 16));
//            FontMetrics fm = g2d.getFontMetrics();
//            String text = "Аватар";
//            int textWidth = fm.stringWidth(text);
//            g2d.drawString(text, centerX - textWidth/2, height - 10);
//        }
//    }
//}
