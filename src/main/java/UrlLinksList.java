import com.sun.istack.internal.NotNull;

import java.util.List;

/**
 * Интерфейс парсинга ссылок из содержимого веб-страницы
 *
 * @author kotolex
 * @version 1.0
 * @see UrlListWithSelenium
 * @see UrlList
 */
public interface UrlLinksList {

    /**
     * Возвращает лист извлеченных из верстки ссылок, в случае проблем доступа или парсинга возвращает пустой список.
     * Лист не содержит одинаковых ссылок
     * @return лист уникальных (не повторяющихся) ссылок
     */
    @NotNull
    List<String> links();
}
