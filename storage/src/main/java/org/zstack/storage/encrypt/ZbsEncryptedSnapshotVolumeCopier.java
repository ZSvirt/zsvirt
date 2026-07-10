package org.zstack.storage.encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import static org.zstack.core.Platform.operr;

public class ZbsEncryptedSnapshotVolumeCopier {
    @Autowired
    private ZbsVolumeEncryptionMaterialFactory materialFactory;
    @Autowired
    private ZbsEncryptedVolumeCloner cloner;

    void copy(ZbsVolumeEncryptionBackend backend, String srcInstallPath, CreateVolumeSpec dst,
              ReturnValueCompletion<VolumeStats> completion) {
        backend.resolveSnapshotPathForQemu(srcInstallPath, new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String resolvedSrcInstallPath) {
                createBackingAndCopy(backend, srcInstallPath, resolvedSrcInstallPath, dst, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createBackingAndCopy(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                      String resolvedSrcInstallPath, CreateVolumeSpec dst,
                                      ReturnValueCompletion<VolumeStats> completion) {
        long virtualSize;
        try {
            virtualSize = resolveSnapshotCopyVirtualSize(backend, srcInstallPath, dst);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        if (dst.getSize() <= 0) {
            dst.setSize(virtualSize);
        }

        backend.createLuksBackingVolume(backend.buildConfiguredVolumePath(dst.getName()), virtualSize,
                new ReturnValueCompletion<String>(completion) {
                    @Override
                    public void success(String dstInstallPath) {
                        copyWithCleanup(backend, srcInstallPath, resolvedSrcInstallPath, dstInstallPath, dst, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private long resolveSnapshotCopyVirtualSize(ZbsVolumeEncryptionBackend backend, String srcInstallPath,
                                                CreateVolumeSpec dst) {
        if (dst.getSize() > 0) {
            return dst.getSize();
        }

        VolumeSnapshotVO snapshot = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.primaryStorageUuid, backend.getPrimaryStorageUuid())
                .eq(VolumeSnapshotVO_.primaryStorageInstallPath, srcInstallPath)
                .find();
        if (snapshot == null) {
            throw new OperationFailureException(operr(
                    "cannot find source snapshot by installPath[%s] on external primary storage[uuid:%s] to prepare encrypted volume[uuid:%s]",
                    srcInstallPath, backend.getPrimaryStorageUuid(), dst.getUuid()));
        }

        Long volumeSize = Q.New(VolumeVO.class)
                .select(VolumeVO_.size)
                .eq(VolumeVO_.uuid, snapshot.getVolumeUuid())
                .findValue();
        if (volumeSize != null && volumeSize > 0) {
            return volumeSize;
        }
        Long snapshotSize = snapshot.getSize();
        if (snapshotSize != null && snapshotSize > 0) {
            return snapshotSize;
        }

        throw new OperationFailureException(operr(
                "cannot determine virtual size for encrypted ZBS volume[uuid:%s] from snapshot[uuid:%s, installPath:%s]",
                dst.getUuid(), snapshot.getUuid(), srcInstallPath));
    }

    private void copyWithCleanup(ZbsVolumeEncryptionBackend backend, String srcInstallPath, String resolvedSrcInstallPath,
                                 String dstInstallPath, CreateVolumeSpec dst,
                                 ReturnValueCompletion<VolumeStats> completion) {
        copy(backend.getPrimaryStorageUuid(), srcInstallPath, resolvedSrcInstallPath, dstInstallPath, dst,
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

    private void copy(String primaryStorageUuid, String srcInstallPath, String resolvedSrcInstallPath,
                      String dstInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> completion) {
        ZbsVolumeEncryptionMaterial material;
        try {
            material = materialFactory.prepareSnapshotCopyEncryption(primaryStorageUuid, srcInstallPath, dst);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        } catch (Exception e) {
            completion.fail(operr("failed to prepare encrypted ZBS copy from snapshot[%s] to target[uuid:%s]: %s",
                    srcInstallPath, dst.getUuid(), e.getMessage()));
            return;
        }

        cloner.clone(primaryStorageUuid, resolvedSrcInstallPath, dstInstallPath, dst, material, completion);
    }
}
