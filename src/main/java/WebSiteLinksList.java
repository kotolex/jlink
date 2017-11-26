import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Класс для проверки всех ссылок на сайте (как явных линков так и картинок, ресурсов) на предмет их доступности. Проверяется, что
 * ссылки возвращают допустимые коды состояния.
 * Проверка идет по всем страницам домена, без перехода на сторонние ресурсы.
 * Например для сайта www.example.com, будет осуществлен переход на страницу www.example.com/1/ и сбор/проверка ссылок там,
 * но не будет перехода на www.blog.example.com
 *
 * @author kotolex
 * @version 1.02
 */
public final class WebSiteLinksList {
    /** Карта, содержащая сломанную ссылку и страницу, где она расположена*/
    private final ConcurrentHashMap<String, String> broken;
    /** Сет посещенных ссылок, то есть ссылок внутри домена на которые был осуществлен переход*/
    private final ConcurrentSkipListSet<String> visited;
    /** Сет всех проверенных ссылок*/
    private final ConcurrentSkipListSet<String> checked;
    /** Очередь запущенных потоков*/
    private final ConcurrentLinkedQueue<Thread> threads;
    /** Основной домен для проверки*/
    private final String mainDomain;

    public WebSiteLinksList(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new ConcurrentSkipListSet<>();
        checked = new ConcurrentSkipListSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

    /**
     * Запускает проверку ссылок на сайте, завершается сообщением о количестве проверенных и сломанных ссылок, затраченном времени
     */
    public void checkLinks() {
        clearAllCollections();
        SimpleConsole console = new SimpleConsole();
        console.println("Starting...");
        console.startCount();
        startThread(new Visitor(mainDomain));
        while (!isAllThreadsStopped()) ;
        console.println("Checked links: " + checkedLinksCount());
        console.println("Broken links: " + brokenLinksCount());
        console.println("Visited links: " + visitedLinksCount());
        console.printTime();
    }

    public int brokenLinksCount() {
        return broken.size();
    }

    public int checkedLinksCount() {
        return checkedLinks().size();
    }

    public int visitedLinksCount() {
        return visitedLinks().size();
    }

    /**
     * Возвращает лист сломанных ссылок в формате "ссылка - страница расположения"
     * @return лист, преобразованный из карты сломанных ссылок
     */
    public List<String> brokenLinksWithPath() {
        return broken.entrySet().stream().map((n) -> n.getKey() + " - " + n.getValue()).collect(Collectors.toList());
    }

    public List<String> checkedLinks() {
        return new ArrayList<>(checked);
    }

    public List<String> visitedLinks() {
        return new ArrayList<>(visited);
    }

    /**
     * Очищает все коллекции для начала работы
     */
    private void clearAllCollections() {
        visited.clear();
        checked.clear();
        broken.clear();
        threads.clear();
    }

    /**
     * Стандартный запуск потоков, с занесением в очередь потоков threads
     * @param runnable - запускаемый поток
     */
    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    /**
     * Возвращает выполнены ли все потоки
     * @return true, если все потоки завершены
     */
    private boolean isAllThreadsStopped() {
        return threads.stream().filter(Thread::isAlive).count() < 1;
    }

    /**
     * Класс-поток для проверки ссылок полученных в листе при создании объекта
     */
    final class Inspector implements Runnable {
        private final List<String> links;
        private final String mainUrl;

        public Inspector(List<String> links, String mainUrl) {
            this.links = links;
            this.mainUrl = mainUrl;
        }

        /**
         * Возвращает является ли ссылка ресурсом, на котрый не нужно переходить
         * @param link - ссылка для проверки
         * @return true, если ссылка является ресурсом
         */
        private boolean isSource(String link) {
            return link.endsWith(".js") || link.endsWith(".rss") || link.endsWith(".jpg")
                    || link.endsWith(".png") || link.endsWith(".css") || link.endsWith(".xml");
        }

        @Override
        public void run() {
            for (String link : links) {
                checked.add(link);
                if (!broken.containsKey(link)) {
                    if (!checkLinkIsAvailable(link)) {
                        broken.putIfAbsent(link, mainUrl);
                    } else {
                        visitNewUrl(link);
                    }
                }
            }
        }

        /**
         * Возвращает является ли ссылка доступной, то есть возвращающей валидный код состояния
         * @param link - ссылка для проверки
         * @return true если ссылка доступнв
         */
        private boolean checkLinkIsAvailable(String link) {
            return new RedirectWebPage(new WebPage(link)).available();
        }

        /**
         * При необходимости запускает поток Визитер для перехода на страницу в пределпх домена.
         * Если ссылка уже посещалась, то перехода не происходит.
         * @param url - ссылка для перехода
         * @see Visitor
         */
        private void visitNewUrl(String url) {
            if (needToVisit(url)) {
                startThread(new Visitor(url));
            }
        }

        /**
         * Возвращает нужно ли переходить на страницу
         * @param link - ссылка для перехода
         * @return true если ссылку еще не посещали, она находится в домене и не является ресурсом
         */
        private boolean needToVisit(String link) {
            return link.startsWith(mainUrl) && !visited.contains(link) && !isSource(link);
        }
    }

    /**
     * Класс-поток для перехода на новую страницу и запуска потоков для проверки ссылок на ней
     *
     * @see Inspector
     */
    final class Visitor implements Runnable {
        private final String mainUrl;

        public Visitor(String mainUrl) {
            this.mainUrl = mainUrl;
        }

        @Override
        public void run() {
            visited.add(mainUrl);
            List<String> links = new UrlList(new WebPage(mainUrl).content()).links().parallelStream().
                    filter((n) -> !checked.contains(n)).
                    collect(Collectors.toList());
            startThread(new Inspector(links.parallelStream().filter((n) -> n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
            startThread(new Inspector(links.parallelStream().filter((n) -> !n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
        }
    }
}
