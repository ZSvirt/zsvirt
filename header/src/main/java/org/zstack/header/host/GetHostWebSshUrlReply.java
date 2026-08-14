package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:35 PM
 */
public class GetHostWebSshUrlReply extends MessageReply {
    @NoLogging
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static GetHostWebSshUrlReply __example__() {
        GetHostWebSshUrlReply reply = new GetHostWebSshUrlReply();
        reply.setUrl("ws://{{ip}}:8888/ws?id=140147795208568");
        return reply;
    }
}
