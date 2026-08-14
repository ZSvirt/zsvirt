package org.zstack.monitoring.media;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/7/7.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateEmailMediaEvent extends APIEvent {
    private EmailMediaInventory inventory;

    public APIUpdateEmailMediaEvent() {
    }

    public APIUpdateEmailMediaEvent(String apiId) {
        super(apiId);
    }

    public EmailMediaInventory getInventory() {
        return inventory;
    }

    public void setInventory(EmailMediaInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateEmailMediaEvent __example__() {
        APIUpdateEmailMediaEvent evt  = new APIUpdateEmailMediaEvent();
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
