package net.floodlightcontroller.applications.dumproute;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class DumpRouteRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
        router.attach("/dump/{addr_type}", DumpRouteResource.class);
        return router;
	}

	@Override
	public String basePath() {
		return "/wm/dumproute";
	}

}
