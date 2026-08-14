package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestRequest(
        path = "/baremetal/chassis/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateBaremetalChassisEvent.class
)
public class APIUpdateBaremetalChassisMsg extends APIMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String uuid;
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(required = false)
    private String ipmiAddress;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer ipmiPort;
    @APIParam(maxLength = 255, required = false)
    private String ipmiUsername;
    @APIParam(maxLength = 255, required = false)
    @NoLogging
    private String ipmiPassword;

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

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public Integer getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(Integer ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    public void setIpmiUsername(String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    public void setIpmiPassword(String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }

    public static APIUpdateBaremetalChassisMsg __example__() {
        APIUpdateBaremetalChassisMsg msg = new APIUpdateBaremetalChassisMsg();
        msg.setUuid(uuid());
        msg.setName("test");
        msg.setIpmiAddress("1.1.1.1");
        msg.setIpmiPort(623);
        msg.setIpmiUsername("root");
        msg.setIpmiPassword("password");
        return msg;
    }
}
