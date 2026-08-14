package org.zstack.storage.backup.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentFormat;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volumebackup.NvRamMetadata;
import org.zstack.header.volumebackup.VolumeBackupMetadataExtensionPoint;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Base64;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.Utils.getLogger;

/**
 * Implementation collecting NvRam for volume backups (root volumes only).
 */
public class NvRamVolumeBackupMetadataCollector implements VolumeBackupMetadataExtensionPoint {
    private static final CLogger logger = getLogger(NvRamVolumeBackupMetadataCollector.class);

    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private TimeHelper timeHelper;

    @Override
    @Transactional(readOnly = true)
    public void collectMetadata(String volumeBackupUuid, Map<String, String> attachments) {
        VolumeBackupVO vo = databaseFacade.findByUuid(volumeBackupUuid, VolumeBackupVO.class);
        if (vo == null) {
            return;
        }
        if (vo.getType() != VolumeType.Root) {
            return;
        }

        VmHostBackupFileVO nvRamFile = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, volumeBackupUuid)
                .eq(VmHostBackupFileVO_.type, VmHostFileType.NvRam)
                .orderByDesc(VmHostBackupFileVO_.createDate)
                .limit(1)
                .find();

        if (nvRamFile == null) {
            return;
        }

        VmHostFileContentVO content = Q.New(VmHostFileContentVO.class)
                .eq(VmHostFileContentVO_.uuid, nvRamFile.getUuid())
                .find();
        if (content == null || content.getContent() == null) {
            throw operr("content of VmHostFileContentVO[uuid=%s] is empty", nvRamFile.getUuid()).toException();
        }

        NvRamMetadata meta = new NvRamMetadata();
        meta.setUuid(nvRamFile.getUuid());
        meta.setContentBase64(Base64.getEncoder().encodeToString(content.getContent()));
        meta.setContentFormat(content.getFormat().toString());
        meta.setCreateDate(content.getCreateDate() != null ? content.getCreateDate().getTime() : 0L);
        attachments.put(VmHostFileType.NvRam.toString(), JSONObjectUtil.toJsonString(meta));
    }

    @Override
    @Transactional
    public void restoreMetadata(String volumeBackupUuid, Map<String, String> attachments) {
        VolumeBackupVO vo = databaseFacade.findByUuid(volumeBackupUuid, VolumeBackupVO.class);
        if (vo == null) {
            return;
        }
        if (vo.getType() != VolumeType.Root) {
            return;
        }

        String json = attachments.get(VmHostFileType.NvRam.toString());
        if (json == null) {
            return;
        }
        NvRamMetadata meta = JSONObjectUtil.toObject(json, NvRamMetadata.class);

        VmHostBackupFileVO backup = databaseFacade.findByUuid(meta.getUuid(), VmHostBackupFileVO.class);
        VmHostFileContentVO content = databaseFacade.findByUuid(meta.getUuid(), VmHostFileContentVO.class);

        // In most cases, both backup and content are null at the same time
        boolean needPersists = backup == null;
        if (needPersists) {
            backup = new VmHostBackupFileVO();
            backup.setUuid(meta.getUuid());
            content = new VmHostFileContentVO();
            content.setUuid(backup.getUuid());

            backup.setCreateDate(timeHelper.getCurrentTimestamp());
            backup.setLastOpDate(backup.getCreateDate());
            content.setCreateDate(backup.getCreateDate());
            content.setLastOpDate(backup.getLastOpDate());
        }

        backup.setResourceUuid(volumeBackupUuid);
        backup.setType(VmHostFileType.NvRam);
        content.setContent(Base64.getDecoder().decode(meta.getContentBase64()));
        content.setFormat(VmHostFileContentFormat.valueOf(meta.getContentFormat()));

        VmHostBackupFileVO finalBackup = backup;
        VmHostFileContentVO finalContent = content;

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (needPersists) {
                    persist(finalBackup);
                    persist(finalContent);
                } else {
                    merge(finalBackup);
                    merge(finalContent);
                }
            }
        }.execute();
    }
}
