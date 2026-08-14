package org.zstack.sns.platform.http;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent;
import org.zstack.sns.APIUpdateSNSApplicationEndpointMsg;

@RestRequest(
        path = "/sns/application-endpoints/http/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSApplicationEndpointEvent.class,
        isAction = true
)
public class APIUpdateSNSHttpEndpointMsg extends APIUpdateSNSApplicationEndpointMsg {

    @APIParam(maxLength = 256, required = false)
    private String url;

    @APIParam(maxLength = 256, required = false)
    private String username;

    @APIParam(maxLength = 256, required = false)
    private String password;

    public static APIUpdateSNSApplicationEndpointMsg __example__() {
        APIUpdateSNSHttpEndpointMsg msg = new APIUpdateSNSHttpEndpointMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUrl("https://open.http.cn/open-apis/bot/v2/hook/006879a3-0898-4428-aad4-3221db3daf81");
        msg.setUsername("admin");
        msg.setPassword("password");
        return msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
