package org.zstack.header.network.service;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestResponse(allTo = "inventories")
public class APIQueryNetworkServiceProviderReply extends APIQueryReply {
    private List<NetworkServiceProviderInventory> inventories;

    public List<NetworkServiceProviderInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkServiceProviderInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryNetworkServiceProviderReply __example__() {
        APIQueryNetworkServiceProviderReply reply = new APIQueryNetworkServiceProviderReply();

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

        reply.setInventories(Arrays.asList(nsp));
        return reply;
    }

}
