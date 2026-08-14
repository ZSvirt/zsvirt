package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public  class VmHostNameHelper {

    @Autowired
    DatabaseFacade dbf;

    public String getHostName(VmInstanceInventory vm) {
        String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);

        if (hostName == null) {
            for (VmNicInventory nic : vm.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid()) && nic.getIp() != null) {
                    if (nic.getIpVersion() == IPv6Constants.IPv4) {
                        hostName = nic.getIp().replaceAll("\\.", "-");
                    } else {
                        hostName = IPv6NetworkUtils.ipv6AddessToHostname(nic.getIp());
                    }
                }
            }
        }

        return hostName;
    }

    public String getHostName(VmInstanceVO vm) {
        String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);

        if (hostName == null) {
            for (VmNicVO nic : vm.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid()) && nic.getIp() != null) {
                    if (NetworkUtils.isIpv4Address(nic.getIp())) {
                        hostName = nic.getIp().replaceAll("\\.", "-");
                    } else {
                        hostName = IPv6NetworkUtils.ipv6AddessToHostname(nic.getIp());
                    }
                }
            }
        }

        return hostName;
    }

    public String getHostName(String vmUuid) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        return getHostName(vm);
    }
}
