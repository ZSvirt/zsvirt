package org.zstack.header.network.service;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = NetworkServiceTypeVO.class)
public class NetworkServiceTypeInventory {
    private String networkServiceProviderUuid;
    private String type;

    public static NetworkServiceTypeInventory valueOf(NetworkServiceTypeVO vo) {
        NetworkServiceTypeInventory inv = new NetworkServiceTypeInventory();
        inv.setNetworkServiceProviderUuid(vo.getNetworkServiceProviderUuid());
        inv.setType(vo.getType());
        return inv;
    }

    public static List<NetworkServiceTypeInventory> valueOf(Collection<NetworkServiceTypeVO> vos) {
        List<NetworkServiceTypeInventory> invs = new ArrayList<NetworkServiceTypeInventory>();
        for (NetworkServiceTypeVO vo : vos) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
