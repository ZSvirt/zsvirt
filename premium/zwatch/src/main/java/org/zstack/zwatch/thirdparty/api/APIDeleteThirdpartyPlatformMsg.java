package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.msg.ThirdpartyPlatformMsg;

@RestRequest(
        path = "/zwatch/third-party/platforms/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteThirdpartyPlatformEvent.class
)
public class APIDeleteThirdpartyPlatformMsg extends APIDeleteMessage implements ThirdpartyPlatformMsg{
    @APIParam(resourceType = ThirdpartyPlatformVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteThirdpartyPlatformMsg __example__() {
        APIDeleteThirdpartyPlatformMsg msg = new APIDeleteThirdpartyPlatformMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getThirdpartyPlatformUuid() {
        return uuid;
    }
}
