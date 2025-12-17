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

        String[] keys = {"W", "S", "A", "D", "UP", "DOWN", "LEFT", "RIGHT"};
        Direction[] directions = {
                Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT,
                Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
        };

        for (int i = 0; i < keys.length; i++) {
            final Direction dir = directions[i];
            inputMap.put(KeyStroke.getKeyStroke(keys[i]), "move" + dir);
            actionMap.put("move" + dir, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (networkClient != null && networkClient.isConnected()) {
                        networkClient.sendMove(dir);
                    }
                }
            });
        }

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

            healthLabel = createInfoLabel("❤️ Здоровье: 100/100", new Color(255, 100, 100));
            diamondsLabel = createInfoLabel("💎 Алмазы: 0", new Color(100, 200, 255));
            levelLabel = createInfoLabel("📊 Уровень: 1", Color.YELLOW);
            playersLabel = createInfoLabel("👥 Игроков: 1/3", new Color(100, 255, 100));

            add(healthLabel);
            add(diamondsLabel);
            add(levelLabel);
            add(playersLabel);
        }

        private JLabel createInfoLabel(String text, Color color) {
            JLabel label = new JLabel(text);
            label.setForeground(color);
            label.setFont(new Font("Arial", Font.BOLD, 14));
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
                healthLabel.setText("❤️ Здоровье: " + player.health + "/" + player.maxHealth);
                diamondsLabel.setText("💎 Алмазы: " + player.diamonds);
            }

            levelLabel.setText("📊 Уровень: " + state.currentLevel);
            playersLabel.setText("👥 Игроков: " + state.players.size() + "/3");
        }
    }

    class ChatPanel extends JPanel {
        private JTextArea chatArea;
        private JTextField inputField;

        public ChatPanel(java.util.function.Consumer<String> onSend) {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(0, 150));
            setBorder(BorderFactory.createTitledBorder("💬 Чат"));

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setBackground(new Color(30, 30, 40));
            chatArea.setForeground(Color.WHITE);
            chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(chatArea);
            scrollPane.setPreferredSize(new Dimension(0, 100));

            inputField = new JTextField();
            inputField.addActionListener(e -> {
                onSend.accept(inputField.getText());
                inputField.setText("");
            });

            add(scrollPane, BorderLayout.CENTER);
            add(inputField, BorderLayout.SOUTH);
        }

        public void addMessage(String message) {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }
}