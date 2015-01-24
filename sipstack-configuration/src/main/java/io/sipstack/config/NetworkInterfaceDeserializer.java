/**
 * 
 */
package io.sipstack.config;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.address.SipURI;
import io.sipstack.netty.codec.sip.Transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author jonas@jonasborjesson.com
 */
public class NetworkInterfaceDeserializer extends JsonDeserializer<NetworkInterfaceConfiguration> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkInterfaceConfiguration deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        final JsonNode node = jp.getCodec().readTree(jp);
        final JsonNode nameNode = node.get("name");
        final JsonNode listenNode = node.get("listen");
        final JsonNode vipNode = node.get("vipAddress");
        final JsonNode transportNode = node.get("transport");

        if (nameNode == null) {
            throw new IllegalArgumentException("You must specify the name of the Network Interface");
        }

        if (listenNode == null) {
            throw new IllegalArgumentException("You must specify a listen address of the Network Interface");
        }

        // only honor host and port.
        final SipURI tmp = SipURI.frame(ensureSipURI(listenNode.asText()));
        final SipURI listenAddress = SipURI.with().host(tmp.getHost()).port(tmp.getPort()).build();

        SipURI vipAddress = null;
        if (vipNode != null) {
            final SipURI tmpVip = SipURI.frame(ensureSipURI(vipNode.asText()));
            vipAddress = SipURI.with().host(tmpVip.getHost()).port(tmpVip.getPort()).build();
        }

        final List<Transport> transports = new ArrayList<>();
        if (transportNode != null) {
            if (transportNode instanceof TextNode) {
                transports.add(Transport.valueOf(transportNode.asText()));
            } else if (transportNode instanceof ArrayNode) {
                final ArrayNode transportNodes = (ArrayNode) transportNode;
                final Iterator<JsonNode> nodes = transportNodes.iterator();
                while (nodes.hasNext()) {
                    transports.add(Transport.valueOf(nodes.next().asText()));
                }
            }
        }

        return new NetworkInterfaceConfiguration(nameNode.asText(), listenAddress, vipAddress,
                Collections.unmodifiableList(transports));
    }

    private static Buffer ensureSipURI(final String value) {
        if (value.startsWith("sip")) {
            return Buffers.wrap(value.trim());
        }
        return Buffers.wrap("sip:" + value.trim());
    }

}
