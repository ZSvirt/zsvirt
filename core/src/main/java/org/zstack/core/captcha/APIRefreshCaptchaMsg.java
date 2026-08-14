package org.zstack.core.captcha;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.captcha.CaptchaVO;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kayo on 2018/7/6.
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/captcha/refresh",
        method = HttpMethod.GET,
        responseClass = APIRefreshCaptchaReply.class
)
public class APIRefreshCaptchaMsg extends APISyncCallMessage {
    @APIParam(resourceType = CaptchaVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRefreshCaptchaMsg __example__() {
        APIRefreshCaptchaMsg msg = new APIRefreshCaptchaMsg();
        msg.setUuid(uuid(CaptchaVO.class));
        return msg;
    }
}
