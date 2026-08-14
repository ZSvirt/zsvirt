package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * @api
 * delete security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupMsg": {
"uuid": "57e6730f5afa4f78ad66d2a289a91287",
"deleteMode": "Permissive",
"session": {
"uuid": "a27df307deaf44a59586707384170b0b"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIDeleteSecurityGroupMsg": {
"uuid": "57e6730f5afa4f78ad66d2a289a91287",
"deleteMode": "Permissive",
"session": {
"uuid": "a27df307deaf44a59586707384170b0b"
},
"timeout": 1800000,
"id": "90644afefacb4d9abbf60ddf766973b0",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDeleteSecurityGroupEvent`
 */
@RestRequest(
        path = "/security-groups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSecurityGroupEvent.class
)
public class APIDeleteSecurityGroupMsg extends APIDeleteMessage implements SecurityGroupMessage {
    /**
     * @desc security group uuid
     */
    @APIParam(resourceType = SecurityGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String securityGroupUuid) {
        this.uuid = securityGroupUuid;
    }

    @Override
    public String getSecurityGroupUuid() {
        return uuid;
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }
 
    public static APIDeleteSecurityGroupMsg __example__() {
        APIDeleteSecurityGroupMsg msg = new APIDeleteSecurityGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
