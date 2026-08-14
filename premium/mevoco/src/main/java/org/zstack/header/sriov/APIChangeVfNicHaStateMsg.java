package org.zstack.header.sriov;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/nics/{vfNicUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVfNicHaStateEvent.class
)
public class APIChangeVfNicHaStateMsg extends APIMessage {
    @APIParam(resourceType = VmVfNicVO.class, nonempty = true)
    private String vfNicUuid;

    @APIParam(validValues = {"Enabled", "Disabled"}, maxLength = 32, required = true)
    private String haState;

    public String getVfNicUuid() {
        return vfNicUuid;
    }

    public void setVfNicUuid(String vfNicUuid) {
        this.vfNicUuid = vfNicUuid;
    }

    public String getHaState() {
        return haState;
    }

    public void setHaState(String haState) {
        this.haState = haState;
    }

    public static APIChangeVfNicHaStateMsg __example__() {
        APIChangeVfNicHaStateMsg msg = new APIChangeVfNicHaStateMsg();
        msg.setVfNicUuid(uuid());
        msg.setHaState("Enabled");
        return msg;
    }
}
