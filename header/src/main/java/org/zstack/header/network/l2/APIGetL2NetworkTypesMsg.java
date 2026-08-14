package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get supported l2Network types
 * @category l2network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APIGetL2NetworkTypesMsg": {
 * "session": {
 * "uuid": "7d2f89d74f6940368289d598048b8df4"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APIGetL2NetworkTypesMsg": {
 * "session": {
 * "uuid": "7d2f89d74f6940368289d598048b8df4"
 * },
 * "timeout": 1800000,
 * "id": "48871dd685d2429f9b1bc09b05342622",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetL2NetworkTypesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l2-networks/types",
        method = HttpMethod.GET,
        responseClass = APIGetL2NetworkTypesReply.class
)
public class APIGetL2NetworkTypesMsg extends APISyncCallMessage {
 
    public static APIGetL2NetworkTypesMsg __example__() {
        APIGetL2NetworkTypesMsg msg = new APIGetL2NetworkTypesMsg();


        return msg;
    }

}
