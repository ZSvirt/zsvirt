package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeStats;

import static org.zstack.core.Platform.operr;

public class ZbsEncryptedEmptyVolumeCreator {
    @Autowired
    private ZbsVolumeEncryptionMaterialFactory materialFactory;
    @Autowired
    private ZbsVolumeEncryptionKvmCaller kvmCaller;
    @Autowired
    private ZbsVolumeEncryptionCleanup cleanup;

    void create(ZbsVolumeEncryptionBackend backend, CreateVolumeSpec spec,
                ReturnValueCompletion<VolumeStats> completion) {
        backend.createLuksBackingVolume(backend.buildConfiguredVolumePath(spec.getName()), spec.getSize(),
                new ReturnValueCompletion<String>(completion) {
                    @Override
                    public void success(String installPath) {
                        createOnPreparedBackingWithCleanup(backend, installPath, spec, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void createOnPreparedBackingWithCleanup(ZbsVolumeEncryptionBackend backend, String installPath,
                                                    CreateVolumeSpec spec,
                                                    ReturnValueCompletion<VolumeStats> completion) {
        createOnPreparedBacking(backend.getPrimaryStorageUuid(), installPath, spec,
                new ReturnValueCompletion<VolumeStats>(completion) {
                    @Override
                    public void success(VolumeStats returnValue) {
                        completion.success(returnValue);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        backend.deleteLuksBackingVolume(installPath);
                        completion.fail(errorCode);
                    }
                });
    }

    private void createOnPreparedBacking(String primaryStorageUuid, String installPath, CreateVolumeSpec spec,
                                         ReturnValueCompletion<VolumeStats> completion) {
        ZbsVolumeEncryptionMaterial material;
        try {
            material = materialFactory.prepareVolumeEncryption(primaryStorageUuid, spec,
                    String.format("create encrypted volume[uuid:%s]", spec.getUuid()));
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        } catch (Exception e) {
            completion.fail(operr("failed to prepare encrypted ZBS volume[uuid:%s] create: %s",
                    spec.getUuid(), e.getMessage()));
            return;
        }

        ZbsVolumeEncryptionCommands.KVMHostLuksCreateEmptyCmd cmd =
                new ZbsVolumeEncryptionCommands.KVMHostLuksCreateEmptyCmd();
        cmd.psUuid = primaryStorageUuid;
        cmd.installPath = installPath;
        cmd.size = spec.getSize();
        cmd.encryptedDek = material.encryptedDek;

        kvmCaller.call(material.hostUuid, ZbsVolumeEncryptionConstants.KVM_HOST_LUKS_CREATE_EMPTY_PATH,
                cmd, ZbsVolumeEncryptionCommands.KVMHostLuksRsp.class, "luks createempty",
                new ReturnValueCompletion<ZbsVolumeEncryptionCommands.KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(ZbsVolumeEncryptionCommands.KVMHostLuksRsp rsp) {
                        VolumeStats stats = new VolumeStats();
                        stats.setInstallPath(cmd.installPath);
                        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        stats.setSize(spec.getSize());
                        stats.setActualSize(rsp.actualSize == null ? 0 : rsp.actualSize);
                        completion.success(stats);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        cleanup.cleanupVolume(primaryStorageUuid, cmd.installPath);
                        completion.fail(errorCode);
                    }
                });
    }
}
