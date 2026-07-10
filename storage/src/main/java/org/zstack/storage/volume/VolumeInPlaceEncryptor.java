package org.zstack.storage.volume;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;
import org.zstack.storage.primary.PrimaryStorageDeleteBitGC;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Performs a LUKS conversion of an existing volume's bits.
 *
 * <p>This is the single source of truth for the "encrypt-in-place" workflow that was
 * previously duplicated between {@link VolumeBase#handleMessage} (for the
 * {@code EncryptVolumeMsg} entry point) and
 * {@link VolumeManagerImpl} (for the create-data-volume-from-template flow).
 *
 * <p>Steps performed:
 * <ol>
 *   <li>Ensure a key-provider binding exists for the volume; auto-attach the default
 *       provider when none is bound yet.</li>
 *   <li>Materialize a DEK via {@link EncryptedResourceKeyManager#getOrCreateKey}.</li>
 *   <li>Seal the DEK for the target host and pass it as {@code encryptedDek}
 *       to the primary storage backend.</li>
 *   <li>Ask the primary storage backend to LUKS-convert the bits via
 *       {@code EncryptVolumeBitsOnPrimaryStorageMsg}. The backend may return a
 *       replacement install path.</li>
 *   <li>Persist {@code VolumeVO.encrypted = true} after a successful conversion.</li>
 * </ol>
 *
 * <p>Idempotency: when {@code volume.encrypted == true} the helper treats the volume as
 * already converted and short-circuits with success. Callers must therefore not pre-mark
 * the row encrypted before invoking this helper -- the encrypted flag is the single
 * authoritative signal that "the bits on disk are already LUKS".
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeInPlaceEncryptor {
    private static final CLogger logger = Utils.getLogger(VolumeInPlaceEncryptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    /**
     * Inputs that don't live on {@link VolumeVO} (host to stage the secret on, overrides
     * for installPath / primaryStorageUuid when the caller already knows them, etc.).
     */
    public static class Context {
        private String hostUuid;
        /** Optional; falls back to {@code volume.getPrimaryStorageUuid()}. */
        private String primaryStorageUuid;
        /** Optional; falls back to {@code volume.getInstallPath()}. */
        private String installPath;
        /** Free-form purpose label for the DEK get-or-create audit trail. */
        private String purpose;

        public String getHostUuid() {
            return hostUuid;
        }

        public Context setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
            return this;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public Context setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
            return this;
        }

        public String getInstallPath() {
            return installPath;
        }

        public Context setInstallPath(String installPath) {
            this.installPath = installPath;
            return this;
        }

        public String getPurpose() {
            return purpose;
        }

        public Context setPurpose(String purpose) {
            this.purpose = purpose;
            return this;
        }
    }

    /**
     * Run the encrypt-in-place workflow.
     *
     * @param volume     the (already-persisted) target volume; the caller is responsible
     *                   for having a fresh row before invoking
     * @param ctx        host / installPath / purpose
     * @param completion success returns the latest {@code VolumeVO} (encrypted row when
     *                   the workflow actually ran, or the original row when it was a
     *                   no-op short-circuit)
     */
    public void encryptInPlace(VolumeVO volume, Context ctx, ReturnValueCompletion<VolumeVO> completion) {
        if (volume == null) {
            completion.fail(operr("encrypt-in-place: volume is null"));
            return;
        }

        // Idempotent short-circuit. The encrypted flag is the authoritative signal that
        // the on-disk bits are already in LUKS form (this helper is the only place that
        // flips it to true, and it does so only after a successful qemu-img convert).
        if (volume.isEncrypted()) {
            completion.success(volume);
            return;
        }

        if (StringUtils.isBlank(ctx.getHostUuid())) {
            completion.fail(operr(
                    "cannot encrypt volume[uuid:%s] in place: hostUuid is required to stage LUKS secret",
                    volume.getUuid()));
            return;
        }

        final String installPath = StringUtils.isNotBlank(ctx.getInstallPath())
                ? ctx.getInstallPath() : volume.getInstallPath();
        if (StringUtils.isBlank(installPath)) {
            completion.fail(operr(
                    "cannot encrypt volume[uuid:%s] in place: installPath unknown (volume not instantiated?)",
                    volume.getUuid()));
            return;
        }

        final String psUuid = StringUtils.isNotBlank(ctx.getPrimaryStorageUuid())
                ? ctx.getPrimaryStorageUuid() : volume.getPrimaryStorageUuid();
        if (StringUtils.isBlank(psUuid)) {
            completion.fail(operr(
                    "cannot encrypt volume[uuid:%s] in place: primaryStorageUuid unknown",
                    volume.getUuid()));
            return;
        }

        // 1) Ensure a key-provider binding exists; auto-attach the default provider when missing.
        //    Binding is no longer eagerly performed at volume-create time, so this helper is the
        //    canonical attach point for the encrypt-from-template and encrypt-existing-volume paths
        //    (the regular instantiate path is covered by VolumeEncryptedInitialExtension).
        boolean attachedProviderHere = false;
        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volume.getUuid());
        if (StringUtils.isBlank(kpUuid)) {
            kpUuid = volumeEncryptedResourceKeyBackend.defaultKeyProviderUuid();
            if (StringUtils.isBlank(kpUuid)) {
                completion.fail(operr(
                        "cannot encrypt volume[uuid:%s] in place: no key provider bound and no default key provider configured",
                        volume.getUuid()));
                return;
            }
            volumeEncryptedResourceKeyBackend.attachKeyProviderToVolume(volume.getUuid(), kpUuid);
            attachedProviderHere = true;
        }
        final boolean rollbackAttachedProvider = attachedProviderHere;

        // 2) Materialize the DEK (idempotent: get-or-create).
        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext keyCtx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        keyCtx.setResourceUuid(volume.getUuid());
        keyCtx.setResourceType(VolumeVO.class.getSimpleName());
        keyCtx.setKeyProviderUuid(kpUuid);
        keyCtx.setPurpose(StringUtils.defaultIfBlank(ctx.getPurpose(), "encrypt-volume-in-place"));

        final EncryptedResourceKeyManager.ResourceKeyResult[] keyResultRef =
                new EncryptedResourceKeyManager.ResourceKeyResult[1];
        final ErrorCode[] keyErrorRef = new ErrorCode[1];
        encryptedResourceKeyManager.getOrCreateKey(keyCtx,
                new ReturnValueCompletion<EncryptedResourceKeyManager.ResourceKeyResult>(completion) {
                    @Override
                    public void success(EncryptedResourceKeyManager.ResourceKeyResult r) {
                        keyResultRef[0] = r;
                    }

                    @Override
                    public void fail(ErrorCode err) {
                        keyErrorRef[0] = err;
                    }
                });
        if (keyErrorRef[0] != null) {
            if (VolumeEncryptedSecretHelper.isKeyProviderUnavailable(keyErrorRef[0])) {
                failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, null,
                        keyErrorRef[0], completion);
                return;
            }

            failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, null, operr(
                    "failed to materialize encryption key for volume[uuid:%s]",
                    volume.getUuid()).withCause(keyErrorRef[0]), completion);
            return;
        }
        final EncryptedResourceKeyManager.ResourceKeyResult keyResult = keyResultRef[0];
        if (keyResult == null) {
            failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, null, operr(
                    "encryption key manager returned no key result for volume[uuid:%s]",
                    volume.getUuid()), completion);
            return;
        }
        final String dekBase64 = keyResult.getDekBase64();
        if (StringUtils.isBlank(dekBase64)) {
            failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, keyResult, operr(
                    "encryption key manager returned empty DEK for volume[uuid:%s]",
                    volume.getUuid()), completion);
            return;
        }

        // 3) Seal the DEK for the host. The kvmagent unwraps it and creates any
        //    short-lived secret material file locally.
        final String encryptedDek;
        try {
            encryptedDek = volumeEncryptedSecretHelper.verifyHostKeyAndHpkeSealDek(
                    ctx.getHostUuid(), volume.getUuid(), dekBase64);
        } catch (OperationFailureException e) {
            failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, keyResult, operr(
                    "failed to prepare LUKS encryptedDek for volume[uuid:%s] on host[uuid:%s]",
                    volume.getUuid(), ctx.getHostUuid())
                    .withCause(e.getErrorCode()), completion);
            return;
        }

        // 4) Ask the PS backend to LUKS-convert the bits.
        EncryptVolumeBitsOnPrimaryStorageMsg emsg = new EncryptVolumeBitsOnPrimaryStorageMsg();
        emsg.setPrimaryStorageUuid(psUuid);
        emsg.setHostUuid(ctx.getHostUuid());
        emsg.setVolumeUuid(volume.getUuid());
        emsg.setInstallPath(installPath);
        emsg.setEncryptedDek(encryptedDek);
        bus.makeTargetServiceIdByResourceUuid(emsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(emsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    failAfterRollingBackMetadata(volume.getUuid(), rollbackAttachedProvider, keyResult,
                            r.getError(), completion);
                    return;
                }
                EncryptVolumeBitsOnPrimaryStorageReply reply = r.castReply();
                String newInstallPath = reply.getInstallPath();
                boolean installPathChanged = StringUtils.isNotBlank(newInstallPath)
                        && !StringUtils.equals(newInstallPath, installPath);
                if (installPathChanged) {
                    volume.setInstallPath(newInstallPath);
                }
                // 5) Persist encrypted=true. The short-circuit above guarantees we only
                //    reach here when the row was previously encrypted=false; this is the
                //    one and only place the flag flips, ensuring it always reflects the
                //    on-disk reality.
                volume.setEncrypted(true);
                VolumeVO latest = dbf.updateAndRefresh(volume);
                if (installPathChanged) {
                    deleteOldVolumeBits(psUuid, latest, installPath, completion);
                    return;
                }
                completion.success(latest);
            }
        });
    }

    void deleteOldVolumeBits(String psUuid, VolumeVO volume, String oldInstallPath,
                             ReturnValueCompletion<VolumeVO> completion) {
        DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
        dmsg.setPrimaryStorageUuid(psUuid);
        dmsg.setInstallPath(oldInstallPath);
        dmsg.setBitsUuid(volume.getUuid());
        dmsg.setBitsType(VolumeVO.class.getSimpleName());
        dmsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(volume.getFormat()).toString());
        dmsg.setSize(volume.getSize());
        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(volume);
                    return;
                }

                PrimaryStorageDeleteBitGC gc = new PrimaryStorageDeleteBitGC();
                gc.NAME = String.format("gc-delete-old-bits-volume-%s-on-primary-storage-%s", volume.getUuid(), psUuid);
                gc.primaryStorageInstallPath = oldInstallPath;
                gc.primaryStorageUuid = psUuid;
                gc.volume = volume;
                gc.submit(PrimaryStorageGlobalConfig.PRIMARY_STORAGE_DELETEBITS_GARBAGE_COLLECTOR_INTERVAL.value(Long.class),
                        TimeUnit.SECONDS);
                logger.warn(String.format(
                        "failed to delete old volume bits[installPath:%s] after encrypting volume[uuid:%s] in place, cleanup GC has been submitted: %s",
                        oldInstallPath, volume.getUuid(), reply.getError()));
                completion.success(volume);
            }
        });
    }

    private void failAfterRollingBackMetadata(String volumeUuid,
                                              boolean attachedProviderHere,
                                              EncryptedResourceKeyManager.ResourceKeyResult keyResult,
                                              ErrorCode originalError,
                                              ReturnValueCompletion<VolumeVO> completion) {
        rollbackEncryptMetadataBestEffort(volumeUuid, attachedProviderHere, keyResult, new Completion(completion) {
            @Override
            public void success() {
                completion.fail(originalError);
            }

            @Override
            public void fail(ErrorCode cleanupError) {
                logger.warn(String.format(
                        "failed to rollback encrypt-in-place metadata for volume[uuid:%s] after failure[%s]: %s",
                        volumeUuid,
                        originalError != null ? originalError.getDetails() : "",
                        cleanupError != null ? cleanupError.getDetails() : ""));
                completion.fail(originalError);
            }
        });
    }

    private void rollbackEncryptMetadataBestEffort(String volumeUuid,
                                                   boolean attachedProviderHere,
                                                   EncryptedResourceKeyManager.ResourceKeyResult keyResult,
                                                   Completion completion) {
        if (keyResult != null && keyResult.isCreatedNewKey()) {
            encryptedResourceKeyManager.rollbackCreatedKey(keyResult, new Completion(completion) {
                @Override
                public void success() {
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        if (attachedProviderHere) {
            try {
                volumeEncryptedResourceKeyBackend.detachKeyProviderFromVolume(volumeUuid);
            } catch (Exception e) {
                completion.fail(operr("failed to detach key provider from volume[uuid:%s] during encrypt-in-place rollback: %s",
                        volumeUuid, e.getMessage()));
                return;
            }
        }
        completion.success();
    }
}
