package org.zstack.header.agent;

public interface ProxyHardwareFactory {
    ProxyHardware getProxyHardware(String hostName);
}
