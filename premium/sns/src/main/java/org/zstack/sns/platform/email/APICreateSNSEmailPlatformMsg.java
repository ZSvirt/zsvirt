package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationPlatformEvent;
import org.zstack.sns.APICreateSNSApplicationPlatformMsg;
import org.zstack.sns.SNSConstants;

@RestRequest(
        path = "/sns/application-platforms/email",
        method = HttpMethod.POST,
        responseClass = APICreateSNSApplicationPlatformEvent.class,
        parameterName = "params"
)
public class APICreateSNSEmailPlatformMsg extends APICreateSNSApplicationPlatformMsg {
    @APIParam
    private String smtpServer;
    @APIParam(numberRange = {1, 65535})
    private Integer smtpPort;
    @APIParam(required = false)
    private String username;
    @APIParam(required = false, password = true)
    @NoLogging
    private String password;
    @APIParam(required = false, validValues = {"SSL", "STARTTLS", "NONE"})
    private String encryptType = "NONE";

    public static APICreateSNSEmailPlatformMsg __example__() {
        APICreateSNSEmailPlatformMsg msg = new APICreateSNSEmailPlatformMsg();
        msg.setName("email platform");
        msg.setSmtpServer("email.zstack.io");
        msg.setSmtpPort(20);
        msg.setUsername("example@zstack.io");
        msg.setPassword("password");
        msg.setEncryptType("STARTTLS");
        return msg;
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
    public String getType() {
        return SNSConstants.EMAIL_PLATFORM;
    }

    public String getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(String encryptType) {
        this.encryptType = encryptType;
    }
}
