package org.zstack.woodpecker.subsystem.storage.subviews;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.APIChangeVolumeEncryptionEvent;
import org.zstack.header.volume.APIChangeVolumeEncryptionMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.woodpecker.APIBundle;
import org.zstack.woodpecker.resourcetree.ResourceTree;
import org.zstack.woodpecker.subsystem.storage.action.MigrateVolumeAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChangeVolumeEncryptionSubView extends StorageSubView {
    private static class Context {
        VolumeInventory volume;
        List<VolumeSnapshotInventory> snapshots;
    }

    @Override
    public void preHandle(APIBundle bundle) {
        APIChangeVolumeEncryptionMsg msg = (APIChangeVolumeEncryptionMsg) bundle.message;
        Context context = new Context();
        context.volume = ((VolumeVO) Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid()).find()).toInventory();
        context.snapshots = getSnapshots(msg.getVolumeUuid());
        bundle.context = context;
    }

    @Override
    public boolean handle(APIBundle bundle) {
        if (!bundle.success) {
            return false;
        }

        Context context = (Context) bundle.context;
        VolumeInventory volume = ((APIChangeVolumeEncryptionEvent) bundle.event).getInventory();
        List<VolumeSnapshotInventory> snapshots = getSnapshots(volume.getUuid());
        if (!Objects.equals(context.volume.getInstallPath(), volume.getInstallPath())) {
            getStorageAction(getPrimaryStorageType(volume.getPrimaryStorageUuid()), MigrateVolumeAction.class)
                    .act(context.volume, context.snapshots, volume, snapshots);
        }

        ResourceTree.updateOrCreate(volume);
        snapshots.forEach(ResourceTree::updateOrCreate);
        return true;
    }

    private List<VolumeSnapshotInventory> getSnapshots(String volumeUuid) {
        List<VolumeSnapshotVO> snapshots = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.volumeUuid, volumeUuid)
                .list();
        List<VolumeSnapshotInventory> inventories = new ArrayList<>();
        for (VolumeSnapshotVO snapshot : snapshots) {
            inventories.add(VolumeSnapshotInventory.valueOf(snapshot));
        }
        return inventories;
    }

    @Override
    public boolean canHandle(APIBundle bundle) {
        return true;
    }

    @Override
    public Class apiClassToHandle() {
        return APIChangeVolumeEncryptionMsg.class;
    }
}
