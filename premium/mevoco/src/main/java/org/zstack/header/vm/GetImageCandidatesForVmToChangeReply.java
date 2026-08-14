package org.zstack.header.vm;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by GuoYi on 11/2/17.
 */
public class GetImageCandidatesForVmToChangeReply extends MessageReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }
}
