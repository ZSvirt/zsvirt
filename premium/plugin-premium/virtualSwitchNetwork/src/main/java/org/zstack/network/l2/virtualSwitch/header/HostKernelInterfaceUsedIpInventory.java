package org.zstack.network.l2.virtualSwitch.header;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

@PythonClassInventory
@Inventory(mappingVOClass = HostKernelInterfaceUsedIpVO.class, collectionValueOfMethod = "valueOf1")
public class HostKernelInterfaceUsedIpInventory extends UsedIpInventory {

    private String hostKernelInterfaceUuid;

    public String getHostKernelInterfaceUuid() {
        return hostKernelInterfaceUuid;
    }

    public void setHostKernelInterfaceUuid(String hostKernelInterfaceUuid) {
        this.hostKernelInterfaceUuid = hostKernelInterfaceUuid;
    }

    public HostKernelInterfaceUsedIpInventory() {

    }

    public HostKernelInterfaceUsedIpInventory(HostKernelInterfaceUsedIpVO vo) {
        super(vo);
        this.hostKernelInterfaceUuid = vo.getHostKernelInterfaceUuid();
    }

    public static HostKernelInterfaceUsedIpInventory valueOf(HostKernelInterfaceUsedIpVO vo) {
        return new HostKernelInterfaceUsedIpInventory(vo);
    }

    public static List<HostKernelInterfaceUsedIpInventory> valueOf1(Collection<HostKernelInterfaceUsedIpVO> vos) {
        List<HostKernelInterfaceUsedIpInventory> invs = new ArrayList<>(vos.size());
        for (HostKernelInterfaceUsedIpVO vo : vos) {
            invs.add(new HostKernelInterfaceUsedIpInventory(vo));
        }

        return invs;
    }

    public static HostKernelInterfaceUsedIpInventory __example__() {
        HostKernelInterfaceUsedIpInventory inv = new HostKernelInterfaceUsedIpInventory();
        inv.setUuid(DocUtils.createFixedUuid(HostKernelInterfaceUsedIpVO.class));
        inv.setHostKernelInterfaceUuid(DocUtils.createFixedUuid(HostKernelInterfaceVO.class));
        inv.setL3NetworkUuid(DocUtils.createFixedUuid(PortGroupVO.class));
        inv.setIpVersion(IPv6Constants.IPv4);
        inv.setIp("192.168.0.2");
        inv.setNetmask("255.255.255.0");
        inv.setIpInLong(NetworkUtils.ipv4StringToLong(inv.getIp()));
        inv.setIpInBinary(NetworkUtils.ipStringToBytes(inv.getIp()));
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        return inv;
    }

}
