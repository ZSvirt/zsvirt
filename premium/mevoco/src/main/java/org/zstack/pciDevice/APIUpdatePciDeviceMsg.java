package org.zstack.pciDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestRequest(
        path = "/pci-device/pci-devices/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdatePciDeviceEvent.class,
        isAction = true
)
public class APIUpdatePciDeviceMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceVO.class)
    private String uuid;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

    @APIParam(required = false, validValues = {"Enabled", "Available"})
    private String passThroughState;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, maxLength = 2048)
    private String metaData;

    public static APIUpdatePciDeviceMsg __example__() {
        APIUpdatePciDeviceMsg msg = new APIUpdatePciDeviceMsg();
        msg.setUuid(uuid());
        msg.setState(PciDeviceState.Disabled.toString());
        msg.setPassThroughState(PciDevicePassThroughState.Enabled.toString());
        msg.setMetaData("key1:value1;key2:value2");
        msg.setName("test pci");
        msg.setDescription("test pci");

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

    public String getPassThroughState() {
        return passThroughState;
    }

    public void setPassThroughState(String passThroughState) {
        this.passThroughState = passThroughState;
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

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }
}
