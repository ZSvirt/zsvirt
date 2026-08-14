package org.zstack.pciDevice.specification.pci;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2019-03-06.
 */
@RestRequest(
        path = "/pci-device-specs/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdatePciDeviceSpecEvent.class,
        isAction = true
)
public class APIUpdatePciDeviceSpecMsg extends APIMessage {
    @APIParam(resourceType = PciDeviceSpecVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false)
    @NoLogging
    private String romContent;

    @APIParam(maxLength = 255, required = false)
    private String romVersion;

    @APIParam(required = false)
    private boolean abandonSpecRom;

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

    public String getRomContent() {
        return romContent;
    }

    public void setRomContent(String romContent) {
        this.romContent = romContent;
    }

    public String getRomVersion() {
        return romVersion;
    }

    public void setRomVersion(String romVersion) {
        this.romVersion = romVersion;
    }

    public boolean isAbandonSpecRom() {
        return abandonSpecRom;
    }

    public void setAbandonSpecRom(boolean abandonSpecRom) {
        this.abandonSpecRom = abandonSpecRom;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdatePciDeviceSpecMsg __example__() {
        APIUpdatePciDeviceSpecMsg msg = new APIUpdatePciDeviceSpecMsg();
        msg.setUuid(uuid(PciDeviceSpecVO.class));
        msg.setName("MSI_GTX1060");
        msg.setDescription("NVIDIA Corporation, GP106 [GeForce GTX 1060 6GB], a1, VGA compatible controller");
        msg.setRomContent("*BASE64 ENCODED ROM CONTENT*");
        msg.setRomVersion("86.06.0E.00.29");
        msg.setState(PciDeviceSpecState.Enabled.toString());
        return msg;
    }
}
