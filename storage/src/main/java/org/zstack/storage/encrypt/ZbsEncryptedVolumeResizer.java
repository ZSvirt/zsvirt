package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;

import static org.zstack.core.Platform.operr;

public class ZbsEncryptedVolumeResizer {
    @Autowired
    private ZbsVolumeEncryptionMaterialFactory materialFactory;
    @Autowired
    private ZbsVolumeEncryptionKvmCaller kvmCaller;

    void resize(String primaryStorageUuid, VolumeInventory volume, long size,
                ReturnValueCompletion<VolumeStats> completion) {
        if (StringUtils.isBlank(primaryStorageUuid)) {
            completion.fail(operr("prepare encrypted ZBS volume resize requires primary storage uuid"));
            return;
        }
        if (volume == null || StringUtils.isBlank(volume.getUuid())) {
            completion.fail(operr("prepare encrypted ZBS volume resize requires target volume inventory"));
            return;
        }
        if (!Boolean.TRUE.equals(volume.getEncrypted())) {
            completion.fail(operr("ZBS volume[uuid:%s, installPath:%s] is not encrypted",
                    volume.getUuid(), volume.getInstallPath()));
            return;
        }

        long currentSize = volume.getSize();
        if (currentSize > 0 && size < currentSize) {
            completion.fail(operr("cannot shrink ZBS volume[uuid:%s, installPath:%s] from %s to %s",
                    volume.getUuid(), volume.getInstallPath(), currentSize, size));
            return;
        }
        if (currentSize > 0 && size == currentSize) {
            VolumeStats stats = new VolumeStats();
            stats.setInstallPath(volume.getInstallPath());
            stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
            stats.setSize(size);
            stats.setActualSize(volume.getActualSize());
            completion.success(stats);
            return;
        }

        ZbsVolumeEncryptionMaterial material;
        try {
            material = materialFactory.prepareVolumeEncryption(primaryStorageUuid, volume.getUuid(),
                    String.format("resize encrypted volume[uuid:%s]", volume.getUuid()));
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        } catch (Exception e) {
            completion.fail(operr("failed to prepare encrypted ZBS volume[uuid:%s, installPath:%s] resize: %s",
                    volume.getUuid(), volume.getInstallPath(), e.getMessage()));
            return;
        }
        if (StringUtils.isBlank(material.hostUuid) || StringUtils.isBlank(material.encryptedDek)) {
            completion.fail(operr("cannot resize encrypted ZBS volume[uuid:%s, installPath:%s], encryption resize info is missing",
                    volume.getUuid(), volume.getInstallPath()));
            return;
        }

        ZbsVolumeEncryptionCommands.KVMHostLuksResizeCmd cmd =
                new ZbsVolumeEncryptionCommands.KVMHostLuksResizeCmd();
        cmd.psUuid = primaryStorageUuid;
        cmd.installPath = volume.getInstallPath();
        cmd.encryptedDek = material.encryptedDek;
        cmd.virtualSize = size;

        kvmCaller.call(material.hostUuid, ZbsVolumeEncryptionConstants.KVM_HOST_LUKS_RESIZE_PATH,
                cmd, ZbsVolumeEncryptionCommands.KVMHostLuksRsp.class, "luks resize",
                new ReturnValueCompletion<ZbsVolumeEncryptionCommands.KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(ZbsVolumeEncryptionCommands.KVMHostLuksRsp rsp) {
                        VolumeStats stats = new VolumeStats();
                        stats.setInstallPath(volume.getInstallPath());
                        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        stats.setSize(size);
                        stats.setActualSize(rsp.actualSize == null ? 0 : rsp.actualSize);
                        completion.success(stats);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }
}
