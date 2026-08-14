package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.SyncBackupResult;
import org.zstack.header.storage.backup.VolumeBackupVO;

/**
 * Created by MaJin on 2019/4/26.
 */
@RestResponse(allTo = "result")
public class APISyncVolumeBackupEvent extends APIEvent {
    private SyncBackupResult result;

    public SyncBackupResult getResult() {
        return result;
    }

    public void setResult(SyncBackupResult result) {
        this.result = result;
    }

    public APISyncVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APISyncVolumeBackupEvent() {
        super();
    }

    public static APISyncVolumeBackupEvent __example__() {
        SyncBackupResult result = new SyncBackupResult(1, 3);
        APISyncVolumeBackupEvent event = new APISyncVolumeBackupEvent(uuid(VolumeBackupVO.class));
        event.setResult(result);
        return event;
    }

}
