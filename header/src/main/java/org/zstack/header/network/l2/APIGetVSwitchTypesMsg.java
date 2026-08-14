package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @api get supported vSwitch types
 * @category l2network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APIGetVSwitchTypesMsg": {
 * "session": {
 * "uuid": "7d2f89d74f6970368289d398048b8df5"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APIGetVSwitchTypesMsg": {
 * "session": {
 * "uuid": "7d2f89d74f6970368289d398048b8df5"
 * },
 * "timeout": 1800000,
 * "id": "48871dd685d2429f9bbc109b05342622",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetVSwitchTypesReply`
 * @since 4.1.0
 */

@RestRequest(
        path = "/l2-networks/vSwitchTypes",
        method = HttpMethod.GET,
        responseClass = APIGetVSwitchTypesReply.class
)
public class APIGetVSwitchTypesMsg extends APISyncCallMessage {

    public static APIGetVSwitchTypesMsg __example__() {
        APIGetVSwitchTypesMsg msg = new APIGetVSwitchTypesMsg();

        return msg;
    }
}
