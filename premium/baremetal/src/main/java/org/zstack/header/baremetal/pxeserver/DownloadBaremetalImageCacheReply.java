package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.baremetal.instance.BaremetalImageCacheInventory;
import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 7/20/18.
 */
public class DownloadBaremetalImageCacheReply extends MessageReply {
    private BaremetalImageCacheInventory inventory;

    public BaremetalImageCacheInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalImageCacheInventory inventory) {
        this.inventory = inventory;
    }
}
