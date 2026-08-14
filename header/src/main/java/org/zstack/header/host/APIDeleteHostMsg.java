package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.header.message.APIDeleteMessage.DeletionMode.Permissive;
import static org.zstack.utils.CollectionDSL.list;


/**
 * @api delete host, vm Running on host will be stopped
 * @cli
 * @httpMsg {
 * "org.zstack.header.host.APIDeleteHostMsg": {
 * "session": {
 * "uuid": "183e2551a3a545d58fbc171af798850c"
 * },
 * "uuid": "87435059abdb4f068f77cab193804292"
 * }
 * }
 * @msg {
 * "org.zstack.header.host.APIDeleteHostMsg": {
 * "uuid": "87435059abdb4f068f77cab193804292",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "183e2551a3a545d58fbc171af798850c"
 * },
 * "timeout": 1800000,
 * "id": "e935dfe78e2b427d8b0ea77bde303bfc",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteHostEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/hosts/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteHostEvent.class
)
public class APIDeleteHostMsg extends APIDeleteMessage implements HostMessage {
    /**
     * @desc host uuid
     */
    @APIParam
    private String uuid;

    public APIDeleteHostMsg() {
    }

    public APIDeleteHostMsg(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }
 
    public static APIDeleteHostMsg __example__() {
        APIDeleteHostMsg msg = new APIDeleteHostMsg();
        msg.setUuid(uuid());
        msg.setDeletionMode(Permissive);
        return msg;
    }
}
