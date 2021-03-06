package ru.ifmo.java.task.server.blocked.hard;

import ru.ifmo.java.task.protocol.Protocol.Request;
import ru.ifmo.java.task.protocol.Protocol.Response;
import ru.ifmo.java.task.server.ServerStat.*;
import ru.ifmo.java.task.server.ServerStat.ClientStat.*;
import ru.ifmo.java.task.server.blocked.AbstractBlockedServerWorker;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class ServerWorker extends AbstractBlockedServerWorker {
//    public final ClientStat clientStat;
//
//    public final CountDownLatch startSignal;
//    public final CountDownLatch doneSignal;

    private final ExecutorService pool;
    private final ExecutorService outputPool = Executors.newSingleThreadExecutor();

    private int taskCounter = clientStat.getTasksNum();

    public ServerWorker(Socket socket, ExecutorService pool, ClientStat clientStat,
                        CountDownLatch startSignal, CountDownLatch doneSignal) throws IOException {
        super(socket, clientStat, startSignal, doneSignal);

        this.pool = pool;

        Thread inputThread = new Thread(initInputThread());
        inputThread.setDaemon(true);
        inputThread.start();
    }

    private Runnable initInputThread() {
        return () -> {
            try {
                startSignal.await();

                for (int i = 0; i < clientStat.getTasksNum(); i++) {
                    RequestStat requestStat = clientStat.registerRequest();

                    Request request = getRequest(requestStat);
                    pool.submit(initTask(request, requestStat));
                }
            } catch(Exception e) {
//                System.out.println("Server: input thread exception: " + e.getMessage());
                doneSignal.countDown();
            }
       };
    }

    @Override
    public void close() throws IOException {
        super.close();
        outputPool.shutdown();
    }

    private Runnable initTask(Request request, RequestStat requestStat) {
        return () -> {
            Response response = processRequest(request, requestStat);
            outputPool.submit(initOutputPool(response, requestStat));
        };
    }

    private Runnable initOutputPool(Response response, RequestStat requestStat) {
        return () -> {
            try {
                sendResponse(response, requestStat);
                taskCounter -= 1;

                if (taskCounter == 0) {
                    doneSignal.countDown();
                }
            } catch(Exception e) {
//                System.out.println("Server: output thread exception: " + e.getMessage());
                doneSignal.countDown();
            }
        };
    }
}
