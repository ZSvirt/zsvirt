package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api delete a zone. All descendant resources, for example cluster/host/vm, are deleted in
 * cascade as well
 * @msg {
 * "org.zstack.header.zone.APIDeleteZoneMsg": {
 * "uuid": "e60128363bb244bf8de1f7ddadc9632f",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "timeout": 1800000,
 * "id": "b10bfe3e9dc4446b9ca3474a61942c76",
 * "serviceId": "api.portal"
 * }
 * }
 * @httpMsg {
 * "org.zstack.header.zone.APIDeleteZoneMsg": {
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "uuid": "e60128363bb244bf8de1f7ddadc9632f"
 * }
 * }
 * @cli
 * @result see :ref:`APIDeleteZoneEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/zones/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteZoneEvent.class
)
public class APIDeleteZoneMsg extends APIDeleteMessage implements ZoneMessage {
    /**
     * @desc zone uuid
     */
    @APIParam(resourceType = ZoneVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public APIDeleteZoneMsg() {
    }

    public APIDeleteZoneMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getZoneUuid() {
        return getUuid();
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }
 
    public static APIDeleteZoneMsg __example__() {
        APIDeleteZoneMsg msg = new APIDeleteZoneMsg();
        msg.setUuid(uuid());
        msg.setDeletionMode(DeletionMode.Permissive);
        return msg;
    }
}
