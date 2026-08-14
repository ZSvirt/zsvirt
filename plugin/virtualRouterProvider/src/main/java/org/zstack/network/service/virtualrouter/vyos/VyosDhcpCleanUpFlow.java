package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l3.L3NetworkSystemTags;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosDhcpCleanUpFlow extends NoRollbackFlow {
    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceInventory vrInv = (VmInstanceInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        for (VmNicInventory nic : vrInv.getVmNics()) {
            String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(nic.getL3NetworkUuid(), L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
            if (uuid == null) {
                continue;
            }

            if (uuid.equals(vrInv.getUuid())) {
                L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.delete(nic.getL3NetworkUuid());
            }
        }

        trigger.next();
    }
}
