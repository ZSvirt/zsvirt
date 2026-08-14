package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/affinity-groups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteAffinityGroupEvent.class
)
public class APIDeleteAffinityGroupMsg extends APIDeleteMessage implements AffinityGroupMessage {
    /**
     * @desc
     * group uuid
     */
    @APIParam(resourceType = AffinityGroupVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteAffinityGroupMsg __example__() {
        APIDeleteAffinityGroupMsg msg = new APIDeleteAffinityGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getAffinityGroupUuid(){
        return uuid;
    }
}
