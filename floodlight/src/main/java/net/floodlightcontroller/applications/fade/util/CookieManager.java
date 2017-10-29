package net.floodlightcontroller.applications.fade.util;

import org.projectfloodlight.openflow.types.U64;

/**
 * manage all cookie values.
 * Cookie value is embedded in dedicated rules,
 * and it is used to identify dedicated rules.
 */
public interface CookieManager {
    /**
     * request a new cookie value
     * @return a new cookie value
     */
    U64 requestCookie();

    /**
     * Release a cookie.
     * This function should be called when dedicated rules expire
     * @param cookie the cookie embedded in the expired rule
     */
    void releaseCookie(U64 cookie);
}
