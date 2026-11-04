package org.zstack.resourceconfig;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryResourceConfigReply extends APIQueryReply {
    private List<ResourceConfigInventory> inventories;

    public List<ResourceConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceConfigInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryResourceConfigReply __example__() {
        APIQueryResourceConfigReply reply = new APIQueryResourceConfigReply();
        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(uuid(ClusterVO.class));
        clusterConfig.setUuid(uuid(ResourceConfigVO.class));
        clusterConfig.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setValue("10");
        reply.setInventories(Collections.singletonList(clusterConfig));
        return reply;
    }
}
