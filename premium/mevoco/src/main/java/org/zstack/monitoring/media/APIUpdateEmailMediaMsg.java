package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.io.Serializable;

/**
 * Created by xing5 on 2017/7/7.
 */
@RestRequest(
        path = "/media/emails/{uuid}",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateEmailMediaEvent.class
)
@Deprecated
public class APIUpdateEmailMediaMsg extends APIMessage implements MediaMessage, Serializable {
    @APIParam(resourceType = EmailMediaVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false)
    private String smtpServer;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer smtpPort;
    @APIParam(required = false)
    private String username;
    @APIParam(required = false, password = true)
    @NoLogging
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    @Override
    public String getMediaMessageUuid() {
        return uuid;
    }

    public static APIUpdateEmailMediaMsg __example__() {
        APIUpdateEmailMediaMsg msg = new APIUpdateEmailMediaMsg();
        msg.uuid = uuid();
        msg.smtpServer = "test.stmp.com";
        msg.smtpPort = 25;
        msg.username = "test";
        msg.password = "password";
        return msg;
    }
}
