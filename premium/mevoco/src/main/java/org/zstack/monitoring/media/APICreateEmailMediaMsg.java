package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestRequest(
        path = "/media/emails",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateMediaEvent.class
)
@Deprecated
public class APICreateEmailMediaMsg extends APICreateMediaMsg {
    @APIParam
    private String smtpServer;
    @APIParam(numberRange = {1, 65535})
    private Integer smtpPort;
    @APIParam(required = false)
    private String username;
    @APIParam(required = false, password = true)
    @NoLogging
    private String password;

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
        return MediaConstants.EMAIL_MEDIA_TYPE;
    }

    public static APICreateEmailMediaMsg __example__() {
        APICreateEmailMediaMsg msg = new APICreateEmailMediaMsg();
        msg.setName("email");
        msg.setSmtpPort(25);
        msg.setSmtpServer("smtp.xxx.com");
        msg.setUsername("sender@xxx.com");
        msg.setPassword("password");
        return msg;
    }
}
