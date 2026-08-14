package org.zstack.storage.backup.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentFormat;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volumebackup.TpmMetadata;
import org.zstack.header.volumebackup.VolumeBackupMetadataExtensionPoint;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Base64;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.Utils.getLogger;

/**
 * Implementation collecting Tpm for volume backups (root volumes only).
 */
public class TpmVolumeBackupMetadataCollector implements VolumeBackupMetadataExtensionPoint {
    private static final CLogger logger = getLogger(TpmVolumeBackupMetadataCollector.class);

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

        VmHostBackupFileVO tpmFile = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, volumeBackupUuid)
                .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                .orderByDesc(VmHostBackupFileVO_.createDate)
                .limit(1)
                .find();

        if (tpmFile == null) {
            return;
        }

        VmHostFileContentVO content = Q.New(VmHostFileContentVO.class)
                .eq(VmHostFileContentVO_.uuid, tpmFile.getUuid())
                .find();
        if (content == null || content.getContent() == null) {
            throw operr("content of VmHostFileContentVO[uuid=%s] is empty", tpmFile.getUuid()).toException();
        }

        TpmMetadata meta = new TpmMetadata();
        meta.setUuid(tpmFile.getUuid());
        meta.setContentBase64(Base64.getEncoder().encodeToString(content.getContent()));
        meta.setContentFormat(content.getFormat().toString());
        meta.setCreateDate(content.getCreateDate() != null ? content.getCreateDate().getTime() : 0L);

        EncryptedResourceKeyRefVO ref = Q.New(TpmVO.class, EncryptedResourceKeyRefVO.class)
                .table0()
                    .eq(TpmVO_.vmInstanceUuid, vo.getVmInstanceUuid())
                    .eq(TpmVO_.uuid).table1(EncryptedResourceKeyRefVO_.resourceUuid)
                .table1()
                    .selectThisTable()
                .find();
        if (ref != null) {
            meta.setProviderName(ref.getProviderName());
            meta.setKeyVersion(ref.getKeyVersion());
            meta.setKekRef(ref.getKekRef());
            meta.setWrappedDek(ref.getWrappedDek());
            meta.setAlgorithm(ref.getAlgorithm());
        }
        attachments.put(VmHostFileType.TpmState.toString(), JSONObjectUtil.toJsonString(meta));
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

        String json = attachments.get(VmHostFileType.TpmState.toString());
        if (json == null) {
            return;
        }
        TpmMetadata meta = JSONObjectUtil.toObject(json, TpmMetadata.class);

        VmHostBackupFileVO backup = databaseFacade.findByUuid(meta.getUuid(), VmHostBackupFileVO.class);
        VmHostFileContentVO content = databaseFacade.findByUuid(meta.getUuid(), VmHostFileContentVO.class);
        EncryptedResourceKeyRefVO ref;

        // In most cases, both backup and content are null at the same time
        boolean backupNeedPersists = backup == null;
        boolean refNeedPersists;
        if (backupNeedPersists) {
            backup = new VmHostBackupFileVO();
            backup.setUuid(meta.getUuid());
            content = new VmHostFileContentVO();
            content.setUuid(backup.getUuid());
            ref = new EncryptedResourceKeyRefVO();
            ref.setResourceUuid(backup.getUuid());
            ref.setResourceType(VmHostBackupFileVO.class.getSimpleName());
            refNeedPersists = true;

            backup.setCreateDate(timeHelper.getCurrentTimestamp());
            backup.setLastOpDate(backup.getCreateDate());
            content.setCreateDate(backup.getCreateDate());
            content.setLastOpDate(backup.getLastOpDate());
            ref.setCreateDate(backup.getCreateDate());
            ref.setLastOpDate(backup.getLastOpDate());
        } else {
            ref = Q.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceUuid, meta.getUuid())
                    .eq(EncryptedResourceKeyRefVO_.resourceType, VmHostBackupFileVO.class.getSimpleName())
                    .find();
            refNeedPersists = ref == null;
            if (ref == null) {
                ref = new EncryptedResourceKeyRefVO();
                ref.setResourceUuid(backup.getUuid());
                ref.setResourceType(VmHostBackupFileVO.class.getSimpleName());
                ref.setCreateDate(timeHelper.getCurrentTimestamp());
                ref.setLastOpDate(ref.getCreateDate());
            }
        }

        backup.setResourceUuid(volumeBackupUuid);
        backup.setType(VmHostFileType.TpmState);
        content.setContent(Base64.getDecoder().decode(meta.getContentBase64()));
        content.setFormat(VmHostFileContentFormat.valueOf(meta.getContentFormat()));
        ref.setProviderName(meta.getProviderName());
        ref.setKeyVersion(meta.getKeyVersion());
        ref.setKekRef(meta.getKekRef());
        ref.setWrappedDek(meta.getWrappedDek());
        ref.setAlgorithm(meta.getAlgorithm());

        VmHostBackupFileVO finalBackup = backup;
        VmHostFileContentVO finalContent = content;
        EncryptedResourceKeyRefVO finalRef = ref;

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (backupNeedPersists) {
                    persist(finalBackup);
                    persist(finalContent);
                } else {
                    merge(finalBackup);
                    merge(finalContent);
                }

                if (refNeedPersists) {
                    persist(finalRef);
                } else {
                    merge(finalRef);
                }
            }
        }.execute();
    }
}
