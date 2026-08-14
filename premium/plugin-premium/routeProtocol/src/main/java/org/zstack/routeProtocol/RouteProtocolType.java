package org.zstack.routeProtocol;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RouteProtocolType  implements Serializable {
    private static Map<String, RouteProtocolType> protocols = Collections.synchronizedMap(new HashMap<String, RouteProtocolType>());
    private final String protocolName;

    public RouteProtocolType(String protocolName) {
        this.protocolName = protocolName;
    }

    public static RouteProtocolType valueOf(String protocolName) {
        RouteProtocolType protocol = protocols.get(protocolName);
        if (protocol != null) {
            throw new IllegalArgumentException("RouteProtocolType protocol: " + protocolName + " was not registered by any one component");
        }
        return protocol;
    }

    @Override
    public String toString() {
        return protocolName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof RouteProtocolType)) {
            return false;
        }

        RouteProtocolType protocol = (RouteProtocolType)t;
        return protocol.toString().equals(protocolName);
    }

    @Override
    public int hashCode() {
        return protocolName.hashCode();
    }
}
