import java.io.PrintStream;

public class SimpleConsole {
    private PrintStream writer;
    private long startTime;

    public SimpleConsole() {
        writer = System.out;
        startCount();
    }

    public void startCount() {
        startTime = System.currentTimeMillis();
    }

    public void println(String text) {
        writer.println(text);
    }

    public void print(String text) {
        writer.print(text);
    }

    public void printTime() {
        println("Time elapsed: " + time() + " sec.");
    }

    private double time() {
        return ((double) (System.currentTimeMillis() - startTime)) / 1000;
    }
}
