package ru.kpfu.itis.client;

import ru.kpfu.itis.common.PlayerState;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.function.Consumer;

public class NetworkClient {

    private ObjectOutputStream out;

    public void connect(Consumer<Collection<PlayerState>> onUpdate) {
        try {
            Socket socket = new Socket("localhost", 7777);
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        Object obj = in.readObject();
                        onUpdate.accept((Collection<PlayerState>) obj);
                    }
                } catch (Exception ignored) {}
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Object o) {
        try {
            out.writeObject(o);
            out.flush();
        } catch (Exception ignored) {}
    }
}
