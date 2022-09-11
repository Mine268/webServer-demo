import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class ServerConfig {

    public enum StatusCode {
        OK(200, "OK"),
        NOT_FOUND(404, "Not found"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        UNSUPPORTED_VERSION(505, "HTTP Version not supported");

        public final Integer code;
        public final String description;

        StatusCode(Integer code, String description) {
            this.code = code;
            this.description = description;
        }
    };

    public static final Integer socket_timeout_limit = 20 * 1000; // ms

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String webRoot = "webroot/";

    public static final String defaultPage = "index.html";

    public static final String notFoundPage = "not_found.html";

    public static String getHtmlHeader(StatusCode statusCode) {
        return String.format("""
            HTTP/1.0 %d %s
            Content-Type: text/html
            Server: Bot
            
            """,
                statusCode.code, statusCode.description);
    }

}
