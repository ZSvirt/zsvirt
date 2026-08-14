package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

@RestRequest(
        path = "/affinity-groups/{affinityGroupUuid}/vm-instances",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveVmFromAffinityGroupEvent.class
)
public class APIRemoveVmFromAffinityGroupMsg extends APIMessage implements AffinityGroupMessage {

    @APIParam(resourceType = AffinityGroupVO.class)
    private String affinityGroupUuid;
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String volumeUuid) {
        this.uuid = volumeUuid;
    }
 
    public static APIRemoveVmFromAffinityGroupMsg __example__() {
        APIRemoveVmFromAffinityGroupMsg msg = new APIRemoveVmFromAffinityGroupMsg();
        msg.setUuid(uuid());
        msg.setAffinityGroupUuid(uuid());

        return msg;
    }

    @Override
    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }
}
