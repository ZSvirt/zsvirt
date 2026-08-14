package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author : jingwang
 * @create 2023/4/14 5:28 PM
 */
@RestRequest(
        path = "/hosts/ipmi/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostIpmiEvent.class,
        isAction = true
)
public class APIUpdateHostIpmiMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostEO.class)
    private String uuid;
    @APIParam(required = false, nonempty = true)
    private String ipmiAddress;
    @APIParam(required = false, nonempty = true)
    private String ipmiUsername;
    @APIParam(required = false, nonempty = true)
    @NoLogging
    private String ipmiPassword;
    @APIParam(required = false, nonempty = true)
    private int ipmiPort = 623;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
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

    public int getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(int ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public static APIUpdateHostIpmiMsg __example__() {
        APIUpdateHostIpmiMsg msg = new APIUpdateHostIpmiMsg();
        msg.setUuid(uuid());
        msg.setIpmiAddress("127.0.0.10");
        msg.setIpmiPort(623);
        msg.setIpmiUsername("admin");
        msg.setIpmiPassword("password");
        return msg;
    }
}
