package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class APIAttachNetworkServiceProviderToL2NetworkEvent extends APIEvent {
    private NetworkServiceProviderInventory inventory;

    public APIAttachNetworkServiceProviderToL2NetworkEvent() {
        super(null);
    }

    public APIAttachNetworkServiceProviderToL2NetworkEvent(String apiId) {
        super(apiId);
    }

    public NetworkServiceProviderInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetworkServiceProviderInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAttachNetworkServiceProviderToL2NetworkEvent __example__() {
        APIAttachNetworkServiceProviderToL2NetworkEvent event = new APIAttachNetworkServiceProviderToL2NetworkEvent();

        NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        NetworkServiceProviderL2NetworkRefVO refVO = new NetworkServiceProviderL2NetworkRefVO();

        refVO.setId(1L);
        refVO.setL2NetworkUuid(uuid());
        refVO.setNetworkServiceProviderUuid(uuid());

        Set<NetworkServiceProviderL2NetworkRefVO> setRefVO = new HashSet<>();

        Set<String> nst = new HashSet<>();
        nst.add("SecurityGroup");

        vo.setUuid(uuid());
        vo.setAttachedL2NetworkRefs(setRefVO);
        vo.setName("SecurityGroup");
        vo.setType("SecurityGroup");
        vo.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vo.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vo.setNetworkServiceTypes(nst);

        NetworkServiceProviderInventory nsp = NetworkServiceProviderInventory.valueOf(vo);

        event.setInventory(nsp);
        return event;
    }

}
