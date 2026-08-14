package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2020/9/15.
 */
public class CreateTemporaryRootVolumeTemplateFromVolumeReply extends MessageReply {
    private ImageInventory inventory;
    private String locateHostUuid;
    private String locatePsUuid;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }

    public String getLocateHostUuid() {
        return locateHostUuid;
    }

    public void setLocateHostUuid(String locateHostUuid) {
        this.locateHostUuid = locateHostUuid;
    }

    public String getLocatePsUuid() {
        return locatePsUuid;
    }

    public void setLocatePsUuid(String locatePsUuid) {
        this.locatePsUuid = locatePsUuid;
    }
}
