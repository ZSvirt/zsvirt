package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/vm-instances/metadata/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVmInstanceMetadataEvent.class,
        isAction = true
)
public class APIUpdateVmInstanceMetadataMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class, nonempty = true)
    private List<String> vmUuids;

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }

    public static APIUpdateVmInstanceMetadataMsg __example__() {
        APIUpdateVmInstanceMetadataMsg msg = new APIUpdateVmInstanceMetadataMsg();
        msg.vmUuids = Collections.singletonList(uuid());
        return msg;
    }
}
