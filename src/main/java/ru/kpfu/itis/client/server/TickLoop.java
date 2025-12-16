package ru.kpfu.itis.client.server;

public class TickLoop implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
    }
}
