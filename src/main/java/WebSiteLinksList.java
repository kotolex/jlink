import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class WebSiteLinksList {
    private ConcurrentHashMap<String, String> broken;
    private HashSet<String> visited;
    private HashSet<String> checked;
    private ConcurrentLinkedQueue<Thread> threads;
    private final String mainDomain;

    public WebSiteLinksList(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new HashSet<>();
        checked = new HashSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

    public void checkLinks() {
        clearAll();
        SimpleConsole console = new SimpleConsole();
        console.println("Starting...");
        console.startCount();
        startThread(new Visitor(mainDomain));
        while (!isAllThreadsStopped()) ;
        console.println("Checked links: " + checked.size());
        console.println("Broken links: " + broken.size());
        console.println("Visited links: " + visited.size());
        console.printTime();
    }

    private void clearAll() {
        visited.clear();
        checked.clear();
        broken.clear();
        threads.clear();
    }

    public int brokenLinksCount() {
        return broken.size();
    }

    public int checkedLinksCount() {
        return checked.size();
    }

    public List<String> brokenLinksWithPath() {
        return broken.entrySet().stream().map((n) -> n.getKey() + " - " + n.getValue()).collect(Collectors.toList());
    }

    public List<String> checkedLinks() {
        return checked.stream().collect(Collectors.toList());
    }

    public List<String> visitedLinks() {
        return visited.stream().collect(Collectors.toList());
    }

    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    private boolean isAllThreadsStopped() {
        return threads.stream().filter((n) -> n.isAlive()).count() < 1;
    }

    public void addToChecked(String link) {
        synchronized (checked) {
            checked.add(link);
        }
    }

    public void addToVisited(String link) {
        synchronized (visited) {
            visited.add(link);
        }
    }

    public boolean brokenContains(String link) {
        return broken.entrySet().parallelStream().filter((n) -> n.getKey().equals(link)).count() > 0;
    }

    public boolean isCheckedContains(String url) {
        boolean isContains;
        synchronized (checked) {
            isContains = checked.contains(url);
        }
        return isContains;
    }

    final class Inspector implements Runnable {
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
                addToChecked(link);
                if (!brokenContains(link)) {
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
            addToVisited(mainUrl);
            List<String> links = new UrlList(new WebPage(mainUrl).content()).links().parallelStream().
                    filter((n) -> !isCheckedContains(n)).
                    collect(Collectors.toList());
            startThread(new Inspector(links.parallelStream().filter((n) -> n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
            startThread(new Inspector(links.parallelStream().filter((n) -> !n.startsWith(mainUrl)).collect(Collectors.toList()), mainUrl));
        }
    }
}
