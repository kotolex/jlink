package com.kotolex.parsers;

import com.kotolex.interfaces.UrlLinksList;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Класс получения ссылок методами Selenium WebDriver, собирает даже относительные ссылки.
 * Сбор ссылок происходит только один раз, при дальнейших запросах возвращается уже сформированный список! Сразу после
 * сбора ссылок экземпляр драйвера уничтожается.
 *
 * @author kotolex
 * @version 1.1
 */
public final class UrlListWithSelenium implements UrlLinksList {
    private final String webPageUrl;
    private final HtmlUnitDriver driver;
    private List<String> list;

    /**
     * Конструктор
     *
     * @param webPageUrl - страница, на которой ищутся ссылки
     */
    public UrlListWithSelenium(String webPageUrl) {
        this.webPageUrl = webPageUrl;
        driver = new HtmlUnitDriver(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> links() {
        if (list != null) {
            return list;
        }
        list = new ArrayList<>();
        disableInfoLogging();
        try {
            driver.get(webPageUrl);
            list.addAll(getListByTagAndAttribute("a", "href"));
            list.addAll(getListByTagAndAttribute("img", "src"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        driver.quit();
        return list;
    }

    /**
     * Отключаем лишнее логирование INFO, иначе много ненужной информации идет в консоль
     */
    private void disableInfoLogging() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(Level.OFF);
    }

    /**
     * Метод для получения списка ссылок из верстки методами ВебДрайвера
     *
     * @param tag       передаем "a" или "img"
     * @param attribute передаем "href" или "src" соответственно
     * @return лист уникальных ссылок
     * @throws WebDriverException в случае ошибок самого вебДрайвера
     */
    private List<String> getListByTagAndAttribute(String tag, String attribute) throws WebDriverException {
        return driver.findElements(By.tagName(tag)).parallelStream()
                .filter((n) -> n.getAttribute(attribute) != null)
                .map((n) -> n.getAttribute(attribute))
                .filter((n) -> !n.startsWith("mailto") && !n.startsWith("javascript"))
                .distinct()
                .collect(Collectors.toList());
    }
}
