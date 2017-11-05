import java.net.HttpURLConnection;
import java.net.URL;

public class Request {
    private final String link;
    private final int success = 200;

    public Request(String link) {
        this.link = link;
    }

    public boolean isSuccess() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(link).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            boolean result = connection.getResponseCode() == success;
            connection.disconnect();
            return result;
        } catch (Exception e) {
            System.err.println(link + " raise exception " + e.getMessage());
            return false;
        }
    }


}
