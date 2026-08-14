package org.zstack.zops;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerStatus;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostStatus;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO_;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zops.api.*;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class ZOpsManagerImpl extends AbstractService implements ZOpsManager {
    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    private ManagementNodeBackend localManagementNodeBackend;

    private static final CLogger logger = Utils.getLogger(ZOpsManagerImpl.class);

    static final String CEPH_CHECK_SKIP = "no ceph in host";
    static final String IS_ZSTONE = "is zstone!!";

    public static final String CHECK_CEPH_HEALTH_COMMAND = String.format("if [ -d '/opt/zstone/bin' ]; then echo '%s'; fi;which ceph >/dev/null 2>&1; if [ $? -eq 0 ]; then ceph health; else echo -n '%s'; fi",
            IS_ZSTONE, CEPH_CHECK_SKIP);

    @Override
    public boolean start() {
        localManagementNodeBackend = new ManagementNodeBackend(Platform.getManagementServerId(), Platform.getManagementServerIp());
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ManagementNodeMessage) {
            passThrough((ManagementNodeMessage) msg);
        } else if (msg instanceof GetChronyServersMsg) {
            handle((GetChronyServersMsg) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetChronyServersMsg) {
            handle((APIGetChronyServersMsg) msg);
        } else if (msg instanceof APIUpdateChronyServersMsg) {
            handle((APIUpdateChronyServersMsg) msg);
        } else if (msg instanceof APISyncChronyServersMsg) {
            handle((APISyncChronyServersMsg) msg);
        } else if (msg instanceof APICheckNetworkReachableMsg) {
            handle((APICheckNetworkReachableMsg) msg);
        } else if (msg instanceof APICheckCephHealthStatusMsg) {
            handle((APICheckCephHealthStatusMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(ManagementNodeMessage msg) {
        localManagementNodeBackend.handleMessage(msg);
    }

    private void handle(APICheckCephHealthStatusMsg msg) {
        APICheckCephHealthStatusReply reply = new APICheckCephHealthStatusReply();
        doCheckCephBeforeSyncChrony(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetChronyServersMsg msg) {
        GetChronyServersReply reply = new GetChronyServersReply();
        doGetChronyServers(new ReturnValueCompletion<List<ChronyServerInfoPair>>(msg) {
            @Override
            public void success(List<ChronyServerInfoPair> returnValue) {
                reply.setServers(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetChronyServersMsg msg) {
        APIGetChronyServersReply reply = new APIGetChronyServersReply();
        doGetChronyServers(new ReturnValueCompletion<List<ChronyServerInfoPair>>(msg) {
            @Override
            public void success(List<ChronyServerInfoPair> returnValue) {
                reply.setServers(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void doGetChronyServers(ReturnValueCompletion<List<ChronyServerInfoPair>> completion) {
        List<ChronyServerInfoPair> servers = new ArrayList<>();
        new While<>(CoreGlobalProperty.CHRONY_SERVERS).step((chronyServer, whileCompletion) -> {
            AbstractHostBackend bkd = getHostBackend(chronyServer);
            bkd.getChronyServers(new ReturnValueCompletion<List<ChronyServerInfoPair>>(completion) {
                @Override
                public void success(List<ChronyServerInfoPair> returnValue) {
                    returnValue.stream()
                            .filter(r -> r.getInternal() != null)
                            .forEach(r -> r.getInternal().setHostname(chronyServer));
                    servers.addAll(returnValue);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.done();
                }
            });


        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success(servers);
            }
        });
    }

    private void handle(APIUpdateChronyServersMsg msg) {
        APIUpdateChronyServersEvent evt = new APIUpdateChronyServersEvent(msg.getId());
        List<String> innerServers = msg.getInternalHostnames();
        List<String> externalServers = msg.getExternalServers();
        doUpdateChronyServers(innerServers, externalServers, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void doUpdateChronyServers(List<String> internalServers, List<String> externalServers, Completion completion) {
        List<String> oldInternalChronyServers = CoreGlobalProperty.CHRONY_SERVERS;
        List<String> newInternalChronyServers;
        List<String> newExternalChronyServers = new ArrayList<>();

        List<AbstractHostBackend> hostBackends = getAllHostBackends();
        List<AbstractHostBackend> internalChronyServerHostBackends = new ArrayList<>();
        List<AbstractHostBackend> updatedHostBackends = new ArrayList<>();

        if (externalServers != null) {
            newExternalChronyServers.addAll(externalServers);

            if (newExternalChronyServers.contains(Platform.getManagementServerVip())) {
                completion.fail(argerr("%s cannot be set as external chrony server!", Platform.getManagementServerVip()));
                return;
            }

            for (AbstractHostBackend hostBackend : hostBackends) {
                for (String chronyServer: newExternalChronyServers) {
                    if (hostBackend.isIpExist(chronyServer)) {
                        completion.fail(argerr("%s cannot be set as external chrony server!", chronyServer));
                        return;
                    }
                }
            }
        }

        if (internalServers == null) {
            newInternalChronyServers = externalServers;
            updatedHostBackends.addAll(hostBackends);
        } else {
            newInternalChronyServers = new ArrayList<>(new HashSet<>(internalServers));
            for (AbstractHostBackend hostBackend : hostBackends) {
                if (hostBackend.isIpsExist(newInternalChronyServers)) {
                    internalChronyServerHostBackends.add(hostBackend);
                } else {
                    updatedHostBackends.add(hostBackend);
                }
            }
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("update-chrony-servers-config");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-ceph-health-status-before-update-chrony-server";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                doCheckCephBeforeSyncChrony(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
                    String __name__ = "check-network-reachable-before-update-chrony-server";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (externalServers == null) {
                            trigger.next();
                            return;
                        }
                        doCheckMultiHostsNetworkReachable(internalServers, externalServers, new ReturnValueCompletion<List<NetworkReachablePair>>(trigger) {
                            @Override
                            public void success(List<NetworkReachablePair> returnValue) {
                                for (NetworkReachablePair networkReachablePair : returnValue) {
                                    if (networkReachablePair.getStatus() != HostConnectedStatus.Connected) {
                                        trigger.fail(operr("%s is unreachable from %s", networkReachablePair.getTargetHostname(), networkReachablePair.getSourceHostname()));
                                        return;
                                    }
                                }
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                }
        ).then(new NoRollbackFlow() {
                   String __name__ = "update-management-node-zstack-properties";

                   @Override
                   public void run(FlowTrigger trigger, Map data) {
                       List<ManagementNodeBackend> successMnbkds = new ArrayList<>();
                       final Boolean[] flag = {false};
                       for (AbstractHostBackend hostBackend : hostBackends) {
                           if (!(hostBackend instanceof ManagementNodeBackend)) {
                               continue;
                           }
                           ManagementNodeBackend mnbkd = (ManagementNodeBackend) hostBackend;
                           mnbkd.updateManagementNodeChronyProperties(newInternalChronyServers, new Completion(trigger) {
                               @Override
                               public void success() {
                                   successMnbkds.add(mnbkd);
                               }

                               @Override
                               public void fail(ErrorCode errorCode) {
                                   for (ManagementNodeBackend mnbkd : successMnbkds) {
                                       mnbkd.updateManagementNodeChronyProperties(oldInternalChronyServers, new Completion(null) {
                                           @Override
                                           public void success() {
                                           }

                                           @Override
                                           public void fail(ErrorCode errorCode) {
                                               logger.warn(String.format("failed to rollback mn chrony server in %s!", mnbkd.hostname));
                                           }
                                       });
                                   }
                                   trigger.fail(errorCode);
                                   flag[0] = true;
                               }
                           });
                           if (flag[0]) return;
                       }
                       trigger.next();
                   }
               }
        ).then(new NoRollbackFlow() {
                   String __name__ = "update-external-chrony-server";

                   @Override
                   public void run(FlowTrigger trigger, Map data) {
                       new While<>(internalChronyServerHostBackends).step((AbstractHostBackend hostBackend, WhileCompletion whileCompletion) -> {
                           hostBackend.updateChronyServers(newExternalChronyServers, new Completion(trigger) {
                               @Override
                               public void success() {
                                   whileCompletion.done();
                               }

                               @Override
                               public void fail(ErrorCode errorCode) {
                                   whileCompletion.addError(errorCode);
                                   whileCompletion.done();
                               }
                           });
                       }, 5).run(new WhileDoneCompletion(trigger) {
                           @Override
                           public void done(ErrorCodeList errorCodeList) {
                               if (!errorCodeList.getCauses().isEmpty()) {
                                   trigger.fail(errorCodeList.getCauses().get(0));
                                   return;
                               }

                               trigger.next();
                           }
                       });
                   }
               }
        ).then(new NoRollbackFlow() {
                   String __name__ = "update-internal-chrony-server";

                   @Override
                   public void run(FlowTrigger trigger, Map data) {
                       new While<>(updatedHostBackends).step((AbstractHostBackend hostBackend, WhileCompletion whileCompletion) -> {
                           hostBackend.updateChronyServers(newInternalChronyServers, new Completion(trigger) {
                               @Override
                               public void success() {
                                   whileCompletion.done();
                               }

                               @Override
                               public void fail(ErrorCode errorCode) {
                                   whileCompletion.addError(errorCode);
                                   whileCompletion.done();
                               }
                           });
                       }, 5).run(new WhileDoneCompletion(trigger) {
                           @Override
                           public void done(ErrorCodeList errorCodeList) {
                               if (!errorCodeList.getCauses().isEmpty()) {
                                   trigger.fail(errorCodeList.getCauses().get(0));
                                   return;
                               }
                               trigger.next();
                           }
                       });
                   }
               }
        ).done(new FlowDoneHandler(completion) {
                   @Override
                   public void handle(Map data) {
                       completion.success();
                   }
               }
        ).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APISyncChronyServersMsg msg) {
        APISyncChronyServersEvent evt = new APISyncChronyServersEvent(msg.getId());
        doSyncChronyServers(new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void doSyncChronyServers(Completion completion) {
        List<AbstractHostBackend> hostBackends = getAllHostBackends();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("update-chrony-servers-config");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-ceph-health-status-before-sync-chrony-server";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                doCheckCephBeforeSyncChrony(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "sync-chrony-server";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(hostBackends).step((AbstractHostBackend hostBackend, WhileCompletion whileCompletion) -> {
                    hostBackend.syncChronyServers(new Completion(completion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            completion.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        completion.success();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
                   @Override
                   public void handle(Map data) {
                       completion.success();
                   }
               }
        ).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APICheckNetworkReachableMsg msg) {
        APICheckNetworkReachableReply reply = new APICheckNetworkReachableReply();
        List<String> sources = msg.getSourceHostnames();
        List<String> targets = msg.getTargetHostnames();
        doCheckMultiHostsNetworkReachable(sources, targets, new ReturnValueCompletion<List<NetworkReachablePair>>(msg) {
            @Override
            public void success(List<NetworkReachablePair> result) {
                reply.setResults(result);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void doCheckMultiHostsNetworkReachable(List<String> sourceHostnames, List<String> targetHostnames, ReturnValueCompletion<List<NetworkReachablePair>> completion) {
        List<NetworkReachablePair> res = new ArrayList<>();
        List<String> realSourceHostnames = sourceHostnames;

        if (realSourceHostnames == null) {
            realSourceHostnames = new ArrayList<>(Collections.singleton(Platform.getManagementServerIp()));
        }

        new While<>(realSourceHostnames).step((String sourceHostname, WhileCompletion whileCompletion) -> {
            AbstractHostBackend bkd = getHostBackend(sourceHostname);
            bkd.checkMultiHostsNetworkReachable(targetHostnames, new ReturnValueCompletion<List<NetworkReachablePair>>(whileCompletion) {
                @Override
                public void success(List<NetworkReachablePair> returnValue) {
                    res.addAll(returnValue);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success(res);
            }
        });
    }

    private void doGetCephHealthStatus(ReturnValueCompletion<List<CephHealthInfo>> completion) {
        Map<String, String> healthMap = new HashMap<>();
        List<AbstractHostBackend> bkds = getHostBackends(HostType.CephBackupStorageMon, HostType.CephPrimaryStorageMon);
        new While<>(bkds).step((bkd, whileCompletion) -> {
            bkd.getCephMonHealthStatus(new ReturnValueCompletion<String>(completion) {
                @Override
                public void success(String results) {
                    healthMap.put(bkd.hostname, results);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.done();
                }
            });

        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                List<CephHealthInfo> details = new ArrayList<>();
                for (Map.Entry<String, String> entry : healthMap.entrySet()) {
                    String hostname = entry.getKey();
                    String res = entry.getValue();

                    CephHealthInfo detail = new CephHealthInfo(hostname, res);

                    if (res.equals(CEPH_CHECK_SKIP)) {
                        detail.setStatus(CephHealthStatus.OK);
                        detail.setDetail(res);
                        details.add(detail);
                        continue;
                    }

                    for (String line: res.split("\n")) {
                        line = line.trim().replaceAll("\r$", "");

                        if (line.contains(IS_ZSTONE)) {
                            detail.setModel(ZOpsConstants.ZSTONE);
                            continue;
                        }

                        if (!line.startsWith("HEALTH_")) continue;
                        if (line.startsWith("HEALTH_ERR")) {
                            detail.setStatus(CephHealthStatus.ERR);
                        } else if (line.startsWith("HEALTH_WARN")){
                            detail.setStatus(CephHealthStatus.WARN);
                        } else if (line.startsWith("HEALTH_OK")) {
                            detail.setStatus(CephHealthStatus.OK);
                        }

                        detail.setDetail(line);
                    }
                    details.add(detail);
                }

                completion.success(details);
            }
        });
    }

    private void doCheckCephBeforeSyncChrony(Completion completion) {
        doGetCephHealthStatus(new ReturnValueCompletion<List<CephHealthInfo>>(completion) {
            @Override
            public void success(List<CephHealthInfo> returnValue) {
                /**
                 * when host has only clock skew error, allow change chrony server.
                 * command: ceph health
                 * forbidden:
                 *  HEALTH_WARN 1/3 mons down, quorum zstack-3,zstack-1; 2 osds down; 1 host (2 osds) down; Degraded data redundancy: 473/4017 objects degraded (11.775%), 89 pgs degraded
                 *  HEALTH_WARN Degraded data redundancy: 1121/3365 objects degraded (33.314%), 427 pgs[1.c5,1.c4,1.c3,2.c1..] degraded; 2 osds down; 2 hosts (2 osds) down; clock skew detected on mon.sds2; 1/3 mons down, quorum sds1,sds2
                 *
                 * allowed :
                 *  HEALTH_WARN clock skew detected on mon.node2, mon.node3
                 * */

                for (CephHealthInfo info : returnValue) {
                    if (info.getModel().equals(ZOpsConstants.ZSTONE)) {
                        completion.fail(operr("ZStone not support update chrony server online yet!"));
                        return;
                    }

                    if (info.getStatus() == CephHealthStatus.OK) {
                        continue;
                    }

                    if (info.getStatus() == CephHealthStatus.WARN) {
                        for (String s : info.getDetail().split(";")) {
                            if (s.isEmpty()) continue;
                            if (!s.contains("clock skew detected")) {
                                completion.fail(operr("ceph status is unhealthy, please check your environment first! %s", info.getDetail()));
                                return;
                            }
                        }
                        continue;
                    }
                    completion.fail(operr("ceph status is unhealthy, please check your environment first! %s", info.getDetail()));
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private AbstractHostBackend getHostBackend(String hostname) {
        for (HostBaseInfo hostBaseInfo : getAllTypeConnectedHostBaseInfos()) {
            if (!hostBaseInfo.getHostname().equals(hostname) && !hostBaseInfo.getExtraIps().contains(hostname)) {
                continue;
            }
            if (hostBaseInfo.getTypes().contains(HostType.ManagementNode)) {
                return new ManagementNodeBackend((ManagementNodeBaseInfo) hostBaseInfo);
            }
            return new InternalHostBackend(hostBaseInfo);
        }

        if (hostname.equals(Platform.getManagementServerIp())) {
            //Platform.getManagementServerIp and ManagementNodeVO may be different
            return localManagementNodeBackend;
        }

        return new ExternalHostBackend(hostname);
    }

    private List<AbstractHostBackend> getHostBackends(HostType... types) {
        List<AbstractHostBackend> bkds = new ArrayList<>();
        for (HostBaseInfo hostBaseInfo : getConnectedHostBaseInfosByTypes(types)) {
            if (hostBaseInfo.getTypes().contains(HostType.ManagementNode)) {
                bkds.add(new ManagementNodeBackend((ManagementNodeBaseInfo) hostBaseInfo));
                continue;
            }

            bkds.add(new InternalHostBackend(hostBaseInfo));
        }

        return bkds;
    }

    private List<AbstractHostBackend> getAllHostBackends() {
        return getHostBackends(HostType.values());
    }

    private List<HostBaseInfo> getConnectedHostBaseInfosByTypes(HostType... types) {
        List<HostBaseInfo> hostBaseInfos = getAllTypeConnectedHostBaseInfos();
        List<HostBaseInfo> filteredHostBaseInfos = new ArrayList<>();
        for (HostBaseInfo hostBaseInfo : hostBaseInfos) {
            for (HostType type : types) {
                if (hostBaseInfo.getTypes().contains(type)) {
                    filteredHostBaseInfos.add(hostBaseInfo);
                    break;
                }
            }
        }
        return filteredHostBaseInfos;
    }

    private List<HostBaseInfo> getAllTypeConnectedHostBaseInfos() {
        List<HostBaseInfo> uniqueHostInfos = new ArrayList<>();

        for (HostBaseInfo h: Arrays.stream(HostType.values())
                .flatMap(type -> getRawConnectedHostBaseInfoByType(type).stream())
                .collect(Collectors.toList())) {
            boolean isDuplicated = false;
            for (HostBaseInfo u: uniqueHostInfos) {
                if (u.getHostname().equals(h.getHostname()) || u.getExtraIps().contains(h.getHostname()) || h.getExtraIps().contains(u.getHostname())) {
                    u.getExtraIps().addAll(h.getExtraIps());
                    if (!u.getHostname().equals(h.getHostname())) {
                        u.getExtraIps().add(h.getHostname());
                    }
                    u.getTypes().addAll(h.getTypes());
                    isDuplicated = true;
                    break;
                }
            }

            if (isDuplicated) continue;
            uniqueHostInfos.add(h);
        }
        return uniqueHostInfos;
    }

    private List<HostBaseInfo> getRawConnectedHostBaseInfoByType(HostType type) {
        switch (type) {
            case ManagementNode:
                return getManagementNodes();
            case KVMHost:
                return getKVMHosts();
            case SftpBackupStorage:
                return getSftpBackupStorage();
            case BarementalPxeServer:
                return getBaremetalPxeServer();
            case CephBackupStorageMon:
                return getCephBackupStorageMons();
            case CephPrimaryStorageMon:
                return getCephPrimaryStorageMons();
            case ImageStoreBackupStorage:
                return getImageStoreBackupStorage();
        }
        return new ArrayList<>();
    }

    private List<HostBaseInfo> getManagementNodes() {
        List<HostBaseInfo> res = new ArrayList<>();
        List<ManagementNodeVO> mnVOs = Q.New(ManagementNodeVO.class).list();
        for (ManagementNodeVO mnVO : mnVOs) {
            ManagementNodeBaseInfo mnInfo = new ManagementNodeBaseInfo(mnVO.getUuid());
            mnInfo.setHostname(mnVO.getHostName());
            mnInfo.setPort(mnVO.getPort());
            res.add(mnInfo);
        }

        return res;
    }

    private List<HostBaseInfo> getKVMHosts() {
        List<HostBaseInfo> res = new ArrayList<>();
        List<KVMHostVO> hostVOs = Q.New(KVMHostVO.class).eq(KVMHostVO_.status, HostStatus.Connected).list();
        for (KVMHostVO h : hostVOs) {
            HostBaseInfo hostInfo = new HostBaseInfo();
            hostInfo.setHostname(h.getManagementIp());
            hostInfo.setPort(h.getPort());
            hostInfo.getTypes().add(HostType.KVMHost);
            String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                    h.getUuid(), HostSystemTags.EXTRA_IPS_TOKEN);
            if (extraIps != null) {
                hostInfo.setExtraIps(new HashSet<>(Arrays.asList(extraIps.split(","))));
            }

            res.add(hostInfo);
        }
        return res;
    }

    private List<HostBaseInfo> getCephPrimaryStorageMons() {
        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.hostname, CephPrimaryStorageMonVO_.sshPort);
        q.add(CephPrimaryStorageMonVO_.status, Op.EQ, MonStatus.Connected);
        return getHostBaseInfos(q, HostType.CephPrimaryStorageMon);
    }

    private List<HostBaseInfo> getCephBackupStorageMons() {
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.hostname, CephBackupStorageMonVO_.sshPort);
        q.add(CephPrimaryStorageMonVO_.status, Op.EQ, MonStatus.Connected);
        return getHostBaseInfos(q, HostType.CephBackupStorageMon);
    }

    private List<HostBaseInfo> getSftpBackupStorage() {
        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.select(SftpBackupStorageVO_.hostname, SftpBackupStorageVO_.sshPort);
        q.add(SftpBackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
        return getHostBaseInfos(q, HostType.SftpBackupStorage);
    }

    private List<HostBaseInfo> getImageStoreBackupStorage() {
        SimpleQuery<ImageStoreBackupStorageVO> q = dbf.createQuery(ImageStoreBackupStorageVO.class);
        q.select(ImageStoreBackupStorageVO_.hostname, ImageStoreBackupStorageVO_.sshPort);
        q.add(ImageStoreBackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
        return getHostBaseInfos(q, HostType.ImageStoreBackupStorage);
    }

    private List<HostBaseInfo> getBaremetalPxeServer() {
        SimpleQuery<BaremetalPxeServerVO> q = dbf.createQuery(BaremetalPxeServerVO.class);
        q.select(BaremetalPxeServerVO_.hostname, BaremetalPxeServerVO_.sshPort);
        q.add(ImageStoreBackupStorageVO_.status, Op.EQ, BaremetalPxeServerStatus.Connected);
        return getHostBaseInfos(q, HostType.BarementalPxeServer);
    }

    private List<HostBaseInfo> getHostBaseInfos(SimpleQuery q, HostType type) {
        List<Tuple> lst = q.listTuple();
        List<HostBaseInfo> hostInfoList = new ArrayList<>();
        for (Tuple t : lst) {
            HostBaseInfo hostInfo = new HostBaseInfo();
            hostInfo.setHostname(t.get(0, String.class));
            hostInfo.setPort(t.get(1, Integer.class));
            hostInfo.getTypes().add(type);
            hostInfoList.add(hostInfo);
        }
        return hostInfoList;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ZOpsConstants.SERVICE_ID);
    }
}
