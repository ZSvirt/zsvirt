package org.zstack.sso.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

/**
 * @Author: DaoDao
 * @Date: 2022/8/30
 */
@RestRequest(
        path = "/delete/sso/client",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIDeleteSSOClientEvent.class
)
public class APIDeleteSSOClientMsg extends APIDeleteMessage {
    @APIParam(resourceType = ThirdPartyAccountSourceVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteSSOClientMsg __example__() {
        APIDeleteSSOClientMsg msg = new APIDeleteSSOClientMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
