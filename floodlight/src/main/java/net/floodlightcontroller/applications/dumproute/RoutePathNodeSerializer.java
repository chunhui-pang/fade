package net.floodlightcontroller.applications.dumproute;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class RoutePathNodeSerializer extends JsonSerializer<RoutePathNode>{

	@Override
	public void serialize(RoutePathNode node, JsonGenerator jGen, SerializerProvider arg2) throws IOException,
			JsonProcessingException {
		jGen.writeStartObject();

		jGen.writeStringField("dpid", node.getDpid().toString());
        jGen.writeStringField("port", node.getPort().toString());

        jGen.writeEndObject();
	}

}
