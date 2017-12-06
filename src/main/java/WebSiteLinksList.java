import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Класс для проверки всех ссылок на сайте (как явных линков так и картинок, ресурсов) на предмет их доступности. Проверяется, что
 * ссылки возвращают допустимые коды состояния.
 * Проверка идет по всем страницам домена, без перехода на сторонние ресурсы.
 * Например для сайта www.example.com, будет осуществлен переход на страницу www.example.com/1/ и сбор/проверка ссылок там,
 * но не будет перехода на www.blog.example.com
 *
 * @author kotolex
 * @version 1.04
 */
public final class WebSiteLinksList {
    /**
     * Карта, содержащая сломанную ссылку и страницу, где она расположена
     */
    private final ConcurrentHashMap<String, String> broken;
    /**
     * Сет посещенных ссылок, то есть ссылок внутри домена на которые был осуществлен переход
     */
    private final HashSet<String> visited;
    /**
     * Сет всех проверенных ссылок
     */
    private final HashSet<String> checked;
    /**
     * Очередь запущенных потоков
     */
    private final ConcurrentLinkedQueue<Thread> threads;
    /**
     * Основной домен для проверки
     */
    private final String mainDomain;

    /**
     * Флаг использовать ли простой способ проверки, в случае false  проверяет с помощью UrlListWithSelenium
     * @see UrlListWithSelenium
     */
    private boolean isSimpleType = true;
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private int maxThreads = 0;

    private WebSiteLinksList(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new HashSet<>();
        checked = new HashSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

    /**
     * Публичный коструктор
     *
     * @param mainDomain - главная страница домена для проверки
     * @param simpleType - проверять простым способом или с помощью Селениум. true - проверка
     *                   простым способом -проверяются и парсятся только явные ссылки. false -  проверка
     *                   с помощью UrlListWithSelenium, проверяются и относительные ссылки.
     *                   Второй способ дольше, но точнее.
     *@see UrlListWithSelenium
     */
    public WebSiteLinksList(String mainDomain, boolean simpleType) {
        this(mainDomain);
        isSimpleType = simpleType;
    }

    /**
     * Запускает проверку ссылок на сайте, завершается сообщением о количестве проверенных и сломанных ссылок, затраченном времени
     */
    public void checkLinks() {
        clearAllCollections();
        SimpleConsole console = new SimpleConsole();
        console.println("Starting...");
        console.startCount();
        startThread(new Visitor(mainDomain, isSimpleType));
        while (!isAllThreadsStopped()) ;
        console.printTime();
        console.println("Checked links: " + checked.size());
        console.println("Visited links: " + visited.size());
        console.println("Broken links: " + brokenLinksCount());
        console.println("Maximum using threads: " + maxThreads);
    }

    public int brokenLinksCount() {
        return broken.size();
    }

    /**
     * Возвращает лист сломанных ссылок в формате "ссылка - страница расположения"
     *
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

    public int getMaxThreads() {
        return maxThreads;
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
     *
     * @param runnable - запускаемый поток
     */
    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
        threadCount.getAndIncrement();
        if (threadCount.get()>maxThreads) {
            maxThreads = threadCount.get();
        }
    }

    /**
     * Возвращает выполнены ли все потоки
     *
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
         *
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
                addToChecked(link);
                if (!broken.containsKey(link)) {
                    if (!checkLinkIsAvailable(link)) {
                        broken.putIfAbsent(link, mainUrl);
                    } else {
                        visitNewUrl(link);
                    }
                }
            }
            threadCount.getAndDecrement();
        }

        /**
         * Добавляет ссылку в проверенные, блокирует коллекцию при этом
         *
         * @param url - ссылка для добавления
         */
        private void addToChecked(String url) {
            synchronized (checked) {
                checked.add(url);
            }
        }

        /**
         * Возвращает является ли ссылка доступной, то есть возвращающей валидный код состояния
         *
         * @param link - ссылка для проверки
         * @return true если ссылка доступнв
         */
        private boolean checkLinkIsAvailable(String link) {
            return new RedirectWebPage(new WebPage(link)).available();
        }

        /**
         * При необходимости запускает поток Визитер для перехода на страницу в пределпх домена.
         * Если ссылка уже посещалась, то перехода не происходит.
         *
         * @param url - ссылка для перехода
         * @see Visitor
         */
        private void visitNewUrl(String url) {
            if (needToVisit(url)) {
                startThread(new Visitor(url, isSimpleType));
            }
        }

        /**
         * Возвращает нужно ли переходить на страницу
         *
         * @param link - ссылка для перехода
         * @return true если ссылку еще не посещали, она находится в домене и не является ресурсом
         */
        private boolean needToVisit(String link) {
            return link.startsWith(mainUrl) && !isVisitedContains(link) && !isSource(link);
        }

        /**
         * Возвращает содержит ли коллекция посещенных ссылок данную
         *
         * @param url - ссылка для проверки
         * @return true, если ссылка уже посещалась
         */
        private boolean isVisitedContains(String url) {
            synchronized (visited) {
                return visited.contains(url);
            }
        }
    }

    /**
     * Класс-поток для перехода на новую страницу и запуска потоков для проверки ссылок на ней
     *
     * @see Inspector
     */
    final class Visitor implements Runnable {
        private final String mainUrl;
        private final boolean isSimpleType;

        /**
         * Консруктор потока
         * @param mainUrl - страница проверки
         * @param isSimpleType - проверять простым способом или с помощью UrlListWithSelenium
         * @see UrlListWithSelenium
         */
        public Visitor(String mainUrl, boolean isSimpleType) {
            this.mainUrl = mainUrl;
            this.isSimpleType = isSimpleType;
        }

        /**
         * Возвращает содержит ли коллекция проверенных ссылок данную
         *
         * @param url - ссылка для проверки
         * @return true, если ссылка уже проверялась
         */
        private boolean isCheckedContains(String url) {
            synchronized (checked) {
                return checked.contains(url);
            }
        }

        /**
         * Добавляет ссылку в посещенные, блокирует коллекцию
         *
         * @param url - ссылка для добавления
         */
        private void addToVisited(String url) {
            synchronized (visited) {
                visited.add(url);
            }
        }

        @Override
        public void run() {
            addToVisited(mainUrl);
            List<String> links = getAllUncheckedLinks();
            startThread(new Inspector(links.parallelStream().filter((n) -> n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
            startThread(new Inspector(links.parallelStream().filter((n) -> !n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
            threadCount.getAndDecrement();
        }

        /**
         * В зависимости от флага isSimpleType получает ссылки простым способом или с помощью UrlListWithSelenium
         *
         * @return список еще не провереных ссылок
         */
        private List<String> getAllUncheckedLinks() {
            UrlLinksList urlList = isSimpleType ? new UrlList(new WebPage(mainUrl).content()) : new UrlListWithSelenium(mainUrl);
            return urlList.links().parallelStream().filter((n) -> !isCheckedContains(n)).collect(Collectors.toList());
        }

    }
}
