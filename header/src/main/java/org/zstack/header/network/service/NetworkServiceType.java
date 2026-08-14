package org.zstack.header.network.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NetworkServiceType {
    private static Map<String, NetworkServiceType> types = Collections.synchronizedMap(new HashMap<String, NetworkServiceType>());

    public static NetworkServiceType DHCP = new NetworkServiceType("DHCP");
    public static NetworkServiceType DNS = new NetworkServiceType("DNS");
    public static NetworkServiceType SNAT = new NetworkServiceType("SNAT");
    public static NetworkServiceType PortForwarding = new NetworkServiceType("PortForwarding");
    public static NetworkServiceType Centralized_DNS = new NetworkServiceType("CentralizedDNS");
    public static NetworkServiceType HostRoute = new NetworkServiceType("HostRoute");

    private final String typeName;

    public NetworkServiceType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static NetworkServiceType valueOf(String typeName) {
        NetworkServiceType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("NetworkServiceType type: " + typeName + " was not provided by any NetworkServiceProvider");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof NetworkServiceType)) {
            return false;
        }

        NetworkServiceType type = (NetworkServiceType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
