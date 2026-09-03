package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


/**
 * @api change state of zone. For details, see field 'state' of :ref:`ZoneInventory`
 * When changing zone state, the states of descendant resources(clusters/hosts)
 * are changed in cascade as well.
 * <p>
 * For example, putting a zone into Disabled will change all clusters in this zone into
 * Disabled state, however, you can enable a cluster without effecting zone's state later.
 * Cluster/Host state are not necessary to be the same with zone state
 * @httpMsg {
 * "org.zstack.header.zone.APIChangeZoneStateMsg": {
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "uuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "stateEvent": "disable"
 * }
 * }
 * @msg {
 * "org.zstack.header.zone.APIChangeZoneStateMsg": {
 * "uuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "timeout": 1800000,
 * "id": "502f0a7d586a48b28653f21d0877243a",
 * "serviceId": "ApiMediator"
 * }
 * }
 * @cli
 * @result see :ref:`APIChangeZoneStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/zones/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeZoneStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeZoneStateMsg extends APIMessage implements ZoneMessage {
    /**
     * @desc zone uuid
     */
    @APIParam(resourceType = ZoneVO.class)
    private String uuid;
    /**
     * @desc - enable zone
     * - disable zone
     * <p>
     * see state in :ref:`ZoneInventory` for details
     * @choices - enable
     * - disable
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public APIChangeZoneStateMsg() {
    }

    public APIChangeZoneStateMsg(String uuid, String stateEvent) {
        super();
        this.uuid = uuid;
        this.stateEvent = stateEvent;
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
    public String getZoneUuid() {
        return getUuid();
    }

    public static APIChangeZoneStateMsg __example__() {
        APIChangeZoneStateMsg msg = new APIChangeZoneStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(ZoneStateEvent.enable.toString());
        return msg;
    }
}
