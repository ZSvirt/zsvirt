package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by GuoYi on 4/26/17.
 */
@TagResourceType(BaremetalChassisVO.class)
@RestRequest(
        path = "/baremetal/chassis",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateBaremetalChassisEvent.class
)
public class APICreateBaremetalChassisMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, emptyString = false)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;
    @APIParam
    private String ipmiAddress;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer ipmiPort = 623;
    @APIParam(maxLength = 255)
    private String ipmiUsername;
    @APIParam(maxLength = 255)
    @NoLogging
    private String ipmiPassword;

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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
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

    public static APICreateBaremetalChassisMsg __example__() {
        APICreateBaremetalChassisMsg msg = new APICreateBaremetalChassisMsg();
        msg.setName("test");
        msg.setClusterUuid(uuid());
        msg.setIpmiAddress("1.1.1.1");
        msg.setIpmiUsername("root");
        msg.setIpmiPassword("password");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateBaremetalChassisEvent)rsp).getInventory().getUuid() : "", BaremetalChassisVO.class);
    }
}
