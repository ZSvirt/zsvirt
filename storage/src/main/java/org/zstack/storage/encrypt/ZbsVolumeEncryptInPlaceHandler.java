package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeStats;

import static org.zstack.core.Platform.operr;

public class ZbsVolumeEncryptInPlaceHandler {
    @Autowired
    private ZbsVolumeEncryptionKvmCaller kvmCaller;

    void encrypt(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                 ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        String targetInstallPath;
        try {
            targetInstallPath = backend.buildEncryptedTargetPath(msg.getInstallPath());
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        checkNoSnapshots(backend, msg, targetInstallPath, completion);
    }

    private void checkNoSnapshots(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                                  String targetInstallPath,
                                  ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        backend.checkNoSnapshots(msg.getInstallPath(), new Completion(completion) {
            @Override
            public void success() {
                querySourceStats(backend, msg, targetInstallPath, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void querySourceStats(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                                  String targetInstallPath,
                                  ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        backend.stats(msg.getInstallPath(), new ReturnValueCompletion<VolumeStats>(completion) {
            @Override
            public void success(VolumeStats sourceStats) {
                createTargetBacking(backend, msg, targetInstallPath, sourceStats, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createTargetBacking(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                                     String targetInstallPath, VolumeStats sourceStats,
                                     ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        backend.createLuksBackingVolume(targetInstallPath, sourceStats.getSize(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String createdTargetInstallPath) {
                encryptWithCleanup(backend, msg, createdTargetInstallPath, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void encryptWithCleanup(ZbsVolumeEncryptionBackend backend, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                                    String targetInstallPath,
                                    ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        msg.setTargetInstallPath(targetInstallPath);
        encrypt(backend.getPrimaryStorageUuid(), msg,
                new ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply>(completion) {
                    @Override
                    public void success(EncryptVolumeBitsOnPrimaryStorageReply returnValue) {
                        completion.success(returnValue);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        backend.deleteLuksBackingVolume(targetInstallPath);
                        completion.fail(errorCode);
                    }
                });
    }

    private void encrypt(String primaryStorageUuid, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                         ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion) {
        if (StringUtils.isBlank(msg.getHostUuid())) {
            completion.fail(operr("zbs encryptInPlace requires hostUuid; volume[uuid:%s] installPath[%s] has none",
                    msg.getVolumeUuid(), msg.getInstallPath()));
            return;
        }
        if (StringUtils.isBlank(msg.getEncryptedDek())) {
            completion.fail(operr("zbs encryptInPlace requires encryptedDek; volume[uuid:%s] installPath[%s] has none",
                    msg.getVolumeUuid(), msg.getInstallPath()));
            return;
        }
        if (StringUtils.isBlank(msg.getTargetInstallPath())) {
            completion.fail(operr("zbs encryptInPlace requires targetInstallPath; volume[uuid:%s] installPath[%s] has none",
                    msg.getVolumeUuid(), msg.getInstallPath()));
            return;
        }

        ZbsVolumeEncryptionCommands.KVMHostEncryptInPlaceCmd cmd =
                new ZbsVolumeEncryptionCommands.KVMHostEncryptInPlaceCmd();
        cmd.psUuid = primaryStorageUuid;
        cmd.installPath = msg.getInstallPath();
        cmd.targetInstallPath = msg.getTargetInstallPath();
        cmd.encryptedDek = msg.getEncryptedDek();

        kvmCaller.call(msg.getHostUuid(), ZbsVolumeEncryptionConstants.KVM_HOST_LUKS_ENCRYPT_IN_PLACE_PATH,
                cmd, ZbsVolumeEncryptionCommands.KVMHostLuksRsp.class, "encryptInPlace",
                new ReturnValueCompletion<ZbsVolumeEncryptionCommands.KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(ZbsVolumeEncryptionCommands.KVMHostLuksRsp rsp) {
                        EncryptVolumeBitsOnPrimaryStorageReply er = new EncryptVolumeBitsOnPrimaryStorageReply();
                        er.setInstallPath(StringUtils.defaultIfBlank(rsp.installPath, msg.getInstallPath()));
                        completion.success(er);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }
}
