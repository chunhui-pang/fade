package net.floodlightcontroller.applications.fade.exception;

/**
 * argument illegal
 */
public class InvalidArgumentException extends RuntimeException {
    public InvalidArgumentException() {
    }

    public InvalidArgumentException(String s) {
        super(s);
    }

    public InvalidArgumentException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
