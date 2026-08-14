package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * delete eip
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.eip.APIDeleteEipMsg": {
"uuid": "7c1fa7260409432cb8d9880697a3407c",
"deleteMode": "Permissive",
"session": {
"uuid": "f0c39ad956124bf19994bd8fafaf9004"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.eip.APIDeleteEipMsg": {
"uuid": "7c1fa7260409432cb8d9880697a3407c",
"deleteMode": "Permissive",
"session": {
"uuid": "f0c39ad956124bf19994bd8fafaf9004"
},
"timeout": 1800000,
"id": "ff0e3313c2514fd09a1407202d7ac243",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachEipEvent`
 */
@RestRequest(
        path = "/eips/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteEipEvent.class
)
public class APIDeleteEipMsg extends APIDeleteMessage implements EipMessage {
    /**
     * @desc eip uuid
     */
    @APIParam(successIfResourceNotExisting = true, resourceType = EipVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getEipUuid() {
        return uuid;
    }
 
    public static APIDeleteEipMsg __example__() {
        APIDeleteEipMsg msg = new APIDeleteEipMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
