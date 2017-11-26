import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс для парсинга ссылок из содержимого любой веб-страницы. Ссылки берутся только в явном виде: начинающиеся с
 * http или https, содержащиеся в верстке страницы после тегов href и src.
 *
 * @author kotolex
 * @version 1.01
 */
public class UrlList {
    /** Лист с содержимым веб-страницы (верстка) */
    private List<String> lines;

    public UrlList(List<String> lines) {
        this.lines = lines;
    }

    /**
     * Возвращает лист извлеченных из верстки ссылок, в случае проблем возвращает пустой список.
     * Лист не содержит одинаковых ссылок
     * @return лист уникальных (не повторяющихся) ссылок
     */
    public List<String> links() {
        List<String> filtered = lines.stream().filter(this::isContainingLink).collect(Collectors.toList());
        if (!filtered.isEmpty()) {
            filtered = filtered.parallelStream().map(this::extractLink).filter((n) -> !n.isEmpty()).distinct().collect(Collectors.toList());
        }
        return filtered;
    }

    /**
     * Возвращает содержит ли строка ссылку, после тегов href и src
     * @param url - строка верстки из веб-страницы
     * @return true, если строка содержит ссылку
     */
    private boolean isContainingLink(String url) {
        return url.contains("href=\"") || url.contains("src=\"");
    }

    /**
     * Возвращает извлеченную из строки ссылку. В случае отсутствия ссылки или неверной верстки возвращает пустую строку.
     * @param line - строка из верстки веб-страницы
     * @return извлеченную ссылку или пустую строку
     */
    private String extractLink(String line) {
        if (!line.contains("\"")) {
            return "";
        }
        String match = line.contains("href=") ? "href=" : "src=";
        String[] tokens = line.substring(line.indexOf(match) + match.length()).split("\"");
        if (tokens.length < 2 || !tokens[1].startsWith("http")) {
            return "";
        }
        return tokens[1];
    }

}
