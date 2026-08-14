package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api delete backup storage
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.backup.APIDeleteBackupStorageMsg": {
 * "uuid": "e2560492c4ff4224916ce9168c2ea522",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "4f6bef4ee7ac42d7ad27c07b0918a3b9"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.backup.APIDeleteBackupStorageMsg": {
 * "uuid": "e2560492c4ff4224916ce9168c2ea522",
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "4f6bef4ee7ac42d7ad27c07b0918a3b9"
 * },
 * "timeout": 1800000,
 * "id": "1adb6f97b32d46d28e1ad07e69bbe04a",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIDeleteBackupStorageEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/backup-storage/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteBackupStorageEvent.class
)
public class APIDeleteBackupStorageMsg extends APIDeleteMessage implements BackupStorageMessage {
    /**
     * @desc backup storage uuid
     */
    @APIParam
    private String uuid;

    public APIDeleteBackupStorageMsg() {
    }

    public APIDeleteBackupStorageMsg(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return getUuid();
    }
 
    public static APIDeleteBackupStorageMsg __example__() {
        APIDeleteBackupStorageMsg msg = new APIDeleteBackupStorageMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
