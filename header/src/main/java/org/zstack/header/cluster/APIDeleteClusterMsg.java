package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api delete a cluster. All descendant resources, for example hosts/vm are deleted in cascade as well
 * @cli
 * @httpMsg {
 * "org.zstack.header.cluster.APIDeleteClusterMsg": {
 * "session": {
 * "uuid": "056414ac9bac43998b974c1af1670bea"
 * },
 * "uuid": "0f070d6a8c7748869edc3bb2bb538af2"
 * }
 * }
 * @msg {
 * "org.zstack.header.cluster.APIDeleteClusterMsg": {
 * "uuid": "0f070d6a8c7748869edc3bb2bb538af2",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "056414ac9bac43998b974c1af1670bea"
 * },
 * "timeout": 1800000,
 * "id": "1cd08e0b8da8415d91b3de68f3753035",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteClusterEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/clusters/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteClusterEvent.class
)
public class APIDeleteClusterMsg extends APIDeleteMessage implements ClusterMessage {
    /**
     * @desc cluster uuid
     */
    @APIParam(resourceType = ClusterVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteClusterMsg() {
    }

    public APIDeleteClusterMsg(String clusterUuid) {
        this.uuid = clusterUuid;
    }

    public void setUuid(String clusterUuid) {
        this.uuid = clusterUuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getClusterUuid() {
        return getUuid();
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }
 
    public static APIDeleteClusterMsg __example__() {
        APIDeleteClusterMsg msg = new APIDeleteClusterMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
