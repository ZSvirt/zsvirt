package org.zstack.crypto.envelope;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostKeyIdentityVO;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.HostKeyIdentityHelper;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMGlobalProperty;
import org.zstack.kvm.KVMPingAgentExtensionPoint;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.utils.logging.CLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * KVMPingAgentExtensionPoint implementation that syncs envelope public key on agent after ping.
 * Registered as one plugin in call-ping-plugins flow; kvmPingAgent() runs the sync logic.
 */
public class KVMEnvelopeKeySyncExtension implements KVMPingAgentExtensionPoint {
    private static final CLogger logger = org.zstack.utils.logging.CLoggerImpl.getLogger(KVMEnvelopeKeySyncExtension.class);

    private static final String DATA_HOST_UUID = "hostUuid";
    private static final String DATA_MANAGEMENT_IP = "managementIp";
    private static final String DATA_IDENTITY = "identity";
    private static final String DATA_NEED_ROTATE = "needRotate";
    private static final String DATA_DO_GET = "doGet";

    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    private static String buildAgentUrl(String managementIp, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(KVMGlobalProperty.AGENT_URL_SCHEME);
        ub.host(managementIp);
        ub.port(KVMGlobalProperty.AGENT_PORT);
        if (KVMGlobalProperty.AGENT_URL_ROOT_PATH != null && !"".equals(KVMGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(KVMGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(path);
        return ub.build().toUriString();
    }

    @Override
    public void kvmPingAgent(KVMHostInventory host, Completion completion) {
        final String hostUuid = host.getUuid();
        final String managementIp = host.getManagementIp();
        if (StringUtils.isBlank(managementIp)) {
            logger.warn("ping host: host " + hostUuid + " has no managementIp, skip envelope key sync");
            completion.success();
            return;
        }

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("kvm-envelope-key-sync-" + hostUuid);
        chain.putData(
                new AbstractMap.SimpleEntry<>(DATA_HOST_UUID, hostUuid),
                new AbstractMap.SimpleEntry<>(DATA_MANAGEMENT_IP, managementIp)
        );
        chain.then(new LoadIdentityFlow());
        chain.then(new VerifyKeyFlow());
        chain.then(new CreateKeyFlow());
        chain.then(new GetKeyFlow());
        chain.then(new RotateKeyFlow());
        chain.then(new GetKeyFlow());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        });
        chain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                String uuid = (String) data.get(DATA_HOST_UUID);
                if (uuid != null) {
                    try {
                        HostKeyIdentityHelper.setVerified(dbf, uuid, false);
                    } catch (Exception ignored) {
                        logger.debug("failed to set host key identity verified to false for host " + uuid);
                    }
                }
                completion.success();
            }
        });
        chain.start();
    }

    private class LoadIdentityFlow extends NoRollbackFlow {
        String __name__ = "load-identity";

        @Override
        public void run(FlowTrigger trigger, Map data) {
            String hostUuid = (String) data.get(DATA_HOST_UUID);
            HostKeyIdentityVO identity = HostKeyIdentityHelper.getHostKeyIdentity(dbf, hostUuid);
            data.put(DATA_IDENTITY, identity);
            trigger.next();
        }
    }

    private class VerifyKeyFlow extends NoRollbackFlow {
        String __name__ = "verify-key";

        @Override
        public void run(FlowTrigger trigger, Map data) {
            String hostUuid = (String) data.get(DATA_HOST_UUID);
            String managementIp = (String) data.get(DATA_MANAGEMENT_IP);
            HostKeyIdentityVO identity = (HostKeyIdentityVO) data.get(DATA_IDENTITY);

            if (identity == null || StringUtils.isBlank(identity.getPublicKey())) {
                trigger.next();
                return;
            }

            String verifyUrl = buildAgentUrl(managementIp, KVMConstant.KVM_VERIFY_ENVELOPE_KEY_PATH);
            restf.asyncJsonPost(verifyUrl, new KVMAgentCommands.VerifyPublicKeyCmd(), new JsonAsyncRESTCallback<KVMAgentCommands.VerifyPublicKeyResponse>(trigger) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn("ping host: verify envelope key failed for host " + hostUuid + ": " + (err != null ? err.getDetails() : ""));
                    HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                    trigger.next();
                }

                @Override
                public void success(KVMAgentCommands.VerifyPublicKeyResponse vrsp) {
                    if (vrsp == null || !vrsp.isSuccess()) {
                        if (vrsp != null && HostKeyIdentityHelper.isRotateNeededGetError(vrsp.getError())) {
                            data.put(DATA_NEED_ROTATE, true);
                        } else {
                            HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                        }
                        trigger.next();
                        return;
                    }
                    String storedFp = identity.getFingerprint();
                    if (StringUtils.isNotBlank(storedFp)) {
                        String computed = HostKeyIdentityHelper.fingerprintFromPublicKey(identity.getPublicKey());
                        if (!storedFp.equals(computed)) {
                            logger.warn("ping host: host " + hostUuid + " verify ok but fingerprint mismatch, rotating and re-getting key");
                            data.put(DATA_NEED_ROTATE, true);
                            trigger.next();
                            return;
                        }
                    }
                    HostKeyIdentityHelper.setVerified(dbf, hostUuid, true);
                    trigger.next();
                }

                @Override
                public Class<KVMAgentCommands.VerifyPublicKeyResponse> getReturnClass() {
                    return KVMAgentCommands.VerifyPublicKeyResponse.class;
                }
            }, TimeUnit.SECONDS, KVMConstant.ENVELOPE_KEY_HTTP_TIMEOUT_SEC);
        }
    }

    private class CreateKeyFlow extends NoRollbackFlow {
        String __name__ = "create-key";

        @Override
        public void run(FlowTrigger trigger, Map data) {
            HostKeyIdentityVO identity = (HostKeyIdentityVO) data.get(DATA_IDENTITY);
            if (identity != null && StringUtils.isNotBlank(identity.getPublicKey())) {
                trigger.next();
                return;
            }

            String hostUuid = (String) data.get(DATA_HOST_UUID);
            String managementIp = (String) data.get(DATA_MANAGEMENT_IP);
            String createUrl = buildAgentUrl(managementIp, KVMConstant.KVM_CREATE_ENVELOPE_KEY_PATH);

            restf.asyncJsonPost(createUrl, new KVMAgentCommands.CreatePublicKeyCmd(), new JsonAsyncRESTCallback<KVMAgentCommands.CreatePublicKeyResponse>(trigger) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn("ping host: create key on agent failed for host " + hostUuid + ": " + (err != null ? err.getDetails() : ""));
                    HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                    trigger.next();
                }

                @Override
                public void success(KVMAgentCommands.CreatePublicKeyResponse createRsp) {
                    if (createRsp == null || !createRsp.isSuccess()) {
                        logger.warn("ping host: create key on agent failed for host " + hostUuid + ": " + (createRsp != null ? createRsp.getError() : "null"));
                        HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                        trigger.next();
                        return;
                    }
                    data.put(DATA_DO_GET, true);
                    trigger.next();
                }

                @Override
                public Class<KVMAgentCommands.CreatePublicKeyResponse> getReturnClass() {
                    return KVMAgentCommands.CreatePublicKeyResponse.class;
                }
            }, TimeUnit.SECONDS, KVMConstant.ENVELOPE_KEY_HTTP_TIMEOUT_SEC);
        }
    }

    private class GetKeyFlow extends NoRollbackFlow {
        String __name__ = "get-key";

        @Override
        public void run(FlowTrigger trigger, Map data) {
            if (Boolean.TRUE != data.get(DATA_DO_GET)) {
                trigger.next();
                return;
            }

            String hostUuid = (String) data.get(DATA_HOST_UUID);
            String managementIp = (String) data.get(DATA_MANAGEMENT_IP);
            String getUrl = buildAgentUrl(managementIp, KVMConstant.KVM_GET_ENVELOPE_KEY_PATH);

            restf.asyncJsonPost(getUrl, new KVMAgentCommands.GetPublicKeyCmd(), new JsonAsyncRESTCallback<KVMAgentCommands.GetPublicKeyResponse>(trigger) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn("ping host: get public key failed for host " + hostUuid + ": " + (err != null ? err.getDetails() : ""));
                    HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                    data.remove(DATA_DO_GET);
                    trigger.next();
                }

                @Override
                public void success(KVMAgentCommands.GetPublicKeyResponse getRsp) {
                    if (getRsp != null && getRsp.isSuccess() && StringUtils.isNotBlank(getRsp.getPublicKey())) {
                        HostKeyIdentityHelper.saveOrUpdateHostKeyIdentity(dbf, hostUuid, getRsp.getPublicKey().trim(), true);
                    } else {
                        HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                    }
                    data.remove(DATA_DO_GET);
                    trigger.next();
                }

                @Override
                public Class<KVMAgentCommands.GetPublicKeyResponse> getReturnClass() {
                    return KVMAgentCommands.GetPublicKeyResponse.class;
                }
            }, TimeUnit.SECONDS, KVMConstant.ENVELOPE_KEY_HTTP_TIMEOUT_SEC);
        }
    }

    private class RotateKeyFlow extends NoRollbackFlow {
        String __name__ = "rotate-key";

        @Override
        public void run(FlowTrigger trigger, Map data) {
            if (Boolean.TRUE != data.get(DATA_NEED_ROTATE)) {
                trigger.next();
                return;
            }
            String hostUuid = (String) data.get(DATA_HOST_UUID);
            String managementIp = (String) data.get(DATA_MANAGEMENT_IP);
            String rotateUrl = buildAgentUrl(managementIp, KVMConstant.KVM_ROTATE_ENVELOPE_KEY_PATH);

            restf.asyncJsonPost(rotateUrl, new KVMAgentCommands.RotatePublicKeyCmd(), new JsonAsyncRESTCallback<KVMAgentCommands.RotatePublicKeyResponse>(trigger) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn("ping host: rotate key on agent failed for host " + hostUuid + ": " + (err != null ? err.getDetails() : ""));
                    HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                    trigger.next();
                }

                @Override
                public void success(KVMAgentCommands.RotatePublicKeyResponse rotateRsp) {
                    if (rotateRsp == null || !rotateRsp.isSuccess()) {
                        logger.warn("ping host: rotate key on agent failed for host " + hostUuid + ": " + (rotateRsp != null ? rotateRsp.getError() : "null"));
                        HostKeyIdentityHelper.setVerified(dbf, hostUuid, false);
                        trigger.next();
                        return;
                    }
                    data.put(DATA_DO_GET, true);
                    trigger.next();
                }

                @Override
                public Class<KVMAgentCommands.RotatePublicKeyResponse> getReturnClass() {
                    return KVMAgentCommands.RotatePublicKeyResponse.class;
                }
            }, TimeUnit.SECONDS, KVMConstant.ENVELOPE_KEY_HTTP_TIMEOUT_SEC);
        }
    }
}
