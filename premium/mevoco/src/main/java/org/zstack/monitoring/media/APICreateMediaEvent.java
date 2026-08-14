package org.zstack.monitoring.media;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse(allTo = "inventory")
public class APICreateMediaEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private MediaInventory inventory;

    public APICreateMediaEvent() {
    }

    public APICreateMediaEvent(String apiId) {
        super(apiId);
    }

    public MediaInventory getInventory() {
        return inventory;
    }

    public void setInventory(MediaInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateMediaEvent __example__() {
        APICreateMediaEvent evt = new APICreateMediaEvent();

        EmailMediaInventory inv = new EmailMediaInventory();
        inv.setName("email");
        inv.setState(MediaState.Enabled.toString());
        inv.setType(MediaConstants.EMAIL_MEDIA_TYPE);
        inv.setUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setSmtpPort(25);
        inv.setSmtpServer("test.smtp.com");
        inv.setUsername("test");
        inv.setPassword("password");
        evt.setInventory(inv);

        return evt;
    }
}
