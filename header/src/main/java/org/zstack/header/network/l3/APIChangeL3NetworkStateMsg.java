package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api change l3Network state
 * @category l3Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIChangeL3NetworkStateMsg": {
 * "uuid": "3424b7a643c348c795aaa8df59c5044f",
 * "stateEvent": "enable",
 * "session": {
 * "uuid": "6d0f7c288ae645458390b5f79ebbc012"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIChangeL3NetworkStateMsg": {
 * "uuid": "3424b7a643c348c795aaa8df59c5044f",
 * "stateEvent": "enable",
 * "session": {
 * "uuid": "6d0f7c288ae645458390b5f79ebbc012"
 * },
 * "timeout": 1800000,
 * "id": "551532e755484338a704bf5fba980a2d",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIChangeL3NetworkStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l3-networks/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeL3NetworkStateEvent.class,
        isAction = true
)
public class APIChangeL3NetworkStateMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String uuid;
    /**
     * @desc - enable: enable l3Network
     * - disable: disable l3Network
     * <p>
     * for details of state of l3Network, see state of :ref:`L3NetworkInventory`
     * @choices - enable
     * - disable
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

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
    public String getL3NetworkUuid() {
        return uuid;
    }
 
    public static APIChangeL3NetworkStateMsg __example__() {
        APIChangeL3NetworkStateMsg msg = new APIChangeL3NetworkStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(L3NetworkStateEvent.enable.toString());
        return msg;
    }
}
