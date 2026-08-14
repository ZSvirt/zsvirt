package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.APIUpdateSNSApplicationEndpointMsg;

@RestRequest(
        path = "/sns/application-endpoints/we-com/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSApplicationEndpointEvent.class,
        isAction = true
)
public class APIUpdateSNSWeComEndpointMsg extends APIUpdateSNSApplicationEndpointMsg {

    @APIParam(maxLength = 256, required = false)
    private String url;
    @APIParam(required = false)
    private Boolean atAll;

    public static APIUpdateSNSApplicationEndpointMsg __example__() {
        APIUpdateSNSWeComEndpointMsg msg = new APIUpdateSNSWeComEndpointMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUrl("https://we-com.com");
        msg.setAtAll(false);
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
}
