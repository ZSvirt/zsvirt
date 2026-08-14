package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api change backup storage state
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.backup.APIChangeBackupStorageStateMsg": {
 * "uuid": "531bfa9085e34f4ea301d9af19fb083c",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "c2f6c5e1d7944a0f8e0d681a262b85aa"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.backup.APIChangeBackupStorageStateMsg": {
 * "uuid": "531bfa9085e34f4ea301d9af19fb083c",
 * "stateEvent": "disable",
 * "session": {
 * "uuid": "c2f6c5e1d7944a0f8e0d681a262b85aa"
 * },
 * "timeout": 1800000,
 * "id": "10d57f259c86407f8cd6e0e8ccc4eca3",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIChangeBackupStorageStateEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/backup-storage/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeBackupStorageStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeBackupStorageStateMsg extends APIMessage implements BackupStorageMessage {
    /**
     * @desc backup storage uuid
     */
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;
    /**
     * @desc - enable: enable backup storage
     * - disable: disable backup storage
     * <p>
     * for details of backup storage state, see state of :ref:`BackupStorageInventory`
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public APIChangeBackupStorageStateMsg() {
    }

    public APIChangeBackupStorageStateMsg(String uuid, String stateEvent) {
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
    public String getBackupStorageUuid() {
        return getUuid();
    }
 
    public static APIChangeBackupStorageStateMsg __example__() {
        APIChangeBackupStorageStateMsg msg = new APIChangeBackupStorageStateMsg();

        msg.setUuid(uuid());
        msg.setStateEvent(BackupStorageStateEvent.disable.toString());

        return msg;
    }
}
