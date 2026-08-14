package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/host/get-block-devices",
        method = HttpMethod.GET,
        responseClass = APIGetPhysicalMachineBlockDevicesReply.class
)
    public class APIGetPhysicalMachineBlockDevicesMsg extends APISyncCallMessage {
    @APIParam(maxLength = 255)
    private String username;
    @APIParam(required = false, maxLength = 255)
    @NoLogging
    private String password;
    @APIParam(numberRange = {1, 65535})
    private Integer sshPort;
    @APIParam(maxLength = 255)
    private String hostName;
    @APIParam(required = false)
    List<String> excludedTypes;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(List<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    public static APIGetPhysicalMachineBlockDevicesMsg __example__() {
        APIGetPhysicalMachineBlockDevicesMsg msg = new APIGetPhysicalMachineBlockDevicesMsg();
        msg.setUsername("username");
        msg.setPassword("password");
        msg.setSshPort(22);
        msg.setHostName("192.168.1.1");
        return msg;
    }
}