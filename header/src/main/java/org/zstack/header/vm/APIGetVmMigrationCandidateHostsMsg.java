package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/migration-target-hosts",
        method = HttpMethod.GET,
        responseClass = APIGetVmMigrationCandidateHostsReply.class
)
public class APIGetVmMigrationCandidateHostsMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIGetVmMigrationCandidateHostsMsg __example__() {
        APIGetVmMigrationCandidateHostsMsg msg = new APIGetVmMigrationCandidateHostsMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

}
