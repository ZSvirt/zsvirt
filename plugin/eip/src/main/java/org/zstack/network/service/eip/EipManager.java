package org.zstack.network.service.eip;

import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

/**
 */
public interface EipManager extends Service {
    EipBackend getEipBackend(String providerType);

    void detachEip(EipStruct struct, String providerType, Completion completion);

    void detachEipAndUpdateDb(EipStruct struct, String providerType, DetachEipOperation dbOperation, Completion completion);

    void attachEip(EipStruct struct, String providerType, Completion completion);

    EipStruct generateEipStruct(VmNicInventory nic, VipInventory vip, EipInventory eip, UsedIpInventory guestIp);
    UsedIpInventory getEipGuestIp(String eipUuid);
}
