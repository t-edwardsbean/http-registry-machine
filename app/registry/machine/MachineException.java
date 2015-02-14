package registry.machine;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class MachineException extends RuntimeException {
    public MachineException(String message) {
        super(message);
    }

    public MachineException(String message, Throwable cause) {
        super(message, cause);
    }
}
