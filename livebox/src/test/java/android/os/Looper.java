package android.os;

/**
 * Mock Looper class in unit tests
 */
public class Looper {
    public static Looper getMainLooper() {
        return new Looper();
    }
    public Thread getThread() {
        return null;
    }
}
