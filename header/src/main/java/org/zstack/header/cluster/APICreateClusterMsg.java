package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

/**
 * @api create a new cluster in zone
 * @cli
 * @httpMsg {
 * "org.zstack.header.cluster.APICreateClusterMsg": {
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "description": "test",
 * "name": "cluster1",
 * "hypervisorType": "KVM"
 * }
 * }
 * @msg {
 * "org.zstack.header.cluster.APICreateClusterMsg": {
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "name": "cluster1",
 * "description": "test",
 * "hypervisorType": "KVM",
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "timeout": 1800000,
 * "id": "0609791c70954aaf9b0256fb27aeaadc",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateClusterEvent`
 * @since 0.1.0
 */
@TagResourceType(ClusterVO.class)
@RestRequest(
        path = "/clusters",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APICreateClusterEvent.class
)
public class APICreateClusterMsg extends APICreateMessage implements CreateClusterMessage, APIAuditor {
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
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc see field 'hypervisorType' of :ref:`ClusterInventory` for details
     * @choices - KVM
     * - Simulator
     * - baremetal
     */
    @APIParam(validValues = {"KVM", "Simulator", "baremetal", "baremetal2", "xdragon"})
    private String hypervisorType;
    /**
     * @desc see field 'type' of :ref:`ClusterInventory` for details
     * @choices zstack
     * - baremetal
     */
    @APIParam(required = false, validValues = {"zstack", "baremetal", "baremetal2"})
    private String type;

    @APIParam(required = false, validValues = {"x86_64", "aarch64", "mips64el", "loongarch64"})
    private String architecture;

    public APICreateClusterMsg() {
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterName() {
        return name;
    }

    public void setClusterName(String clusterName) {
        this.name = clusterName;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public static APICreateClusterMsg __example__() {
        APICreateClusterMsg msg = new APICreateClusterMsg();
        msg.setClusterName("cluster1");
        msg.setDescription("test");
        msg.setHypervisorType("KVM");
        msg.setZoneUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";
        if (rsp.isSuccess()) {
            APICreateClusterEvent evt = (APICreateClusterEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }
        return new Result(resUuid, ClusterVO.class);
    }
}
