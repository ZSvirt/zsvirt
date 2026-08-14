package org.zstack.xdragon;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.host.HostVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.io.Serializable;

@TagResourceType(HostVO.class)
@RestRequest(
        path = "/hosts/xdragon",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddHostEvent.class
)
public class APIAddXDragonHostMsg extends APIAddHostMsg implements AddXDragonHostMessage, Serializable {
    /**
     * @desc user name used for ssh login.
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String username;

    /**
     * @desc password for ssh login
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;

    @APIParam(required = false, numberRange = {64, 256})
    private Integer cpuNum;

    @APIParam(required = false, numberRange = {1, 32})
    private Integer cpuSockets;

    @APIParam(required = false, numberRange = {137438953472L, Long.MAX_VALUE})
    private Long totalPhysicalMemory;

    /**
     * @desc ssh port for login
     * port range (1,65535)
     */
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

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

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public Long getTotalPhysicalMemory() {
        return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(Long totalPhysicalMemory) {
        this.totalPhysicalMemory = totalPhysicalMemory;
    }

    public static APIAddXDragonHostMsg __example__() {
        APIAddXDragonHostMsg msg = new APIAddXDragonHostMsg();
        msg.setUsername("userName");
        msg.setPassword("password");
        msg.setSshPort(22);
        msg.setClusterUuid(uuid());
        msg.setName("newHost");
        msg.setCpuNum(64);
        msg.setCpuSockets(4);
        msg.setTotalPhysicalMemory(128 * 1024 * 1024 * 1024L);
        msg.setManagementIp("127.0.0.1");
        return msg;
    }

}
