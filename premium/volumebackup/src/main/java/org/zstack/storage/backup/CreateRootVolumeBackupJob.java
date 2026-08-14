package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerType;

/**
 * Created by MaJin on 2020/4/1.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateRootVolumeBackupJob extends CreateVolumeBackupJob {
    public CreateRootVolumeBackupJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public CreateRootVolumeBackupJob() {
        super();
    }

    @Override
    public String getType() {
        return SchedulerType.ROOT_VOLUME_BACKUP;
    }
}
