package org.zstack.pciDevice.specification.mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2019-05-05.
 */
@RestRequest(
        path = "/mdev-device-specs/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMdevDeviceSpecEvent.class,
        isAction = true
)
public class APIUpdateMdevDeviceSpecMsg extends APIMessage {
    @APIParam(resourceType = MdevDeviceSpecVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdateMdevDeviceSpecMsg __example__() {
        APIUpdateMdevDeviceSpecMsg msg = new APIUpdateMdevDeviceSpecMsg();
        msg.setUuid(uuid(MdevDeviceSpecVO.class));
        msg.setName("GRID_M60-2A");
        msg.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], a1, VGA compatible controller");
        msg.setState(MdevDeviceSpecState.Enabled.toString());
        return msg;
    }
}
