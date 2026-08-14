package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api change state of cluster. See field 'state' of :ref:`ClusterInventory` for details.
 * When changing cluster state, the states of descendant resources(hosts) are changed in cascade as well.
 * <p>
 * For example, putting a cluster into Disabled will change all hosts in this cluster into
 * Disabled state, however, you can enable a host without effecting cluster's state later.
 * Host state is not necessary to be the same with cluster state
 * @cli
 * @httpMsg {
 * "org.zstack.header.cluster.APIChangeClusterStateMsg": {
 * "session": {
 * "uuid": "056414ac9bac43998b974c1af1670bea"
 * },
 * "uuid": "44e981a73c7d414a995d5894b086670a",
 * "stateEvent": "enable"
 * }
 * }
 * @msg {
 * "org.zstack.header.cluster.APIChangeClusterStateMsg": {
 * "uuid": "44e981a73c7d414a995d5894b086670a",
 * "stateEvent": "enable",
 * "session": {
 * "uuid": "056414ac9bac43998b974c1af1670bea"
 * },
 * "timeout": 1800000,
 * "id": "1e886cf057944a54888d8f826edf2028",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIChangeClusterStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/clusters/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeClusterStateEvent.class
)
public class APIChangeClusterStateMsg extends APIMessage implements ClusterMessage {
    /**
     * @desc cluster uuid
     */
    @APIParam(resourceType = ClusterVO.class)
    private String uuid;
    /**
     * @desc - enable: enable cluster
     * - disable: disable cluster
     * <p>
     * see state in :ref:`ClusterInventory` for details
     * @choices - enable
     * - disable
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public APIChangeClusterStateMsg() {
    }

    public APIChangeClusterStateMsg(String uuid, String stateEvent) {
        this.uuid = uuid;
        this.stateEvent = stateEvent;
    }

    public void setUuid(String clusterUuid) {
        this.uuid = clusterUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getClusterUuid() {
        return getUuid();
    }
 
    public static APIChangeClusterStateMsg __example__() {
        APIChangeClusterStateMsg msg = new APIChangeClusterStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(ClusterStateEvent.disable.toString());
        return msg;
    }
}
