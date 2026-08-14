package org.zstack.sns.platform.http;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationEndpointMsg;
import org.zstack.sns.SNSApplicationPlatformMessage;
import org.zstack.sns.SNSConstants;

@RestRequest(path = "/sns/application-endpoints/http", method = HttpMethod.POST,
        responseClass = APICreateSNSHttpEndpointEvent.class,
        parameterName = "params")
public class APICreateSNSHttpEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    @APIParam(maxLength = 2048)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    @APIParam(maxLength = 128, required = false)
    private String username;
    @APIParam(maxLength = 512, required = false, password = true)
    @NoLogging
    private String password;

    public static APICreateSNSHttpEndpointMsg __example__() {
        APICreateSNSHttpEndpointMsg msg = new APICreateSNSHttpEndpointMsg();
        msg.setName("http");
        msg.setUrl("http://127.0.0.1:6666/test-url");
        msg.setUsername("username");
        msg.setPassword("password");
        return msg;
    }

    @Override
    public String getPlatformUuid() {
        return SNSConstants.SYSTEM_PLATFORM_UUID;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return getPlatformUuid();
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

    @Override
    public String getApplicationEndpointType() {
        return SNSHttpEndpointFactory.type.toString();
    }
}
