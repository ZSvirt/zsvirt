package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.SyncBackupResult;
import org.zstack.header.storage.backup.VolumeBackupVO;

/**
 * Created by MaJin on 2019/5/6.
 */
@RestResponse(allTo = "result")
public class APISyncVmBackupEvent extends APIEvent {
    private SyncBackupResult result;

    public SyncBackupResult getResult() {
        return result;
    }

    public void setResult(SyncBackupResult result) {
        this.result = result;
    }

    public APISyncVmBackupEvent(String apiId) {
        super(apiId);
    }

    public APISyncVmBackupEvent() {
        super();
    }

    public static APISyncVmBackupEvent __example__() {
        SyncBackupResult result = new SyncBackupResult(1, 3);
        APISyncVmBackupEvent event = new APISyncVmBackupEvent(uuid(VolumeBackupVO.class));
        event.setResult(result);
        return event;
    }
}
