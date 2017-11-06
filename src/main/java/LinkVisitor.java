import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class LinkVisitor implements ThreadWaiting {
    private ConcurrentHashMap<String, String> broken;
    private ConcurrentSkipListSet<String> visited;
    private final String mainDomain;
    private ConcurrentLinkedQueue<Thread> threads;
    private long checked = 0;


    public LinkVisitor(String mainDomain) {
        this.mainDomain = mainDomain;
        visited = new ConcurrentSkipListSet<>();
        broken = new ConcurrentHashMap<>();
        threads = new ConcurrentLinkedQueue<>();
    }

    public void start() {
        System.out.println("Starting...");
        long startTime = System.currentTimeMillis();
        startThread(new Visitor(mainDomain));
        while (!isAllThreadsStopped()) ;
        System.out.println("Checked links: " + checked);
        System.out.println("Broken links: " + broken.size());
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime) / 1000);
        if (broken.size() > 0) {
            for (Map.Entry pair : broken.entrySet()) {
                System.out.println(pair.getKey() + " - " + pair.getValue());
            }
        }
    }

    private void startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        threads.add(thread);
        thread.start();
    }

    @Override
    public boolean isAllThreadsStopped() {
        Iterator <Thread> iterator = threads.iterator();
        while (iterator.hasNext()) {
            Thread thread = iterator.next();
            if (!thread.isAlive()) {
                iterator.remove();
            }
        }
        return threads.isEmpty();
    }


    class Inspector implements Runnable {
        private List<String> links;
        private final String mainURl;

        public Inspector(List<String> links, String mainURl) {
            this.links = links;
            this.mainURl = mainURl;
        }

        private boolean isSource(String link) {
            if (link.endsWith(".js") || link.endsWith(".rss") || link.endsWith(".jpg") || link.endsWith(".png")) {
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            for (String link : links) {
                checked++;
                if (!new Request(link).isSuccess()) {
                    broken.putIfAbsent(link, mainURl);
                } else {
                    if (link.startsWith(mainURl) && !visited.contains(link) && !isSource(link)) {
                        startThread(new Visitor(link));
                    }
                }
            }
        }
    }

    class Visitor implements Runnable {
        private final String mainUrl;

        public Visitor(String mainUrl) {
            this.mainUrl = mainUrl;
        }

        @Override
        public void run() {
            System.out.println("Visit " + mainUrl);
            visited.add(mainUrl);
            List<String> links = new Parse(new Request(mainUrl).pageSource()).links();
            List<String> domain = links.stream().filter((n) -> n.startsWith(mainUrl)).collect(Collectors.toList());
            List<String> others = links.stream().filter((n) -> !n.startsWith(mainUrl)).collect(Collectors.toList());
            startThread(new Inspector(domain, mainUrl));
            startThread(new Inspector(others, mainUrl));
        }
    }
}
