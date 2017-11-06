import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class WebSiteLinksList  {
    private ConcurrentHashMap<String, String> broken;
    private ConcurrentSkipListSet<String> visited;
    private ConcurrentSkipListSet<String> checked;
    private ConcurrentLinkedQueue<Thread> threads;
    private final String mainDomain;

    public WebSiteLinksList(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new ConcurrentSkipListSet<>();
        checked = new ConcurrentSkipListSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

    public void checkLinks() {
        clearAll();
        System.out.println("Starting...");
        long startTime = System.currentTimeMillis();
        startThread(new Visitor(mainDomain));
        while (!isAllThreadsStopped()) ;
        System.out.println("Checked links: " + checked.size());
        System.out.println("Broken links: " + broken.size());
        System.out.println("Visited links: " + visited.size());
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime) / 1000);
    }

    private void clearAll() {
        visited.clear();
        checked.clear();
        broken.clear();
        threads.clear();
    }

    public int brokenLinksCount() {
        if (visited.size() < 1) {
            checkLinks();
        }
        return broken.size();
    }

    public int checkedLinksCount() {
        if (visited.size() < 1) {
            checkLinks();
        }
        return checked.size();
    }

    public List<String> brokenLinks() {
        List<String> values = new LinkedList<>();
        if (broken.size() > 0) {
            for (Map.Entry pair : broken.entrySet()) {
                values.add(pair.getKey() + " - " + pair.getValue());
            }
        }
        return values;
    }

    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    private boolean isAllThreadsStopped() {
        if (Thread.activeCount() < 3) {
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    class Inspector implements Runnable {
        private List<String> links;
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
                if (!checked.contains(link)) {
                    checked.add(link);
                    if (!new RedirectWebPage(new WebPage(link)).available()) {
                        broken.putIfAbsent(link, mainUrl);
                    } else {
                        if (needToVisit(link)) {
                            startThread(new Visitor(link));
                        }
                    }
                }
            }
        }

        private boolean needToVisit(String link) {
            return link.startsWith(mainUrl) && !visited.contains(link) && !isSource(link);
        }
    }

    class Visitor implements Runnable {
        private final String mainUrl;

        public Visitor(String mainUrl) {
            this.mainUrl = mainUrl;
        }

        @Override
        public void run() {
            visited.add(mainUrl);
            List<String> links = new UrlList(new WebPage(mainUrl).content()).links();
            List<String> domain = links.stream().filter((n) -> n.startsWith(mainUrl)).collect(Collectors.toList());
            List<String> others = links.stream().filter((n) -> !n.startsWith(mainUrl)).collect(Collectors.toList());
            startThread(new Inspector(domain, mainUrl));
            startThread(new Inspector(others, mainUrl));
        }
    }
}
