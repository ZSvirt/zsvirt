package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestRequest(
        path = "/images/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetImageQgaEvent.class
)
public class APISetImageQgaMsg extends APIMessage {
    @APIParam(resourceType = ImageVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String uuid;

    @APIParam
    private boolean enable;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public static APISetImageQgaMsg __example__() {
        APISetImageQgaMsg msg = new APISetImageQgaMsg();
        msg.setUuid(uuid());
        msg.setEnable(true);
        return msg;
    }

}
