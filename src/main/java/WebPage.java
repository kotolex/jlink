import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class WebPage implements InternetPage {
    private final String link;
    private final int success = 200;

    public WebPage(String link) {
        this.link = link;
    }

    @Override
    public int responseCode() {
        Optional<HttpURLConnection> optional = connection();
        if (!optional.isPresent()) {
            return 0;
        }
        try {
            HttpURLConnection connection = optional.get();
            int result = connection.getResponseCode();
            connection.disconnect();
            return result;
        } catch (IOException e) {
            printException(e);
            return 0;
        }
    }

    public boolean available() {
        return responseCode() == success;
    }

    public List<String> content() {
        Optional<HttpURLConnection> optional = connection();
        LinkedList<String> linkedList = new LinkedList<>();
        if (optional.isPresent()) {
            try {
                HttpURLConnection connection = optional.get();
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNext()) {
                    linkedList.add(scanner.next());
                }
                scanner.close();
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
        new SimpleConsole().println(link + " raise exception " + ex.getMessage());
    }

}
