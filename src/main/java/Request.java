import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Request {
    private final String link;
    private final int success = 200;

    public Request(String link) {
        this.link = link;
    }

    public boolean isSuccess() {
        Optional<HttpURLConnection> optional = connection();
        if (!optional.isPresent()) {
            return false;
        }
        try {
            HttpURLConnection connection = optional.get();
            boolean result = connection.getResponseCode() == success;
            connection.disconnect();
            return result;
        } catch (IOException e) {
            printException(e);
            return false;
        }
    }

    public List<String> pageSource() {
        Optional<HttpURLConnection> optional = connection();
        LinkedList<String> linkedList = new LinkedList<>();
        if (optional.isPresent()) {
            try {
                HttpURLConnection connection = optional.get();
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNext()) {
                    linkedList.add(scanner.next());
                }
                connection.disconnect();
            } catch (Exception e) {
                printException(e);
            }
        }
        return linkedList;
    }

    private Optional<HttpURLConnection> connection() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(link).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            return Optional.of(connection);
        } catch (Exception e) {
            printException(e);
            return Optional.empty();
        }
    }

    private void printException(Exception ex) {
        System.err.println(link + " raise exception " + ex.getMessage());
    }

}
