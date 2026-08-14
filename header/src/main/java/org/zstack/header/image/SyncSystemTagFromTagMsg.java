package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.tag.SystemTagInventory;

import java.util.List;

public class SyncSystemTagFromTagMsg extends NeedReplyMessage implements ImageMessage {
    private String imageUuid;
    private List<String> vmSystemTags;
    private List<String> volumeSystemTags;


    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getVmSystemTags() {
        return vmSystemTags;
    }

    public void setVmSystemTags(List<String> vmSystemTags) {
        this.vmSystemTags = vmSystemTags;
    }

    public List<String> getVolumeSystemTags() {
        return volumeSystemTags;
    }

    public void setVolumeSystemTags(List<String> volumeSystemTags) {
        this.volumeSystemTags = volumeSystemTags;
    }
}
