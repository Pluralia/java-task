package ru.ifmo.java.task.server.blocked.soft;

import ru.ifmo.java.task.Constants;
import ru.ifmo.java.task.server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockedSoftServer extends Server {
    private ExecutorService pool;
    private ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        new BlockedSoftServer().run();
    }

    public void run() throws IOException {
        pool = Executors.newCachedThreadPool();
        serverSocket = new ServerSocket(Constants.BLOCKED_SOFT_PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            pool.submit(new ServerWorker(socket));
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
        pool.shutdown();
    }
}
