package org.zstack.header.agent;

public interface ProxyHardwareFactory {
    ProxyHardware getProxyHardware(String hostName);

    default String getClusterUuid(String hostName) {
        return null;
    }
}
