package ru.kpfu.itis;

import ru.kpfu.itis.frame.MyFrame;
import ru.kpfu.itis.panel.MyPanel;

public class Main {
    public static void main(String[] args) {
        MyFrame frame = new MyFrame(new MyPanel());
        frame.setVisible(true);
    }
}
