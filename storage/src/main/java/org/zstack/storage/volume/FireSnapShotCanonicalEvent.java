package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus;
import org.zstack.header.volume.SnapShotCanonicalEvents;

import java.util.Date;

/**
 * Created by camile on 2016/5/23.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FireSnapShotCanonicalEvent {
    @Autowired
    private EventFacade evtf;

    public void fireSnapShotStatusChangedEvent(VolumeSnapshotStatus oldStatus, VolumeSnapshotInventory vsi) {
        SnapShotCanonicalEvents.SnapShotStatusChangedData d = new SnapShotCanonicalEvents.SnapShotStatusChangedData();
        d.setInventory(vsi);
        d.setDate(new Date());
        d.setNewStatus(vsi.getStatus());
        d.setOldStatus(oldStatus == null ? null : oldStatus.toString());
        d.setSnapShotUuid(vsi.getUuid());
        evtf.fire(SnapShotCanonicalEvents.SNAPSHOT_STATUS_CHANGED_PATH, d);
    }
}
