//package ru.kpfu.itis.client.ui;
//
//import ru.kpfu.itis.common.GameConstants;
//
//import javax.swing.*;
//import java.awt.*;
//
//public class MainWindow extends JFrame {
//    private CardLayout cardLayout;
//    private JPanel mainPanel;
//
//    public MainWindow() {
//        super("Diamond Dungeons - Кооперативная игра");
//        initWindow();
//        initUI();
//    }
//
//    private void initWindow() {
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
//        setMinimumSize(new Dimension(GameConstants.MIN_WIDTH, GameConstants.MIN_HEIGHT));
//        setLocationRelativeTo(null);
//
//        ImageIcon icon = new ImageIcon("resources/icon.png");
//        if (icon.getImage() != null) {
//            setIconImage(icon.getImage());
//        }
//    }
//
//    private void initUI() {
//        cardLayout = new CardLayout();
//        mainPanel = new JPanel(cardLayout);
//
//        LoginPanel loginPanel = new LoginPanel(this);
//        GamePanel gamePanel = new GamePanel(this);
//        ProfilePanel profilePanel = new ProfilePanel(this);
//        ServerPanel serverPanel = new ServerPanel(this);
//
//        mainPanel.add(loginPanel, "LOGIN");
//        mainPanel.add(gamePanel, "GAME");
//        mainPanel.add(profilePanel, "PROFILE");
//        mainPanel.add(serverPanel, "SERVER");
//
//        add(mainPanel);
//
//        showLoginPanel();
//
//        addComponentListener(new java.awt.event.ComponentAdapter() {
//            @Override
//            public void componentResized(java.awt.event.ComponentEvent e) {
//                updateLayout();
//            }
//        });
//    }
//
//    public void showLoginPanel() {
//        cardLayout.show(mainPanel, "LOGIN");
//    }
//
//    public void showGamePanel() {
//        cardLayout.show(mainPanel, "GAME");
//    }
//
//    public void showProfilePanel() {
//        cardLayout.show(mainPanel, "PROFILE");
//    }
//
//    public void showServerPanel() {
//        cardLayout.show(mainPanel, "SERVER");
//    }
//
//    private void updateLayout() {
//        mainPanel.revalidate();
//        mainPanel.repaint();
//    }
//
//    public CardLayout getCardLayout() {
//        return cardLayout;
//    }
//
//    public JPanel getMainPanel() {
//        return mainPanel;
//    }
//}
