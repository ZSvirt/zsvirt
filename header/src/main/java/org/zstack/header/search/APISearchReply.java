package org.zstack.header.search;

import org.zstack.header.message.APIReply;

public class APISearchReply extends APIReply {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
 
    public static APISearchReply __example__() {
        APISearchReply reply = new APISearchReply();


        return reply;
    }

}
