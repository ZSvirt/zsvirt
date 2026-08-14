package org.zstack.resourceconfig;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateResourceConfigEvent extends APIEvent {
    private ResourceConfigInventory inventory;

    public APIUpdateResourceConfigEvent() {
    }

    public APIUpdateResourceConfigEvent(String apiId) {
        super(apiId);
    }

    public ResourceConfigInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceConfigInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateResourceConfigEvent __example__() {
        APIUpdateResourceConfigEvent reply = new APIUpdateResourceConfigEvent();
        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(uuid(ClusterVO.class));
        clusterConfig.setUuid(uuid(ResourceConfigVO.class));
        clusterConfig.setCreateDate(DocUtils.timestamp());
        clusterConfig.setLastOpDate(DocUtils.timestamp());
        clusterConfig.setValue("10");
        reply.inventory = clusterConfig;
        return reply;
    }
}
