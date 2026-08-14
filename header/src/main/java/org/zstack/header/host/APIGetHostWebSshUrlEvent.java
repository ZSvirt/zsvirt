package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:33 PM
 */
@RestResponse(fieldsTo = "all")
public class APIGetHostWebSshUrlEvent extends APIEvent {
    public APIGetHostWebSshUrlEvent() {
    }

    public APIGetHostWebSshUrlEvent(String apiId) {
        super(apiId);
    }

    @NoLogging
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static APIGetHostWebSshUrlEvent __example__() {
        APIGetHostWebSshUrlEvent event = new APIGetHostWebSshUrlEvent();
        event.setUrl("ws://{{ip}}:8888/ws?id=140147795208568");
        return event;
    }
}
