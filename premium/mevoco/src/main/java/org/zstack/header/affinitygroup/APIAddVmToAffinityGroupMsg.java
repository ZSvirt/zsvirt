package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;


@RestRequest(
        path = "/affinity-groups/{affinityGroupUuid}/vm-instances/{uuid}",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIAddVmToAffinityGroupEvent.class
)
public class APIAddVmToAffinityGroupMsg extends APIMessage implements AffinityGroupMessage {
    /**
     * @desc affinity group uuid
     */
    @APIParam(resourceType = AffinityGroupVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String affinityGroupUuid;
    /**
     * @desc uuid of VM instance
     */
    @APIParam(resourceType = VmInstanceVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }

    public static APIAddVmToAffinityGroupMsg __example__() {
        APIAddVmToAffinityGroupMsg msg = new APIAddVmToAffinityGroupMsg();
        msg.uuid = uuid();
        msg.affinityGroupUuid = uuid();
        return msg;
    }

    @Override
    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }
}
