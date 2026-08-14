package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * @cli
 * @httpMsg {
 * "org.zstack.header.host.APIGetHypervisorTypesMsg": {
 * "session": {
 * "uuid": "c58ec5b783ea458a8c2234c5130b7299"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.host.APIGetHypervisorTypesMsg": {
 * "session": {
 * "uuid": "c58ec5b783ea458a8c2234c5130b7299"
 * },
 * "timeout": 1800000,
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetHypervisorTypesReply`
 * @since 0.1.0
 */
@RestRequest(
        path = "/hosts/hypervisor-types",
        method = HttpMethod.GET,
        responseClass = APIGetHypervisorTypesReply.class
)
public class APIGetHypervisorTypesMsg extends APISyncCallMessage {
 
    public static APIGetHypervisorTypesMsg __example__() {
        APIGetHypervisorTypesMsg msg = new APIGetHypervisorTypesMsg();


        return msg;
    }

}
