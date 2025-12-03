package ru.kpfu.itis.frame;

import ru.kpfu.itis.panel.MyPanel;

import javax.swing.*;

public class MyFrame extends JFrame {

    public MyFrame(MyPanel panel) {
        setTitle("Diamond Rush");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        add(panel);

        setVisible(true);
    }
}
