package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.APIUpdateSNSApplicationEndpointMsg;

@RestRequest(
        path = "/sns/application-endpoints/ding-talk/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSApplicationEndpointEvent.class,
        isAction = true
)
public class APIUpdateSNSDingTalkEndpointMsg extends APIUpdateSNSApplicationEndpointMsg {

    @APIParam(maxLength = 256, required = false)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;
    @APIParam(maxLength = 256, required = false)
    private String secret;

    public static APIUpdateSNSApplicationEndpointMsg __example__() {
        APIUpdateSNSDingTalkEndpointMsg msg = new APIUpdateSNSDingTalkEndpointMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUrl("https://open.ding-talk.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        msg.setAtAll(false);
        msg.setSecret("fiSmveXkeD2jIjrENHYjQd");
        return msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getAtAll() {
        return atAll;
    }

    public void setAtAll(Boolean atAll) {
        this.atAll = atAll;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
