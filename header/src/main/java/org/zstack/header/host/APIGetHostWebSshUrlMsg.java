package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author : jingwang
 * @create 2023/4/25 11:29 AM
 */
@RestRequest(
        path = "/hosts/webssh",
        method = HttpMethod.POST,
        responseClass = APIGetHostWebSshUrlEvent.class,
        parameterName = "params"
)
public class APIGetHostWebSshUrlMsg extends APIMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;

    @APIParam(required = false)
    private Boolean https = false;

    @APIParam(nonempty = true)
    private String userName;

    @APIParam(nonempty = true)
    @NoLogging
    private String password;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getHttps() {
        return https;
    }

    public void setHttps(Boolean https) {
        this.https = https;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIGetHostWebSshUrlMsg __example__() {
        APIGetHostWebSshUrlMsg msg = new APIGetHostWebSshUrlMsg();
        msg.setUuid(uuid());
        msg.setUserName("root");
        msg.setPassword("password");
        return msg;
    }
}
