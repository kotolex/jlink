package com.kotolex.interfaces;

import java.util.List;

/**
 * Интерфейс работы с веб-страницей, позволяет запросить контент или код состояния веб-страницы
 *
 * @author kotolex
 * @version 1.0
 */
public interface InternetPage {
    /**
     * Возвращает код состояния веб-страницы
     * @return код состояния, при ошибках возвращает 0
     */
    int responseCode();

    /**
     * Возвращает доступность веб-страницы, исходя из полученного кода состояния
     * @return true если возвращен валидный код состояния
     */
    boolean available();

    /**
     * Возвращает все содержимое страницы
     * @return содержимое веб-страницы, в случае проблем доступа возвращает пустой лист
     */
    List<String> content ();

}
