package org.zstack.scheduler.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.core.NopeNoErrorCompletion;
import org.zstack.header.core.PaginateCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.scheduler.snapshot.SchedulerSnapshotGlobalProperty.ADD_TAG_TO_SCHEDULER_SNAPSHOT;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SchedulerSnapshotUpgradeExtension implements Component, ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(SchedulerSnapshotUpgradeExtension.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    private TagManager tagMgr;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        if (ADD_TAG_TO_SCHEDULER_SNAPSHOT) {
            new VolumeSnapshot().addTagToVolumeSnapshot();
        }
    }

    class VolumeSnapshot {
        private final long count = Q.New(VolumeSnapshotVO.class).count();
        private final String sql = getSql();

        private String getSql() {
            String schedulerSnapShot = "%-snapshot-%";
            return String.format("select snapVO.uuid from VolumeSnapshotVO snapVO " +
                    "where snapVO.name like '%s'", schedulerSnapShot);
        }

        @AsyncThread
        public void addTagToVolumeSnapshot() {
            SQL.New(sql, String.class)
                    .limit(1000)
                    .paginate(count, (List<String> volumeSnapshotUuids) -> volumeSnapshotUuids.forEach(volumeSnapshotUuid -> {
                        tagMgr.createNonInherentSystemTag(volumeSnapshotUuid,
                                VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                VolumeSnapshotVO.class.getSimpleName());
                    }));
        }
    }
}
