package registry.machine;

/**
 * Created by edwardsbean on 2015/2/25 0025.
 */
public class AIMAException extends RuntimeException {
    public AIMAException(String message) {
        super(message);
    }

    public AIMAException(String message, Throwable cause) {
        super(message, cause);
    }
}
