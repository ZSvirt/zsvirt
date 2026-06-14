package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostKeyIdentityVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.message.MessageReply;
import org.zstack.header.secret.SecretHostDefineMsg;
import org.zstack.header.secret.SecretHostDefineReply;
import org.zstack.header.secret.SecretHostDeleteMsg;
import org.zstack.header.secret.SecretHostGetMsg;
import org.zstack.header.secret.SecretHostGetReply;
import org.zstack.header.secret.SecretHostEnsureLuksSecretFileMsg;
import org.zstack.header.secret.SecretHostEnsureLuksSecretFileReply;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.HostKeyIdentityHelper;
import org.zstack.kvm.HostSecretEnvelopeCryptoExtensionPoint;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Shared helpers for the volume LUKS secret lifecycle on a KVM host:
 *
 * <ul>
 *   <li>{@link #materializeDek} — unseal/get-or-create the DEK for a volume's
 *       bound key provider. Idempotent; safe to call on every start_vm.</li>
 *   <li>{@link #defineLibvirtSecretOnHost} — define+set-value the libvirt secret
 *       on the destination host (RAM-only). Idempotent in key-agent
 *       (volume secrets are keyed by {@code purpose, usageInstance, keyVersion};
 *       {@code vmUuid} is kept as an RPC compatibility field).</li>
 *   <li>{@link #getSecretOnHost} — ask the host whether a previously-defined
 *       libvirt secret is still resident; returns {@code null} on miss so
 *       callers can decide to re-define.</li>
 * </ul>
 *
 * <p>Used by the create path ({@link VolumeEncryptedInitialExtension}), the
 * start path ({@link VolumeEncryptedStartExtension}) and the hot-attach path
 * ({@link VolumeEncryptedAttachExtension}). All cloudbus calls are synchronous
 * via {@link CloudBus#call(org.zstack.header.message.NeedReplyMessage)};
 * timeouts are enforced by the underlying KVMHost handlers (HTTP layer has its
 * own {@code ENVELOPE_KEY_HTTP_TIMEOUT_SEC}). {@link EncryptedResourceKeyManager#getOrCreateKey}
 * currently completes synchronously, but {@link #materializeDek} still waits
 * on the completion so this helper does not depend on callback timing.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedSecretHelper {
    private static final CLogger logger = Utils.getLogger(VolumeEncryptedSecretHelper.class);
    private static final String KEY_PROVIDER_UNAVAILABLE = "keyProviderUnavailable";

    @Autowired
    private CloudBus bus;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    public EncryptedResourceKeyManager.ResourceKeyResult materializeDek(String volUuid, String kpUuid) {
        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(volUuid);
        ctx.setResourceType(VolumeVO.class.getSimpleName());
        ctx.setKeyProviderUuid(kpUuid);
        ctx.setPurpose("instantiate-volume");

        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        encryptedResourceKeyManager.getOrCreateKey(ctx, completion);
        completion.await(TimeUnit.MINUTES.toMillis(5));
        if (!completion.isSuccess()) {
            ErrorCode errorCode = completion.getErrorCode();
            if (isKeyProviderUnavailable(errorCode)) {
                throw new OperationFailureException(errorCode);
            }

            throw new OperationFailureException(operr(
                    "failed to materialize encryption key for volume[uuid:%s]", volUuid)
                    .withCause(errorCode));
        }

        EncryptedResourceKeyManager.ResourceKeyResult result = completion.getResult();
        if (result == null || StringUtils.isBlank(result.getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for encrypted volume[uuid:%s]", volUuid));
        }
        return result;
    }

    public static boolean isKeyProviderUnavailable(ErrorCode errorCode) {
        return errorCode != null && errorCode.getOpaque() != null
                && Boolean.TRUE.equals(errorCode.getOpaque().get(KEY_PROVIDER_UNAVAILABLE));
    }

    public String ensureLuksSecretFileOnHost(String hostUuid, String resourceUuid, String dekBase64) {
        SecretHostEnsureLuksSecretFileMsg ensureMsg = new SecretHostEnsureLuksSecretFileMsg();
        ensureMsg.setHostUuid(hostUuid);
        ensureMsg.setDekBase64(dekBase64);
        bus.makeTargetServiceIdByResourceUuid(ensureMsg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(ensureMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to prepare secret material file for encrypted resource[uuid:%s] on host[uuid:%s]",
                    resourceUuid, hostUuid).withCause(reply.getError()));
        }
        SecretHostEnsureLuksSecretFileReply r = reply.castReply();
        if (StringUtils.isBlank(r.getSecFilePath())) {
            throw new OperationFailureException(operr(
                    "ensure LUKS secret file on host succeeded but secFilePath is empty, host[uuid:%s]",
                    hostUuid));
        }
        return r.getSecFilePath();
    }

    public String prepareLuksSecretMaterialFileOnHost(String hostUuid, String volumeUuid) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr(
                    "prepare LUKS secret material file requires non-blank hostUuid and volumeUuid"));
        }

        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volumeUuid);
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "volume[uuid:%s] requires LUKS secret material but has no key provider binding",
                    volumeUuid));
        }

        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(volumeUuid, kpUuid);
        String dekBase64 = keyResult.getDekBase64();
        if (StringUtils.isBlank(dekBase64)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s]: key manager returned empty DEK for LUKS secret material file",
                    volumeUuid));
        }

        return ensureLuksSecretFileOnHost(hostUuid, volumeUuid, dekBase64);
    }

    public String verifyHostKeyAndHpkeSealDek(String hostUuid, String resourceUuid, String dekBase64) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(resourceUuid) ||
                StringUtils.isBlank(dekBase64)) {
            throw new OperationFailureException(operr(
                    "prepare LUKS envelope DEK requires non-blank hostUuid, resourceUuid and dekBase64"));
        }

        HostKeyIdentityVO identity = HostKeyIdentityHelper.getHostKeyIdentity(dbf, hostUuid);
        String pubKey = identity != null ? StringUtils.trimToNull(identity.getPublicKey()) : null;
        Boolean verifyOk = identity != null ? identity.getVerified() : null;
        if (pubKey == null) {
            throw new OperationFailureException(operr("no public key for host[uuid:%s], connect/reconnect did not sync key", hostUuid));
        }

        String storedFingerprint = StringUtils.trimToNull(identity.getFingerprint());
        String computed = HostKeyIdentityHelper.fingerprintFromPublicKey(pubKey);
        if (storedFingerprint == null || !StringUtils.equals(storedFingerprint, computed)) {
            throw new OperationFailureException(operr(
                    "host[uuid:%s] public key fingerprint mismatch, key may be corrupted or tampered",
                    hostUuid));
        }

        if (!Boolean.TRUE.equals(verifyOk)) {
            throw new OperationFailureException(operr("host[uuid:%s] secret key verify not ok, not synced", hostUuid));
        }

        byte[] dekRaw = null;
        byte[] envelope = null;
        try {
            dekRaw = Base64.getDecoder().decode(dekBase64.trim());
            if (dekRaw.length == 0) {
                throw new OperationFailureException(operr("dekBase64 decoded to empty"));
            }
            if (dekRaw.length > KVMConstant.MAX_DEK_BYTES) {
                throw new OperationFailureException(operr("dekBase64 decoded payload is too large"));
            }

            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKey);
            if (pubKeyBytes.length != 32) {
                throw new OperationFailureException(operr("host[uuid:%s] public key must be 32 bytes (X25519)", hostUuid));
            }

            List<HostSecretEnvelopeCryptoExtensionPoint> sealers =
                    pluginRegistry.getExtensionList(HostSecretEnvelopeCryptoExtensionPoint.class);
            if (sealers == null || sealers.isEmpty()) {
                throw new OperationFailureException(operr(
                        "host secret envelope sealer not available (premium crypto module required)"));
            }

            envelope = sealers.get(0).seal(pubKeyBytes, dekRaw);
            return Base64.getEncoder().encodeToString(envelope);
        } catch (IllegalArgumentException e) {
            throw new OperationFailureException(operr("invalid base64 while preparing LUKS envelope DEK: %s", e.getMessage()));
        } catch (OperationFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailureException(operr("HPKE seal failed: %s", e.getMessage()));
        } finally {
            if (dekRaw != null) {
                for (int i = 0; i < dekRaw.length; i++) {
                    dekRaw[i] = 0;
                }
            }
            if (envelope != null) {
                for (int i = 0; i < envelope.length; i++) {
                    envelope[i] = 0;
                }
            }
        }
    }

    @ExceptionSafe
    public ErrorableValue<String> verifyHostKeyAndHpkeSealDekErrorable(String hostUuid, String resourceUuid, String dekBase64) {
        return ErrorableValue.of(verifyHostKeyAndHpkeSealDek(hostUuid, resourceUuid, dekBase64));
    }

    public String materializeAndSealVolumeDekForHost(String hostUuid, String volumeUuid) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr(
                    "prepare LUKS envelope DEK requires non-blank hostUuid and volumeUuid"));
        }

        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volumeUuid);
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "volume[uuid:%s] requires LUKS secret material but has no key provider binding",
                    volumeUuid));
        }

        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(volumeUuid, kpUuid);
        String dekBase64 = keyResult.getDekBase64();
        if (StringUtils.isBlank(dekBase64)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s]: key manager returned empty DEK for LUKS envelope",
                    volumeUuid));
        }

        return verifyHostKeyAndHpkeSealDek(hostUuid, volumeUuid, dekBase64);
    }

    /**
     * Define a per-volume libvirt secret on {@code hostUuid}. Returns the
     * libvirt secret UUID. Throws on failure / blank reply.
     */
    public String defineLibvirtSecretOnHost(String hostUuid, String vmUuid, String volUuid,
                                            String dekBase64, Integer keyVersion) {
        return defineLibvirtSecretOnHost(hostUuid, vmUuid, volUuid, dekBase64, keyVersion, null);
    }

    public String defineLibvirtSecretOnHost(String hostUuid, String vmUuid, String volUuid,
                                            String dekBase64, Integer keyVersion, String secretUuid) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(volUuid) ||
                StringUtils.isBlank(dekBase64) || keyVersion == null) {
            throw new OperationFailureException(operr(
                    "defineLibvirtSecretOnHost requires non-blank hostUuid, volUuid, dekBase64 and a non-null keyVersion"));
        }
        SecretHostDefineMsg defineMsg = new SecretHostDefineMsg();
        defineMsg.setHostUuid(hostUuid);
        defineMsg.setVmUuid(vmUuid);
        defineMsg.setDekBase64(dekBase64);
        defineMsg.setPurpose("volume");
        defineMsg.setKeyVersion(keyVersion);
        defineMsg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        if (StringUtils.isNotBlank(secretUuid)) {
            defineMsg.setSecretUuid(secretUuid);
        }
        defineMsg.setDescription(String.format("LUKS DEK for volume %s", volUuid));
        bus.makeTargetServiceIdByResourceUuid(defineMsg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(defineMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to ensure libvirt secret for encrypted volume[uuid:%s] on host[uuid:%s]",
                    volUuid, hostUuid).withCause(reply.getError()));
        }
        SecretHostDefineReply r = reply.castReply();
        if (StringUtils.isBlank(r.getSecretUuid())) {
            throw new OperationFailureException(operr(
                    "ensure volume LUKS secret on host succeeded but secretUuid is empty, host[uuid:%s]",
                    hostUuid));
        }

        // Remember which host now owns this volume's libvirt secret so that
        // expunge can clean it up later, even if the owning VM is gone by then.
        // recreate=true overwrites any stale tag from a previous host.
        try {
            SystemTagCreator tc = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.newSystemTagCreator(volUuid);
            tc.setTagByTokens(Collections.singletonMap(
                    VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN, hostUuid));
            tc.inherent = false;
            tc.recreate = true;
            tc.create();
        } catch (RuntimeException tagEx) {
            // Tag write failure must not break the actual secret define -- the
            // define already succeeded, the tag is for cleanup bookkeeping only.
            logger.warn(String.format(
                    "failed to stamp VOLUME_LIBVIRT_SECRET_HOST tag on volume[uuid:%s] for host[uuid:%s]: %s",
                    volUuid, hostUuid, tagEx.getMessage()));
        }

        return r.getSecretUuid();
    }

    public String lookupVmInstanceUuid(String volumeUuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.vmInstanceUuid)
                .findValue();
    }

    /**
     * Materialize the DEK for {@code volUuid} under the binding in
     * {@code kpUuid}, then define+set-value the libvirt secret on
     * {@code hostUuid}. Used by both the create-time path and the start-time
     * fallback when the host's libvirt secret value was lost.
     */
    public String defineSecretFromBinding(String hostUuid, String vmUuid, String volUuid, String kpUuid) {
        return defineSecretFromBinding(hostUuid, vmUuid, volUuid, kpUuid, null);
    }

    public String defineSecretFromBinding(String hostUuid, String vmUuid, String volUuid, String kpUuid, String secretUuid) {
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s] has no key provider binding; cannot define libvirt secret on host[uuid:%s]",
                    volUuid, hostUuid));
        }
        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(volUuid, kpUuid);
        String dekBase64 = keyResult.getDekBase64();
        if (StringUtils.isBlank(dekBase64)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s]: key manager returned empty DEK for libvirt secret",
                    volUuid));
        }
        return defineLibvirtSecretOnHost(hostUuid, vmUuid, volUuid, dekBase64, keyResult.getKeyVersion(), secretUuid);
    }

    /**
     * Ask {@code hostUuid} for the libvirt secret UUID identified by the
     * per-volume usage instance and keyVersion. Returns null on SECRET_NOT_FOUND
     * so callers can fall back to {@link #defineSecretFromBinding}; throws on
     * any other failure. {@code vmUuid} is still passed to satisfy the RPC
     * contract, but key-agent does not include it in volume secret usage names.
     */
    public String getSecretOnHost(String hostUuid, String vmUuid, String volUuid, Integer keyVersion) {
        SecretHostGetMsg msg = new SecretHostGetMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose("volume");
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (reply.isSuccess()) {
            SecretHostGetReply r = reply.castReply();
            return r.getSecretUuid();
        }
        ErrorCode err = reply.getError();
        if (err != null && SecretHostGetReply.ERROR_CODE_SECRET_NOT_FOUND.equals(err.getCode())) {
            return null;
        }
        throw new OperationFailureException(operr(
                "failed to get libvirt LUKS secret on host[uuid:%s] vm[uuid:%s] volume[uuid:%s] keyVersion[%s]: %s",
                hostUuid, vmUuid, volUuid, keyVersion, err));
    }

    public void deleteSecretOnHostBestEffort(String hostUuid, String vmUuid, String volUuid, Integer keyVersion) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(vmUuid)
                || StringUtils.isBlank(volUuid) || keyVersion == null) {
            return;
        }
        SecretHostDeleteMsg msg = new SecretHostDeleteMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose("volume");
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format(
                    "best-effort delete libvirt LUKS secret failed for volume[uuid:%s] on host[uuid:%s] vm[uuid:%s]: %s",
                    volUuid, hostUuid, vmUuid, reply.getError()));
        }
    }

    /**
     * One-shot resolver for the steady-state start / attach path: reuse an
     * existing host-side libvirt secret when present, otherwise materialize the
     * bound volume key and define the secret on {@code hostUuid}.
     */
    public String resolveOrDefineSecretForVolume(String hostUuid, String vmUuid, String volUuid) {
        Integer keyVersion = volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volUuid);
        if (keyVersion == null) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s] has no key version bound (EncryptedResourceKeyRefVO missing);" +
                            " cannot resolve libvirt LUKS secret on host[uuid:%s] for vm[uuid:%s]",
                    volUuid, hostUuid, vmUuid));
        }
        String secretUuid = getSecretOnHost(hostUuid, vmUuid, volUuid, keyVersion);
        if (StringUtils.isNotBlank(secretUuid)) {
            return secretUuid;
        }
        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
        return defineSecretFromBinding(hostUuid, vmUuid, volUuid, kpUuid);
    }

    private String resolveVolumeLibvirtSecretUuidFromDomainXml(String hostUuid, String vmUuid, String volUuid) {
        KVMAgentCommands.ResolveVolumeLibvirtSecretCmd cmd = new KVMAgentCommands.ResolveVolumeLibvirtSecretCmd();
        cmd.setVmUuid(vmUuid);
        cmd.setVolumeUuid(volUuid);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(KVMConstant.KVM_VOLUME_RESOLVE_LIBVIRT_SECRET_UUID_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format(
                    "failed to resolve volume LUKS secret UUID from domain XML, host[uuid:%s], vm[uuid:%s], volume[uuid:%s]: %s",
                    hostUuid, vmUuid, volUuid, reply.getError()));
            return null;
        }

        KVMHostAsyncHttpCallReply kReply = reply.castReply();
        KVMAgentCommands.ResolveVolumeLibvirtSecretResponse rsp =
                kReply.toResponse(KVMAgentCommands.ResolveVolumeLibvirtSecretResponse.class);
        if (rsp != null && rsp.isSuccess() && StringUtils.isNotBlank(rsp.getSecretUuid())) {
            return rsp.getSecretUuid();
        }

        String err = rsp == null ? "empty agent response" : rsp.getError();
        logger.warn(String.format(
                "volume LUKS secret UUID is not available from domain XML, host[uuid:%s], vm[uuid:%s], volume[uuid:%s]: %s",
                hostUuid, vmUuid, volUuid, err));
        return null;
    }

    public String resolveOrDefineSecretForVolumeMigration(String srcHostUuid, String dstHostUuid, String vmUuid, String volUuid) {
        if (StringUtils.isBlank(srcHostUuid) || StringUtils.isBlank(dstHostUuid)
                || StringUtils.isBlank(vmUuid) || StringUtils.isBlank(volUuid)) {
            throw new OperationFailureException(operr(
                    "resolve migration LUKS secret requires non-blank srcHostUuid, dstHostUuid, vmUuid and volUuid"));
        }

        Integer keyVersion = volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volUuid);
        if (keyVersion == null) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s] has no key version bound (EncryptedResourceKeyRefVO missing);" +
                            " cannot resolve migration LUKS secret for vm[uuid:%s]",
                    volUuid, vmUuid));
        }

        String sourceSecretUuid = resolveVolumeLibvirtSecretUuidFromDomainXml(srcHostUuid, vmUuid, volUuid);
        if (StringUtils.isNotBlank(sourceSecretUuid)) {
            logger.info(String.format(
                    "resolved source volume LUKS secret UUID from domain XML before migration, vm[uuid:%s], volume[uuid:%s], host[uuid:%s], secretUuid:%s",
                    vmUuid, volUuid, srcHostUuid, sourceSecretUuid));
        }
        if (StringUtils.isBlank(sourceSecretUuid)) {
            sourceSecretUuid = getSecretOnHost(srcHostUuid, vmUuid, volUuid, keyVersion);
        }
        if (StringUtils.isBlank(sourceSecretUuid)) {
            String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
            sourceSecretUuid = defineSecretFromBinding(srcHostUuid, vmUuid, volUuid, kpUuid);
        }

        String destSecretUuid = getSecretOnHost(dstHostUuid, vmUuid, volUuid, keyVersion);
        if (StringUtils.equals(destSecretUuid, sourceSecretUuid)) {
            return destSecretUuid;
        }

        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
        return defineSecretFromBinding(dstHostUuid, vmUuid, volUuid, kpUuid, sourceSecretUuid);
    }
}
