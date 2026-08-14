package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationPlatformTestConnectionMessage;
import org.zstack.sns.SNSApplicationPlatformType;

@RestRequest(path = "/sns/application-platforms/email/{uuid}/actions", method = HttpMethod.PUT, responseClass = APIValidateSNSEmailPlatformEvent.class, isAction = true)
public class APIValidateSNSEmailPlatformMsg extends APIMessage implements SNSApplicationPlatformTestConnectionMessage {
    @APIParam(required = false)
    private String uuid;
    @APIParam(required = false)
    private String smtpServer;
    @APIParam(required = false, numberRange = {1, 65535})
    private Integer smtpPort;
    @APIParam(required = false)
    private String username;
    @APIParam(required = false)
    @NoLogging
    private String password;

    public static APIValidateSNSEmailPlatformMsg __example__() {
        APIValidateSNSEmailPlatformMsg msg = new APIValidateSNSEmailPlatformMsg();
        msg.setUuid(uuid());
        msg.setSmtpServer("email.zstack.io");
        msg.setSmtpPort(20);
        msg.setUsername("example@zstack.io");
        msg.setPassword("password");
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
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
    public SNSApplicationPlatformType getPlatformType() {
        return SNSEmailPlatformFactory.type;
    }
}
