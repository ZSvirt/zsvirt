package org.zstack.storage.migration.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

import java.util.Set;

/**
 * Created by GuoYi on 10/14/17.
 */
public class MigrateVolumeOnPrimaryStorageReply extends MessageReply {
    private VolumeInventory inventory;

    private Set<String> volumePathsToTrash;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public Set<String> getVolumePathsToTrash() {
        return volumePathsToTrash;
    }

    public void setVolumePathsToTrash(Set<String> volumePathsToTrash) {
        this.volumePathsToTrash = volumePathsToTrash;
    }
}
