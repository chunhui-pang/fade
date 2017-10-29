package net.floodlightcontroller.applications.fade.exception;

/**
 * split fails
 */
public class CannotSplitException extends RuntimeException {
    public CannotSplitException() {
    }

    public CannotSplitException(String s) {
        super(s);
    }

    public CannotSplitException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
