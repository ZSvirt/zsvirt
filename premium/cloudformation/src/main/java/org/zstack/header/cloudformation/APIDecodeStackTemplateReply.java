package org.zstack.header.cloudformation;

import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.cloudformation.template.struct.ResourceType;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionDSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/8/3.
 */
@RestResponse(allTo = "resources")
public class APIDecodeStackTemplateReply extends APIReply {
    private List<ResourceStruct> resources = new ArrayList<>();

    public List<ResourceStruct> getResources() {
        return resources;
    }

    public void setResources(List<ResourceStruct> resources) {
        this.resources = resources;
    }

    public static APIDecodeStackTemplateReply __example__() {
        APIDecodeStackTemplateReply reply = new APIDecodeStackTemplateReply();
        Map<String, Object> properties = new HashMap<>();

        ResourceStruct resource1 = new ResourceStruct();
        resource1.setResourceName("WebServer");
        resource1.setResourceType("VmInstance");
        resource1.setType(ResourceType.Resource);
        resource1.setCreated(false);
        resource1.setDeletePolicy("Retain");
        resource1.getInDegree().addAll(CollectionDSL.list("imageUuid", "instanceOfferingUuid", "l3", "addDns"));
        properties.put("name", "test-vm-1");
        properties.put("instanceOfferingUuid", "ce9bef5bc1e74dab80a23fbc9b4d1f71");
        properties.put("imageUuid", "4d39f7276faa41ee9cee39bb3f27c8d4");
        properties.put("l3NetworkUuids", CollectionDSL.list("${l3::uuid}"));
        resource1.setProperties(properties);

        properties.clear();
        ResourceStruct resource2 = new ResourceStruct();
        resource2.setResourceName("l3");
        resource2.setResourceType("L3Network");
        resource2.setType(ResourceType.Resource);
        resource2.setCreated(false);
        resource2.getInDegree().addAll(CollectionDSL.list("l2NetworkUuid"));
        properties.put("name", "l3-network");
        properties.put("l2NetworkUuid", "d55161ef863942d8a53a4f8448074749");
        resource2.setProperties(properties);

        properties.clear();
        ResourceStruct resource3 = new ResourceStruct();
        resource3.setResourceName("addIpRange");
        resource3.setType(ResourceType.Action);
        resource3.setCreated(false);
        resource3.getInDegree().addAll(CollectionDSL.list("l3"));
        properties.put("startIp", "192.168.0.10");
        properties.put("endIp", "192.168.0.100");
        properties.put("netmask", "255.255.255.0");
        properties.put("name", "ip-range-${l3::uuid}");
        properties.put("l3NetworkUuid", "${l3::uuid}");
        properties.put("gateway", "192.168.0.1");
        resource3.setProperties(properties);

        properties.clear();
        ResourceStruct resource4 = new ResourceStruct();
        resource4.setResourceName("addDns");
        resource4.setType(ResourceType.Action);
        resource4.setCreated(false);
        resource4.getInDegree().addAll(CollectionDSL.list("l3", "addIpRange"));
        properties.put("dns", "223.5.5.5");
        properties.put("l3NetworkUuid", "${l3::uuid}");
        resource4.setProperties(properties);

        properties.clear();
        ResourceStruct resource5 = new ResourceStruct();
        resource5.setResourceName("imageUuid");
        resource5.setResourceType("Image");
        resource5.setType(ResourceType.Resource);
        resource5.setCreated(true);
        properties.put("uuid", "4d39f7276faa41ee9cee39bb3f27c8d4");
        resource5.setProperties(properties);

        properties.clear();
        ResourceStruct resource6 = new ResourceStruct();
        resource6.setResourceName("instanceOfferingUuid");
        resource6.setResourceType("InstanceOffering");
        resource6.setType(ResourceType.Resource);
        resource6.setCreated(true);
        properties.put("uuid", "ce9bef5bc1e74dab80a23fbc9b4d1f71");
        resource6.setProperties(properties);

        properties.clear();
        ResourceStruct resource7 = new ResourceStruct();
        resource7.setResourceName("l2NetworkUuid");
        resource7.setResourceType("L2Network");
        resource7.setType(ResourceType.Resource);
        resource7.setCreated(true);
        properties.put("uuid", "d55161ef863942d8a53a4f8448074749");
        resource7.setProperties(properties);

        reply.setResources(CollectionDSL.list(resource1, resource2, resource3, resource4, resource5, resource6, resource7));
        return reply;
    }
}
