import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Класс для работы с веб-страницой, возвращает содержимое страницы и/или код состояния.
 * Рабочей (available) ссылкой считается только та, что возвращает код 200
 *
 * @author kotolex
 * @version 1.0
 */
public final class WebPage implements InternetPage {
    private final String link;
    private final int success = 200;

    public WebPage(String link) {
        this.link = link;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available() {
        return responseCode() == success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * Метод для получения соединения с веб-страницей
     * @return Optional, чтобы избежать возвращения null при ошибках доступа к страницам
     */
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

    /**
     * Печатает исключение в консоль в общем виде
     * @param ex - исключение
     * @see SimpleConsole
     */
    private void printException(Exception ex) {
        new SimpleConsole().println(link + " raise exception " + ex.getMessage());
    }

}
