package org.zstack.header.cloudformation;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionDSL;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/7/11.
 */
@RestResponse(fieldsTo = {"resources"})
public class APIGetSupportedCloudFormationResourcesReply extends APIReply {
    private List<SupportedResourceStruct> resources = new ArrayList<>();

    public List<SupportedResourceStruct> getResources() {
        return resources;
    }

    public void setResources(List<SupportedResourceStruct> resources) {
        this.resources = resources;
    }

    public static APIGetSupportedCloudFormationResourcesReply __example__() {
        APIGetSupportedCloudFormationResourcesReply reply = new APIGetSupportedCloudFormationResourcesReply();
        SupportedResourceStruct s1 = new SupportedResourceStruct();
        s1.setName("VmInstance");
        s1.setActionName("CreateVmInstanceAction");
        s1.setType("Resource");
        s1.setResources(CollectionDSL.list("InstanceOffering", "Image", "L3Network", "DiskOffering", "Zone", "Cluster", "Host", "PrimaryStorage"));

        SupportedResourceStruct s2 = new SupportedResourceStruct();
        s2.setName("AttachDataVolumeToVm");
        s2.setActionName("AttachDataVolumeToVmAction");
        s2.setType("Action");
        s2.setResources(CollectionDSL.list("VmInstance", "Volume"));

        reply.setResources(CollectionDSL.list(s1, s2));
        return reply;
    }
}
