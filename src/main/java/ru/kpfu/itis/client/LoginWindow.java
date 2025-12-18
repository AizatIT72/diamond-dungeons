package ru.kpfu.itis.client;

import ru.kpfu.itis.common.GameConstants;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
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
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        initUI();
    }

    private void initUI() {

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(40, 44, 52));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("<html><nobr><center>💎 DIAMOND DUNGEONS</center></nobr></html>", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 215, 0));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(100, 100, 120));
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 20, 0);
        mainPanel.add(separator, gbc);

        gbc.insets = new Insets(8, 10, 8, 10);

        JLabel nameLabel = createStyledLabel("👤 Имя игрока:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        mainPanel.add(nameLabel, gbc);

        usernameField = createStyledTextField("Игрок");
        gbc.gridx = 1;
        mainPanel.add(usernameField, gbc);

        JLabel charLabel = createStyledLabel("🎭 Персонаж:");
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(charLabel, gbc);

        final Color baseColor = new Color(60, 64, 72);
        final float[] comboHoverAlpha = {1.0f};
        final int[] hoveredIndex = {-1};

        characterCombo = new JComboBox<String>(GameConstants.CHARACTER_NAMES) {
            @Override
            protected void paintComponent(Graphics g) {

                if (getWidth() <= 0 || getHeight() <= 0) return;

                BufferedImage buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dBuffer = buffer.createGraphics();
                g2dBuffer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                super.paintComponent(g2dBuffer);

                g2dBuffer.dispose();

                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, comboHoverAlpha[0]));
                g2d.drawImage(buffer, 0, 0, null);
                g2d.dispose();
            }
        };

        styleComboBox(characterCombo, comboHoverAlpha, hoveredIndex);
        gbc.gridx = 1;
        mainPanel.add(characterCombo, gbc);

        JLabel hostLabel = createStyledLabel("🌐 Хост:");
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(hostLabel, gbc);

        hostField = createStyledTextField("localhost");
        gbc.gridx = 1;
        mainPanel.add(hostField, gbc);

        JLabel portLabel = createStyledLabel("🔌 Порт:");
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(portLabel, gbc);

        portField = createStyledTextField("7777");
        gbc.gridx = 1;
        mainPanel.add(portField, gbc);

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

        statusLabel = new JLabel("<html><nobr><center>✅ Готов к подключению</center></nobr></html>", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(100, 255, 100));
        gbc.gridy = 7; gbc.insets = new Insets(20, 0, 0, 0);
        mainPanel.add(statusLabel, gbc);

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

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateFontSizes();
            }
        });

        getRootPane().setDefaultButton(connectButton);
    }

    private JLabel createStyledLabel(String text) {

        JLabel label = new JLabel("<html><div style='white-space: nowrap; display: inline-block;'>" + text + "</div></html>");
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

    private void styleComboBox(JComboBox<String> combo, final float[] hoverAlpha, final int[] hoveredIndex) {
        final Color baseColor = new Color(60, 64, 72);

        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(baseColor);
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 120), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        class TransparentListCell extends JLabel {
            private final int cellIndex;

            public TransparentListCell(int index) {
                this.cellIndex = index;
                setFont(combo.getFont());
                setForeground(Color.WHITE);
                setBackground(baseColor);
                setOpaque(true);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            }

            @Override
            protected void paintComponent(Graphics g) {

                float alpha = (cellIndex == hoveredIndex[0]) ? 0.75f : 1.0f;

                if (getWidth() <= 0 || getHeight() <= 0) return;

                BufferedImage buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dBuffer = buffer.createGraphics();
                g2dBuffer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2dBuffer.setColor(baseColor);
                g2dBuffer.fillRect(0, 0, getWidth(), getHeight());

                g2dBuffer.setColor(getForeground());
                g2dBuffer.setFont(getFont());
                FontMetrics fm = g2dBuffer.getFontMetrics();
                String text = getText();
                if (text != null) {
                    int textY = (getHeight() + fm.getAscent()) / 2 - 2;
                    g2dBuffer.drawString(text, 8, textY);
                }

                g2dBuffer.dispose();

                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.drawImage(buffer, 0, 0, null);
                g2d.dispose();
            }
        }

        combo.setRenderer(new ListCellRenderer<String>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                TransparentListCell cell = new TransparentListCell(index);
                cell.setText(value != null ? value : "");
                return cell;
            }
        });

        UIManager.put("ComboBox.background", baseColor);
        UIManager.put("ComboBox.buttonBackground", baseColor);
        UIManager.put("ComboBox.selectionBackground", baseColor);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton();
                button.setBackground(baseColor);
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setFocusPainted(false);
                button.setContentAreaFilled(false);
                return button;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();

                SwingUtilities.invokeLater(() -> {
                    try {
                        java.lang.reflect.Method getListMethod = popup.getClass().getMethod("getList");
                        JList<?> list = (JList<?>) getListMethod.invoke(popup);

                        if (list != null) {

                            list.addMouseMotionListener(new MouseMotionAdapter() {
                                @Override
                                public void mouseMoved(MouseEvent e) {
                                    int index = list.locationToIndex(e.getPoint());
                                    if (index >= 0 && index != hoveredIndex[0]) {
                                        hoveredIndex[0] = index;
                                        list.repaint();
                                    }
                                }
                            });

                            list.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseExited(MouseEvent e) {
                                    hoveredIndex[0] = -1;
                                    list.repaint();
                                }
                            });
                        }
                    } catch (Exception e) {

                    }
                });

                return popup;
            }
        });

        combo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverAlpha[0] = 0.75f; 
                combo.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverAlpha[0] = 1.0f; 
                combo.repaint();
            }
        });
    }

    private JButton createStyledButton(String text, Color color) {

        final Color buttonColor = color;
        final float[] hoverAlpha = {1.0f}; 

        JButton button = new JButton("<html>" + text + "</html>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha[0]));

                g2d.setColor(buttonColor);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(buttonColor.darker());
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                g2d.dispose();

                Graphics textG = g.create();
                textG.setColor(Color.WHITE);

                super.paintComponent(textG);
                textG.dispose();
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false); 
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverAlpha[0] = 0.75f; 
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverAlpha[0] = 1.0f; 
                button.repaint();
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

                    statusLabel.setText("<html><nobr>🚀 Запуск сервера...</nobr></html>");
                    statusLabel.setForeground(Color.YELLOW);

                    new Thread(() -> {
                        try {
                            ru.kpfu.itis.server.ServerMain.main(new String[]{portText});
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> {
                                statusLabel.setText("<html><nobr>❌ Ошибка запуска</nobr></html>");
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

            statusLabel.setText("<html><nobr>🔗 Подключение...</nobr></html>");
            statusLabel.setForeground(Color.YELLOW);

            new Thread(() -> {
                try (Socket testSocket = new Socket()) {
                    testSocket.connect(new InetSocketAddress(host, port), 3000);

                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("<html><nobr>✅ Подключено</nobr></html>");
                        statusLabel.setForeground(Color.GREEN);

                        dispose();
                        new GameClient(host, port, username, characterType).setVisible(true);
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("<html><nobr>❌ Не удалось подключиться</nobr></html>");
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