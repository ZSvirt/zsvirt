package org.zstack.pki;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.migration.MigrationTlsSanCollector;
import org.zstack.header.pki.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

public class HostCertificateServiceImpl implements HostCertificateService, Component, HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostCertificateServiceImpl.class);
    private final ConcurrentHashMap<String, Object> issueLocks = new ConcurrentHashMap<>();
    private static final List<String> MIGRATION_ROLES = Collections.unmodifiableList(Arrays.asList(
            HostPkiConstant.HOST_CERT_ROLE_SERVER,
            HostPkiConstant.HOST_CERT_ROLE_CLIENT));

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostPkiService hostPkiService;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        if (!MigrationTlsGlobalConfig.MIGRATE_TLS_ENABLE.value(Boolean.class)) {
            return;
        }

        try {
            HostCertificateVO existing = getCertificateMetadata(host.getUuid(), HostPkiConstant.USAGE_MIGRATION);
            Completion logOnly = new Completion(null) {
                @Override
                public void success() {}

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to check Host PKI certificate on host[uuid:%s] connected: %s",
                            host.getUuid(), errorCode));
                }
            };

            if (existing == null) {
                ensureProfile(host.getUuid(), HostPkiConstant.USAGE_MIGRATION, logOnly);
            } else {
                refreshStatus(host.getUuid(), HostPkiConstant.USAGE_MIGRATION, logOnly);
            }
        } catch (Throwable t) {
            logger.warn(String.format(
                    "unexpected error while bootstrapping Host PKI on host[uuid:%s] connected, host reconnect is not affected: %s",
                    host.getUuid(), t.getMessage()), t);
        }
    }

    @Override
    public void ensureProfile(String hostUuid, String usage, Completion completion) {
        HostCertificateVO existing = getCertificateMetadata(hostUuid, usage);
        if (existing == null) {
            issueAndInstall(hostUuid, usage, null, HostPkiConstant.HOST_CERT_STATUS_PENDING, completion);
            return;
        }
        // DB Active does not guarantee agent-side materials exist; reconcile before skipping reinstall.
        refreshStatus(hostUuid, usage, new Completion(completion) {
            @Override
            public void success() {
                if (isReady(hostUuid, usage)) {
                    completion.success();
                    return;
                }
                HostCertificateVO reloaded = getCertificateMetadata(hostUuid, usage);
                issueAndInstall(hostUuid, usage, reloaded, HostPkiConstant.HOST_CERT_STATUS_PENDING, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                issueAndInstall(hostUuid, usage, existing, HostPkiConstant.HOST_CERT_STATUS_PENDING, completion);
            }
        });
    }

    @Override
    public void renew(String hostUuid, String usage, Completion completion) {
        issueAndInstall(hostUuid, usage, getCertificateMetadata(hostUuid, usage),
                HostPkiConstant.HOST_CERT_STATUS_RENEWING, completion);
    }

    @Override
    public void revoke(String hostUuid, String usage, String reason, Completion completion) {
        HostCertificateVO vo = getCertificateMetadata(hostUuid, usage);
        if (vo == null) {
            completion.success();
            return;
        }

        try {
            hostPkiService.revokeHostCert(vo.getSerial(), reason);
        } catch (Throwable t) {
            vo.setLastError(t.getMessage());
            dbf.update(vo);
            completion.fail(operr(t.getMessage()));
            return;
        }

        HostPkiAgentCommands.RevokeLocalCmd cmd = new HostPkiAgentCommands.RevokeLocalCmd();
        cmd.setUsage(usage);
        sendToAgent(hostUuid, HostPkiConstant.HOST_PKI_REVOKE_LOCAL_PATH, cmd,
                HostPkiAgentCommands.RevokeLocalResponse.class, new Completion(completion) {
                    @Override
                    public void success() {
                        vo.setStatus(HostPkiConstant.HOST_CERT_STATUS_REVOKED);
                        vo.setLastError(reason);
                        vo.setLastOpDate(dbf.getCurrentSqlTime());
                        dbf.update(vo);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        vo.setLastError(errorCode.toString());
                        dbf.update(vo);
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void refreshStatus(String hostUuid, String usage, Completion completion) {
        HostCertificateVO vo = getCertificateMetadata(hostUuid, usage);
        if (vo == null) {
            completion.success();
            return;
        }

        HostPkiAgentCommands.StatusCmd statusCmd = new HostPkiAgentCommands.StatusCmd();
        statusCmd.setUsage(usage);
        KVMHostAsyncHttpCallMsg msg = buildMsg(hostUuid, HostPkiConstant.HOST_PKI_STATUS_PATH, statusCmd);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    recordRefreshError(vo, reply.getError().toString());
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply kvmReply = reply.castReply();
                HostPkiAgentCommands.StatusResponse rsp = kvmReply.toResponse(HostPkiAgentCommands.StatusResponse.class);
                if (!rsp.isSuccess()) {
                    recordRefreshError(vo, rsp.getError());
                    completion.fail(operr(rsp.getError()));
                    return;
                }
                vo.setStatus(rsp.isReady() ? HostPkiConstant.HOST_CERT_STATUS_ACTIVE : HostPkiConstant.HOST_CERT_STATUS_PENDING);
                vo.setFingerprint(rsp.getFingerprint());
                if (rsp.getSanList() != null && !rsp.getSanList().isEmpty()) {
                    vo.setSanSnapshot(StringUtils.join(rsp.getSanList(), ","));
                }
                vo.setLastError(rsp.getError());
                vo.setLastOpDate(dbf.getCurrentSqlTime());
                dbf.update(vo);
                completion.success();
            }
        });
    }

    private void issueAndInstall(String hostUuid, String usage, HostCertificateVO existing,
                                 String initialStatus, Completion completion) {
        String lockKey = hostUuid + ":" + usage;
        Object lock = issueLocks.computeIfAbsent(lockKey, k -> new Object());

        // fast path: if another thread already completed issuance, skip
        synchronized (lock) {
            if (existing == null) {
                HostCertificateVO reloaded = getCertificateMetadata(hostUuid, usage);
                if (reloaded != null && HostPkiConstant.HOST_CERT_STATUS_ACTIVE.equals(reloaded.getStatus())
                        && reloaded.getNotAfter() != null
                        && reloaded.getNotAfter().after(new Timestamp(new Date().getTime()))) {
                    issueLocks.remove(lockKey, lock);
                    completion.success();
                    return;
                }
            }
        }

        PkiCaVO ca;
        List<String> sanList;
        try {
            ca = hostPkiService.ensureGlobalHostCa();
            sanList = collectSan(hostUuid, usage);
        } catch (Throwable t) {
            issueLocks.remove(lockKey, lock);
            if (existing != null) {
                markInstallFailed(existing, t.getMessage());
            }
            completion.fail(operr(t.getMessage()));
            return;
        }

        HostCertificateVO vo = existing == null ? new HostCertificateVO() : existing;
        if (existing == null) {
            vo.setUuid(Platform.getUuid());
            vo.setHostUuid(hostUuid);
            vo.setUsage(usage);
            vo.setCreateDate(dbf.getCurrentSqlTime());
        }
        vo.setCaUuid(ca.getUuid());
        vo.setStatus(initialStatus);
        vo.setSanSnapshot(StringUtils.join(sanList, ","));
        vo.setLastError(null);
        vo.setLastOpDate(dbf.getCurrentSqlTime());

        final HostCertificateVO finalVo;
        if (existing == null) {
            try {
                dbf.persist(vo);
                finalVo = vo;
            } catch (Exception e) {
                issueLocks.remove(lockKey, lock);
                if (!ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                    completion.fail(operr(e.getMessage()));
                    return;
                }
                HostCertificateVO reloaded = getCertificateMetadata(hostUuid, usage);
                issueLocks.remove(lockKey, lock);
                if (reloaded == null) {
                    completion.fail(operr(e.getMessage()));
                    return;
                }
                completion.success();
                return;
            }
        } else {
            dbf.update(vo);
            finalVo = vo;
        }

        // step 1: generate CSR on agent
        HostPkiAgentCommands.GenerateCsrCmd csrCmd = new HostPkiAgentCommands.GenerateCsrCmd();
        csrCmd.setUsage(usage);
        csrCmd.setSubject(String.format("CN=%s", hostUuid));
        csrCmd.setSanList(sanList);
        csrCmd.setKeyAlgorithm("RSA_2048");
        csrCmd.setRoles(MIGRATION_ROLES);

        KVMHostAsyncHttpCallMsg csrMsg = buildMsg(hostUuid, HostPkiConstant.HOST_PKI_GENERATE_CSR_PATH, csrCmd);
        bus.send(csrMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    issueLocks.remove(lockKey, lock);
                    markInstallFailed(finalVo, reply.getError().toString());
                    completion.fail(reply.getError());
                    return;
                }

                HostPkiAgentCommands.GenerateCsrResponse csrRsp =
                        reply.<KVMHostAsyncHttpCallReply>castReply().toResponse(HostPkiAgentCommands.GenerateCsrResponse.class);
                if (!csrRsp.isSuccess()) {
                    issueLocks.remove(lockKey, lock);
                    markInstallFailed(finalVo, csrRsp.getError());
                    completion.fail(operr(csrRsp.getError()));
                    return;
                }

                // step 2: sign CSR on MN (CPU-bound, no IO wait)
                Map<String, String> certPemByRole = new HashMap<>();
                Map<String, String> fingerprintByRole = new HashMap<>();
                Map<String, String> serialByRole = new HashMap<>();
                Timestamp[] notBefore = {null};
                Timestamp[] notAfter = {null};

                for (String role : MIGRATION_ROLES) {
                    String csrPem = csrRsp.getCsrPemByRole() == null ? null : csrRsp.getCsrPemByRole().get(role);
                    if (StringUtils.isBlank(csrPem)) {
                        issueLocks.remove(lockKey, lock);
                        String err = String.format("kvmagent did not return CSR for role[%s]", role);
                        markInstallFailed(finalVo, err);
                        completion.fail(operr(err));
                        return;
                    }
                    try {
                        HostCertificateBundle bundle = hostPkiService.signCsr(csrPem,
                                buildProfile(usage, hostUuid, sanList, role));
                        certPemByRole.put(role, bundle.getCertificatePem());
                        fingerprintByRole.put(role, bundle.getFingerprint());
                        serialByRole.put(role, bundle.getSerial());
                        notBefore[0] = earliest(notBefore[0], bundle.getNotBefore());
                        notAfter[0] = latest(notAfter[0], bundle.getNotAfter());
                    } catch (Exception e) {
                        issueLocks.remove(lockKey, lock);
                        markInstallFailed(finalVo, e.getMessage());
                        completion.fail(operr(e.getMessage()));
                        return;
                    }
                }

                // step 3: install on agent
                HostPkiAgentCommands.InstallCmd installCmd = new HostPkiAgentCommands.InstallCmd();
                installCmd.setUsage(usage);
                installCmd.setCertPemByRole(certPemByRole);
                installCmd.setCaChainPem(ca.getCertChainPem());
                installCmd.setCrlPem(hostPkiService.refreshCrl());
                installCmd.setExpectedFingerprintByRole(fingerprintByRole);

                KVMHostAsyncHttpCallMsg installMsg = buildMsg(hostUuid, HostPkiConstant.HOST_PKI_INSTALL_PATH, installCmd);
                bus.send(installMsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        issueLocks.remove(lockKey, lock);
                        if (!reply.isSuccess()) {
                            markInstallFailed(finalVo, reply.getError().toString());
                            completion.fail(reply.getError());
                            return;
                        }

                        HostPkiAgentCommands.InstallResponse installRsp =
                                reply.<KVMHostAsyncHttpCallReply>castReply().toResponse(HostPkiAgentCommands.InstallResponse.class);
                        if (!installRsp.isSuccess()) {
                            markInstallFailed(finalVo, installRsp.getError());
                            completion.fail(operr(installRsp.getError()));
                            return;
                        }

                        refreshStatus(hostUuid, usage, new Completion(completion) {
                            @Override
                            public void success() {
                                HostCertificateVO refreshed = getCertificateMetadata(hostUuid, usage);
                                if (refreshed != null && HostPkiConstant.HOST_CERT_STATUS_ACTIVE.equals(refreshed.getStatus())) {
                                    refreshed.setSerial(serialByRole.get(HostPkiConstant.HOST_CERT_ROLE_SERVER));
                                    refreshed.setNotBefore(notBefore[0]);
                                    refreshed.setNotAfter(notAfter[0]);
                                    refreshed.setLastInstallDate(dbf.getCurrentSqlTime());
                                    refreshed.setLastError(null);
                                    refreshed.setLastOpDate(dbf.getCurrentSqlTime());
                                    dbf.update(refreshed);
                                    logger.info(String.format("installed Host PKI certificate[host:%s, usage:%s]", hostUuid, usage));
                                    completion.success();
                                    return;
                                }
                                markInstallFailed(finalVo, "agent reported migration TLS certificate is not ready after install");
                                completion.fail(operr("agent reported migration TLS certificate is not ready after install"));
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                markInstallFailed(finalVo, errorCode.getDetails());
                                completion.fail(errorCode);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public boolean isReady(String hostUuid, String usage) {
        HostCertificateVO vo = getCertificateMetadata(hostUuid, usage);
        if (vo == null || !HostPkiConstant.HOST_CERT_STATUS_ACTIVE.equals(vo.getStatus())) {
            return false;
        }
        if (StringUtils.isBlank(vo.getFingerprint())) {
            return false;
        }
        Timestamp notAfter = vo.getNotAfter();
        return notAfter == null || notAfter.after(new Timestamp(new Date().getTime()));
    }

    @Override
    public HostCertificateVO getCertificateMetadata(String hostUuid, String usage) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(usage)) {
            return null;
        }
        return Q.New(HostCertificateVO.class)
                .eq(HostCertificateVO_.hostUuid, hostUuid)
                .eq(HostCertificateVO_.usage, usage)
                .find();
    }

    private List<String> collectSan(String hostUuid, String usage) {
        if (!HostPkiConstant.USAGE_MIGRATION.equals(usage)) {
            return new ArrayList<>();
        }
        for (MigrationTlsSanCollector collector : pluginRgty.getExtensionList(MigrationTlsSanCollector.class)) {
            List<String> san = collector.collectSanForHost(hostUuid);
            if (san != null && !san.isEmpty()) {
                return san;
            }
        }
        return new ArrayList<>();
    }

    private KVMHostAsyncHttpCallMsg buildMsg(String hostUuid, String path, Object cmd) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        return msg;
    }

    private <T> void sendToAgent(String hostUuid, String path, Object cmd, Class<T> responseClass, Completion completion) {
        KVMHostAsyncHttpCallMsg msg = buildMsg(hostUuid, path, cmd);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }

    private HostCertProfile buildProfile(String usage, String hostUuid, List<String> sanList, String role) {
        HostCertProfile profile = new HostCertProfile();
        profile.setUsage(usage);
        // signCsr uses profile.subjectDn and overrides the agent CSR subject (CN=<hostUuid>).
        profile.setSubjectDn(String.format("CN=%s-%s", hostUuid, role));
        profile.setSubjectAlternativeNames(sanList);
        profile.setRoles(Collections.singletonList(role));
        return profile;
    }

    private void recordRefreshError(HostCertificateVO vo, String error) {
        vo.setLastError(error);
        vo.setLastOpDate(dbf.getCurrentSqlTime());
        dbf.update(vo);
        logger.warn(String.format("Host PKI certificate status refresh failed[host:%s, usage:%s, status:%s]: %s",
                vo.getHostUuid(), vo.getUsage(), vo.getStatus(), error));
    }

    private void markInstallFailed(HostCertificateVO vo, String error) {
        vo.setStatus(HostPkiConstant.HOST_CERT_STATUS_INSTALL_FAILED);
        vo.setLastError(error);
        vo.setLastOpDate(dbf.getCurrentSqlTime());
        dbf.update(vo);
        logger.warn(String.format("Host PKI certificate install failed[host:%s, usage:%s]: %s",
                vo.getHostUuid(), vo.getUsage(), error));
    }

    private Timestamp earliest(Timestamp current, Timestamp candidate) {
        if (current == null || (candidate != null && candidate.before(current))) {
            return candidate;
        }
        return current;
    }

    private Timestamp latest(Timestamp current, Timestamp candidate) {
        if (current == null || (candidate != null && candidate.after(current))) {
            return candidate;
        }
        return current;
    }
}
