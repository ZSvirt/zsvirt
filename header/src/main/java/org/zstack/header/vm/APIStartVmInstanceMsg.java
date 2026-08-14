package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api start a vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIStartVmInstanceMsg": {
 * "uuid": "a57042713a3d4cdbb99c7c98b6628da8",
 * "session": {
 * "uuid": "9f08a11275d647b3a96c1c9766d8a4a4"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIStartVmInstanceMsg": {
 * "uuid": "a57042713a3d4cdbb99c7c98b6628da8",
 * "session": {
 * "uuid": "9f08a11275d647b3a96c1c9766d8a4a4"
 * },
 * "timeout": 1800000,
 * "id": "c685c27b821649d29added08e7fecdc2",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIStartVmInstanceEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIStartVmInstanceEvent.class,
        isAction = true
)
@SkipVmTracer(replyClass = APIStartVmInstanceEvent.class)
public class APIStartVmInstanceMsg extends APIMessage implements VmInstanceMessage, CheckAttachedVolumesMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(resourceType = ClusterVO.class, required = false)
    private String clusterUuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APIStartVmInstanceMsg __example__() {
        APIStartVmInstanceMsg msg = new APIStartVmInstanceMsg();
        msg.uuid = uuid();
        return msg;
    }
}
