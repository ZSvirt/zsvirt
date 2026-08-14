package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get supported primary storage type
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.primary.APIGetPrimaryStorageTypesMsg": {
 * "session": {
 * "uuid": "a096426cb6c64ede865cf9577f745906"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.primary.APIGetPrimaryStorageTypesMsg": {
 * "session": {
 * "uuid": "a096426cb6c64ede865cf9577f745906"
 * },
 * "timeout": 1800000,
 * "id": "7ff3b617bd534634937beb8763d2ed92",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetPrimaryStorageTypesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/primary-storage/types",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageTypesReply.class
)
public class APIGetPrimaryStorageTypesMsg extends APISyncCallMessage {
 
    public static APIGetPrimaryStorageTypesMsg __example__() {
        APIGetPrimaryStorageTypesMsg msg = new APIGetPrimaryStorageTypesMsg();

        return msg;
    }

}
