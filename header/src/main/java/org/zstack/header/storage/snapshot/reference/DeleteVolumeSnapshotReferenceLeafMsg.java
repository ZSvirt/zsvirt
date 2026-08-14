package org.zstack.header.storage.snapshot.reference;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

public class DeleteVolumeSnapshotReferenceLeafMsg extends NeedReplyMessage implements VolumeSnapshotReferenceMessage {
    private VolumeSnapshotReferenceInventory leaf;
    private List<VolumeSnapshotReferenceInventory> otherLeafs;
    private VolumeSnapshotReferenceTreeInventory tree;
    private VolumeInventory deletedVolume;

    @Override
    public VolumeSnapshotReferenceTreeInventory getTree() {
        return tree;
    }

    public void setTree(VolumeSnapshotReferenceTreeInventory tree) {
        this.tree = tree;
    }

    public VolumeSnapshotReferenceInventory getLeaf() {
        return leaf;
    }

    public void setLeaf(VolumeSnapshotReferenceInventory leaf) {
        this.leaf = leaf;
    }

    public List<VolumeSnapshotReferenceInventory> getOtherLeafs() {
        return otherLeafs;
    }

    public void setOtherLeafs(List<VolumeSnapshotReferenceInventory> otherLeafs) {
        this.otherLeafs = otherLeafs;
    }

    public VolumeInventory getDeletedVolume() {
        return deletedVolume;
    }

    public void setDeletedVolume(VolumeInventory deletedVolume) {
        this.deletedVolume = deletedVolume;
    }
}
