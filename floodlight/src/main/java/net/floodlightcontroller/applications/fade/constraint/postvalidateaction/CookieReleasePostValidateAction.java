package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.util.CookieManager;
import org.projectfloodlight.openflow.types.U64;

/**
 * release cookie
 */
public class CookieReleasePostValidateAction implements PostValidateAction {
    private CookieManager cookieManager;
    private U64 cookie;

    public CookieReleasePostValidateAction(CookieManager cookieManager, U64 cookie) {
        this.cookieManager = cookieManager;
        this.cookie = cookie;
    }

    @Override
    public void execute(Constraint constraint, boolean validationResult) {
        this.cookieManager.releaseCookie(cookie);
    }

    @Override
    public String toString() {
        return "CookieReleasePostValidateAction{" +
                "cookie=" + cookie +
                '}';
    }
}


