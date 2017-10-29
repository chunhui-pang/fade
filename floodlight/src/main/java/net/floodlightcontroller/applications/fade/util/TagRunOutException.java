package net.floodlightcontroller.applications.fade.util;

/**
 * Tags are run out, please use more bits
 */
public class TagRunOutException extends RuntimeException {
    public TagRunOutException() {
    }

    public TagRunOutException(String s) {
        super(s);
    }

    public TagRunOutException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
