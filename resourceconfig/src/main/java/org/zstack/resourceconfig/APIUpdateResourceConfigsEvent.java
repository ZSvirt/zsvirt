package org.zstack.resourceconfig;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIUpdateResourceConfigsEvent extends APIEvent {
    private List<ResourceConfigStruct> inventories;

    public APIUpdateResourceConfigsEvent() {
    }

    public APIUpdateResourceConfigsEvent(String apiId) {
        super(apiId);
    }

    public List<ResourceConfigStruct> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceConfigStruct> inventories) {
        this.inventories = inventories;
    }

    public static APIUpdateResourceConfigsEvent __example__() {
        APIUpdateResourceConfigsEvent reply = new APIUpdateResourceConfigsEvent();
        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(uuid(ClusterVO.class));
        clusterConfig.setUuid(uuid(ResourceConfigVO.class));
        clusterConfig.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setValue("10");
        ResourceConfigStruct rs = new ResourceConfigStruct();
        rs.setEffectiveConfigs(Collections.singletonList(clusterConfig));
        rs.setName("cpu.overProvisioning.ratio");
        rs.setValue("10");
        reply.setInventories(Collections.singletonList(rs));
        return reply;
    }
}
