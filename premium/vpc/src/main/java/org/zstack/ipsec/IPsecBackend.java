package org.zstack.ipsec;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

/**
 * Created by xing5 on 2016/11/8.
 */
public interface IPsecBackend {
    void createIPsecConnection(IPsecConnectionInventory inv, Completion completion);

    void deleteIPsecConnection(IPsecConnectionInventory inv, String l3NetworkUuid, VirtualRouterVmInventory vrInv, boolean skip_vip, NoErrorCompletion completion);

    void changeIPsecConnectionState(IPsecConnectionInventory inv, IPsecState nextState, Completion completion);

    void syncIPsecConnection(IPsecConnectionInventory inv, Completion completion);

    void deleteIpSecconnection(VirtualRouterVmInventory vr, IPsecConnectionInventory inv, Completion completion);

    void createIpsecConnection(VirtualRouterVmInventory vr, IPsecConnectionInventory inv, Completion completion);

    void updateIpsecVersion(String vrUuid, String targetVersion, Completion completion);

    String getNetworkServiceProviderType();
}
