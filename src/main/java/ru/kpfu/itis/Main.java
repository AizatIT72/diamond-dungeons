package ru.kpfu.itis;

import ru.kpfu.itis.client.ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);

            JOptionPane.showMessageDialog(mainWindow,
                    "Diamond Dungeons - Кооперативная игра\n\n" +
                            "Текущая версия: Демо\n" +
                            "Функции:\n" +
                            "• Вход и выбор персонажа\n" +
                            "• Игровая панель с движением\n" +
                            "• Анимированные алмазы\n" +
                            "• Панель профиля\n" +
                            "• Управление сервером\n\n" +
                            "Управление в игре: WASD или стрелки",
                    "Добро пожаловать!",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
