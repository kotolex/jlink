import java.util.List;

public interface InternetPage {
    int responseCode();
    boolean available();
    List<String> content ();

}
