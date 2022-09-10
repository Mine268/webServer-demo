import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RequestManager {

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
                handleRequest(serverSocket.accept());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleRequest(Socket socket) {
        threadPool.execute(new WebRequestServer(socket));
    }

}
