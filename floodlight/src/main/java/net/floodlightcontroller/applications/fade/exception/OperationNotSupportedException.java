package net.floodlightcontroller.applications.fade.exception;

/**
 * operation not supported
 */
public class OperationNotSupportedException extends RuntimeException {
    public OperationNotSupportedException() {
    }

    public OperationNotSupportedException(String s) {
        super(s);
    }

    public OperationNotSupportedException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
