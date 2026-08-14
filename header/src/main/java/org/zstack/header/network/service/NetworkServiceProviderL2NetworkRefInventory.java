package org.zstack.header.network.service;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = NetworkServiceProviderL2NetworkRefVO.class)
public class NetworkServiceProviderL2NetworkRefInventory {
    private String networkServiceProviderUuid;
    private String l2NetworkUuid;

    public static NetworkServiceProviderL2NetworkRefInventory valueOf(NetworkServiceProviderL2NetworkRefVO vo) {
        NetworkServiceProviderL2NetworkRefInventory inv = new NetworkServiceProviderL2NetworkRefInventory();
        inv.setNetworkServiceProviderUuid(vo.getNetworkServiceProviderUuid());
        inv.setL2NetworkUuid(vo.getL2NetworkUuid());
        return inv;
    }

    public static List<NetworkServiceProviderL2NetworkRefInventory> valueOf(Collection<NetworkServiceProviderL2NetworkRefVO> vos) {
        List<NetworkServiceProviderL2NetworkRefInventory> invs = new ArrayList<NetworkServiceProviderL2NetworkRefInventory>();
        for (NetworkServiceProviderL2NetworkRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
}
