package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get supported backup storage types
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.backup.APIGetBackupStorageTypesMsg": {
 * "session": {
 * "uuid": "55c5261ec8544e79a8f2bab91c9d7f6b"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.backup.APIGetBackupStorageTypesMsg": {
 * "session": {
 * "uuid": "55c5261ec8544e79a8f2bab91c9d7f6b"
 * },
 * "timeout": 1800000,
 * "id": "6831fcd2c67c458c94882daa665e8917",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetBackupStorageTypesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/backup-storage/types",
        method = HttpMethod.GET,
        responseClass = APIGetBackupStorageTypesReply.class
)
public class APIGetBackupStorageTypesMsg extends APISyncCallMessage {
 
    public static APIGetBackupStorageTypesMsg __example__() {
        APIGetBackupStorageTypesMsg msg = new APIGetBackupStorageTypesMsg();


        return msg;
    }

}
