package net.floodlightcontroller.applications.dumproute;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Route;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

public interface IDumpRouteService extends IFloodlightService {
	/**
	 * get route by host's source MAC and host's destination MAC
	 * @param srcMac the source host's MAC
	 * @param dstMac the destination host's MAC
	 * @return the route. If several paths are found, an error would be thrown
	 */
	Route getRoute(MacAddress srcMac, MacAddress dstMac);

	/**
	 * get route by source host's IP address and destination host's IP address
	 * @param src the source host's IP address
	 * @param dst the destination host's IP address
	 * @return the path between the two hosts. An error was thrown if there are more than 1 paths.
	 */
	Route getRoute(IPv4Address src, IPv4Address dst);
}
