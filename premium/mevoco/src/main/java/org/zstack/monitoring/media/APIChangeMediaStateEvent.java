package org.zstack.monitoring.media;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.awt.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse(allTo = "inventory")
public class APIChangeMediaStateEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private MediaInventory inventory;

    public APIChangeMediaStateEvent() {
    }

    public APIChangeMediaStateEvent(String apiId) {
        super(apiId);
    }

    public MediaInventory getInventory() {
        return inventory;
    }

    public void setInventory(MediaInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeMediaStateEvent __example__() {
        APIChangeMediaStateEvent evt = new APIChangeMediaStateEvent();

        MediaInventory inv = new MediaInventory();
        inv.setName("notification");
        inv.setState(MediaState.Enabled.toString());
        inv.setType(MediaConstants.NOTIFICATION_TYPE);
        inv.setUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(inv);

        return evt;
    }
}
