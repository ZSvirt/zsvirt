package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.message.OverlayMessage;

import java.util.List;

public interface VirtualRouterHaGroupExtensionPoint {

    List<String> getPublicIp(String vrUuid, String l3Uuid);
    String getPublicIpUuid(String vrUuid, String l3Uuid);

    void VirtualRouterVmHaAttachL3Network(String vrUuid, String l3NetworkUuid, boolean applyToVirtualRouter, Completion completion);
    void VirtualRouterVmHaDetachL3Network(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion);
    Boolean isVirtualRouterInSameHaPair(List<String> vrUuids);

    void syncVirtualRouterHaConfigToBackend(String vrUuid, boolean syncPeer, Completion completion);

    String getPeerUuid(String vrUuid);

    void submitTaskToHaRouter(VirtualRouterHaTask task, Completion completion);

    void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid);
    void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid, boolean override);
    void detachNetworkServiceFromHaRouter(String type, List<String> uuids, String vrUuid);
    List<String> getHaVrUuidsFromNetworkService(String type, String Uuid);
    List<String> getHaVrUuidsFromNetworkService(String type);
    List<String> getNetworkServicesFromHaVrUuid(String type, String vrUuid);
    List<String> getNetworkServicesFromHaGroupUuid(String type, String haGroupUuid);

    String getHaGroupName(String vrUuid);
    String getHaGroupUuid(String vrUuid);
    void virtualRouterOverlayMsgHandle(OverlayMessage msg, Completion completion);
}
