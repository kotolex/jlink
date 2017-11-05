import java.util.List;
import java.util.stream.Collectors;

public class Parse {
    private List<String> lines;

    public Parse(List<String> lines) {
        this.lines = lines;
    }

    public List<String> links() {
        List<String> filtered = lines.stream().filter(this::isContainingLink).collect(Collectors.toList());
        if (!filtered.isEmpty()) {
            filtered = filtered.stream().map(this::extractLink).filter((n) -> !n.isEmpty()).collect(Collectors.toList());
        }
        return filtered;
    }

    private boolean isContainingLink(String url) {
        return url.contains("href=") || url.contains("src=");
    }

    private String extractLink(String line) {
        if (!line.contains("\"")) {
            return "";
        }
        String match = "href=";
        if (!line.contains(match)) {
            match = "src=";
        }
        String result = line.substring(line.indexOf(match) + match.length()).split("\"")[1];
        if (!result.startsWith("http")) {
            return "";
        }
        return result;
    }

}
