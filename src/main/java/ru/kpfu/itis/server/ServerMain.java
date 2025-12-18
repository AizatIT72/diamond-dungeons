package ru.kpfu.itis.server;

import ru.kpfu.itis.common.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    private static GameServer server;
    private static int actualPort = -1;

    public static void main(String[] args) {
        try {
            logger.info("╔═══════════════════════════════════════╗");
            logger.info("║     DIAMOND DUNGEONS - СЕРВЕР        ║");
            logger.info("╚═══════════════════════════════════════╝");

            int port = GameConstants.SERVER_PORT;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    logger.warn("Неверный порт, используем {}", GameConstants.SERVER_PORT);
                    port = GameConstants.SERVER_PORT;
                }
            }

            if (startServer(port)) {
                logger.info("\n✅ Сервер успешно запущен на порту {}", actualPort);
                logger.info("👥 Ожидание подключения игроков...");
                logger.info("🛑 Для остановки нажмите Enter");

                System.in.read();

                server.stop();
                logger.info("🛑 Сервер остановлен");
            }

        } catch (Exception e) {
            logger.error("Ошибка", e);
        }
    }

    private static boolean startServer(int port) {
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            int currentPort = port + i;

            try {
                ServerSocket testSocket = new ServerSocket(currentPort);
                testSocket.close();

                server = new GameServer(currentPort);
                server.start();
                actualPort = currentPort;

                return true;

            } catch (java.net.BindException e) {
                logger.warn("Порт {} занят", currentPort);
            } catch (IOException e) {
                logger.error("Ошибка на порту {}", currentPort, e);
            }
        }

        logger.error("Не удалось запустить сервер");
        return false;
    }

    public static int getActualPort() {
        return actualPort;
    }
}