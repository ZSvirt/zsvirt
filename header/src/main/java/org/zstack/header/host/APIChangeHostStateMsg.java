package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api change host state. When host state is Disabled, no vm can be created on this host.
 * @cli
 * @httpMsg {
 * "org.zstack.header.host.APIChangeHostStateMsg": {
 * "session": {
 * "uuid": "7dd11952b3c94fd5bbe94a140d7fbac6"
 * },
 * "uuid": "5ea9605b1d754077b2c9dfca05fc904b",
 * "stateEvent": "disable"
 * }
 * }
 * @msg {
 * "org.zstack.header.host.APIChangeHostStateMsg": {
 * "uuid": "5ea9605b1d754077b2c9dfca05fc904b",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "7dd11952b3c94fd5bbe94a140d7fbac6"
 * },
 * "timeout": 1800000,
 * "id": "11eb8fea18584e17a75b2072827f7a39",
 * "serviceId": "host.e2c716e0f6bb4a7a99e4a60d10e57d4e"
 * }
 * }
 * @result see :ref:`APIChangeHostStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/hosts/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeHostStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeHostStateMsg extends APIMessage implements HostMessage {
    /**
     * @desc host uuid
     */
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    /**
     * @desc - enable: enable host
     * - disable: disable host
     * - maintain: putting host in to Maintenance
     * <p>
     * see state in :ref:`HostInventory` for details
     * @choices - enable
     * - disable
     * - maintain
     */
    @APIParam(validValues = {"enable", "disable", "maintain"})
    private String stateEvent;

    public APIChangeHostStateMsg() {
    }

    public APIChangeHostStateMsg(String uuid, String event) {
        this.uuid = uuid;
        this.stateEvent = event;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }
 
    public static APIChangeHostStateMsg __example__() {
        APIChangeHostStateMsg msg = new APIChangeHostStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(HostStateEvent.enable.toString());
        return msg;
    }
}
