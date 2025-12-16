package ru.kpfu.itis.client;

import ru.kpfu.itis.common.Direction;
import ru.kpfu.itis.common.PlayerState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;

public class GamePanel extends JPanel {

    private final NetworkClient net = new NetworkClient();
    private Collection<PlayerState> players;

    public GamePanel() {
        setPreferredSize(new Dimension(640, 640));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Direction d = switch (e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP -> Direction.UP;
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN -> Direction.DOWN;
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT -> Direction.LEFT;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> Direction.RIGHT;
                    default -> null;
                };
                if (d != null) net.send(d);
            }
        });
    }

    public void start() {
        net.connect(state -> {
            players = state;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (players == null) return;

        for (PlayerState p : players) {
            g.setColor(Color.RED);
            g.fillOval(p.x * 32, p.y * 32, 32, 32);
        }
    }
}
