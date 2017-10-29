package net.floodlightcontroller.applications.fade.util;

import net.floodlightcontroller.core.util.AppCookie;
import org.projectfloodlight.openflow.types.U64;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * implementation of {@link CookieManager}.
 * We use atomicLong to generate ID of cookie value.
 * The cookie values would cycle when these IDs run out.
 */
public class CookieManagerImpl implements CookieManager {
    private static final int MIN_COOKIE_VAL = 0x01;
    private AtomicInteger nextCookie = new AtomicInteger(MIN_COOKIE_VAL);
    private int appId;
    private String appName;

    public CookieManagerImpl(int appId, String appName){
        this.appId = appId;
        AppCookie.registerApp(appId, appName);
    }

    @Override
    public U64 requestCookie() {
        int user = this.nextCookie.getAndIncrement();
        return AppCookie.makeCookie(this.appId, user);
    }

    @Override
    public void releaseCookie(U64 cookie) {
        // do nothing
    }
}
