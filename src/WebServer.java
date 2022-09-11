import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebServer {

    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            10,
            10,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(20),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    ServerSocket serverSocket;

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        while (true) {
            try {
                var socket = serverSocket.accept();
                threadPool.execute(new RequestHandler(socket));
//                new RequestHandler(socket).run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
