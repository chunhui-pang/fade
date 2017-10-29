package net.floodlightcontroller.applications.dumproute;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using=RoutePathNodeSerializer.class)
public class RoutePathNode {
	private DatapathId dpid;
	private OFPort port;
	
	public RoutePathNode(DatapathId dpid, OFPort port) {
		super();
		this.dpid = dpid;
		this.port = port;
	}
	public DatapathId getDpid() {
		return dpid;
	}
	public void setDpid(DatapathId dpid) {
		this.dpid = dpid;
	}
	public OFPort getPort() {
		return port;
	}
	public void setPort(OFPort port) {
		this.port = port;
	}
    @Override
    public String toString() {
	return "RoutePathNode [dpid=" + dpid + ", port=" + port + "]";
    }
	
}
