package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import static org.zstack.core.Platform.operr;

public class ZbsEncryptedVolumeCloner {
    @Autowired
    private ZbsVolumeEncryptionMaterialFactory materialFactory;
    @Autowired
    private ZbsVolumeEncryptionKvmCaller kvmCaller;
    @Autowired
    private ZbsVolumeEncryptionCleanup cleanup;
    @Autowired
    private ZbsEncryptedVolumeResizer volumeResizer;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;

    void cloneFromImage(ZbsVolumeEncryptionBackend backend, String srcInstallPath, CreateVolumeSpec dst,
                        ReturnValueCompletion<VolumeStats> completion) {
        if (isEncryptedSource(backend, srcInstallPath)) {
            cloneEncryptedSourceAsBacking(backend, srcInstallPath, dst, completion);
            return;
        }

        backend.resolveSnapshotPathForQemu(srcInstallPath, new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String resolvedSrcInstallPath) {
                createBackingAndClone(backend, srcInstallPath, resolvedSrcInstallPath, dst, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createBackingAndClone(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                       String resolvedSrcInstallPath, CreateVolumeSpec dst,
                                       ReturnValueCompletion<VolumeStats> completion) {
        resolveCloneVirtualSize(backend, srcInstallPath, resolvedSrcInstallPath, dst,
                new ReturnValueCompletion<Long>(completion) {
                    @Override
                    public void success(Long virtualSize) {
                        createBackingAndClone(backend, resolvedSrcInstallPath, dst, virtualSize, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void createBackingAndClone(ZbsVolumeEncryptionBackend backend, String srcInstallPath, CreateVolumeSpec dst,
                                       long virtualSize, ReturnValueCompletion<VolumeStats> completion) {
        backend.createLuksBackingVolume(backend.buildConfiguredVolumePath(dst.getName()), virtualSize,
                new ReturnValueCompletion<String>(completion) {
                    @Override
                    public void success(String dstInstallPath) {
                        cloneFromImageWithCleanup(backend, srcInstallPath, dstInstallPath, dst, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void cloneEncryptedSourceAsBacking(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                               CreateVolumeSpec dst,
                                               ReturnValueCompletion<VolumeStats> completion) {
        resolveCloneVirtualSize(backend, srcInstallPath, srcInstallPath, dst,
                new ReturnValueCompletion<Long>(completion) {
                    @Override
                    public void success(Long virtualSize) {
                        dst.setSize(virtualSize);
                        backend.cloneVolumeAsBacking(srcInstallPath, dst,
                                new ReturnValueCompletion<VolumeStats>(completion) {
                                    @Override
                                    public void success(VolumeStats stats) {
                                        resizeClonedEncryptedBacking(backend, stats, dst, completion);
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        completion.fail(errorCode);
                                    }
                                });
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void resizeClonedEncryptedBacking(ZbsVolumeEncryptionBackend backend, VolumeStats stats,
                                              CreateVolumeSpec dst,
                                              ReturnValueCompletion<VolumeStats> completion) {
        VolumeInventory volume = new VolumeInventory();
        volume.setUuid(dst.getUuid());
        volume.setInstallPath(stats.getInstallPath());
        volume.setSize(0);
        volume.setActualSize(stats.getActualSize());
        volume.setEncrypted(true);

        volumeResizer.resize(backend.getPrimaryStorageUuid(), volume, dst.getSize(),
                new ReturnValueCompletion<VolumeStats>(completion) {
                    @Override
                    public void success(VolumeStats returnValue) {
                        returnValue.setParentUri(stats.getParentUri());
                        completion.success(returnValue);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        backend.deleteLuksBackingVolume(stats.getInstallPath());
                        completion.fail(errorCode);
                    }
                });
    }

    private void resolveCloneVirtualSize(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                         String resolvedSrcInstallPath, CreateVolumeSpec dst,
                                         ReturnValueCompletion<Long> completion) {
        if (dst.getSize() > 0) {
            completion.success(dst.getSize());
            return;
        }

        Long dbSize = findSourceVirtualSizeInDb(backend, srcInstallPath);
        if (dbSize != null && dbSize > 0) {
            dst.setSize(dbSize);
            completion.success(dbSize);
            return;
        }

        backend.stats(resolvedSrcInstallPath, new ReturnValueCompletion<VolumeStats>(completion) {
            @Override
            public void success(VolumeStats stats) {
                Long size = stats.getSize();
                if (size == null || size <= 0) {
                    completion.fail(operr(
                            "cannot determine virtual size for encrypted ZBS volume[uuid:%s] from source[%s]",
                            dst.getUuid(), srcInstallPath));
                    return;
                }

                dst.setSize(size);
                completion.success(size);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private Long findSourceVirtualSizeInDb(ZbsVolumeEncryptionBackend backend, String srcInstallPath) {
        VolumeSnapshotVO snapshot = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(VolumeSnapshotVO_.primaryStorageInstallPath, srcInstallPath)
                .find();
        if (snapshot != null) {
            Long volumeSize = Q.New(VolumeVO.class)
                    .select(VolumeVO_.size)
                    .eq(VolumeVO_.uuid, snapshot.getVolumeUuid())
                    .findValue();
            if (volumeSize != null && volumeSize > 0) {
                return volumeSize;
            }
            return snapshot.getSize() > 0 ? snapshot.getSize() : null;
        }

        Long volumeSize = Q.New(VolumeVO.class)
                .select(VolumeVO_.size)
                .eq(VolumeVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(VolumeVO_.installPath, srcInstallPath)
                .findValue();
        if (volumeSize != null && volumeSize > 0) {
            return volumeSize;
        }

        return Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.size)
                .eq(ImageCacheVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(ImageCacheVO_.installUrl, srcInstallPath)
                .findValue();
    }

    private boolean isEncryptedSource(ZbsVolumeEncryptionBackend backend, String srcInstallPath) {
        Boolean snapshotEncrypted = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.encrypted)
                .eq(VolumeSnapshotVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(VolumeSnapshotVO_.primaryStorageInstallPath, srcInstallPath)
                .findValue();
        if (Boolean.TRUE.equals(snapshotEncrypted)) {
            return true;
        }

        Boolean volumeEncrypted = Q.New(VolumeVO.class)
                .select(VolumeVO_.encrypted)
                .eq(VolumeVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(VolumeVO_.installPath, srcInstallPath)
                .findValue();
        if (Boolean.TRUE.equals(volumeEncrypted)) {
            return true;
        }

        String imageUuid = Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.imageUuid)
                .eq(ImageCacheVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(ImageCacheVO_.installUrl, srcInstallPath)
                .findValue();
        return imageUuid != null && snapshotEncryptionHelper.hasTemporarySnapshotImageKey(imageUuid);
    }

    private void cloneFromImageWithCleanup(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                           String dstInstallPath, CreateVolumeSpec dst,
                                           ReturnValueCompletion<VolumeStats> completion) {
        cloneFromImage(backend.getPrimaryStorageUuid(), srcInstallPath, dstInstallPath, dst,
                new ReturnValueCompletion<VolumeStats>(completion) {
                    @Override
                    public void success(VolumeStats returnValue) {
                        completion.success(returnValue);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        backend.deleteLuksBackingVolume(dstInstallPath);
                        completion.fail(errorCode);
                    }
                });
    }

    void cloneFromImage(String primaryStorageUuid, String srcInstallPath, String dstInstallPath,
                        CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> completion) {
        ZbsVolumeEncryptionMaterial material;
        try {
            material = materialFactory.prepareVolumeEncryption(primaryStorageUuid, dst.getUuid(),
                    String.format("clone encrypted volume[uuid:%s] from image", dst.getUuid()));
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        } catch (Exception e) {
            completion.fail(operr("failed to prepare encrypted ZBS volume[uuid:%s] clone from image: %s",
                    dst.getUuid(), e.getMessage()));
            return;
        }

        clone(primaryStorageUuid, srcInstallPath, dstInstallPath, dst, material, completion);
    }

    void clone(String primaryStorageUuid, String srcInstallPath, String dstInstallPath,
               CreateVolumeSpec dst, ZbsVolumeEncryptionMaterial material,
               ReturnValueCompletion<VolumeStats> completion) {
        ZbsVolumeEncryptionCommands.KVMHostLuksCloneCmd cmd =
                new ZbsVolumeEncryptionCommands.KVMHostLuksCloneCmd();
        cmd.psUuid = primaryStorageUuid;
        cmd.srcPath = srcInstallPath;
        cmd.dstPath = dstInstallPath;
        cmd.encryptedDek = material.encryptedDek;
        cmd.virtualSizeForLuksClone = dst.getSize();

        kvmCaller.call(material.hostUuid, ZbsVolumeEncryptionConstants.KVM_HOST_LUKS_CLONE_PATH,
                cmd, ZbsVolumeEncryptionCommands.KVMHostLuksRsp.class, "luks clone",
                new ReturnValueCompletion<ZbsVolumeEncryptionCommands.KVMHostLuksRsp>(completion) {
                    @Override
                    public void success(ZbsVolumeEncryptionCommands.KVMHostLuksRsp rsp) {
                        VolumeStats stats = new VolumeStats();
                        stats.setInstallPath(cmd.dstPath);
                        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        stats.setSize(dst.getSize());
                        stats.setActualSize(rsp.actualSize == null ? 0 : rsp.actualSize);
                        completion.success(stats);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        cleanup.cleanupVolume(primaryStorageUuid, cmd.dstPath);
                        completion.fail(errorCode);
                    }
                });
    }
}
