package org.zstack.crypto.keyprovider;

import io.grpc.StatusRuntimeException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.SkipResetGlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderEvent;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderEvent;
import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderMsg;
import org.zstack.crypto.keyprovider.api.APIRekeyKeyProviderRefsEvent;
import org.zstack.crypto.keyprovider.api.APIRekeyKeyProviderRefsMsg;
import org.zstack.crypto.keyprovider.api.RekeyFailedResource;
import org.zstack.crypto.keyprovider.api.RekeyProviderResult;
import org.zstack.crypto.keyprovider.api.RekeySkippedResource;
import org.zstack.crypto.keyprovider.kms.KmsHealthTracker;
import org.zstack.crypto.keyprovider.kms.api.APICreateKmsEvent;
import org.zstack.crypto.keyprovider.kms.api.APICreateKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIDeleteKmsEvent;
import org.zstack.crypto.keyprovider.kms.api.APIDeleteKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIGetKmsServerCertFromKmsEvent;
import org.zstack.crypto.keyprovider.kms.api.APIGetKmsServerCertFromKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsEvent;
import org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrEvent;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientIdentityEvent;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientIdentityMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientSignedCertEvent;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientSignedCertMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsServerCertEvent;
import org.zstack.crypto.keyprovider.kms.api.APIUploadKmsServerCertMsg;
import org.zstack.crypto.keyprovider.nkp.NkpHealthTracker;
import org.zstack.crypto.keyprovider.nkp.api.APIBackupNkpEvent;
import org.zstack.crypto.keyprovider.nkp.api.APIBackupNkpMsg;
import org.zstack.crypto.keyprovider.nkp.api.APICreateNkpEvent;
import org.zstack.crypto.keyprovider.nkp.api.APICreateNkpMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIDeleteNkpEvent;
import org.zstack.crypto.keyprovider.nkp.api.APIDeleteNkpMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreEvent;
import org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpEvent;
import org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIUpdateNkpEvent;
import org.zstack.crypto.keyprovider.nkp.api.APIUpdateNkpMsg;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.keyprovider.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.persistence.metamodel.SingularAttribute;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;

public class KeyProviderManagerImpl extends AbstractService implements KeyProviderManager, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(KeyProviderManagerImpl.class);
    private static final long KEY_PROVIDER_NAME_LOCK_TIMEOUT = TimeUnit.MINUTES.toSeconds(1);
    private static final int KMS_CERT_FETCH_TIMEOUT_MS = 5000;
    private static final String REKEY_SYNC_SIGNATURE = "key-provider-rekey-global";
    private static final int NKP_ROOT_KEY_SYNC_RETRY_TIMES = 6;
    private static final long NKP_ROOT_KEY_SYNC_RETRY_INTERVAL_MS = 5000L;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private KmsHealthTracker kmsHealthTracker;
    @Autowired
    private NkpHealthTracker nkpHealthTracker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ResourceDestinationMaker destMaker;

    private Map<String, KeyProviderFactory> keyProviderFactories = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pendingNkpRootKeySyncTargets = new ConcurrentHashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncNkpRootKeyMsg) {
            handle((SyncNkpRootKeyMsg) msg);
        } else if (msg instanceof TriggerKeyProviderHealthCheckMsg) {
            handle((TriggerKeyProviderHealthCheckMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateKmsMsg) {
            handle((APICreateKmsMsg) msg);
        } else if (msg instanceof APICreateNkpMsg) {
            handle((APICreateNkpMsg) msg);
        } else if (msg instanceof APIUpdateKmsMsg) {
            handle((APIUpdateKmsMsg) msg);
        } else if (msg instanceof APIUploadKmsClientIdentityMsg) {
            handle((APIUploadKmsClientIdentityMsg) msg);
        } else if (msg instanceof APIUploadKmsClientCsrMsg) {
            handle((APIUploadKmsClientCsrMsg) msg);
        } else if (msg instanceof APIUploadKmsClientSignedCertMsg) {
            handle((APIUploadKmsClientSignedCertMsg) msg);
        } else if (msg instanceof APIUploadKmsServerCertMsg) {
            handle((APIUploadKmsServerCertMsg) msg);
        } else if (msg instanceof APIGetKmsServerCertFromKmsMsg) {
            handle((APIGetKmsServerCertFromKmsMsg) msg);
        } else if (msg instanceof APIUpdateNkpMsg) {
            handle((APIUpdateNkpMsg) msg);
        } else if (msg instanceof APIDeleteKeyProviderMsg) {
            handle((APIDeleteKeyProviderMsg) msg);
        } else if (msg instanceof APIBackupNkpMsg) {
            handle((APIBackupNkpMsg) msg);
        } else if (msg instanceof APIParseNkpRestoreMsg) {
            handle((APIParseNkpRestoreMsg) msg);
        } else if (msg instanceof APIRestoreNkpMsg) {
            handle((APIRestoreNkpMsg) msg);
        } else if (msg instanceof APIRekeyKeyProviderRefsMsg) {
            handle((APIRekeyKeyProviderRefsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateKmsMsg msg) {
        APICreateKmsEvent event = new APICreateKmsEvent(msg.getId());
        handleCreate(msg, event);
        if (event.getInventory() != null) {
            triggerKmsHealthCheck(event.getInventory().getUuid());
        }
        bus.publish(event);
    }

    private void handle(APICreateNkpMsg msg) {
        APICreateNkpEvent event = new APICreateNkpEvent(msg.getId());
        handleCreate(msg, event);
        if (event.getInventory() != null) {
            triggerNkpHealthCheck(event.getInventory().getUuid());
        }
        bus.publish(event);
    }

    private void handle(APIUpdateKmsMsg msg) {
        APIUpdateKmsEvent event = new APIUpdateKmsEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()),
                () -> {
                    event.setInventory(updateKeyProvider(msg, msg.getUuid()));
                    triggerKmsHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIUploadKmsClientIdentityMsg msg) {
        APIUploadKmsClientIdentityEvent event = new APIUploadKmsClientIdentityEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()),
                () -> {
                    event.setInventory(KmsIdentityInventory.valueOf(uploadKmsClientIdentity(msg)));
                    triggerKmsHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIUploadKmsClientCsrMsg msg) {
        APIUploadKmsClientCsrEvent event = new APIUploadKmsClientCsrEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()),
                () -> {
                    event.setInventory(KmsIdentityInventory.valueOf(uploadKmsClientCsr(msg)));
                    triggerKmsHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIUploadKmsClientSignedCertMsg msg) {
        APIUploadKmsClientSignedCertEvent event = new APIUploadKmsClientSignedCertEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()),
                () -> {
                    event.setInventory(KmsIdentityInventory.valueOf(uploadKmsClientSignedCert(msg)));
                    triggerKmsHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIUploadKmsServerCertMsg msg) {
        APIUploadKmsServerCertEvent event = new APIUploadKmsServerCertEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()),
                () -> {
                    event.setInventory(KmsInventory.valueOf(uploadKmsServerCert(msg)));
                    triggerKmsHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIGetKmsServerCertFromKmsMsg msg) {
        APIGetKmsServerCertFromKmsEvent event = new APIGetKmsServerCertFromKmsEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()), () -> {
            KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
            X509Certificate cert = fetchKmsServerCertificate(kms);
            String certPem = KeyProviderUtils.encodeCertPem(cert);
            event.setServerCertPem(certPem);
            boolean selfSigned = KeyProviderUtils.isSelfSignedCertificate(cert);
            event.setSelfSigned(selfSigned);
            event.setServerCertInfo(KeyProviderUtils.getCertificateInfo(cert));
        }, event);
    }

    private void handle(APIUpdateNkpMsg msg) {
        APIUpdateNkpEvent event = new APIUpdateNkpEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.NKP, msg.getUuid()),
                () -> {
                    event.setInventory(updateKeyProvider(msg, msg.getUuid()));
                    triggerNkpHealthCheck(msg.getUuid());
                }, event);
    }

    private void handle(APIDeleteKeyProviderMsg msg) {
        APIDeleteKeyProviderEvent event;
        if (msg instanceof APIDeleteKmsMsg) {
            event = new APIDeleteKmsEvent(msg.getId());
            submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.KMS, msg.getUuid()), () -> deleteKeyProvider(msg), event);
        } else if (msg instanceof APIDeleteNkpMsg) {
            event = new APIDeleteNkpEvent(msg.getId());
            submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.NKP, msg.getUuid()), () -> deleteKeyProvider(msg), event);
        } else {
            throw new CloudRuntimeException(String.format("unknown delete key provider message[%s]", msg.getClass().getName()));
        }
    }

    private void handle(APIBackupNkpMsg msg) {
        APIBackupNkpEvent event = new APIBackupNkpEvent(msg.getId());
        submitKeyProviderTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.NKP, msg.getUuid()), () -> {
            NkpVO vo = dbf.findByUuid(msg.getUuid(), NkpVO.class);
            if (vo != null && !vo.isBackedUp()) {
                vo.setBackedUp(true);
                vo = dbf.updateAndRefresh(vo);
            }
            event.setContent(vo == null ? null : KeyToolGrpcClient.packNkp(msg.getId(), vo.getName(), vo.getUuid(), msg.getPassword(), NkpRestoreInfo.valueOf(vo)));
        }, event);
    }

    private void handle(APIParseNkpRestoreMsg msg) {
        APIParseNkpRestoreEvent event = new APIParseNkpRestoreEvent(msg.getId());
        event.setCode(msg.getParseCode());
        event.setReason(msg.getParseReason());
        event.setRestoreInfo(msg.getRestoreInfo());
        bus.publish(event);
    }

    private void handle(APIRestoreNkpMsg msg) {
        APIRestoreNkpEvent event = new APIRestoreNkpEvent(msg.getId());
        NkpRestoreInfo content = msg.getRestoreInfo();
        if (content == null) {
            throw new OperationFailureException(operr("nkp restore info is empty"));
        }
        GLock lock = new GLock(getKeyProviderNameLock(content.getName()), KEY_PROVIDER_NAME_LOCK_TIMEOUT);
        lock.lock();
        try {
            if (KeyProviderUtils.isNameExists(content.getName())) {
                throw new OperationFailureException(operr("key provider name[%s] already exists", content.getName()));
            }
            boolean imported = false;
            NkpVO created = null;
            try {
                KeyToolGrpcClient.importNkp(msg.getId(), msg.getContentBase64(), msg.getPassword());
                imported = true;
                created = dbf.persistAndRefresh(buildNkpFromRestore(content));
                syncNkpRootKeyToPeerNodes(created, msg.getId());
                setDefaultKeyProviderIfNeeded(created);
                updateProviderUuidByProviderNameAfterNkpRestore(created.getUuid(), created.getName());
                event.setInventory(NkpInventory.valueOf(created));
                triggerNkpHealthCheck(created.getUuid());
            } catch (Throwable t) {
                if (created != null) {
                    dbf.removeByPrimaryKey(created.getUuid(), KeyProviderVO.class);
                }
                if (imported) {
                    try {
                        KeyToolGrpcClient.removeNkp(msg.getId(), content.getName(), content.getUuid());
                    } catch (Throwable cleanupError) {
                        logger.warn(String.format("failed to rollback imported nkp backend[name:%s, uuid:%s]: %s",
                                content.getName(), content.getUuid(), cleanupError.getMessage()));
                    }
                }
                if (t instanceof OperationFailureException) {
                    throw (OperationFailureException) t;
                }
                throw new OperationFailureException(operr("failed to restore nkp").withException(t.getMessage()));
            }
        } finally {
            lock.unlock();
        }
        bus.publish(event);
    }

    private void updateProviderUuidByProviderNameAfterNkpRestore(String providerUuid, String providerName) {
        if (StringUtils.isBlank(providerUuid) || StringUtils.isBlank(providerName)) {
            return;
        }

        try {
            int updated = SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.providerName, providerName)
                    .isNull(EncryptedResourceKeyRefVO_.providerUuid)
                    .set(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                    .set(EncryptedResourceKeyRefVO_.lastOpDate, null)
                    .update();
            if (updated > 0) {
                logger.info(String.format(
                        "repaired EncryptedResourceKeyRef.providerUuid after NKP restore, provider[name:%s, uuid:%s], rows:%d",
                        providerName, providerUuid, updated));
            }
        } catch (Exception e) {
            logger.warn(String.format(
                    "failed to repair EncryptedResourceKeyRef.providerUuid after NKP restore, provider[name:%s, uuid:%s]: %s",
                    providerName, providerUuid, e.getMessage()), e);
        }
    }

    private void triggerKmsHealthCheck(String kmsUuid) {
        if (kmsHealthTracker == null || StringUtils.isBlank(kmsUuid)) {
            return;
        }

        TriggerKeyProviderHealthCheckMsg msg = new TriggerKeyProviderHealthCheckMsg();
        msg.setProviderUuid(kmsUuid);
        msg.setProviderType(KeyProviderType.KMS.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, KeyProviderConstant.SERVICE_ID, kmsUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to trigger kms health check[uuid:%s], because %s",
                    kmsUuid, reply.getError().getReadableDetails()));
        }
    }

    private void triggerNkpHealthCheck(String nkpUuid) {
        if (nkpHealthTracker == null || StringUtils.isBlank(nkpUuid)) {
            return;
        }

        TriggerKeyProviderHealthCheckMsg msg = new TriggerKeyProviderHealthCheckMsg();
        msg.setProviderUuid(nkpUuid);
        msg.setProviderType(KeyProviderType.NKP.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, KeyProviderConstant.SERVICE_ID, nkpUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to trigger nkp health check[uuid:%s], because %s",
                    nkpUuid, reply.getError().getReadableDetails()));
        }
    }

    private void handle(TriggerKeyProviderHealthCheckMsg msg) {
        TriggerKeyProviderHealthCheckReply reply = new TriggerKeyProviderHealthCheckReply();
        if (StringUtils.isBlank(msg.getProviderUuid()) || StringUtils.isBlank(msg.getProviderType())) {
            reply.setError(argerr("providerUuid or providerType is empty"));
            bus.reply(msg, reply);
            return;
        }

        try {
            KeyProviderType type = KeyProviderType.valueOf(msg.getProviderType());
            if (type == KeyProviderType.KMS) {
                if (kmsHealthTracker == null) {
                    reply.setError(operr("kms health tracker is not available"));
                } else {
                    kmsHealthTracker.triggerHealthCheck(msg.getProviderUuid());
                }
            } else if (type == KeyProviderType.NKP) {
                if (nkpHealthTracker == null) {
                    reply.setError(operr("nkp health tracker is not available"));
                } else {
                    nkpHealthTracker.triggerHealthCheck(msg.getProviderUuid());
                }
            } else {
                reply.setError(operr("unsupported key provider type[%s]", msg.getProviderType()));
            }
        } catch (IllegalArgumentException e) {
            reply.setError(argerr("invalid key provider type[%s]", msg.getProviderType()).withException(e.getMessage()));
        } catch (Throwable t) {
            reply.setError(operr("failed to trigger key provider health check[uuid:%s, type:%s]",
                    msg.getProviderUuid(), msg.getProviderType()).withException(t.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void handle(APIRekeyKeyProviderRefsMsg msg) {
        APIRekeyKeyProviderRefsEvent event = new APIRekeyKeyProviderRefsEvent(msg.getId());
        // Serialize all rekey requests to avoid concurrent overwrite on same refs.
        submitKeyProviderTask(msg, REKEY_SYNC_SIGNATURE,
                () -> {
                    try {
                        applyRekeyResult(msg, event, rekeyKeyProviderRefs(msg));
                    } catch (RekeyPartialFailureException e) {
                        applyRekeyResult(msg, event, e.result);
                        throw e;
                    }
                }, event);
    }

    private void handle(SyncNkpRootKeyMsg msg) {
        SyncNkpRootKeyReply reply = new SyncNkpRootKeyReply();
        submitLocalTask(msg, KeyProviderUtils.getSyncSignature(KeyProviderType.NKP, msg.getProviderUuid()), () -> {
            if (StringUtils.isBlank(msg.getContentBase64())) {
                throw new OperationFailureException(operr("sync nkp root key content is empty"));
            }
            KeyToolGrpcClient.importNkpRootKey(msg.getRequestId(), msg.getContentBase64());
        }, reply);
    }

    private RekeyRefsResult rekeyKeyProviderRefs(APIRekeyKeyProviderRefsMsg msg) {
        KeyProviderVO targetProvider = KeyProviderRekeyValidator.getAvailableTargetProvider(msg.getProviderUuid());

        List<EncryptedResourceKeyRefVO> refs = findRefsForRekey(msg);
        RekeyRefsResult result = new RekeyRefsResult();
        if (refs.isEmpty()) {
            return result;
        }

        NkpConfig targetNkp = null;
        KmipConfig targetKmip = null;
        if (targetProvider.getType() == KeyProviderType.NKP) {
            NkpVO targetNkpVo = (NkpVO) targetProvider;
            targetNkp = new NkpConfig(targetNkpVo.getName(), targetNkpVo.getUuid());
        } else if (targetProvider.getType() == KeyProviderType.KMS) {
            targetKmip = buildKmipConfig((KmsVO) targetProvider, "target");
        } else {
            throw new OperationFailureException(operr("unsupported target key provider type[%s]", targetProvider.getType()));
        }

        for (EncryptedResourceKeyRefVO ref : refs) {
            RekeyProviderResult providerResult = getOrCreateProviderResult(result, ref);
            providerResult.setTotalRefCount(providerResult.getTotalRefCount() + 1);

            ErrorCode skipError = getSkipErrorForRef(ref);
            if (skipError != null) {
                RekeySkippedResource skipped = buildSkippedResource(ref, skipError);
                providerResult.getSkippedResources().add(skipped);
                providerResult.setSkippedRefCount(providerResult.getSkippedRefCount() + 1);
                logger.warn(String.format("skip rekey encrypted resource key ref[id:%s, resourceType:%s, resourceUuid:%s]: %s",
                        ref.getId(), ref.getResourceType(), ref.getResourceUuid(), skipError.getReadableDetails()));
                continue;
            }

            String oldKekRef = ref.getKekRef();
            String oldProviderUuid = ref.getProviderUuid();
            String oldProviderName = ref.getProviderName();
            Integer oldKeyVersion = ref.getKeyVersion();
            RekeySecretResult rekeyResult;
            try {
                rekeyResult = targetNkp != null
                        ? rekeySingleRefToNkp(ref, targetProvider, targetNkp, msg.getId())
                        : rekeySingleRefToKms(ref, targetProvider, targetKmip, msg.getId());
            } catch (OperationFailureException | StatusRuntimeException e) {
                String failReason = getReadableSkipReason(e);
                RekeyFailedResource failed = buildFailedResource(ref, failReason);
                providerResult.getFailedResources().add(failed);
                providerResult.setFailedRefCount(providerResult.getFailedRefCount() + 1);
                logger.warn(String.format("rekey failed for encrypted resource key ref[id:%s, resourceType:%s, resourceUuid:%s]: %s",
                        ref.getId(), ref.getResourceType(), ref.getResourceUuid(), failReason));
                continue;
            }

            try {
                applyRekeySecretResult(ref, rekeyResult);
                dbf.update(ref);
                providerResult.setSuccessRefCount(providerResult.getSuccessRefCount() + 1);
            } catch (Throwable t) {
                ref.setKekRef(oldKekRef);
                ref.setProviderUuid(oldProviderUuid);
                ref.setProviderName(oldProviderName);
                ref.setKeyVersion(oldKeyVersion);
                try {
                    rollbackRekeyedSecret(ref, rekeyResult, t);
                } catch (OperationFailureException e) {
                    RekeyFailedResource failed = buildFailedResource(ref, String.format("failed to persist rekey result: %s",
                            e.getErrorCode().getReadableDetails()));
                    providerResult.getFailedResources().add(failed);
                    providerResult.setFailedRefCount(providerResult.getFailedRefCount() + 1);
                    throw new RekeyPartialFailureException(e.getErrorCode(), result);
                }

                if (t instanceof OperationFailureException) {
                    RekeyFailedResource failed = buildFailedResource(ref, String.format("failed to persist rekey result: %s",
                            ((OperationFailureException) t).getErrorCode().getReadableDetails()));
                    providerResult.getFailedResources().add(failed);
                    providerResult.setFailedRefCount(providerResult.getFailedRefCount() + 1);
                    throw new RekeyPartialFailureException(((OperationFailureException) t).getErrorCode(), result);
                }
                ErrorCode persistError = operr("failed to persist rekey result for encrypted resource key ref[id:%s, resourceType:%s, resourceUuid:%s]",
                        ref.getId(), ref.getResourceType(), ref.getResourceUuid()).withException(t.getMessage());
                RekeyFailedResource failed = buildFailedResource(ref, String.format("failed to persist rekey result: %s",
                        persistError.getReadableDetails()));
                providerResult.getFailedResources().add(failed);
                providerResult.setFailedRefCount(providerResult.getFailedRefCount() + 1);
                throw new RekeyPartialFailureException(persistError, result);
            }
        }
        return result;
    }

    private String getReadableSkipReason(Throwable t) {
        if (t instanceof OperationFailureException) {
            return ((OperationFailureException) t).getErrorCode().getReadableDetails();
        }
        if (t instanceof StatusRuntimeException) {
            StatusRuntimeException e = (StatusRuntimeException) t;
            String description = e.getStatus() == null ? null : e.getStatus().getDescription();
            return StringUtils.defaultIfBlank(description, e.getMessage());
        }
        return StringUtils.defaultIfBlank(t.getMessage(), t.getClass().getSimpleName());
    }

    private ErrorCode getSkipErrorForRef(EncryptedResourceKeyRefVO ref) {
        if (StringUtils.isBlank(ref.getProviderUuid()) && StringUtils.isBlank(ref.getProviderName())) {
            return operr("not bound to any key provider");
        }
        if (StringUtils.isBlank(ref.getKekRef())) {
            return operr("empty secret ref");
        }

        // TODO: this is a workaround in issue ZSV-11845.
        //       You'd better to delete EncryptedResourceKeyRefVO when deleting resource.
        //       We will refactor here soon.
        if (!Q.New(ResourceVO.class)
                .eq(ResourceVO_.uuid, ref.getResourceUuid())
                .isExists()) {
            SQL.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.id, ref.getId())
                    .delete();
            return operr("skipped: orphan key ref cleaned, resource[uuid:%s] already deleted", ref.getResourceUuid());
        }
        return null;
    }

    private void applyRekeyResult(APIRekeyKeyProviderRefsMsg msg, APIRekeyKeyProviderRefsEvent event, RekeyRefsResult result) {
        Map<String, RekeyProviderResult> sourceProviderResultMap = new LinkedHashMap<>();
        for (RekeyProviderResult hit : result.providerResults) {
            String providerKey = String.format("%s|%s", hit.getProviderUuid(), hit.getProviderName());
            sourceProviderResultMap.putIfAbsent(providerKey, hit);
        }

        // Use providers associated by matched resource refs only.
        List<RekeyProviderResult> completedProviderResults = new ArrayList<>(sourceProviderResultMap.values());

        int totalProviderCount = completedProviderResults.size();
        int failedProviderCount = (int) completedProviderResults.stream().filter(pr -> pr.getFailedRefCount() > 0).count();
        int skippedProviderCount = (int) completedProviderResults.stream().filter(pr -> pr.getSkippedRefCount() > 0).count();
        int successProviderCount = totalProviderCount - failedProviderCount;

        event.setTotalCount(totalProviderCount);
        event.setSuccessCount(successProviderCount);
        event.setSkippedCount(skippedProviderCount);
        event.setFailedCount(failedProviderCount);
        event.setProviderResults(completedProviderResults);
    }

    private RekeyProviderResult getOrCreateProviderResult(RekeyRefsResult result, EncryptedResourceKeyRefVO ref) {
        String providerUuid = StringUtils.defaultIfBlank(ref.getProviderUuid(), "UNBOUND");
        String providerName = StringUtils.defaultIfBlank(ref.getProviderName(), "UNBOUND");
        String key = String.format("%s|%s", providerUuid, providerName);
        RekeyProviderResult providerResult = result.providerResultMap.get(key);
        if (providerResult != null) {
            return providerResult;
        }
        RekeyProviderResult created = new RekeyProviderResult();
        created.setProviderUuid(providerUuid);
        created.setProviderName(providerName);
        result.providerResultMap.put(key, created);
        result.providerResults.add(created);
        return created;
    }

    private RekeySkippedResource buildSkippedResource(EncryptedResourceKeyRefVO ref, ErrorCode errorCode) {
        RekeySkippedResource skipped = new RekeySkippedResource();
        skipped.setKeyRefId(ref.getId());
        skipped.setResourceType(ref.getResourceType());
        skipped.setResourceUuid(ref.getResourceUuid());
        skipped.setReason(errorCode.getReadableDetails());
        return skipped;
    }

    private RekeyFailedResource buildFailedResource(EncryptedResourceKeyRefVO ref, String reason) {
        RekeyFailedResource failed = new RekeyFailedResource();
        failed.setKeyRefId(ref.getId());
        failed.setResourceType(ref.getResourceType());
        failed.setResourceUuid(ref.getResourceUuid());
        failed.setReason(reason);
        return failed;
    }

    private List<EncryptedResourceKeyRefVO> findRefsForRekey(APIRekeyKeyProviderRefsMsg msg) {
        Map<Long, EncryptedResourceKeyRefVO> selected = new LinkedHashMap<>();

        List<Long> refIds = msg.getRefIds();
        if (!CollectionUtils.isEmpty(refIds)) {
            List<EncryptedResourceKeyRefVO> refsById = Q.New(EncryptedResourceKeyRefVO.class)
                    .in(EncryptedResourceKeyRefVO_.id, refIds)
                    .list();
            for (EncryptedResourceKeyRefVO ref : refsById) {
                selected.put(ref.getId(), ref);
            }

            Set<Long> missing = new LinkedHashSet<>();
            for (Long refId : refIds) {
                if (!selected.containsKey(refId)) {
                    missing.add(refId);
                }
            }
            if (!missing.isEmpty()) {
                throw new OperationFailureException(operr("cannot find encrypted resource key refs: %s", missing));
            }
        }

        if (StringUtils.isNotBlank(msg.getResourceType()) && !CollectionUtils.isEmpty(msg.getResourceUuids())) {
            List<EncryptedResourceKeyRefVO> refsByResource = findRefsByResource(msg.getResourceType(), msg.getResourceUuids());
            for (EncryptedResourceKeyRefVO ref : refsByResource) {
                selected.put(ref.getId(), ref);
            }
        }

        if (msg.isRekeyAll()) {
            List<EncryptedResourceKeyRefVO> allRefs = Q.New(EncryptedResourceKeyRefVO.class).list();
            for (EncryptedResourceKeyRefVO ref : allRefs) {
                selected.put(ref.getId(), ref);
            }
        }

        return new ArrayList<>(selected.values());
    }

    private List<EncryptedResourceKeyRefVO> findRefsByResource(String resourceType, List<String> resourceUuids) {
        List<EncryptedResourceKeyRefVO> refs = new ArrayList<>(queryRefsByResource(resourceType, resourceUuids));

        for (KeyProviderRekeyAssociationExtensionPoint ext : pluginRgty.getExtensionList(KeyProviderRekeyAssociationExtensionPoint.class)) {
            if (!resourceType.equals(ext.getType())) {
                continue;
            }
            List<String> associatedResourceUuids = ext.getAssociatedResourceUuids(resourceUuids);
            if (CollectionUtils.isEmpty(associatedResourceUuids)) {
                continue;
            }

            refs.addAll(queryRefsByResource(ext.getAssociatedResourceType(), associatedResourceUuids));
        }
        return refs;
    }

    private List<EncryptedResourceKeyRefVO> queryRefsByResource(String resourceType, List<String> resourceUuids) {
        if (CollectionUtils.isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }
        return Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, resourceType)
                .in(EncryptedResourceKeyRefVO_.resourceUuid, resourceUuids)
                .list();
    }

    private RekeySecretResult rekeySingleRefToNkp(EncryptedResourceKeyRefVO ref, KeyProviderVO targetProvider, NkpConfig targetNkp, String requestId) {
        return rekeySingleRef(ref, targetProvider, targetNkp, null, requestId);
    }

    private RekeySecretResult rekeySingleRefToKms(EncryptedResourceKeyRefVO ref, KeyProviderVO targetProvider, KmipConfig targetKmip, String requestId) {
        return rekeySingleRef(ref, targetProvider, null, targetKmip, requestId);
    }

    private RekeySecretResult rekeySingleRef(EncryptedResourceKeyRefVO ref, KeyProviderVO targetProvider,
                                             NkpConfig targetNkp, KmipConfig targetKmip, String requestId) {
        // Defensive guard in case the caller-side skip filter changes.
        if (StringUtils.isBlank(ref.getKekRef())) {
            throw new OperationFailureException(operr("encrypted resource key ref[id:%s] has empty secret ref", ref.getId()));
        }

        NkpConfig originNkp = null;
        KmipConfig originKmip = null;
        if (isNkpSecretRef(ref.getKekRef())) {
            NkpVO origin = getOriginNkp(ref);
            originNkp = new NkpConfig(origin.getName(), origin.getUuid());
        } else if (isKmipSecretRef(ref.getKekRef())) {
            KmsVO origin = getOriginKms(ref);
            originKmip = buildKmipConfig(origin, "origin");
        } else {
            throw new OperationFailureException(operr("unsupported secret ref format for encrypted resource key ref[id:%s]: %s",
                    ref.getId(), ref.getKekRef()));
        }

        String rekeyResourceType = StringUtils.defaultIfBlank(ref.getResourceType(), "Resource");
        String rekeyResourceId = StringUtils.isNotBlank(ref.getResourceUuid())
                ? ref.getResourceUuid()
                : ("ref-" + ref.getId());
        String rekeyName = String.format("%s-%s-%d", rekeyResourceType, rekeyResourceId, System.currentTimeMillis());
        String newSecretRef = KeyToolGrpcClient.rekeySecret(requestId, ref.getKekRef(), rekeyName,
                targetNkp, targetKmip, originNkp, originKmip);
        return new RekeySecretResult(newSecretRef, targetProvider.getUuid(), targetProvider.getName(),
                ref.getKeyVersion() == null ? 1 : ref.getKeyVersion(), targetNkp, targetKmip);
    }

    private void applyRekeySecretResult(EncryptedResourceKeyRefVO ref, RekeySecretResult result) {
        ref.setProviderUuid(result.targetProviderUuid);
        ref.setProviderName(result.targetProviderName);
        ref.setKekRef(result.newSecretRef);
        ref.setWrappedDek("");
        ref.setKeyVersion(result.newKeyVersion);
    }

    private void rollbackRekeyedSecret(EncryptedResourceKeyRefVO ref, RekeySecretResult result, Throwable cause) {
        if (result == null || StringUtils.isBlank(result.newSecretRef)) {
            return;
        }

        // KMIP supports delete key; NKP does not — skip remote delete when rekey target was NKP.
        if (result.targetKmip == null) {
            return;
        }
        try {
            KeyToolGrpcClient.deleteSecret(Platform.getUuid(),
                    result.newSecretRef, null, result.targetKmip);
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to rollback rekeyed secret for encrypted resource key ref[id:%s, resourceType:%s, resourceUuid:%s]",
                    ref.getId(), ref.getResourceType(), ref.getResourceUuid()).withException(String.format("original error: %s; rollback error: %s",
                    StringUtils.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName()),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()))));
        }
    }

    private NkpVO getOriginNkp(EncryptedResourceKeyRefVO ref) {
        return findOriginProvider(ref, NkpVO.class, NkpVO_.name, "nkp");
    }

    private KmsVO getOriginKms(EncryptedResourceKeyRefVO ref) {
        return findOriginProvider(ref, KmsVO.class, KmsVO_.name, "kms");
    }

    private <T> T findOriginProvider(EncryptedResourceKeyRefVO ref, Class<T> voClass,
                                     SingularAttribute<? super T, String> nameField, String typeName) {
        T origin = null;
        if (StringUtils.isNotBlank(ref.getProviderUuid())) {
            origin = dbf.findByUuid(ref.getProviderUuid(), voClass);
        }

        if (origin == null && StringUtils.isNotBlank(ref.getProviderName())) {
            origin = findUniqueOriginProviderByName(ref, voClass, nameField, typeName);
            if (origin != null) {
                logger.warn(String.format("fallback to origin %s lookup by name for encrypted resource key ref[id:%s, providerUuid:%s, providerName:%s], please verify this is the original provider",
                        typeName, ref.getId(), ref.getProviderUuid(), ref.getProviderName()));
            }
        }

        if (origin == null && StringUtils.isBlank(ref.getProviderUuid())) {
            if (StringUtils.isNotBlank(ref.getProviderName())) {
                throw new OperationFailureException(operr("cannot find origin %s by providerName[%s] for encrypted resource key ref[id:%s]",
                        typeName, ref.getProviderName(), ref.getId()));
            }
            throw new OperationFailureException(operr("encrypted resource key ref[id:%s] missing provider uuid and provider name", ref.getId()));
        }

        if (origin == null) {
            throw new OperationFailureException(operr("cannot find origin %s[uuid:%s, name:%s] for encrypted resource key ref[id:%s]",
                    typeName, ref.getProviderUuid(), ref.getProviderName(), ref.getId()));
        }
        return origin;
    }

    private <T> T findUniqueOriginProviderByName(EncryptedResourceKeyRefVO ref, Class<T> voClass,
                                                 SingularAttribute<? super T, String> nameField, String typeName) {
        List<T> origins = Q.New(voClass)
                .eq(nameField, ref.getProviderName())
                .limit(2)
                .list();
        if (origins.isEmpty()) {
            return null;
        }
        if (origins.size() > 1) {
            throw new OperationFailureException(operr("multiple origin %s found by name[%s] for encrypted resource key ref[id:%s]",
                    typeName, ref.getProviderName(), ref.getId()));
        }
        return origins.get(0);
    }

    private KmipConfig buildKmipConfig(KmsVO kms, String role) {
        KmsIdentityVO identity = kms.getActiveIdentity();
        return KmipConfig.fromKms(kms, identity, role);
    }

    private boolean isNkpSecretRef(String secretRef) {
        return !StringUtils.isBlank(secretRef) && secretRef.startsWith("nkp://");
    }

    private boolean isKmipSecretRef(String secretRef) {
        return !StringUtils.isBlank(secretRef) && secretRef.startsWith("kmip://");
    }

    private static class RekeyRefsResult {
        List<RekeyProviderResult> providerResults = new ArrayList<>();
        Map<String, RekeyProviderResult> providerResultMap = new LinkedHashMap<>();
    }

    private static class RekeySecretResult {
        final String newSecretRef;
        final String targetProviderUuid;
        final String targetProviderName;
        final Integer newKeyVersion;
        final NkpConfig targetNkp;
        final KmipConfig targetKmip;

        private RekeySecretResult(String newSecretRef, String targetProviderUuid, String targetProviderName,
                                  Integer newKeyVersion, NkpConfig targetNkp, KmipConfig targetKmip) {
            this.newSecretRef = newSecretRef;
            this.targetProviderUuid = targetProviderUuid;
            this.targetProviderName = targetProviderName;
            this.newKeyVersion = newKeyVersion;
            this.targetNkp = targetNkp;
            this.targetKmip = targetKmip;
        }
    }

    private static class RekeyPartialFailureException extends OperationFailureException {
        final RekeyRefsResult result;

        private RekeyPartialFailureException(ErrorCode errorCode, RekeyRefsResult result) {
            super(errorCode);
            this.result = result;
        }
    }

    private void submitKeyProviderTask(APIMessage msg, String syncSignature, Runnable task, APIEvent event) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                try {
                    task.run();
                } catch (OperationFailureException e) {
                    event.setError(e.getErrorCode());
                } catch (Throwable t) {
                    event.setError(inerr(t.getMessage()));
                } finally {
                    bus.publish(event);
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void submitLocalTask(Message msg, String syncSignature, Runnable task, MessageReply reply) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                try {
                    task.run();
                } catch (OperationFailureException e) {
                    reply.setError(e.getErrorCode());
                } catch (Throwable t) {
                    reply.setError(inerr(t.getMessage()));
                } finally {
                    bus.reply(msg, reply);
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void deleteKeyProvider(APIDeleteKeyProviderMsg msg) {
        KeyProviderVO vo = dbf.findByUuid(msg.getUuid(), KeyProviderVO.class);
        if (vo != null) {
            if (vo.getType() == KeyProviderType.NKP) {
                KeyToolGrpcClient.removeNkp(msg.getId(), vo.getName(), vo.getUuid());
            }
            dbf.removeByPrimaryKey(vo.getUuid(), KeyProviderVO.class);
            String currentDefault = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
            if (StringUtils.equals(currentDefault, vo.getUuid())) {
                KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.updateValue(KeyProviderGlobalConfig.NONE);
            }
        }
    }

    private KmsIdentityVO uploadKmsClientIdentity(APIUploadKmsClientIdentityMsg msg) {
        KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
        KmsIdentityType identityType = KmsIdentityType.valueOf(msg.getIdentityType());
        KmsIdentityVO identity = KeyProviderUtils.findKmsIdentity(kms.getUuid(), identityType);
        if (identity == null) {
            identity = new KmsIdentityVO();
            identity.setUuid(Platform.getUuid());
            identity.setKmsUuid(kms.getUuid());
            identity.setIdentityType(identityType);
            identity.setClientCertPem(msg.getKmsClientCertPem());
            identity.setClientKeyPem(msg.getKmsClientKeyPem());
            identity.setCertExpiredDate(KeyProviderUtils.parseCertExpiredDate(msg.getKmsClientCertPem()));
            identity = dbf.persistAndRefresh(identity);
        } else {
            identity.setClientCertPem(msg.getKmsClientCertPem());
            identity.setClientKeyPem(msg.getKmsClientKeyPem());
            identity.setCertExpiredDate(KeyProviderUtils.parseCertExpiredDate(msg.getKmsClientCertPem()));
            identity = dbf.updateAndRefresh(identity);
        }
        return identity;
    }

    private KmsIdentityVO uploadKmsClientCsr(APIUploadKmsClientCsrMsg msg) {
        KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
        KmsIdentityType identityType = KmsIdentityType.CSR;
        KmsIdentityVO identity = KeyProviderUtils.findKmsIdentity(kms.getUuid(), identityType);
        if (identity == null) {
            identity = new KmsIdentityVO();
            identity.setUuid(Platform.getUuid());
            identity.setKmsUuid(kms.getUuid());
            identity.setIdentityType(identityType);
            identity.setCsrPem(msg.getCsrPem());
            identity.setClientKeyPem(msg.getCsrKeyPem());
            identity = dbf.persistAndRefresh(identity);
        } else {
            identity.setCsrPem(msg.getCsrPem());
            identity.setClientKeyPem(msg.getCsrKeyPem());
            identity.setClientCertPem(null);
            identity.setCertExpiredDate(null);
            identity = dbf.updateAndRefresh(identity);
        }
        return identity;
    }

    private KmsIdentityVO uploadKmsClientSignedCert(APIUploadKmsClientSignedCertMsg msg) {
        KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
        KmsIdentityType identityType = KmsIdentityType.CSR;
        KmsIdentityVO identity = KeyProviderUtils.findKmsIdentity(kms.getUuid(), identityType);
        if (identity == null) {
            throw new OperationFailureException(operr("csr identity not found for kms[uuid:%s]", kms.getUuid()));
        }
        if (StringUtils.isBlank(identity.getClientKeyPem())) {
            throw new OperationFailureException(operr("csr identity missing client key for kms[uuid:%s]", kms.getUuid()));
        }
        X509Certificate cert;
        PrivateKey privateKey;
        try {
            cert = KeyProviderUtils.parseCertificate(msg.getSignedClientCertPem(), "signedClientCertPem");
            privateKey = KeyProviderUtils.parsePrivateKey(identity.getClientKeyPem(), "csrKeyPem");
            KeyProviderUtils.validateKeyMatchesPublicKey(privateKey, cert.getPublicKey(), "signedClientCertPem");
        } catch (IllegalArgumentException e) {
            throw new OperationFailureException(argerr(e.getMessage()).withException(e.getMessage()));
        }
        identity.setClientCertPem(msg.getSignedClientCertPem());
        identity.setCertExpiredDate(KeyProviderUtils.parseCertExpiredDate(msg.getSignedClientCertPem()));
        identity = dbf.updateAndRefresh(identity);
        return identity;
    }

    private KmsVO uploadKmsServerCert(APIUploadKmsServerCertMsg msg) {
        KmsVO kms = KeyProviderUtils.getKmsByUuid(msg.getUuid());
        kms.setServerCertPem(msg.getServerCertPem());
        kms.setServerCertExpiredDate(KeyProviderUtils.parseCertExpiredDate(msg.getServerCertPem()));
        return dbf.updateAndRefresh(kms);
    }

    private X509Certificate fetchKmsServerCertificate(KmsVO kms) {
        Throwable firstError = null;
        KmsIdentityVO identity = getKmsClientIdentityForTls(kms);
        if (identity != null) {
            try {
                return fetchKmsServerCertificate(kms, identity);
            } catch (Throwable t) {
                firstError = t;
                logger.debug(String.format("failed to fetch kms server certificate with client identity[kmsUuid:%s, identityUuid:%s], fallback to anonymous tls: %s",
                        kms.getUuid(), identity.getUuid(), t.getMessage()));
            }
        }

        try {
            return fetchKmsServerCertificate(kms, null);
        } catch (OperationFailureException e) {
            if (firstError != null) {
                throw new OperationFailureException(operr("failed to get kms server certificate")
                        .withException(String.format("mTLS: %s; anonymous TLS: %s", firstError.getMessage(), e.getErrorCode().getDetails())));
            }
            throw e;
        } catch (Exception e) {
            if (firstError != null) {
                throw new OperationFailureException(operr("failed to get kms server certificate")
                        .withException(String.format("mTLS: %s; anonymous TLS: %s", firstError.getMessage(), e.getMessage())));
            }
            throw new OperationFailureException(operr("failed to get kms server certificate").withException(e.getMessage()));
        }
    }

    private X509Certificate fetchKmsServerCertificate(KmsVO kms, KmsIdentityVO identity) {
        try {
            SSLContext context = buildTlsContext(identity);
            SSLSocketFactory factory = context.getSocketFactory();
            try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
                configureSocket(socket, kms.getEndpoint());
                socket.connect(new InetSocketAddress(kms.getEndpoint(), kms.getPort()), KMS_CERT_FETCH_TIMEOUT_MS);
                socket.setSoTimeout(KMS_CERT_FETCH_TIMEOUT_MS);
                socket.startHandshake();
                SSLSession session = socket.getSession();
                Certificate[] certs = session.getPeerCertificates();
                if (certs == null || certs.length == 0) {
                    throw new OperationFailureException(operr("kms server returned empty certificate chain"));
                }
                if (!(certs[0] instanceof X509Certificate)) {
                    throw new OperationFailureException(operr("kms server certificate is not X509"));
                }
                return (X509Certificate) certs[0];
            }
        } catch (OperationFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailureException(operr("failed to get kms server certificate").withException(e.getMessage()));
        }
    }

    private SSLContext buildTlsContext(KmsIdentityVO identity) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        if (identity == null || StringUtils.isBlank(identity.getClientCertPem()) || StringUtils.isBlank(identity.getClientKeyPem())) {
            context.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            return context;
        }

        X509Certificate cert = KeyProviderUtils.parseCertificate(identity.getClientCertPem(), "kmsClientCertPem");
        PrivateKey key = KeyProviderUtils.parsePrivateKey(identity.getClientKeyPem(), "kmsClientKeyPem");
        KeyProviderUtils.validateKeyMatchesPublicKey(key, cert.getPublicKey(), "kmsClient identity");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "".toCharArray();
        keyStore.load(null, password);
        keyStore.setKeyEntry("kms-client", key, password, new Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        context.init(kmf.getKeyManagers(), new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());
        return context;
    }

    private void configureSocket(SSLSocket socket, String endpoint) {
        List<String> enabled = new ArrayList<>();
        for (String protocol : socket.getSupportedProtocols()) {
            if ("TLSv1.3".equals(protocol) || "TLSv1.2".equals(protocol)) {
                enabled.add(protocol);
            }
        }
        if (!enabled.isEmpty()) {
            socket.setEnabledProtocols(enabled.toArray(new String[0]));
        }

        if (!InetAddressValidator.getInstance().isValid(endpoint)) {
            SSLParameters params = socket.getSSLParameters();
            params.setServerNames(Collections.singletonList(new SNIHostName(endpoint)));
            socket.setSSLParameters(params);
        }
    }

    private KmsIdentityVO getKmsClientIdentityForTls(KmsVO kms) {
        if (StringUtils.isNotBlank(kms.getActiveIdentityUuid())) {
            KmsIdentityVO active = Q.New(KmsIdentityVO.class).eq(KmsIdentityVO_.uuid, kms.getActiveIdentityUuid()).find();
            if (active != null && StringUtils.isNotBlank(active.getClientCertPem()) && StringUtils.isNotBlank(active.getClientKeyPem())) {
                return active;
            }
        }

        KmsIdentityVO uploaded = KeyProviderUtils.findKmsIdentity(kms.getUuid(), KmsIdentityType.UPLOADED);
        if (uploaded != null && StringUtils.isNotBlank(uploaded.getClientCertPem()) && StringUtils.isNotBlank(uploaded.getClientKeyPem())) {
            return uploaded;
        }

        KmsIdentityVO csr = KeyProviderUtils.findKmsIdentity(kms.getUuid(), KmsIdentityType.CSR);
        if (csr != null && StringUtils.isNotBlank(csr.getClientCertPem()) && StringUtils.isNotBlank(csr.getClientKeyPem())) {
            return csr;
        }
        return null;
    }

    private void handleCreate(APICreateKeyProviderMsg msg, APICreateKeyProviderEvent event) {
        GLock lock = new GLock(getKeyProviderNameLock(msg.getName()), KEY_PROVIDER_NAME_LOCK_TIMEOUT);
        lock.lock();
        try {
            if (KeyProviderUtils.isNameExists(msg.getName())) {
                throw new OperationFailureException(operr("key provider name[%s] already exists", msg.getName()));
            }
            KeyProviderType type = KeyProviderType.valueOf(msg.getType());
            KeyProviderFactory factory = getKeyProviderFactory(type);
            KeyProviderVO vo = buildBaseKeyProvider(msg, type);
            KeyProviderVO created = factory.createKeyProvider(vo, msg);
            if (type == KeyProviderType.NKP) {
                try {
                    KeyToolGrpcClient.initNkp(msg.getId(), created.getName(), created.getUuid());
                    syncNkpRootKeyToPeerNodes((NkpVO) created, msg.getId());
                } catch (Throwable t) {
                    try {
                        KeyToolGrpcClient.removeNkp(msg.getId(), created.getName(), created.getUuid());
                    } catch (Throwable cleanupError) {
                        logger.warn(String.format("failed to cleanup nkp backend after create failure[name:%s, uuid:%s]: %s",
                                created.getName(), created.getUuid(), cleanupError.getMessage()));
                    }
                    dbf.removeByPrimaryKey(created.getUuid(), KeyProviderVO.class);
                    if (t instanceof OperationFailureException) {
                        throw (OperationFailureException) t;
                    }
                    throw new OperationFailureException(operr("failed to initialize or sync nkp backend").withException(t.getMessage()));
                }
            }
            setDefaultKeyProviderIfNeeded(created);
            tagMgr.createTagsFromAPICreateMessage(msg, created.getUuid(), KeyProviderVO.class.getSimpleName());
            event.setInventory(factory.valueOf(created));
        } finally {
            lock.unlock();
        }
    }

    private void setDefaultKeyProviderIfNeeded(KeyProviderVO created) {
        if (!KeyProviderUtils.isDefaultKeyProviderUuidEmpty()) {
            return;
        }

        Long count = Q.New(KeyProviderVO.class).count();
        if (count != null && count == 1) {
            KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.updateValue(created.getUuid());
        }
    }

    private void installDefaultKeyProviderResetExtension() {
        KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.installBeforeResetExtension(session -> {
            String currentDefault = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value();
            if (StringUtils.isBlank(currentDefault) || KeyProviderGlobalConfig.NONE.equals(currentDefault)) {
                return;
            }

            boolean defaultProviderExists = Q.New(KeyProviderVO.class)
                    .eq(KeyProviderVO_.uuid, currentDefault)
                    .isExists();
            if (defaultProviderExists) {
                throw new SkipResetGlobalConfigException();
            }
        });
    }

    private NkpVO buildNkpFromRestore(NkpRestoreInfo content) {
        NkpVO vo = new NkpVO();
        vo.setUuid(content.getUuid());
        vo.setName(content.getName());
        vo.setDescription(content.getDescription());
        vo.setType(KeyProviderType.NKP);
        vo.setConnected(true);
        vo.setKdf(StringUtils.isEmpty(content.getKdf()) ? NkpKdf.HKDF_SHA256.toString() : content.getKdf());
        vo.setSaltPolicy(StringUtils.isEmpty(content.getSaltPolicy()) ? NkpSaltPolicy.PROVIDER_NAME.toString() : content.getSaltPolicy());
        vo.setCurrentVersion(content.getCurrentVersion() == null ? 1 : content.getCurrentVersion());
        vo.setBackedUp(true);
        return vo;
    }

    private String getKeyProviderNameLock(String name) {
        return String.format("key-provider-name-%s", StringDSL.getMd5Sum(name));
    }

    private KeyProviderVO buildBaseKeyProvider(APICreateKeyProviderMsg msg, KeyProviderType type) {
        KeyProviderVO vo = new KeyProviderVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(type);
        vo.setConnected(false);
        return vo;
    }

    private void syncNkpRootKeyToPeerNodes(NkpVO nkp, String requestId) {
        List<String> mnUuids = Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.uuid)
                .eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING)
                .listValues();
        if (CollectionUtils.isEmpty(mnUuids)) {
            return;
        }

        String reqId = StringUtils.isBlank(requestId) ? Platform.getUuid() : requestId;
        String contentBase64 = KeyToolGrpcClient.exportNkpRootKey(reqId, nkp);
        Set<String> failedTargets = new LinkedHashSet<>();

        for (String mnUuid : mnUuids) {
            if (StringUtils.equals(mnUuid, Platform.getManagementServerId())) {
                continue;
            }
            try {
                syncNkpRootKeyToNode(nkp, mnUuid, reqId, contentBase64);
            } catch (Throwable t) {
                failedTargets.add(mnUuid);
                logger.warn(String.format("failed to sync nkp root key[providerUuid:%s] to management node[uuid:%s], will retry: %s",
                        nkp.getUuid(), mnUuid, t.getMessage()));
            }
        }

        if (!CollectionUtils.isEmpty(failedTargets)) {
            markPendingSyncTargets(nkp.getUuid(), failedTargets);
            retryPendingNkpRootKeySync(nkp.getUuid(), reqId, contentBase64);
        }
    }

    private void syncNkpRootKeyToNode(NkpVO nkp, String targetMnUuid, String requestId) {
        String contentBase64 = KeyToolGrpcClient.exportNkpRootKey(requestId, nkp);
        syncNkpRootKeyToNode(nkp, targetMnUuid, requestId, contentBase64);
    }

    private void syncNkpRootKeyToNode(NkpVO nkp, String targetMnUuid, String requestId, String contentBase64) {
        SyncNkpRootKeyMsg msg = new SyncNkpRootKeyMsg();
        msg.setProviderUuid(nkp.getUuid());
        msg.setContentBase64(contentBase64);
        msg.setRequestId(StringUtils.isBlank(requestId) ? Platform.getUuid() : requestId);
        bus.makeServiceIdByManagementNodeId(msg, KeyProviderConstant.SERVICE_ID, targetMnUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to sync nkp root key[providerUuid:%s] to management node[uuid:%s]: %s",
                    nkp.getUuid(), targetMnUuid, reply.getError() == null ? "unknown error" : reply.getError().getDetails()));
        }
        clearPendingSyncTarget(nkp.getUuid(), targetMnUuid);
    }

    private void markPendingSyncTargets(String nkpUuid, Collection<String> targetMnUuids) {
        if (StringUtils.isBlank(nkpUuid) || CollectionUtils.isEmpty(targetMnUuids)) {
            return;
        }
        pendingNkpRootKeySyncTargets.compute(nkpUuid, (uuid, old) -> {
            Set<String> targets = old == null ? ConcurrentHashMap.newKeySet() : old;
            targets.addAll(targetMnUuids);
            return targets;
        });
    }

    private void clearPendingSyncTarget(String nkpUuid, String targetMnUuid) {
        if (StringUtils.isBlank(nkpUuid) || StringUtils.isBlank(targetMnUuid)) {
            return;
        }
        pendingNkpRootKeySyncTargets.computeIfPresent(nkpUuid, (uuid, old) -> {
            old.remove(targetMnUuid);
            return old.isEmpty() ? null : old;
        });
    }

    @AsyncThread
    private void retryPendingNkpRootKeySync(String nkpUuid, String requestId, String contentBase64) {
        for (int i = 0; i < NKP_ROOT_KEY_SYNC_RETRY_TIMES; i++) {
            NkpVO nkp = dbf.findByUuid(nkpUuid, NkpVO.class);
            if (nkp == null) {
                pendingNkpRootKeySyncTargets.remove(nkpUuid);
                return;
            }

            Set<String> pending = pendingNkpRootKeySyncTargets.get(nkpUuid);
            if (CollectionUtils.isEmpty(pending)) {
                return;
            }

            for (String mnUuid : new ArrayList<>(pending)) {
                if (StringUtils.equals(mnUuid, Platform.getManagementServerId())) {
                    clearPendingSyncTarget(nkpUuid, mnUuid);
                    continue;
                }

                try {
                    syncNkpRootKeyToNode(nkp, mnUuid, requestId, contentBase64);
                } catch (Throwable t) {
                    logger.warn(String.format("retry sync nkp root key failed[providerUuid:%s, targetMnUuid:%s, attempt:%s/%s]: %s",
                            nkpUuid, mnUuid, i + 1, NKP_ROOT_KEY_SYNC_RETRY_TIMES, t.getMessage()));
                }
            }

            if (i < NKP_ROOT_KEY_SYNC_RETRY_TIMES - 1) {
                try {
                    Thread.sleep(NKP_ROOT_KEY_SYNC_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        Set<String> pending = pendingNkpRootKeySyncTargets.get(nkpUuid);
        if (!CollectionUtils.isEmpty(pending)) {
            logger.warn(String.format("nkp root key sync still has pending targets after retries[providerUuid:%s, targets:%s]",
                    nkpUuid, pending));
        }
    }

    private KeyProviderInventory updateKeyProvider(APIMessage msg, String uuid) {
        KeyProviderVO vo = dbf.findByUuid(uuid, KeyProviderVO.class);
        KeyProviderFactory factory = getKeyProviderFactory(vo.getType());
        KeyProviderVO updated = factory.updateKeyProvider(vo, msg);
        return factory.valueOf(updated);
    }

    @Override
    public KeyProviderFactory getKeyProviderFactory(KeyProviderType type) {
        KeyProviderFactory factory = keyProviderFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("cannot find KeyProviderFactory for type[%s]", type));
        }
        return factory;
    }

    private void populateExtensions() {
        for (KeyProviderFactory factory : pluginRgty.getExtensionList(KeyProviderFactory.class)) {
            KeyProviderFactory old = keyProviderFactories.get(factory.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate KeyProviderFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), factory.getClass().getName(), factory.getType()));
            }
            keyProviderFactories.put(factory.getType().toString(), factory);
        }
        keyProviderFactories = Collections.unmodifiableMap(keyProviderFactories);

        installDefaultKeyProviderResetExtension();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(KeyProviderConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
    }

    @Override
    @AsyncThread
    public void nodeJoin(ManagementNodeInventory inv) {
        if (inv == null || StringUtils.isBlank(inv.getUuid())) {
            return;
        }
        if (StringUtils.equals(inv.getUuid(), Platform.getManagementServerId())) {
            return;
        }

        List<NkpVO> nkps = Q.New(NkpVO.class).list();
        for (NkpVO nkp : nkps) {
            if (!destMaker.isManagedByUs(nkp.getUuid())) {
                continue;
            }
            try {
                syncNkpRootKeyToNode(nkp, inv.getUuid(), Platform.getUuid());
            } catch (Throwable t) {
                logger.warn(String.format("failed to sync nkp root key on node join[targetMnUuid:%s, providerUuid:%s]: %s",
                        inv.getUuid(), nkp.getUuid(), t.getMessage()));
            }
        }
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

}
