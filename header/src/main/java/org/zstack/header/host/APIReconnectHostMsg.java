package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api reestablish connection to hypervisor agent
 * @cli
 * @httpMsg {
 * "org.zstack.header.host.APIReconnectHostMsg": {
 * "session": {
 * "uuid": "7dd11952b3c94fd5bbe94a140d7fbac6"
 * },
 * "uuid": "5ea9605b1d754077b2c9dfca05fc904b"
 * }
 * }
 * @msg {
 * "org.zstack.header.host.APIReconnectHostMsg": {
 * "uuid": "5ea9605b1d754077b2c9dfca05fc904b",
 * "session": {
 * "uuid": "7dd11952b3c94fd5bbe94a140d7fbac6"
 * },
 * "timeout": 1800000,
 * "id": "f366ad3eff954f0fbf9e572ac462ff7b",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIReconnectHostEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/hosts/{uuid}/actions",
        responseClass = APIReconnectHostEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIReconnectHostMsg extends APIMessage implements HostMessage {
    /**
     * @desc host uuid
     */
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    public APIReconnectHostMsg() {
    }

    public APIReconnectHostMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }

 
    public static APIReconnectHostMsg __example__() {
        APIReconnectHostMsg msg = new APIReconnectHostMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
