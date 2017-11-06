import java.util.List;
import java.util.stream.Collectors;

public class UrlList {
    private List<String> lines;

    public UrlList(List<String> lines) {
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
        return url.contains("href=\"") || url.contains("src=\"");
    }

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
