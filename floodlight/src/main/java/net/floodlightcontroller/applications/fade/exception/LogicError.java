package net.floodlightcontroller.applications.fade.exception;

/**
 * the program logic error, it should never happens in normal programs.
 */
public class LogicError extends Error {
    public LogicError() {
    }

    public LogicError(String s) {
        super(s);
    }

    public LogicError(String s, Throwable throwable) {
        super(s, throwable);
    }
}
