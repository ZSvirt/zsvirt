package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;

import static org.zstack.core.Platform.operr;

public class ZbsVolumeEncryptionConverter {
    private static final CLogger logger = Utils.getLogger(ZbsVolumeEncryptionConverter.class);

    @Autowired
    private ZbsVolumeEncryptionMaterialFactory materialFactory;
    @Autowired
    private ZbsVolumeEncryptionKvmCaller kvmCaller;

    void convert(ZbsVolumeEncryptionBackend backend, ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                 ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem item;
        try {
            item = validate(msg);
            backend.validateConversionPaths(item.getSourceInstallPath(), item.getTargetInstallPath());
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        try {
            backend.createConversionTarget(item.getTargetInstallPath(), msg.getVolume().getSize(),
                    msg.isTargetEncrypted(), new ReturnValueCompletion<String>(completion) {
                        @Override
                        public void success(String createdTargetInstallPath) {
                            convertCreatedTarget(backend, msg, item, createdTargetInstallPath, completion);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    private ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem validate(
            ConvertVolumeEncryptionOnPrimaryStorageMsg msg) {
        if (msg.getVolume() == null || StringUtils.isBlank(msg.getVolume().getUuid())) {
            throw new OperationFailureException(operr("ZBS volume encryption conversion requires a valid volume"));
        }
        if (msg.getItems() == null || msg.getItems().size() != 1) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires exactly one active volume item, but got[%s]",
                    msg.getItems() == null ? 0 : msg.getItems().size()));
        }

        ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem item = msg.getItems().get(0);
        if (item == null || !VolumeVO.class.getSimpleName().equals(item.getResourceType()) ||
                !msg.getVolume().getUuid().equals(item.getResourceUuid())) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion item must identify active volume[uuid:%s]",
                    msg.getVolume().getUuid()));
        }
        if (StringUtils.isBlank(item.getSourceInstallPath()) || StringUtils.isBlank(item.getTargetInstallPath())) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires non-empty source and target paths for volume[uuid:%s]",
                    msg.getVolume().getUuid()));
        }
        if (StringUtils.isNotBlank(item.getTargetBackingInstallPath())) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion does not support backing path[%s] for volume[uuid:%s]",
                    item.getTargetBackingInstallPath(), msg.getVolume().getUuid()));
        }
        return item;
    }

    private void convertCreatedTarget(ZbsVolumeEncryptionBackend backend,
                                      ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                      ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem item,
                                      String createdTargetInstallPath,
                                      ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        ZbsVolumeEncryptionMaterial material;
        try {
            material = materialFactory.prepareVolumeEncryption(backend.getPrimaryStorageUuid(),
                    msg.getVolume().getUuid(), String.format(
                            "convert ZBS volume[uuid:%s] encryption", msg.getVolume().getUuid()));
        } catch (OperationFailureException e) {
            cleanupAndFail(backend, createdTargetInstallPath, e.getErrorCode(), completion);
            return;
        } catch (RuntimeException e) {
            cleanupAndFail(backend, createdTargetInstallPath,
                    operr("failed to prepare ZBS volume encryption material for volume[uuid:%s]",
                            msg.getVolume().getUuid()).withException(e.getMessage()), completion);
            return;
        }

        ZbsVolumeEncryptionCommands.KVMHostLuksConvertCmd cmd =
                new ZbsVolumeEncryptionCommands.KVMHostLuksConvertCmd();
        cmd.psUuid = backend.getPrimaryStorageUuid();
        cmd.encryptedDek = material.encryptedDek;
        cmd.installPath = item.getSourceInstallPath();
        cmd.targetInstallPath = createdTargetInstallPath;
        cmd.targetEncrypted = msg.isTargetEncrypted();
        cmd.virtualSize = msg.getVolume().getSize();

        try {
            kvmCaller.call(material.hostUuid, ZbsVolumeEncryptionConstants.KVM_HOST_LUKS_CONVERT_PATH,
                    cmd, ZbsVolumeEncryptionCommands.KVMHostLuksRsp.class, "volume encryption conversion",
                    new ReturnValueCompletion<ZbsVolumeEncryptionCommands.KVMHostLuksRsp>(completion) {
                        @Override
                        public void success(ZbsVolumeEncryptionCommands.KVMHostLuksRsp rsp) {
                            if (rsp.actualSize != null) {
                                complete(msg, rsp.actualSize, completion);
                                return;
                            }
                            queryTargetStats(backend, msg, createdTargetInstallPath, completion);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            cleanupAndFail(backend, createdTargetInstallPath, errorCode, completion);
                        }
                    });
        } catch (OperationFailureException e) {
            cleanupAndFail(backend, createdTargetInstallPath, e.getErrorCode(), completion);
        } catch (RuntimeException e) {
            cleanupAndFail(backend, createdTargetInstallPath,
                    operr("failed to dispatch ZBS volume encryption conversion for volume[uuid:%s]",
                            msg.getVolume().getUuid()).withException(e.getMessage()), completion);
        }
    }

    private void queryTargetStats(ZbsVolumeEncryptionBackend backend,
                                  ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                  String targetInstallPath,
                                  ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        try {
            backend.stats(targetInstallPath, new ReturnValueCompletion<VolumeStats>(completion) {
                @Override
                public void success(VolumeStats stats) {
                    if (stats == null || stats.getActualSize() == null) {
                        cleanupAndFail(backend, targetInstallPath,
                                operr("cannot determine converted ZBS volume[uuid:%s] actual size",
                                        msg.getVolume().getUuid()), completion);
                        return;
                    }
                    complete(msg, stats.getActualSize(), completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    cleanupAndFail(backend, targetInstallPath, errorCode, completion);
                }
            });
        } catch (OperationFailureException e) {
            cleanupAndFail(backend, targetInstallPath, e.getErrorCode(), completion);
        } catch (RuntimeException e) {
            cleanupAndFail(backend, targetInstallPath,
                    operr("failed to query converted ZBS volume[uuid:%s] stats",
                            msg.getVolume().getUuid()).withException(e.getMessage()), completion);
        }
    }

    private void complete(ConvertVolumeEncryptionOnPrimaryStorageMsg msg, long actualSize,
                          ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        ConvertVolumeEncryptionOnPrimaryStorageReply reply = new ConvertVolumeEncryptionOnPrimaryStorageReply();
        reply.setActualSizes(Collections.singletonMap(msg.getVolume().getUuid(), actualSize));
        completion.success(reply);
    }

    private void cleanupAndFail(ZbsVolumeEncryptionBackend backend, String targetInstallPath, ErrorCode originalError,
                                ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply> completion) {
        try {
            backend.deleteConversionTarget(targetInstallPath, new Completion(completion) {
                @Override
                public void success() {
                    completion.fail(originalError);
                }

                @Override
                public void fail(ErrorCode cleanupError) {
                    logger.warn(String.format(
                            "failed to cleanup converted ZBS target[installPath:%s] while preserving original conversion error: %s",
                            targetInstallPath, cleanupError));
                    completion.fail(originalError);
                }
            });
        } catch (Exception e) {
            logger.warn(String.format(
                    "failed to cleanup converted ZBS target[installPath:%s] while preserving original conversion error: %s",
                    targetInstallPath, e.getMessage()));
            completion.fail(originalError);
        }
    }
}
