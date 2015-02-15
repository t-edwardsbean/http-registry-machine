package registry.machine;

/**
 * Created by edwardsbean on 15-2-15.
 */
public class MachineNetworkException extends RuntimeException {
    public MachineNetworkException(String message) {
        super(message);
    }

    public MachineNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
