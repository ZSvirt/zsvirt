package org.zstack.iam1.api.ensemble;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.concurrent.TimeUnit;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
@RestRequest(
        path = "/iam1/resource-ensemble/view-sharing",
        method = HttpMethod.GET,
        responseClass = APIGetResourceSharingReply.class
)
@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 5)
public class APIGetResourceSharingMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceVO.class, scope = APIParam.SCOPE_ALLOWED_SHARING)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetResourceSharingMsg __example__() {
        APIGetResourceSharingMsg msg = new APIGetResourceSharingMsg();
        msg.setUuid(uuid(ResourceVO.class));
        return msg;
    }
}
