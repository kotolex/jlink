import java.util.List;

/**
 * Класс-декоратор для использования с редиректами на веб-страницах.
 * В отличие от WebPage при использовании данного класса ссылка, возвращающая 3хх коды считается рабочей
 *
 * @author kotolex
 * @version 1.0
 * @see WebPage
 */
public class RedirectWebPage implements InternetPage {
    private final WebPage webPage;

    public RedirectWebPage(WebPage webPage) {
        this.webPage = webPage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int responseCode() {
        return webPage.responseCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available() {
        int code = responseCode();
        return code == 200 || isInRedirectionCodesRange(code);
    }

    /**
     * Проверяет, относится ли код к кодам редиректа
     *
     * @param code - код ответа веб-страницы
     * @return true, если код имеет тип 3хх
     */
    private boolean isInRedirectionCodesRange(int code) {
        return code >= 300 && code <= 307;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> content() {
        return webPage.content();
    }
}
