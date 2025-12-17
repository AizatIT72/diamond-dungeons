package ru.kpfu.itis;

import ru.kpfu.itis.client.LoginWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
//        // Устанавливаем красивый Look and Feel
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // Запускаем окно входа в EDT (Event Dispatch Thread)
//        SwingUtilities.invokeLater(() -> {
//            LoginWindow loginWindow = new LoginWindow();
//            loginWindow.setVisible(true);
//
//            // Показываем приветственное сообщение
//            JOptionPane.showMessageDialog(loginWindow,
//                    "Добро пожаловать в Diamond Dungeons!\n\n" +
//                            "Как играть:\n" +
//                            "1. Сначала запустите сервер (ServerMain.java)\n" +
//                            "2. Затем запустите 3 клиента (этот файл 3 раза)\n" +
//                            "3. Подключитесь к localhost:7777\n" +
//                            "4. Собирайте алмазы и находите выход!\n\n" +
//                            "Управление: WASD/стрелки - движение, Пробел - действие",
//                    "Diamond Dungeons",
//                    JOptionPane.INFORMATION_MESSAGE);
//        });
        ru.kpfu.itis.client.LoginWindow.main(args);
    }
}