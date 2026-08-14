package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get allocation strategy of primary storage
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesMsg": {
 * "session": {
 * "uuid": "b58fd452adeb4a4daefe02f8522af196"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesMsg": {
 * "session": {
 * "uuid": "b58fd452adeb4a4daefe02f8522af196"
 * },
 * "timeout": 1800000,
 * "id": "7d7685e74c5949d3afb955ec217da9fb",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetPrimaryStorageAllocatorStrategiesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/primary-storage/allocators/strategies",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageAllocatorStrategiesReply.class
)
public class APIGetPrimaryStorageAllocatorStrategiesMsg extends APISyncCallMessage {
 
    public static APIGetPrimaryStorageAllocatorStrategiesMsg __example__() {
        APIGetPrimaryStorageAllocatorStrategiesMsg msg = new APIGetPrimaryStorageAllocatorStrategiesMsg();

        return msg;
    }

}
