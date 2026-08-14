package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for network service information of l3Network
 * @category l3network
 * @example {
 * "l3NetworkUuid": "f73926eb4f234f8195c61c33d8db419d",
 * "networkServiceProviderUuid": "bbb525dc4cc8451295d379797e092dba",
 * "networkServiceType": "PortForwarding"
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = NetworkServiceL3NetworkRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "serviceProvider", inventoryClass = NetworkServiceProviderInventory.class,
                foreignKey = "networkServiceProviderUuid", expandedInventoryKey = "uuid"),
})
public class NetworkServiceL3NetworkRefInventory implements Serializable {
    /**
     * @desc l3Network uuid
     */
    private String l3NetworkUuid;
    /**
     * @desc uuid of network service provider that provides this service. See :ref:`NetworkServiceProviderInventory`
     */
    private String networkServiceProviderUuid;
    /**
     * @desc network service type
     */
    private String networkServiceType;

    public NetworkServiceL3NetworkRefInventory() {
    }

    protected NetworkServiceL3NetworkRefInventory(NetworkServiceL3NetworkRefVO vo) {
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setNetworkServiceProviderUuid(vo.getNetworkServiceProviderUuid());
        this.setNetworkServiceType(vo.getNetworkServiceType());
    }

    public static NetworkServiceL3NetworkRefInventory valueOf(NetworkServiceL3NetworkRefVO vo) {
        NetworkServiceL3NetworkRefInventory inv = new NetworkServiceL3NetworkRefInventory(vo);
        return inv;
    }

    public static List<NetworkServiceL3NetworkRefInventory> valueOf(Collection<NetworkServiceL3NetworkRefVO> vos) {
        List<NetworkServiceL3NetworkRefInventory> invs = new ArrayList<NetworkServiceL3NetworkRefInventory>(vos.size());
        for (NetworkServiceL3NetworkRefVO vo : vos) {
            invs.add(NetworkServiceL3NetworkRefInventory.valueOf(vo));
        }
        return invs;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }

}
