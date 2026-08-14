package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get supported network service types
 * @category network service
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.service.APIGetNetworkServiceTypesMsg": {
 * "session": {
 * "uuid": "acc010d17fa64ab0bf86fce209b67753"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.service.APIGetNetworkServiceTypesMsg": {
 * "session": {
 * "uuid": "acc010d17fa64ab0bf86fce209b67753"
 * },
 * "timeout": 1800000,
 * "id": "1819bb8648264fe79a03a6953108ae6e",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetNetworkServiceTypesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/network-services/types",
        method = HttpMethod.GET,
        responseClass = APIGetNetworkServiceTypesReply.class
)
public class APIGetNetworkServiceTypesMsg extends APISyncCallMessage {
 
    public static APIGetNetworkServiceTypesMsg __example__() {
        APIGetNetworkServiceTypesMsg msg = new APIGetNetworkServiceTypesMsg();

        return msg;
    }

}
