package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

@TagResourceType(ClusterVO.class)
@RestRequest(
        path = "/mini-clusters",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APICreateMiniClusterEvent.class
)
public class APICreateMiniClusterMsg extends APICreateMessage implements APIAuditor {
    /**
     * @desc uuid of zone this cluster is going to create in
     */
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    /**
     * @desc zone name, max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;

    /**
     * @desc IPv4 address or DNS name of management NICs
     */
    @APIParam(nonempty = true)
    private List<String> hostManagementIps;

    /**
     * @desc user name used for ssh login.
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255, required = false)
    private String username = "root";

    /**
     * @desc password for ssh login
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;

    /**
     * @desc ssh port for login
     * port range (1,65535)
     */
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    /**
     * @desc see field 'hypervisorType' of :ref:`ClusterInventory` for details
     * @choices - KVM
     * - Simulator
     */
    @APIParam(validValues = {"KVM", "Simulator"})
    private String hypervisorType;

    public APICreateMiniClusterMsg() {
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getHostManagementIps() {
        return hostManagementIps;
    }

    public void setHostManagementIps(List<String> hostManagementIps) {
        this.hostManagementIps = hostManagementIps;
    }

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

    public static APICreateMiniClusterMsg __example__() {
        APICreateMiniClusterMsg msg = new APICreateMiniClusterMsg();
        msg.setName("cluster1");
        msg.setDescription("test");
        msg.setHypervisorType("KVM");
        msg.setZoneUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";

        if (rsp.isSuccess()) {
            APICreateMiniClusterEvent evt = (APICreateMiniClusterEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }

        return new Result(resUuid, ClusterVO.class);
    }
}
