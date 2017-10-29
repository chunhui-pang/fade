package net.floodlightcontroller.applications.fade.factory;

/**
 * A abstract factory method to create the whole context of fade execution.
 */
public interface FadeFactory {
    /**
     * create the fade context
     * @return the fade execution context
     */
    FadeContext createFadeContext();
}
