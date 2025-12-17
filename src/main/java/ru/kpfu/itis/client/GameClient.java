package ru.kpfu.itis.client;

import ru.kpfu.itis.common.*;
import ru.kpfu.itis.server.GameWorld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameClient extends JFrame {
    private NetworkClient networkClient;
    private GamePanel gamePanel;
    private ChatPanel chatPanel;
    private PlayerInfoPanel infoPanel;
    private String username;
    private String characterType;
    private int playerId = -1;

    public GameClient(String host, int port, String username, String characterType) {
        this.username = username;
        this.characterType = characterType;

        setTitle("Diamond Dungeons - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(true);

        initUI();
        connectToServer(host, port);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        infoPanel = new PlayerInfoPanel();
        add(infoPanel, BorderLayout.NORTH);

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        chatPanel = new ChatPanel(this::sendChatMessage);
        add(chatPanel, BorderLayout.SOUTH);

        setupKeyBindings();
    }

    private void connectToServer(String host, int port) {
        networkClient = new NetworkClient();

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnMessageReceived(this::handleMessage);

        boolean connected = networkClient.connect(host, port, username, characterType);

        if (!connected) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось подключиться к " + host + ":" + port,
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        } else {
            // Получаем playerId после подключения
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Даем время на получение CONNECT сообщения
                    SwingUtilities.invokeLater(() -> {
                        playerId = networkClient.getPlayerId();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void updateGameState(GameWorld.GameState state) {
        SwingUtilities.invokeLater(() -> {
            gamePanel.updateGameState(state, playerId);
            infoPanel.updateInfo(state, playerId);
        });
    }

    private void handleMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case Message.CHAT:
                    String sender = message.getPlayerId() == 0 ? "Сервер" :
                            message.getPlayerId() == playerId ? "Вы" :
                                    "Игрок " + message.getPlayerId();
                    chatPanel.addMessage(sender + ": " + message.getData());
                    break;

                case Message.ACTION:
                case Message.LEVEL_UPDATE:
                    chatPanel.addMessage("⚡ " + message.getData());
                    break;
            }
        });
    }

    private void setupKeyBindings() {
        InputMap inputMap = gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = gamePanel.getActionMap();

        // WASD клавиши
        String[] wasdKeys = {"W", "S", "A", "D"};
        Direction[] wasdDirections = {
                Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
        };

        for (int i = 0; i < wasdKeys.length; i++) {
            final Direction dir = wasdDirections[i];
            inputMap.put(KeyStroke.getKeyStroke(wasdKeys[i]), "move" + dir);
            actionMap.put("move" + dir, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (networkClient != null && networkClient.isConnected()) {
                        networkClient.sendMove(dir);
                    }
                }
            });
        }

        // Стрелки
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUP");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDOWN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLEFT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRIGHT");
        
        actionMap.put("moveUP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendMove(Direction.UP);
                }
            }
        });
        actionMap.put("moveDOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendMove(Direction.DOWN);
                }
            }
        });
        actionMap.put("moveLEFT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendMove(Direction.LEFT);
                }
            }
        });
        actionMap.put("moveRIGHT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendMove(Direction.RIGHT);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "action");
        actionMap.put("action", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkClient != null) {
                    networkClient.sendAction("Использовать");
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "chat");
        actionMap.put("chat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chatPanel.inputField.requestFocusInWindow();
            }
        });

        gamePanel.requestFocusInWindow();
    }

    private void sendChatMessage(String text) {
        if (networkClient != null && !text.trim().isEmpty()) {
            networkClient.sendChat(text);
            chatPanel.addMessage("Вы: " + text);
        }
    }

    @Override
    public void dispose() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        super.dispose();
    }

    class PlayerInfoPanel extends JPanel {
        private JLabel healthLabel;
        private JLabel diamondsLabel;
        private JLabel levelLabel;
        private JLabel playersLabel;

        public PlayerInfoPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5));
            setBackground(new Color(40, 40, 50));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            healthLabel = createInfoLabel("❤ Здоровье: 100/100", new Color(255, 100, 100));
            diamondsLabel = createInfoLabel("💎 Алмазы: 0", new Color(100, 200, 255));
            levelLabel = createInfoLabel("📊 Уровень: 1", Color.YELLOW);
            playersLabel = createInfoLabel("👥 Игроков: 1/3", new Color(100, 255, 100));

            add(healthLabel);
            add(diamondsLabel);
            add(levelLabel);
            add(playersLabel);
        }

        private JLabel createInfoLabel(String text, Color color) {
            // Используем HTML с nowrap для отображения в одну строку
            JLabel label = new JLabel("<html><nobr>" + text + "</nobr></html>");
            label.setForeground(color);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            return label;
        }

        public void updateInfo(GameWorld.GameState state, int currentPlayerId) {
            if (state == null) return;

            PlayerState player = null;
            for (PlayerState p : state.players) {
                if (p.id == currentPlayerId) {
                    player = p;
                    break;
                }
            }

            if (player != null) {
                healthLabel.setText("<html><nobr>❤ Здоровье: " + player.health + "/" + player.maxHealth + "</nobr></html>");
                diamondsLabel.setText("<html><nobr>💎 Алмазы: " + player.diamonds + "</nobr></html>");
            }

            levelLabel.setText("<html><nobr>📊 Уровень: " + state.currentLevel + "</nobr></html>");
            playersLabel.setText("<html><nobr>👥 Игроков: " + state.players.size() + "/3</nobr></html>");
        }
    }

    class ChatPanel extends JPanel {
        private JTextArea chatArea;
        private JTextField inputField;

        public ChatPanel(java.util.function.Consumer<String> onSend) {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(0, 150));
            setBackground(new Color(30, 30, 40));
            
            // Создаем заголовок с HTML для правильного отображения эмодзи без переноса
            JLabel titleLabel = new JLabel("<html><nobr><b>💬 Чат</b></nobr></html>");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            headerPanel.setBackground(new Color(40, 40, 50));
            headerPanel.add(titleLabel);
            
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setBackground(new Color(30, 30, 40));
            chatArea.setForeground(Color.WHITE);
            chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(chatArea);
            scrollPane.setPreferredSize(new Dimension(0, 100));
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 120)));

            inputField = new JTextField();
            inputField.addActionListener(e -> {
                onSend.accept(inputField.getText());
                inputField.setText("");
            });

            add(headerPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            add(inputField, BorderLayout.SOUTH);
        }

        public void addMessage(String message) {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }
}