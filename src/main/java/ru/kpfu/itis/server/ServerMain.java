package ru.kpfu.itis.server;

import ru.kpfu.itis.common.GameConstants;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerMain {
    private static GameServer server;
    private static int actualPort = -1;

    public static void main(String[] args) {
        try {
            System.out.println("╔═══════════════════════════════════════╗");
            System.out.println("║     DIAMOND DUNGEONS - СЕРВЕР        ║");
            System.out.println("╚═══════════════════════════════════════╝");

            // Пробуем стандартный порт или из аргументов
            int port = GameConstants.SERVER_PORT;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Неверный порт, используем " + GameConstants.SERVER_PORT);
                    port = GameConstants.SERVER_PORT;
                }
            }

            // Запускаем сервер
            if (startServer(port)) {
                System.out.println("\n✅ Сервер успешно запущен на порту " + actualPort);
                System.out.println("👥 Ожидание подключения игроков...");
                System.out.println("🛑 Для остановки нажмите Enter");

                // Ожидание остановки
                System.in.read();

                server.stop();
                System.out.println("🛑 Сервер остановлен");
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка: " + e.getMessage());
        }
    }

    private static boolean startServer(int port) {
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            int currentPort = port + i;

            try {
                // Проверяем порт
                ServerSocket testSocket = new ServerSocket(currentPort);
                testSocket.close();

                // Запускаем сервер
                server = new GameServer(currentPort);
                server.start();
                actualPort = currentPort;

                return true;

            } catch (java.net.BindException e) {
                System.out.println("⚠️  Порт " + currentPort + " занят");
            } catch (IOException e) {
                System.err.println("❌ Ошибка на порту " + currentPort + ": " + e.getMessage());
            }
        }

        System.err.println("❌ Не удалось запустить сервер");
        return false;
    }

    public static int getActualPort() {
        return actualPort;
    }
}