package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api attach primary storage to a cluster
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.primary.APIAttachPrimaryStorageMsg": {
 * "clusterUuid": "15ae59ce1b25420696636d8ea05219ce",
 * "primaryStorageUuid": "d654fecb02ab4e6db13d681baccdd9b4",
 * "session": {
 * "uuid": "5f9bf4b3af1b46bbacfafc754e9a3e81"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.primary.APIAttachPrimaryStorageMsg": {
 * "clusterUuid": "15ae59ce1b25420696636d8ea05219ce",
 * "primaryStorageUuid": "d654fecb02ab4e6db13d681baccdd9b4",
 * "session": {
 * "uuid": "5f9bf4b3af1b46bbacfafc754e9a3e81"
 * },
 * "id": "fc55eb9b37df40d3b9bd63963623313f",
 * "timeout": 1800000,
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAttachPrimaryStorageEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachPrimaryStorageToClusterEvent.class
)
public class APIAttachPrimaryStorageToClusterMsg extends APIMessage implements PrimaryStorageMessage {
    /**
     * @desc uuid of cluster this primary storage is attaching to
     */
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;
    /**
     * @desc primary storage uuid
     */
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    public APIAttachPrimaryStorageToClusterMsg() {
    }

    public APIAttachPrimaryStorageToClusterMsg(String clusterUuid, String uuid) {
        super();
        this.clusterUuid = clusterUuid;
        this.primaryStorageUuid = uuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

 
    public static APIAttachPrimaryStorageToClusterMsg __example__() {
        APIAttachPrimaryStorageToClusterMsg msg = new APIAttachPrimaryStorageToClusterMsg();

        msg.setPrimaryStorageUuid(uuid());
        msg.setClusterUuid(uuid());

        return msg;
    }
}
