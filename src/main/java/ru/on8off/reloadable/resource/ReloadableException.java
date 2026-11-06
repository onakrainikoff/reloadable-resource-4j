package ru.on8off.reloadable.resource;

/**
 * Unchecked exception raised when loading or converting a reloadable resource fails.
 */
public class ReloadableException extends RuntimeException{
    /**
     * Creates an exception with a detail message.
     *
     * @param message detail message
     */
    public ReloadableException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a detail message and cause.
     *
     * @param message detail message
     * @param cause underlying failure
     */
    public ReloadableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param cause underlying failure
     */
    public ReloadableException(Throwable cause) {
        super(cause);
    }
}
