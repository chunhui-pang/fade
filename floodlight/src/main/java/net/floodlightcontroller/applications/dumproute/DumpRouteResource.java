package net.floodlightcontroller.applications.dumproute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class DumpRouteResource extends ServerResource {
    private static final String ADDR_TYPE_SELECTOR = "addr_type";
    private enum AddrType {
        BYMAC("byMAC"), BYIP("byIP");
        String desc;
        AddrType(String desc){ this.desc = desc; }
        public String getDesc(){ return this.desc; }
    }
	@Post("json")
	public Representation dump(String json) {
		IDumpRouteService dumper = (IDumpRouteService) getContext().getAttributes().get(
				IDumpRouteService.class.getCanonicalName());
        AddrType addrType = this.getAddrType();
        if(addrType == null){
            this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Cannot read data");
            return new StringRepresentation("addr_type could only be byMAC or byIP");
        }
		try{

			Route route = null;
			switch (addrType){
                case BYIP:
                    Pair<IPv4Address, IPv4Address> iPs = json2IPs(json);
                    route = dumper.getRoute(iPs.getLeft(), iPs.getRight());
                    break;
                case BYMAC:
                    Pair<MacAddress, MacAddress> macs = jsonToMacs(json);
                    route = dumper.getRoute(macs.getLeft(), macs.getRight());
                    break;
                default:
                    // impossible
                    break;
            }

			if(route == null || route.getPath() == null || route.getPath().isEmpty()){
				this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Cannot find a route between the two hosts.");
				return null;
			}
			List<RoutePathNode> path = new ArrayList<RoutePathNode>(route.getPath().size());
			for(NodePortTuple npt : route.getPath()){
				path.add(new RoutePathNode(npt.getNodeId(), npt.getPortId()));
			}
			return new JacksonRepresentation<List<RoutePathNode>>(path);
		}catch(IOException e){
			this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Cannot read data");
			return new StringRepresentation("json format error");
		}catch(IllegalArgumentException e){
			 this.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "unformatted addresses");
			 return null;
		}
	}

	private AddrType getAddrType(){
        String addr = (String) getRequestAttributes().get(ADDR_TYPE_SELECTOR);
        AddrType addrType = AddrType.BYMAC;
        if(addr != null) {
            addrType = null;
            for (AddrType at : AddrType.values()) {
                if (at.getDesc().toLowerCase().equals(addr.toLowerCase())) {
                    addrType = at;
                }
            }
        }
        return addrType;
    }

	private static Pair<MacAddress, MacAddress> jsonToMacs(String json) throws IOException, IllegalArgumentException {
		MacAddress src = null, dst = null;
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		try {
			jp = f.createParser(json);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String key = jp.getCurrentName();
			jp.nextToken();
			String value = jp.getText();
			if (value.equals(""))
				continue;

			// used for removing rule
			if ("src".equals(key)) {
				src = MacAddress.of(value);
			}

			else if ("dst".equals(key)) {
				dst = MacAddress.of(value);
			}
		}
		return new ImmutablePair<>(src, dst);
	}

    private static Pair<IPv4Address, IPv4Address> json2IPs(String json) throws IOException, IllegalArgumentException {
        MappingJsonFactory f = new MappingJsonFactory();
        JsonParser jp;
        IPv4Address src = null, dst = null;
        try {
            jp = f.createParser(json);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        jp.nextToken();
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected START_OBJECT");
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                throw new IOException("Expected FIELD_NAME");
            }

            String key = jp.getCurrentName();
            jp.nextToken();
            String value = jp.getText();
            if (value.equals(""))
                continue;

            // used for removing rule
            if ("src".equals(key)) {
                src = IPv4Address.of(value);
            }

            else if ("dst".equals(key)) {
                dst = IPv4Address.of(value);
            }
        }
        if(src == null || dst == null){
            throw new IllegalArgumentException("the ip address of the source host and the destination host should be all set.");
        }
        return new ImmutablePair<IPv4Address, IPv4Address>(src, dst);
    }

}
