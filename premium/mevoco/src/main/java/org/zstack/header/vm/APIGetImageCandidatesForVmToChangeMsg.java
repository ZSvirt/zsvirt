package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 11/2/17.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/image-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetImageCandidatesForVmToChangeReply.class
)
public class APIGetImageCandidatesForVmToChangeMsg extends APISyncCallMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIGetImageCandidatesForVmToChangeMsg __example__() {
        APIGetImageCandidatesForVmToChangeMsg msg = new APIGetImageCandidatesForVmToChangeMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }
}
