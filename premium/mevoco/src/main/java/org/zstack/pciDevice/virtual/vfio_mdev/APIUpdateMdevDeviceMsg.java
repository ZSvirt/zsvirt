package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2019-04-28.
 */
@RestRequest(
        path = "/mdev-devices/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMdevDeviceEvent.class,
        isAction = true
)
public class APIUpdateMdevDeviceMsg extends APIMessage implements MdevDeviceMessage {
    @APIParam(resourceType = MdevDeviceVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
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

    @Override
    public String getMdevDeviceUuid() {
        return uuid;
    }

    public static APIUpdateMdevDeviceMsg __example__() {
        APIUpdateMdevDeviceMsg msg = new APIUpdateMdevDeviceMsg();
        msg.setUuid(uuid());
        msg.setName("NVIDIA_M60-2A");
        msg.setDescription("NVIDIA_M60-2A_2048MB_1920*1080_4Ins_60FPS");
        msg.setState("Enabled");
        return msg;
    }
}
