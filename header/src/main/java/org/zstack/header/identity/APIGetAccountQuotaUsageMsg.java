package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 2/22/2016.
 */
@RestRequest(
        path = "/accounts/quota/{uuid}/usages",
        method = HttpMethod.GET,
        responseClass = APIGetAccountQuotaUsageReply.class
)
public class APIGetAccountQuotaUsageMsg extends APISyncCallMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class)
    private String uuid;

    @Override
    public String getAccountUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIGetAccountQuotaUsageMsg __example__() {
        APIGetAccountQuotaUsageMsg msg = new APIGetAccountQuotaUsageMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
