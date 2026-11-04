package org.zstack.resourceconfig;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by MaJin on 2019/2/23.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetResourceConfigReply extends APIReply {
    private String value;
    private List<ResourceConfigInventory> effectiveConfigs;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ResourceConfigInventory> getEffectiveConfigs() {
        return effectiveConfigs;
    }

    public void setEffectiveConfigs(List<ResourceConfigInventory> effectiveConfigs) {
        this.effectiveConfigs = effectiveConfigs;
    }

    public static APIGetResourceConfigReply __example__ () {
        APIGetResourceConfigReply reply = new APIGetResourceConfigReply();
        reply.value = "5";
        ResourceConfigInventory hostConfig = new ResourceConfigInventory();
        hostConfig.setCategory("host");
        hostConfig.setName("cpu.overProvisioning.ratio");
        hostConfig.setResourceType(HostVO.class.getSimpleName());
        hostConfig.setResourceUuid(uuid(HostVO.class));
        hostConfig.setUuid(uuid(ResourceConfigVO.class));
        hostConfig.setCreateDate(DocUtils.timestamp());
        hostConfig.setLastOpDate(DocUtils.timestamp());
        hostConfig.setValue("5");

        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(uuid(ClusterVO.class));
        clusterConfig.setUuid(uuid(ResourceConfigInventory.class)); // hostConfig.uuid and this uuid must be different
        clusterConfig.setCreateDate(DocUtils.timestamp());
        clusterConfig.setLastOpDate(DocUtils.timestamp());
        clusterConfig.setValue("10");

        reply.effectiveConfigs = Arrays.asList(hostConfig, clusterConfig);
        return reply;
    }

}
