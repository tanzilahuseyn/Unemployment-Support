package ee.taltech.ite4120.support.registry;

/**
 * A registry could not answer — transport failure or upstream fault. The service
 * turns it into a check with status UNAVAILABLE; it never reaches the caller.
 */
public class RegistryUnavailableException extends RuntimeException {

    public RegistryUnavailableException(String message) {
        super(message);
    }

    public RegistryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
