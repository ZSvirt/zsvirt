package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2020/7/22.
 */
@RestRequest(
        path = "/vm-instances/devices",
        method = HttpMethod.GET,
        responseClass = APIGetVmDeviceAddressReply.class
)
public class APIGetVmDeviceAddressMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(validValues = {"VolumeVO"})
    private List<String> resourceTypes;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public static APIGetVmDeviceAddressMsg __example__() {
        APIGetVmDeviceAddressMsg msg = new APIGetVmDeviceAddressMsg();
        msg.uuid = uuid();
        msg.resourceTypes = Collections.singletonList("VolumeVO");
        return msg;
    }
}
