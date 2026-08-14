package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestRequest(
        path = "/pci-device/hosts/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostIommuStateEvent.class,
        isAction = true
)
public class APIUpdateHostIommuStateMsg extends APIMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    @APIParam(validValues = {"Enabled", "Disabled"})
    private String state;

    public static APIUpdateHostIommuStateMsg __example__() {
        APIUpdateHostIommuStateMsg msg = new APIUpdateHostIommuStateMsg();
        msg.setUuid(uuid());
        msg.setState(HostIommuStateType.Enabled.toString());

        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
