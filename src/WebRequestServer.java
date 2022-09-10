import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.Socket;

public class WebRequestServer implements Runnable {

    private final Socket socket;

    WebRequestServer(Socket sock) {
        socket = sock;
    }

    @Override
    public void run() {
        try {
            var pstream = new PrintStream(socket.getOutputStream());
            var fstream = new FileInputStream(ServerConfig.webRoot + ServerConfig.defaultPage);

            pstream.writeBytes(ServerConfig.htmlHeader.getBytes());
            pstream.writeBytes(fstream.readAllBytes());

            pstream.flush();

            pstream.close();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
