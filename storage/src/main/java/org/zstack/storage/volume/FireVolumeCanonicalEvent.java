package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.volume.VolumeCanonicalEvents;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.identity.Account;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/12.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FireVolumeCanonicalEvent {
    @Autowired
    private EventFacade evtf;

    public void fireVolumeStatusChangedEvent(VolumeStatus oldStatus, VolumeInventory vol) {
        // may be null
        String accountUuid = Account.getAccountUuidOfResource(vol.getUuid());

        VolumeCanonicalEvents.VolumeStatusChangedData d = new VolumeCanonicalEvents.VolumeStatusChangedData();
        d.setInventory(vol);
        d.setDate(new Date());
        d.setNewStatus(vol.getStatus());
        d.setOldStatus(oldStatus == null ? null : oldStatus.toString());
        d.setVolumeUuid(vol.getUuid());
        d.setAccountUuid(accountUuid);
        evtf.fire(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, d);
    }

    public void fireVolumeStatusChangedEvent(VolumeStatus oldStatus, VolumeInventory vol, String accountUuid) {
        VolumeCanonicalEvents.VolumeStatusChangedData d = new VolumeCanonicalEvents.VolumeStatusChangedData();
        d.setInventory(vol);
        d.setDate(new Date());
        d.setNewStatus(vol.getStatus());
        d.setOldStatus(oldStatus == null ? null : oldStatus.toString());
        d.setVolumeUuid(vol.getUuid());
        d.setAccountUuid(accountUuid);
        evtf.fire(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, d);
    }

    public void fireVolumeConfigChangedEvent(VolumeInventory vol, String accountUuid) {
        VolumeCanonicalEvents.VolumeConfigChangedData d = new VolumeCanonicalEvents.VolumeConfigChangedData();
        d.setInventory(vol);
        d.setAccoutUuid(accountUuid);
        d.setDate(new Date());
        evtf.fire(VolumeCanonicalEvents.VOLUME_CONFIG_CHANGED_PATH, d);
    }
}
