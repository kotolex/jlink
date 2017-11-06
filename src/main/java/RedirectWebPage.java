import java.util.List;

public class RedirectWebPage implements InternetPage {
    private final WebPage webPage;

    public RedirectWebPage(WebPage webPage) {
        this.webPage = webPage;
    }

    @Override
    public int responseCode() {
        return webPage.responseCode();
    }

    @Override
    public boolean available() {
        int code = responseCode();
        return code == 200 || code == 301;
    }

    @Override
    public List<String> content() {
        return webPage.content();
    }
}
