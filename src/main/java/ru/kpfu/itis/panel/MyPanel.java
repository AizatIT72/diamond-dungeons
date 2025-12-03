package ru.kpfu.itis.panel;

import javax.swing.*;
import java.awt.*;

public class MyPanel extends JPanel {

    public MyPanel() {
        setBackground(new Color(30, 30, 50));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        g2d.setColor(new Color(255, 215, 0));

        FontMetrics fm = g2d.getFontMetrics();
        String title = "DIAMOND RUSH";
        int titleWidth = fm.stringWidth(title);
        int x = (getWidth() - titleWidth) / 2;
        int y = getHeight() / 2 - 50;

        g2d.drawString(title, x, y);

        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.WHITE);
        String subtitle = "The Ultimate Adventure Game";
        fm = g2d.getFontMetrics();
        int subtitleWidth = fm.stringWidth(subtitle);
        x = (getWidth() - subtitleWidth) / 2;
        y = getHeight() / 2 + 40;
        g2d.drawString(subtitle, x, y);

        g2d.setFont(new Font("Arial", Font.ITALIC, 18));
        g2d.setColor(Color.LIGHT_GRAY);
        String instruction = "Press SPACE to start...";
        fm = g2d.getFontMetrics();
        int instructionWidth = fm.stringWidth(instruction);
        x = (getWidth() - instructionWidth) / 2;
        y = getHeight() - 100;
        g2d.drawString(instruction, x, y);
    }
}