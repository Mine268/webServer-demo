import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class RequestHandler implements Runnable {

    public enum RequestType {
        POST, GET
    }

    private final Socket socket;

    RequestHandler(Socket sock) {
        socket = sock;
    }

    @Override
    public void run() {
        try {
//            var socket_req_context = new String(readStream(socket.getInputStream()));
//            var output_stream = socket.getOutputStream();
//            var page_input_stream = new FileInputStream(ServerConfig.webRoot + ServerConfig.defaultPage);
//
//            output_stream.write(ServerConfig.getHtmlHeader(ServerConfig.StatusCode.OK).getBytes());
//            output_stream.write(page_input_stream.readAllBytes());
//
//            page_input_stream.close();
//            output_stream.close();
//            socket.close();
            handle(socket);
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handle(Socket socket) throws Exception {
        var remote_ip = socket.getInetAddress();
        var remote_port = socket.getPort();
        var http_header = new String(readStream(socket.getInputStream()));
        var header_first_line = http_header.substring(0, http_header.indexOf("\n")).trim();

        var request_type = header_first_line.matches("GET.*") ? RequestType.GET : RequestType.POST;
        String request_path, request_arguments;

        if (request_type == RequestType.GET) {
            var r_p_a = header_first_line.split("\\s")[1].split("&");
            request_path = r_p_a[0];
            request_arguments = r_p_a.length == 2 ? r_p_a[1] : "";
        } else {
            request_path = header_first_line.substring(
                    header_first_line.indexOf(" "),
                    header_first_line.lastIndexOf(" ") + 1
            );
            String[] split = http_header.split("\\n");
            request_arguments = split[split.length - 1];
        }

        returnDesignatedPage(socket, request_path.substring(1));

        System.out.printf("[%s][from %s:%d][req path %s][req arg %s]\n", ServerConfig.dateFormat.format(new Date()),
                remote_ip.getHostAddress(), remote_port, request_path, request_arguments);
    }

    private void returnDesignatedPage(Socket socket, String page_path) throws Exception {
        if (page_path.isEmpty()) {
            returnDefaultPage(socket);
        } else {
            try {
                var file_stream = new FileInputStream(ServerConfig.webRoot + page_path);
                returnPage(socket, ServerConfig.StatusCode.OK, file_stream);
                file_stream.close();
            } catch (Exception ex) {
                return404Page(socket);
            }
        }
    }

    private void returnPage(Socket socket, ServerConfig.StatusCode code, InputStream input_stream) throws IOException {
        var output_stream = socket.getOutputStream();
        output_stream.write(ServerConfig.getHtmlHeader(code).getBytes());
        output_stream.write(input_stream.readAllBytes());
        output_stream.close();
    }

    private void return404Page(Socket socket) throws IOException {
        var page_input_stream = new FileInputStream(ServerConfig.webRoot + ServerConfig.notFoundPage);
        returnPage(socket, ServerConfig.StatusCode.NOT_FOUND, page_input_stream);
        page_input_stream.close();
    }

    private void returnDefaultPage(Socket socket) throws IOException {
        var page_input_stream = new FileInputStream(ServerConfig.webRoot + ServerConfig.defaultPage);
        returnPage(socket, ServerConfig.StatusCode.OK, page_input_stream);
        page_input_stream.close();
    }

    private byte[] readStream(InputStream input_stream) throws Exception {
        var bao = new ByteArrayOutputStream();
        readStreamRecursion(bao, input_stream);
        bao.close();
        return bao.toByteArray();
    }

    private void readStreamRecursion(ByteArrayOutputStream output_stream, InputStream input_stream) throws Exception {
        var start_time = System.currentTimeMillis();
        while (input_stream.available() == 0) {
            if (System.currentTimeMillis() - start_time >= ServerConfig.socket_timeout_limit) {
                throw new SocketTimeoutException("Socket reading time-outs.");
            }
        }

        var buffer = new byte[2048];
        var read_count = input_stream.read(buffer);
        output_stream.write(buffer, 0, read_count);
        Thread.sleep(100);
        if (input_stream.available() != 0) {
            readStreamRecursion(output_stream, input_stream);
        }
    }
}
