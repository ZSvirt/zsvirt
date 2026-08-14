package org.zstack.ipsec;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.*;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class IPsecconnectionBase {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private IPsecManager ipsecMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VirtualRouterManager vrMgr;

    protected IPsecConnectionVO self;

    public IPsecconnectionBase(IPsecConnectionVO self) {
        this.self = self;
    }

    private String getSyncId() {
        return String.format("operate-IPsecconnection-on-vip-%s", self.getVipUuid());
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg){
        if (msg instanceof IPsecConnectionDeletionMsg){
            handle((IPsecConnectionDeletionMsg) msg);
        } else if (msg instanceof IPsecConnectionSyncMsg) {
            handle((IPsecConnectionSyncMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handleApiMessage(APIMessage msg){
        if (msg instanceof APIAttachL3NetworksToIPsecConnectionMsg) {
            handle((APIAttachL3NetworksToIPsecConnectionMsg)msg);
        } else if (msg instanceof APIDetachL3NetworksFromIPsecConnectionMsg) {
            handle((APIDetachL3NetworksFromIPsecConnectionMsg)msg);
        } else if (msg instanceof APIAddRemoteCidrsToIPsecConnectionMsg) {
            handle((APIAddRemoteCidrsToIPsecConnectionMsg)msg);
        } else if (msg instanceof APIRemoveRemoteCidrsFromIPsecConnectionMsg) {
            handle((APIRemoveRemoteCidrsFromIPsecConnectionMsg)msg);
        } else if (msg instanceof APIDeleteIPsecConnectionMsg) {
            handle((APIDeleteIPsecConnectionMsg)msg);
        } else if (msg instanceof APIUpdateIPsecConnectionMsg) {
            handle((APIUpdateIPsecConnectionMsg)msg);
        } else if (msg instanceof APIChangeIPsecConnectionMsg) {
            handle((APIChangeIPsecConnectionMsg)msg);
        } else if (msg instanceof APIChangeIPSecConnectionStateMsg) {
            handle((APIChangeIPSecConnectionStateMsg)msg);
        } else if (msg instanceof APIReconnectIPsecConnectionMsg) {
            handle((APIReconnectIPsecConnectionMsg)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void doDeleteIPsecConnection(APIDeleteIPsecConnectionMsg msg, Completion completion){
        final String issuer = IPsecConnectionVO.class.getSimpleName();
        final List<IPsecConnectionInventory> ctx = Arrays.asList(IPsecConnectionInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-ipsec-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-ipsec-permissive-check";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-ipsec-permissive-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-ipsec-force-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIDeleteIPsecConnectionMsg msg){
        APIDeleteIPsecConnectionEvent evt = new APIDeleteIPsecConnectionEvent(msg.getId());
        doDeleteIPsecConnection(msg, new Completion(msg) {
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

    protected void handle(APIReconnectIPsecConnectionMsg msg) {
        APIReconnectIPsecConnectionEvent evt = new APIReconnectIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                IPsecConnectionVO vo = Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, msg.getUuid()).find();
                IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                smsg.setInv(IPsecConnectionInventory.valueOf(vo));
                smsg.setL3NetworkUuid(Q.New(IPsecL3NetworkRefVO.class).eq(IPsecL3NetworkRefVO_.connectionUuid, msg.getUuid()).select(IPsecL3NetworkRefVO_.l3NetworkUuid).limit(1).findValue());
                L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(smsg.getL3NetworkUuid(), L3NetworkVO.class));
                smsg.setVr(vrMgr.getVirtualRouterVm(l3Inv));
                smsg.setSkip_vip_release(true);
                IPsecConstants.IPsecBackendAction oper = IPsecConstants.IPsecBackendAction.RECONNECT;
                doSyncIPsecConnection(oper, smsg, new Completion(smsg) {
                    @Override
                    public void success() {
                        try {
                            new Retry<Boolean>() {
                                String __name__ = String.format("test-ipsec-connection-%s-status", vo.getUuid());
                                @Override
                                @RetryCondition(onExceptions = {RuntimeException.class}, times = 6, interval = 20)
                                protected Boolean call() {
                                    if (Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, vo.getUuid()).eq(IPsecConnectionVO_.status, IPSecStatus.Ready).isExists()) {
                                        vo.setStatus(IPSecStatus.Ready);
                                        evt.setInventory(IPsecConnectionInventory.valueOf(vo));
                                        return true;
                                    } else {
                                        throw new RuntimeException(String.format("IPsec connection[%s] status down", vo.getUuid()));
                                    }
                                }
                            }.run();
                        } catch (RuntimeException e){
                            evt.setError(argerr("%s", e.getMessage()));
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        vo.setStatus(IPSecStatus.Disconnected);
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    protected void handle(APIChangeIPsecConnectionMsg msg){
        APIChangeIPsecConnectionEvent evt = new APIChangeIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                IPsecConnectionVO vo = Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, msg.getUuid()).find();
                if (msg.getAuthKey() != null) {
                    vo.setAuthKey(msg.getAuthKey());
                }
                if (msg.getAuthMode() != null) {
                    vo.setAuthMode(msg.getAuthMode());
                }
                if (msg.getIkeAuthAlgorithm() != null) {
                    vo.setIkeAuthAlgorithm(msg.getIkeAuthAlgorithm());
                }
                if (msg.getIkeDhGroup() != 0) {
                    vo.setIkeDhGroup(msg.getIkeDhGroup());
                }
                if (msg.getIkeEncryptionAlgorithm() != null) {
                    vo.setIkeEncryptionAlgorithm(msg.getIkeEncryptionAlgorithm());
                }
                if (msg.getPeerAddress() != null) {
                    vo.setPeerAddress(msg.getPeerAddress());
                }
                if (msg.getPfs() != null) {
                    vo.setPfs(msg.getPfs());
                }
                if (msg.getPolicyAuthAlgorithm() != null) {
                    vo.setPolicyAuthAlgorithm(msg.getPolicyAuthAlgorithm());
                }
                if (msg.getPolicyMode() != null) {
                    vo.setPolicyMode(msg.getPolicyMode());
                }
                if (msg.getTransformProtocol() != null) {
                    vo.setTransformProtocol(msg.getTransformProtocol());
                }
                if (msg.getPolicyEncryptionAlgorithm() != null) {
                    vo.setPolicyEncryptionAlgorithm(msg.getPolicyEncryptionAlgorithm());
                }
                if (msg.getIkeVersion() != null) {
                    vo.setIkeVersion(msg.getIkeVersion());
                }
                if (msg.getIdType() != null) {
                    vo.setIdType(msg.getIdType());
                }
                if (msg.getLocalId() != null) {
                    vo.setLocalId(msg.getLocalId());
                }
                if (msg.getRemoteId() != null) {
                    vo.setRemoteId(msg.getRemoteId());
                }
                if (msg.getIkeLifeTime() != 0) {
                    vo.setIkeLifeTime(msg.getIkeLifeTime());
                }
                if (msg.getLifeTime() != 0) {
                    vo.setLifeTime(msg.getLifeTime());
                }

                IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                smsg.setInv(IPsecConnectionInventory.valueOf(vo));
                smsg.setL3NetworkUuid(Q.New(IPsecL3NetworkRefVO.class).eq(IPsecL3NetworkRefVO_.connectionUuid, msg.getUuid()).select(IPsecL3NetworkRefVO_.l3NetworkUuid).limit(1).findValue());
                L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(smsg.getL3NetworkUuid(), L3NetworkVO.class));
                smsg.setVr(vrMgr.getVirtualRouterVm(l3Inv));
                smsg.setSkip_vip_release(true);
                IPsecConstants.IPsecBackendAction oper = IPsecConstants.IPsecBackendAction.SYNC;
                doSyncIPsecConnection(oper, smsg, new Completion(smsg) {
                    @Override
                    public void success() {
                        try {
                            new Retry<Boolean>() {
                                String __name__ = String.format("test-ipsec-connection-%s-status", vo.getUuid());
                                @Override
                                @RetryCondition(onExceptions = {RuntimeException.class}, times = 6, interval = 20)
                                protected Boolean call() {
                                    if (Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, vo.getUuid()).eq(IPsecConnectionVO_.status, IPSecStatus.Ready).isExists()) {
                                        vo.setStatus(IPSecStatus.Ready);
                                        evt.setInventory(IPsecConnectionInventory.valueOf(vo));
                                        return true;
                                    } else {
                                        throw new RuntimeException(String.format("IPsec connection[%s] status down", vo.getUuid()));
                                    }
                                }
                            }.run();
                        } catch (RuntimeException e){
                            vo.setStatus(IPSecStatus.Disconnected);
                            evt.setError(argerr("%s", e.getMessage()));
                        }
                        dbf.update(vo);
                        bus.publish(evt);
                        chain.next();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        vo.setStatus(IPSecStatus.Disconnected);
                        dbf.update(vo);
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    protected void handle(APIUpdateIPsecConnectionMsg msg){
        APIUpdateIPsecConnectionEvent evt = new APIUpdateIPsecConnectionEvent(msg.getId());
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        evt.setInventory(IPsecConnectionInventory.valueOf(self));
        bus.publish(evt);
    }

    private void doChangeIPSecConnectionState(APIChangeIPSecConnectionStateMsg msg, Completion completion) {
        final IPsecState nextState = self.getState().nextState(IPSecStateEvent.valueOf(msg.getStateEvent()));
        if (nextState == self.getState()) {
            completion.success();
            return;
        }

        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
        IPsecBackend bkd = ipsecMgr.getBackend(inv.getLocalL3Networks().get(0));
        bkd.changeIPsecConnectionState(inv, nextState, new Completion(msg) {
            @Override
            public void success() {
                self.setState(nextState);
                self = dbf.updateAndRefresh(self);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    protected void handle(APIChangeIPSecConnectionStateMsg msg){
        final APIChangeIPSecConnectionStateEvent evt = new APIChangeIPSecConnectionStateEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeIPSecConnectionState(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(IPsecConnectionInventory.valueOf(self));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("APIChangeIPSecConnectionState-%s", msg.getUuid());
            }
        });
    }

    private void doSyncIPsecConnection(IPsecConstants.IPsecBackendAction oper, IPsecConnectionSyncMsg msg, Completion completion) {
        IPsecBackend bkd = ipsecMgr.getBackend(msg.getL3NetworkUuid());
        if (oper == IPsecConstants.IPsecBackendAction.CREATE) {
            bkd.createIPsecConnection(msg.getInv(), new Completion(completion) {
                @Override
                public void success() {
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        } else if (oper == IPsecConstants.IPsecBackendAction.DELETE) {
            bkd.deleteIPsecConnection(msg.getInv(), msg.getL3NetworkUuid(), msg.getVr(), msg.isSkip_vip_release(), new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    completion.success();
                }
            });
        } else if (oper == IPsecConstants.IPsecBackendAction.SYNC) {
            bkd.syncIPsecConnection(msg.getInv(), completion);
        } else if (oper == IPsecConstants.IPsecBackendAction.RECONNECT) {
            bkd.syncIPsecConnection(msg.getInv(), completion);
        } else {
            completion.success();
        }
    }

    private void doAttachL3NetworkToIPsecConnection (APIAttachL3NetworksToIPsecConnectionMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-l3Network-IPsec-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "attach-l3Network-IPsec-writedb";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConnectionInventory invOld = IPsecConnectionInventory.valueOf(self);
                        Set<String> oldLocal = invOld.getLocalL3Cidrs();
                        List<IPsecL3NetworkRefVO> refs = new ArrayList<>();
                        for (String l3Uuid : msg.getL3NetworkUuids()) {
                            IPsecL3NetworkRefVO ref = new IPsecL3NetworkRefVO();
                            ref.setL3NetworkUuid(l3Uuid);
                            ref.setConnectionUuid(msg.getIPsecConnectionUuid());
                            ref.setUuid(Platform.getUuid());
                            refs.add(ref);
                        }
                        dbf.persistCollection(refs);
                        self = dbf.reload(self);
                        IPsecConnectionInventory invNew = IPsecConnectionInventory.valueOf(self);
                        List<String> newRemote = invNew.getPeerCidrSignatures();
                        if (newRemote == null ||  newRemote.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.NONE);
                        } else if (oldLocal == null || oldLocal.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.CREATE);
                        } else {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.SYNC);
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<IPsecL3NetworkRefVO> refs = Q.New(IPsecL3NetworkRefVO.class).eq(IPsecL3NetworkRefVO_.connectionUuid, self.getUuid())
                                .in(IPsecL3NetworkRefVO_.l3NetworkUuid, msg.getL3NetworkUuids()).list();
                        dbf.removeCollection(refs, IPsecL3NetworkRefVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-l3Network-IPsec-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConstants.IPsecBackendAction oper = (IPsecConstants.IPsecBackendAction)data.get(IPsecConstants.Param.BACKEND_ACTION_TYPE);
                        if (oper == IPsecConstants.IPsecBackendAction.NONE) {
                            trigger.next();
                            return;
                        }

                        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
                        IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                        smsg.setInv(inv);
                        smsg.setL3NetworkUuid(msg.getL3NetworkUuids().get(0));
                        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuids().get(0), L3NetworkVO.class));
                        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
                        smsg.setVr(vrInv);
                        doSyncIPsecConnection(oper, smsg, new Completion(completion) {
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
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-l3Network-to-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConstants.IPsecBackendAction oper = (IPsecConstants.IPsecBackendAction)data.get(IPsecConstants.Param.BACKEND_ACTION_TYPE);
                        if (oper == IPsecConstants.IPsecBackendAction.CREATE) {
                            trigger.next();
                            return;
                        }

                        List<ErrorCode> errs = new ArrayList<>();
                        new While<>(msg.getL3NetworkUuids()).each((l3Uuid, whileComplection) -> {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
                            vipStruct.setPeerL3NetworkUuid(l3Uuid);
                            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3Uuid, IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE);
                            vipStruct.setServiceProvider(providerType.toString());
                            vipStruct.setServiceUuid(self.getUuid());
                            Vip vip = new Vip(self.getVipUuid());
                            vip.setStruct(vipStruct);
                            vip.acquire(new Completion(whileComplection) {
                                @Override
                                public void success() {
                                    whileComplection.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errs.add(errorCode);
                                    whileComplection.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errs.size() > 0) {
                                    trigger.fail(errs.get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APIAttachL3NetworksToIPsecConnectionMsg msg){
        APIAttachL3NetworksToIPsecConnectionEvent evt = new APIAttachL3NetworksToIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAttachL3NetworkToIPsecConnection(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(IPsecConnectionInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("APIAttachL3NetworkToIPsecConnection-%s", msg.getUuid());
            }
        });
    }

    private void doDetachL3NetworkFromIPsecConnection (APIDetachL3NetworksFromIPsecConnectionMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("detach-l3Network-IPsec-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-L3Networks-from-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
                        vipStruct.setServiceUuid(self.getUuid());

                        if (msg.getL3NetworkUuids().isEmpty()) {
                            trigger.next();
                            return;
                        }
                        Set<String> guestL3NetworkUuids = new HashSet<>(msg.getL3NetworkUuids());

                        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(msg.getL3NetworkUuids().get(0), IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType.toString());
                        Vip v = new Vip(self.getVipUuid());
                        v.setStruct(vipStruct);
                        v.stop(new Completion(trigger) {
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
                });

                flow(new Flow() {
                    String __name__ = "detach-l3Network-IPsec-writedb";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConnectionInventory invOld = IPsecConnectionInventory.valueOf(self);
                        List<IPsecL3NetworkRefVO> refs = Q.New(IPsecL3NetworkRefVO.class)
                                .eq(IPsecL3NetworkRefVO_.connectionUuid, msg.getIPsecConnectionUuid())
                                .in(IPsecL3NetworkRefVO_.l3NetworkUuid, msg.getL3NetworkUuids()).list();
                        dbf.removeCollection(refs, IPsecL3NetworkRefVO.class);
                        self = dbf.reload(self);
                        IPsecConnectionInventory invNew = IPsecConnectionInventory.valueOf(self);
                        Set<String> newLocal = invNew.getLocalL3Cidrs();
                        List<String> oldRemote = invOld.getPeerCidrSignatures();
                        if (oldRemote == null || oldRemote.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.NONE);
                        } else if (newLocal != null && !newLocal.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.SYNC);
                        } else {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.DELETE);
                        }

                        if ((newLocal == null || newLocal.isEmpty()) && IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTag(invOld.getUuid(), IPsecConnectionVO.class) != null) {
                            IpSecconnectionSystemTags.IPSEC_LOW_VERSION.deleteInherentTag(invOld.getUuid(), IPsecConnectionVO.class);
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<IPsecL3NetworkRefVO> refs = new ArrayList<>();
                        for (String l3Uuid : msg.getL3NetworkUuids()) {
                            IPsecL3NetworkRefVO ref = new IPsecL3NetworkRefVO();
                            ref.setL3NetworkUuid(l3Uuid);
                            ref.setConnectionUuid(msg.getIPsecConnectionUuid());
                            ref.setUuid(Platform.getUuid());
                            refs.add(ref);
                        }
                        dbf.persistCollection(refs);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-l3Network-IPsec-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConstants.IPsecBackendAction oper = (IPsecConstants.IPsecBackendAction)data.get(IPsecConstants.Param.BACKEND_ACTION_TYPE);
                        if (oper == IPsecConstants.IPsecBackendAction.NONE) {
                            trigger.next();
                            return;
                        }

                        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
                        IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                        smsg.setInv(inv);
                        smsg.setL3NetworkUuid(msg.getL3NetworkUuids().get(0));
                        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuids().get(0), L3NetworkVO.class));
                        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
                        smsg.setVr(vrInv);
                        if ((self.getPeerCidrs() != null && !self.getPeerCidrs().isEmpty()) ||
                                (self.getL3Networks() != null && !self.getL3Networks().isEmpty())) {
                            smsg.setSkip_vip_release(true);
                        }
                        doSyncIPsecConnection(oper, smsg, new Completion(completion) {
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
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIDetachL3NetworksFromIPsecConnectionMsg msg){
        APIDetachL3NetworksFromIPsecConnectionEvent evt = new APIDetachL3NetworksFromIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDetachL3NetworkFromIPsecConnection(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(IPsecConnectionInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("APIAttachL3NetworkToIPsecConnection-%s", msg.getUuid());
            }
        });
    }

    private void doAttachRemoteCIDRToIPsecConnection (APIAddRemoteCidrsToIPsecConnectionMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-remotecidr-IPsec-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "attach-remotecidr-IPsec-writedb";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConnectionInventory invOld = IPsecConnectionInventory.valueOf(self);
                        Set<String> oldLocal = invOld.getLocalL3Cidrs();
                        List<String> oldRemote = invOld.getPeerCidrSignatures();
                        List<IPsecPeerCidrVO> refs = new ArrayList<>();
                        for (String cidr : msg.getPeerCidrs()) {
                            IPsecPeerCidrVO ref = new IPsecPeerCidrVO();
                            ref.setCidr(cidr);
                            ref.setConnectionUuid(msg.getIPsecConnectionUuid());
                            ref.setUuid(Platform.getUuid());
                            refs.add(ref);
                        }
                        dbf.persistCollection(refs);
                        self = dbf.reload(self);
                        if (oldLocal == null || oldLocal.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.NONE);
                        } else if (oldRemote == null || oldRemote.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.CREATE);
                        } else {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.SYNC);
                        }

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<IPsecPeerCidrVO> refs = Q.New(IPsecPeerCidrVO.class).eq(IPsecPeerCidrVO_.connectionUuid, msg.getIPsecConnectionUuid())
                                .in(IPsecPeerCidrVO_.cidr, msg.getPeerCidrs()).list();
                        dbf.removeCollection(refs, IPsecPeerCidrVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-l3Network-IPsec-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConstants.IPsecBackendAction oper = (IPsecConstants.IPsecBackendAction)data.get(IPsecConstants.Param.BACKEND_ACTION_TYPE);
                        if (oper == IPsecConstants.IPsecBackendAction.NONE) {
                            trigger.next();
                            return;
                        }

                        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
                        List<String> l3Uuids = inv.getLocalL3Networks();
                        IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                        smsg.setInv(inv);
                        smsg.setL3NetworkUuid(l3Uuids.get(0));
                        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(l3Uuids.get(0), L3NetworkVO.class));
                        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
                        smsg.setVr(vrInv);
                        doSyncIPsecConnection(oper, smsg, new Completion(completion) {
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
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIAddRemoteCidrsToIPsecConnectionMsg msg){
        APIAddRemoteCidrsToIPsecConnectionEvent evt = new APIAddRemoteCidrsToIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAttachRemoteCIDRToIPsecConnection(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(IPsecConnectionInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("APIAttachL3NetworkToIPsecConnection-%s", msg.getUuid());
            }
        });
    }

    private void doDetachRemoteCIDRFromIPsecConnection (APIRemoveRemoteCidrsFromIPsecConnectionMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("detach-remotecidr-IPsec-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "detach-remotecidr-IPsec-writedb";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConnectionInventory invOld = IPsecConnectionInventory.valueOf(self);
                        Set<String> oldLocal = invOld.getLocalL3Cidrs();
                        List<IPsecPeerCidrVO> refs = Q.New(IPsecPeerCidrVO.class)
                                .eq(IPsecPeerCidrVO_.connectionUuid, msg.getIPsecConnectionUuid())
                                .in(IPsecPeerCidrVO_.cidr, msg.getPeerCidrs()).list();
                        dbf.removeCollection(refs, IPsecPeerCidrVO.class);

                        self = dbf.reload(self);
                        IPsecConnectionInventory invNew = IPsecConnectionInventory.valueOf(self);
                        List<String> newPeer = invNew.getPeerCidrSignatures();
                        if (oldLocal == null || oldLocal.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.NONE);
                        } else if (newPeer == null || newPeer.isEmpty()) {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.DELETE);
                        } else {
                            data.put(IPsecConstants.Param.BACKEND_ACTION_TYPE, IPsecConstants.IPsecBackendAction.SYNC);
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<IPsecPeerCidrVO> refs = new ArrayList<>();
                        for (String cidr : msg.getPeerCidrs()) {
                            IPsecPeerCidrVO ref = new IPsecPeerCidrVO();
                            ref.setCidr(cidr);
                            ref.setConnectionUuid(msg.getIPsecConnectionUuid());
                            ref.setUuid(Platform.getUuid());
                            refs.add(ref);
                        }
                        dbf.persistCollection(refs);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-l3Network-IPsec-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IPsecConstants.IPsecBackendAction oper = (IPsecConstants.IPsecBackendAction)data.get(IPsecConstants.Param.BACKEND_ACTION_TYPE);
                        if (oper == IPsecConstants.IPsecBackendAction.NONE) {
                            trigger.next();
                            return;
                        }

                        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
                        IPsecConnectionSyncMsg smsg = new IPsecConnectionSyncMsg();
                        smsg.setInv(inv);
                        smsg.setL3NetworkUuid(inv.getLocalL3Networks().get(0));
                        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(inv.getLocalL3Networks().get(0), L3NetworkVO.class));
                        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
                        smsg.setVr(vrInv);
                        if ((self.getPeerCidrs() != null && !self.getPeerCidrs().isEmpty()) ||
                                (self.getL3Networks() != null && !self.getL3Networks().isEmpty())) {
                            smsg.setSkip_vip_release(true);
                        }
                        doSyncIPsecConnection(oper, smsg, new Completion(completion) {
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
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIRemoveRemoteCidrsFromIPsecConnectionMsg msg){
        APIRemoveRemoteCidrsFromIPsecConnectionEvent evt = new APIRemoveRemoteCidrsFromIPsecConnectionEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDetachRemoteCIDRFromIPsecConnection(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        evt.setInventory(IPsecConnectionInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("APIAttachL3NetworkToIPsecConnection-%s", msg.getUuid());
            }
        });
    }

    private void doDeleteionIPsecConnection(IPsecConnectionDeletionMsg msg, final Completion completion) {
        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getUuid(), IPsecConnectionVO.class));

        if (inv.getLocalL3Networks().isEmpty() || inv.getPeerCidrSignatures().isEmpty()) {
            /* delete usefor: ipsec from vip, it will not fail */
            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
            vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
            vipStruct.setServiceUuid(self.getUuid());
            Vip vip = new Vip(inv.getVipUuid());
            vip.setStruct(vipStruct);
            vip.release(new Completion(completion) {
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

        IPsecBackend bkd = ipsecMgr.getBackend(inv.getLocalL3Networks().get(0));
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(inv.getLocalL3Networks().get(0), L3NetworkVO.class));
        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
        bkd.deleteIPsecConnection(inv, inv.getLocalL3Networks().get(0), vrInv, false, new NoErrorCompletion(msg) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    protected void handle(IPsecConnectionDeletionMsg msg){
        IPsecConnectionDeletionReply reply = new IPsecConnectionDeletionReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteionIPsecConnection(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(msg.getUuid(), IPsecConnectionVO.class);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("IPsecConnectionDeletion-%s", msg.getUuid());
            }
        });
    }

    protected void handle(IPsecConnectionSyncMsg msg){
        IPsecConnectionSyncReply reply = new IPsecConnectionSyncReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(self);
                Set<String> localCidrs = inv.getLocalL3Cidrs();

                IPsecConstants.IPsecBackendAction oper;
                if (localCidrs == null || localCidrs.isEmpty()) {
                    oper = IPsecConstants.IPsecBackendAction.DELETE;
                } else {
                    oper = IPsecConstants.IPsecBackendAction.SYNC;
                }

                doSyncIPsecConnection(oper, msg, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("IPsecConnectionSync-%s", msg.getIPsecConnectionUuid());
            }
        });
    }
}
