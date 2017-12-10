package com.kotolex.pages;

import com.kotolex.interfaces.InternetPage;
import com.kotolex.services.SimpleConsole;

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
 * @version 1.1
 */
public final class WebPage implements InternetPage {
    private final String link;
    /**
     * Код состояния доступности страницы
     */
    private final int success = 200;

    /**
     * Необходимо для более корретной проверки ссылок, иначе возвращает не 200 в некоторых случаях
     */
    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36";

    /**
     * Конструктор
     *
     * @param link - страница, на которой ищутся ссылки
     */
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
            connection.setRequestProperty("user-agent", USER_AGENT);
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
