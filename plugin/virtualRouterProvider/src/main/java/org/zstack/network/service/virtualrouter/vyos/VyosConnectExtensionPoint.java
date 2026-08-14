package org.zstack.network.service.virtualrouter.vyos;

public interface VyosConnectExtensionPoint {
    void syncVersionToDb(String vrUuid, String version);
}