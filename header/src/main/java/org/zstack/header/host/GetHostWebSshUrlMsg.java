package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:26 PM
 */
public class GetHostWebSshUrlMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;

    private Boolean https;

    private String userName;

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

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
