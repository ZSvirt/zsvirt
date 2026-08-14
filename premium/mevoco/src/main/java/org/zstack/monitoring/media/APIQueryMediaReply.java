package org.zstack.monitoring.media;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryMediaReply extends APIQueryReply {
    private List<MediaInventory> inventories;

    public List<MediaInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MediaInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMediaReply __example__() {
        APIQueryMediaReply reply = new APIQueryMediaReply();

        MediaInventory inv = new MediaInventory();
        inv.setName("notification");
        inv.setState(MediaState.Enabled.toString());
        inv.setType(MediaConstants.NOTIFICATION_TYPE);
        inv.setUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inv));
        return reply;
    }
}
