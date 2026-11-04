package org.zstack.storage.primary.allocate;

import org.zstack.core.db.Q;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

public class PrimaryStorageBackupStorageAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        if (!PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())
                || spec.getImageUuid() == null) {
            next();
            return;
        }

        List<BackupStorageVO> backupStorages = Q.New(BackupStorageVO.class, ImageBackupStorageRefVO.class)
                .table0().selectThisTable()
                .table0().eq(BackupStorageVO_.uuid).table1(ImageBackupStorageRefVO_.backupStorageUuid)
                .table1().eq(ImageBackupStorageRefVO_.imageUuid, spec.getImageUuid())
                .list();

        List<String> relatedPsUuids = new ArrayList<>();
        for (BackupStorageVO bs : backupStorages) {
            List<String> pss = BackupStorageType.valueOf(bs.getType()).findRelatedPrimaryStorage(bs.getUuid());
            if (pss != null && !pss.isEmpty()) {
                relatedPsUuids.addAll(pss);
            }
        }

        if (relatedPsUuids.isEmpty()) {
            next();
            return;
        }

        Set<String> related = new HashSet<>(relatedPsUuids);
        for (PrimaryStorageCandidate candidate : candidates) {
            if (!related.contains(candidate.getUuid())) {
                reject(candidate, i18m("not related to backup storage of image[uuid:%s]", spec.getImageUuid()));
            }
        }
        next();
    }
}
