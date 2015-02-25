package registry.machine;

/**
 * Created by edwardsbean on 2015/2/25 0025.
 */
public class MachineDelayException extends RuntimeException {
    public MachineDelayException(String message) {
        super(message);
    }

    public MachineDelayException(String message, Throwable cause) {
        super(message, cause);
    }
}
