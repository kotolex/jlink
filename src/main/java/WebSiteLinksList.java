import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public final class WebSiteLinksList {
    private final ConcurrentHashMap<String, String> broken;
    private final ConcurrentSkipListSet<String> visited;
    private final ConcurrentSkipListSet<String> checked;
    private final ConcurrentLinkedQueue<Thread> threads;
    private final String mainDomain;

    public WebSiteLinksList(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new ConcurrentSkipListSet<>();
        checked = new ConcurrentSkipListSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

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

    public List<String> brokenLinksWithPath() {
        return broken.entrySet().stream().map((n) -> n.getKey() + " - " + n.getValue()).collect(Collectors.toList());
    }

    public List<String> checkedLinks() {
        return new ArrayList<>(checked);
    }

    public List<String> visitedLinks() {
        return new ArrayList<>(visited);
    }

    private void clearAllCollections() {
        visited.clear();
        checked.clear();
        broken.clear();
        threads.clear();
    }

    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    private boolean isAllThreadsStopped() {
        return threads.stream().filter(Thread::isAlive).count() < 1;
    }

    final class Inspector implements Runnable {
        private final List<String> links;
        private final String mainUrl;

        public Inspector(List<String> links, String mainUrl) {
            this.links = links;
            this.mainUrl = mainUrl;
        }

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

        private boolean checkLinkIsAvailable(String link) {
            return new RedirectWebPage(new WebPage(link)).available();
        }

        private void visitNewUrl(String url) {
            if (needToVisit(url)) {
                startThread(new Visitor(url));
            }
        }

        private boolean needToVisit(String link) {
            return link.startsWith(mainUrl) && !visited.contains(link) && !isSource(link);
        }
    }

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
