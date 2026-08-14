package org.zstack.network.l2;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.identity.ResourceHelper;
import org.zstack.network.l3.ServiceTypeExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NoVlanNetwork implements L2Network {
    private static final CLogger logger = Utils.getLogger(L2NoVlanNetwork.class);
    private static final L2NetworkHostHelper l2NetworkHostHelper = new L2NetworkHostHelper();

    @Autowired
    protected L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2Mgr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;

    protected L2NetworkVO self;

    public L2NoVlanNetwork(L2NetworkVO self) {
        this.self = self;
    }

    public L2NoVlanNetwork() {
    }

    protected L2NetworkInventory getSelfInventory() {
        return L2NetworkInventory.valueOf(self);
    }

    public String getSyncId() {
        return String.format("l2-network-%s", self.getUuid());
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof DetachL2NetworkFromClusterMsg) {
            handle((DetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof DetachL2NetworkFromHostMsg) {
            handle((DetachL2NetworkFromHostMsg) msg);
        } else if (msg instanceof DeleteL2NetworkMsg) {
            handle((DeleteL2NetworkMsg) msg);
        } else if (msg instanceof AttachL2NetworkToClusterMsg) {
            handle((AttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof AttachL2NetworkToHostMsg) {
            handle((AttachL2NetworkToHostMsg) msg);
        } else if (msg instanceof L2NetworkDetachFromClusterMsg) {
            handle((L2NetworkDetachFromClusterMsg) msg);
        } else if (msg instanceof L2NetworkDetachFromHostMsg) {
            handle((L2NetworkDetachFromHostMsg) msg);
        } else if (msg instanceof BatchL2NetworkDetachFromHostMsg) {
            handle((BatchL2NetworkDetachFromHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(L2NetworkDetachFromClusterMsg msg) {
        L2NetworkDetachFromClusterReply reply = new L2NetworkDetachFromClusterReply();

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
        struct.setClusterUuid(msg.getClusterUuid());
        struct.setL2NetworkUuid(msg.getL2NetworkUuid());
        ctx.add(struct);
        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] from cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.reload(self);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(L2NetworkDetachFromHostMsg msg) {
        L2NetworkDetachFromHostReply reply = new L2NetworkDetachFromHostReply();

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
        struct.setHostUuid(msg.getHostUuid());
        struct.setL2NetworkUuid(msg.getL2NetworkUuid());
        ctx.add(struct);
        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_FROM_HOST_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] from host[uuid:%s]", self.getUuid(), msg.getHostUuid()));
                self = dbf.reload(self);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BatchL2NetworkDetachFromHostMsg msg) {
        BatchL2NetworkDetachFromHostReply reply = new BatchL2NetworkDetachFromHostReply();

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        for (String hostUuid : msg.getHostUuids()) {
            L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
            struct.setHostUuid(hostUuid);
            struct.setL2NetworkUuid(msg.getL2NetworkUuid());
            ctx.add(struct);
        }

        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_FROM_HOST_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] from hosts %s", self.getUuid(), msg.getHostUuids()));
                self = dbf.reload(self);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DeleteL2NetworkMsg msg) {
        DeleteL2NetworkReply reply = new DeleteL2NetworkReply();
        final String issuer = L2NetworkVO.class.getSimpleName();
        final List<L2NetworkInventory> ctx = L2NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-l2Network-%s", msg.getL2NetworkUuid()));
        if (!msg.isForceDelete()) {
            chain.then(new NoRollbackFlow() {
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
            }).then(new NoRollbackFlow() {
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
            chain.then(new NoRollbackFlow() {
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void syncManagementServiceType(ServiceTypeExtensionPoint ext, L2NetworkVO l2NetworkVO, List<String> hostUuids, boolean isDelete) {
        String l2NetworkType = l2NetworkVO.getType();
        switch (l2NetworkType) {
            case L2NetworkConstant.VXLAN_NETWORK_TYPE:
            case L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, "vxlan" + l2NetworkVO.getVirtualNetworkId(), null, isDelete);
                break;

            case L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE:
            case L2NetworkConstant.L2_VLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), isDelete);
                break;

            default:
                break;
        }
    }

    protected void afterDetachL2NetworkFromCluster(final DetachL2NetworkFromClusterMsg msg) {
        SQL.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .delete();

        if (!L2NetworkType.valueOf(self.getType()).isAttachToAllHosts()) {
            List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
            L2NetworkHostUtils.deleteL2NetworkHostRef(msg.getL2NetworkUuid(), hostUuids);
        }
    }

    private void handle(DetachL2NetworkFromClusterMsg msg) {
        if (!L2NetworkGlobalConfig.DeleteL2BridgePhysically.value(Boolean.class)) {
            afterDetachL2NetworkFromCluster(msg);

            DetachL2NetworkFromClusterReply reply = new DetachL2NetworkFromClusterReply();
            bus.reply(msg, reply);
        } else {
            DetachL2NetworkFromClusterReply reply = new DetachL2NetworkFromClusterReply();
            List<String> clusterUuids = new ArrayList<>();
            clusterUuids.add(msg.getClusterUuid());
            deleteL2Bridge(clusterUuids,
                    new Completion(msg) {
                        @Override
                        public void success() {
                            L2NetworkVO l2NetworkVO = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
                            List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                                    .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
                            boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                                    .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
                            if (isExistSystemL3) {
                                for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                                    syncManagementServiceType(ext, l2NetworkVO, hostUuids, true);
                                }
                            }

                            afterDetachL2NetworkFromCluster(msg);
                            bus.reply(msg, reply);
                        }

                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                        }
                    }
            );
        }
    }

    protected String getL2ProviderType(String clusterUuid) {
        for (L2NetworkClusterRefVO ref : self.getAttachedClusterRefs()) {
            if (clusterUuid.equals(ref.getClusterUuid())) {
                return ref.getL2ProviderType();
            }
        }

        return null;
    }

    protected String getL2ProviderTypeByHostUuid(String hostUuid) {
        for (L2NetworkHostRefVO ref : self.getAttachedHostRefs()) {
            if (hostUuid.equals(ref.getHostUuid())) {
                return ref.getL2ProviderType();
            }
        }

        return null;
    }

    protected void afterDetachL2NetworkFromHost(final DetachL2NetworkFromHostMsg msg) {
        if (!L2NetworkType.valueOf(self.getType()).isAttachToAllHosts()) {
            L2NetworkHostUtils.deleteL2NetworkHostRef(msg.getL2NetworkUuid(), msg.getHostUuid());
        }
    }

    private void handle(DetachL2NetworkFromHostMsg msg) {
        DetachL2NetworkFromHostReply reply = new DetachL2NetworkFromHostReply();

        if (!L2NetworkGlobalConfig.DeleteL2BridgePhysically.value(Boolean.class)) {
            afterDetachL2NetworkFromHost(msg);
            bus.reply(msg, reply);
        } else {
            HostVO host = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
            Tuple t = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid())
                    .select(HostVO_.clusterUuid, HostVO_.hypervisorType).findTuple();

            final String clusterUuid = t.get(0, String.class);
            final HypervisorType hvType = HypervisorType.valueOf(t.get(1, String.class));
            final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());
            final String providerType = getL2ProviderType(clusterUuid);

            L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType, providerType);

            ext.delete(getSelfInventory(), host.getUuid(), new Completion(msg) {
                @Override
                public void success() {
                    afterDetachL2NetworkFromHost(msg);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    afterDetachL2NetworkFromHost(msg);
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }

            });
        }
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
        final PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        L2NetworkClusterRefVO ref = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getHost().getClusterUuid()).find();

        prepareL2NetworkOnHosts(Arrays.asList(msg.getHost()), ref.getL2ProviderType(), new Completion(msg) {
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

    private void handle(final CheckL2NetworkOnHostMsg msg) {
        Tuple t = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid())
                .select(HostVO_.clusterUuid, HostVO_.hypervisorType).findTuple();

        final String clusterUuid = t.get(0, String.class);
        final HypervisorType hvType = HypervisorType.valueOf(t.get(1, String.class));
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());
        final String providerType = getL2ProviderType(clusterUuid);

        final CheckL2NetworkOnHostReply reply = new CheckL2NetworkOnHostReply();
        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType, providerType);
        ext.check(getSelfInventory(), msg.getHostUuid(), new Completion(msg) {
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

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        deleteHook(new Completion(msg) {
            @Override
            public void success() {
                extpEmitter.afterDelete(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToHostMsg) {
            handle((APIAttachL2NetworkToHostMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromHostMsg) {
            handle((APIDetachL2NetworkFromHostMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkMsg) {
            handle((APIUpdateL2NetworkMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkVirtualNetworkIdMsg) {
            handle((APIUpdateL2NetworkVirtualNetworkIdMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(final APIUpdateL2NetworkVirtualNetworkIdMsg msg){
        APIUpdateL2NetworkVirtualNetworkIdEvent event = new APIUpdateL2NetworkVirtualNetworkIdEvent(msg.getId());
        if (Objects.equals(self.getVirtualNetworkId(), msg.getVirtualNetworkId())) {
            logger.debug("no need to update the virtual network id, as it's the same");
            event.setInventory(getSelfInventory());
            bus.publish(event);
            return;
        }
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("update-l2-network-%s-virtual-network-id", msg.getL2NetworkUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateL2NetworkVirtualNetworkId(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        extpEmitter.afterUpdate(getSelfInventory());
                        event.setInventory(getSelfInventory());
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
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

    private void handle(APIUpdateL2NetworkMsg msg) {
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

        APIUpdateL2NetworkEvent evt = new APIUpdateL2NetworkEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        final APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
        struct.setClusterUuid(msg.getClusterUuid());
        struct.setL2NetworkUuid(msg.getL2NetworkUuid());
        ctx.add(struct);
        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] from cluster[uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.reload(self);
                evt.setInventory(self.toInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIDetachL2NetworkFromHostMsg msg) {
        final APIDetachL2NetworkFromHostEvent evt = new APIDetachL2NetworkFromHostEvent(msg.getId());

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
        struct.setHostUuid(msg.getHostUuid());
        struct.setL2NetworkUuid(msg.getL2NetworkUuid());
        ctx.add(struct);
        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_FROM_HOST_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] from host[uuid:%s]", self.getUuid(), msg.getHostUuid()));
                self = dbf.reload(self);
                evt.setInventory(self.toInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected void updateNetwork(L2NetworkInventory oldL2Network, L2NetworkInventory newL2Network,
                                 String hostUuid, String htype, String providerType, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType, providerType);
        ext.update(oldL2Network, newL2Network, hostUuid, completion);
    }

    protected void realizeNetwork(String hostUuid, String htype, String providerType, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType, providerType);
        ext.realize(getSelfInventory(), hostUuid, completion);
    }

    protected void afterAttachNetwork(String hostUuid, String htype, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkAttachClusterExtensionPoint ext = l2Mgr.getAttachClusterExtension(l2Type, hvType);
        if (ext == null) {
            completion.success();
        } else {
            ext.afterAttach(getSelfInventory(), hostUuid, completion);
        }
    }

    protected String getInterfaceNameOfHost(String hostUuid) {
        return self.getPhysicalInterface();
    }

    protected void checkNetworkPhysicalInterface(final List<HostInventory> hosts, final Completion completion) {
        if (hosts.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(hosts).step((host, wcomp) -> {
            CheckNetworkPhysicalInterfaceMsg cmsg = new CheckNetworkPhysicalInterfaceMsg();
            cmsg.setHostUuid(host.getUuid());
            cmsg.setPhysicalInterface(getInterfaceNameOfHost(host.getUuid()));
            bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(cmsg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        wcomp.addError(reply.getError());
                        wcomp.allDone();
                        return;
                    }
                    wcomp.done();
                }
            });
        }, hosts.size()).run((new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.hasError()) {
                    completion.fail(multiErr(errorCodeList));
                    return;
                }
                completion.success();
            }
        }));
    }

    protected Flow updateL2NetworkVirtualNetworkIdInDb(final APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        return new NoRollbackFlow() {
            String __name__ = "update-l2-network-virtual-network-id-in-db";

            @Transactional
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String currentType = self.getType();
                String targetType;
                if (msg.getVirtualNetworkId() == 0) {
                    targetType = L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
                } else {
                    targetType = L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
                }
                int targetVlan = msg.getVirtualNetworkId() == null ? 0 : msg.getVirtualNetworkId();

                if (!currentType.equals(targetType)) {
                    // change to L2VlanNetwork or L2NoVlanNetwork
                    L2NetworkVO l2Vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, self.getUuid()).find();
                    l2Vo.setType(targetType);
                    l2Vo.setVirtualNetworkId(targetVlan);
                    self = dbf.updateAndRefresh(l2Vo);
                    // can not operate L2VlanNetworkVO directly due to it extends from L2NetworkVO
                    // use native sql to insert or delete
                    if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(targetType)) {
                        // L2VlanNetwork change to L2NoVlanNetwork
                        dbf.getEntityManager().createNativeQuery(
                                String.format("insert into L2VlanNetworkVO (uuid, vlan) " +
                                        "values ('%s', %d)", l2Vo.getUuid(), targetVlan)
                        ).executeUpdate();
                    } else if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(targetType)) {
                        // L2NoVlanNetwork change to L2VlanNetwork
                        dbf.getEntityManager().createNativeQuery(
                                String.format("delete from L2VlanNetworkVO where uuid = '%s'", l2Vo.getUuid())
                        ).executeUpdate();
                    }
                } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(currentType)) {
                    // update L2VlanNetwork vlan id
                    L2VlanNetworkVO l2VlanNetworkVO = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, self.getUuid()).find();
                    l2VlanNetworkVO.setVirtualNetworkId(targetVlan);
                    l2VlanNetworkVO.setVlan(targetVlan);
                    self = dbf.updateAndRefresh(l2VlanNetworkVO);
                }
                trigger.next();
            }
        };
    }

    private void updateL2NetworkVirtualNetworkId(final APIUpdateL2NetworkVirtualNetworkIdMsg msg, final Completion completion) {
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("update-l2-%s-virtual-network-id-on-hosts", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                final List<HostVO> updatedHosts = Collections.synchronizedList(new ArrayList<>());
                L2NetworkInventory oldInv = self.toInventory();
                L2NetworkInventory newInv = self.toInventory();
                if (msg.getVirtualNetworkId() != null) {
                    newInv.setVirtualNetworkId(msg.getVirtualNetworkId());
                }

                flow(updateL2NetworkVirtualNetworkIdInDb(msg));

                flow(new Flow() {
                    String __name__ = "update-l2-network-virtual-network-id";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(L2NetworkHostUtils.getHostsByAttachedL2Network(getSelfInventory())).step((host, whileCompletion) -> {
                            updateNetwork(oldInv, newInv, host.getUuid(), host.getHypervisorType(),
                                    getL2ProviderTypeByHostUuid(host.getUuid()), new Completion(whileCompletion) {
                                @Override
                                public void success() {
                                    updatedHosts.add(host);
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.error(String.format("update l2 network in host:[%s] failed", host.getUuid()));
                                    whileCompletion.addError(errorCode);
                                    whileCompletion.allDone();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new While<>(updatedHosts).step((host, whileCompletion) -> {
                            updateNetwork(newInv, oldInv, host.getUuid(), host.getHypervisorType(),
                                    getL2ProviderTypeByHostUuid(host.getUuid()), new Completion(whileCompletion) {
                                @Override
                                public void success() {
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.error(String.format("rollback l2 network in host:[%s] failed", host.getUuid()));
                                    whileCompletion.done();
                                }
                            });
                        }, updatedHosts.size()).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.rollback();
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

    private void prepareL2NetworkOnHosts(final List<HostInventory> hosts, String providerType, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                checkNetworkPhysicalInterface(hosts, new Completion(trigger) {
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
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                new While<>(hosts).step((host, whileCompletion) -> {
                    realizeNetwork(host.getUuid(), host.getHypervisorType(), providerType, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.error(String.format("realize l2 network on host:[%s] failed", host.getUuid()));
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }

                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "after-l2-network-attached";

            private void after(final Iterator<HostInventory> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostInventory host = it.next();
                afterAttachNetwork(host.getUuid(), host.getHypervisorType(), new Completion(trigger) {
                    @Override
                    public void success() {
                        after(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                after(hosts.iterator(), trigger);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());

        amsg.setL2NetworkUuid(msg.getL2NetworkUuid());
        amsg.setClusterUuid(msg.getClusterUuid());
        amsg.setL2ProviderType(msg.getL2ProviderType());
        amsg.setHostParams(msg.getHostParams());

        bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
        bus.send(amsg, new CloudBusCallBack(amsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(getSelfInventory());
                    bus.publish(evt);
                } else {
                    evt.setError(err(L2Errors.ATTACH_ERROR, "attach l2 network[uuid:%s] to cluster[uuid:%s] failed:%s",
                            msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError()));
                    bus.publish(evt);
                }
            }
        });

    }

    private void handle(final AttachL2NetworkToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                AttachL2NetworkToClusterReply reply = new AttachL2NetworkToClusterReply();

                attachL2NetworkToCluster(msg, new Completion(chain) {
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
                return String.format("attach-l2-network-%s-to-cluster-%s", msg.getL2NetworkUuid(), msg.getClusterUuid());
            }
        });
    }

    private void handle(final APIAttachL2NetworkToHostMsg msg) {
        AttachL2NetworkToHostMsg amsg = new AttachL2NetworkToHostMsg();
        final APIAttachL2NetworkToHostEvent evt = new APIAttachL2NetworkToHostEvent(msg.getId());

        amsg.setL2NetworkUuid(msg.getL2NetworkUuid());
        amsg.setHostUuid(msg.getHostUuid());
        amsg.setL2ProviderType(msg.getL2ProviderType());
        amsg.setHostParam(msg.getHostParam());

        bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, amsg.getL2NetworkUuid());
        bus.send(amsg, new CloudBusCallBack(amsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(getSelfInventory());
                    bus.publish(evt);
                } else {
                    evt.setError(err(L2Errors.ATTACH_ERROR, "attach l2 network[uuid:%s] to host[uuid:%s] failed:%s",
                            msg.getL2NetworkUuid(), msg.getHostUuid(), reply.getError()));
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(AttachL2NetworkToHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                AttachL2NetworkToHostReply reply = new AttachL2NetworkToHostReply();

                attachL2NetworkToHost(msg, new Completion(chain) {
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
                return String.format("attach-l2-network-%s-to-host-%s", msg.getL2NetworkUuid(), msg.getHostUuid());
            }
        });
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
        final APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
        final String issuer = L2NetworkVO.class.getSimpleName();
        final List<L2NetworkInventory> ctx = L2NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-l2Network-%s", msg.getL2NetworkUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
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
            }).then(new NoRollbackFlow() {
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
            chain.then(new NoRollbackFlow() {
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                ResourceHelper.cleanShareRecords(msg.getL2NetworkUuid());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    protected List<HostInventory> getAttachableHostsInCluster(String clusterUuid, List<HostParam> hostParams) {
        List<HostVO> hosts = Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid)
                .notIn(HostVO_.state, asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected).list();
        return HostInventory.valueOf(hosts);
    }

    protected String makeBridgeName() {
        return null;
    }

    protected void beforeAttachL2NetworkToCluster(final AttachL2NetworkToClusterMsg msg, final List<HostInventory> hosts) {
        if (!L2NetworkType.valueOf(self.getType()).isAttachToAllHosts()) {
            List<String> attachableHosts = hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList());
            l2NetworkHostHelper.initL2NetworkHostRef(msg.getL2NetworkUuid(), attachableHosts,
                    msg.getL2ProviderType(), makeBridgeName());
        }
    }

    protected void afterAttachL2NetworkToCluster(final AttachL2NetworkToClusterMsg msg, final List<HostInventory> hosts) {
    }

    protected void afterAttachL2NetworkToClusterFailed(final AttachL2NetworkToClusterMsg msg) {
        if (!L2NetworkType.valueOf(self.getType()).isAttachToAllHosts()) {
            List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
            L2NetworkHostUtils.deleteL2NetworkHostRef(msg.getL2NetworkUuid(), hostUuids);
        }
    }

    @SuppressWarnings("unchecked")
    private void attachL2NetworkToCluster(final AttachL2NetworkToClusterMsg msg, final Completion completion) {
        long count = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid()).count();
        if (count != 0) {
            completion.success();
            return;
        }

        new SQLBatch() {

            @Override
            protected void scripts() {

                String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();

                if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(type)) {
                    List<L2NetworkVO> l2s = SQL.New("select l2" +
                                    " from L2NetworkVO l2, L2NetworkClusterRefVO ref" +
                                    " where l2.uuid = ref.l2NetworkUuid" +
                                    " and ref.clusterUuid = :clusterUuid" +
                                    " and type = 'L2NoVlanNetwork'")
                            .param("clusterUuid", msg.getClusterUuid()).list();

                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2NetworkVO tl2 = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).find();
                    for (L2NetworkVO l2 : l2s) {
                        if (l2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new ApiMessageInterceptionException(argerr("There has been a l2Network[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s]. Failed to attach l2Network[uuid:%s]",
                                    l2.getUuid(), l2.getName(), msg.getClusterUuid(), l2.getPhysicalInterface(), tl2.getUuid()));
                        }
                    }
                } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(type)) {
                    List<L2VlanNetworkVO> l2s = SQL.New("select l2" +
                                    " from L2VlanNetworkVO l2, L2NetworkClusterRefVO ref" +
                                    " where l2.uuid = ref.l2NetworkUuid" +
                                    " and ref.clusterUuid = :clusterUuid")
                            .param("clusterUuid", msg.getClusterUuid()).list();
                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2VlanNetworkVO tl2 = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, msg.getL2NetworkUuid()).find();

                    for (L2VlanNetworkVO vl2 : l2s) {
                        if (vl2.getVlan() == tl2.getVlan() && vl2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new OperationFailureException(argerr("There has been a L2VlanNetwork[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s], vlan[%s]. Failed to attach L2VlanNetwork[uuid:%s]",
                                    vl2.getUuid(), vl2.getName(), msg.getClusterUuid(), vl2.getPhysicalInterface(), vl2.getVlan(), tl2.getUuid()));
                        }
                    }
                }

            }

        }.execute();

        L2NetworkVO l2NetworkVO = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid()).listValues();
        boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
        if (isExistSystemL3) {
            for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                syncManagementServiceType(ext, l2NetworkVO, hostUuids, false);
            }
        }

        List<HostParam> hostParams = new ArrayList<>();
        if (!StringUtils.isEmpty(msg.getHostParams())) {
            hostParams.addAll(JSONObjectUtil.toCollection(msg.getHostParams(), ArrayList.class, HostParam.class));
        }
        List<HostInventory> invs = getAttachableHostsInCluster(msg.getClusterUuid(), hostParams);
        logger.debug(String.format("%s[uuid:%s, name:%s] get attached hosts[%s]",
                self.getType(), self.getUuid(), self.getName(),
                invs.stream().map(HostInventory::getName).collect(Collectors.toList())));

        beforeAttachL2NetworkToCluster(msg, invs);
        prepareL2NetworkOnHosts(invs, msg.getL2ProviderType(), new Completion(msg, completion) {
            @Override
            public void success() {
                afterAttachL2NetworkToCluster(msg, invs);
                L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                rvo.setClusterUuid(msg.getClusterUuid());
                rvo.setL2NetworkUuid(self.getUuid());
                rvo.setL2ProviderType(msg.getL2ProviderType());
                dbf.persistAndRefresh(rvo);
                logger.debug(String.format("successfully attached L2Network[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                afterAttachL2NetworkToClusterFailed(msg);
                completion.fail(errorCode);
            }
        });
    }

    protected void beforeAttachL2NetworkToHost(final AttachL2NetworkToHostMsg msg) {
        l2NetworkHostHelper.initL2NetworkHostRef(msg.getL2NetworkUuid(), msg.getHostUuid(),
                msg.getL2ProviderType(), makeBridgeName());
    }

    protected void afterAttachL2NetworkToHost(final AttachL2NetworkToHostMsg msg) {
    }

    protected void afterAttachL2NetworkToHostFailed(final AttachL2NetworkToHostMsg msg) {
        L2NetworkHostUtils.deleteL2NetworkHostRef(msg.getL2NetworkUuid(), msg.getHostUuid());
    }

    private void attachL2NetworkToHost(final AttachL2NetworkToHostMsg msg, final Completion completion) {
        if (L2NetworkHostUtils.checkIfL2AttachedToHost(msg.getL2NetworkUuid(), msg.getHostUuid())) {
            completion.success();
            return;
        }

        beforeAttachL2NetworkToHost(msg);
        HostInventory inv = HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class));
        prepareL2NetworkOnHosts(Collections.singletonList(inv), msg.getL2ProviderType(), new Completion(msg, completion) {
            @Override
            public void success() {
                afterAttachL2NetworkToHost(msg);
                logger.debug(String.format("successfully attached L2Network[uuid:%s] to host [uuid:%s]", self.getUuid(), msg.getHostUuid()));
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                afterAttachL2NetworkToHostFailed(msg);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteHook(Completion completion) {
        if (L2NetworkGlobalConfig.DeleteL2BridgePhysically.value(Boolean.class)) {
            L2NetworkInventory l2Inv = getSelfInventory();
            deleteL2Bridge(l2Inv.getAttachedClusterUuids(), completion);
        } else {
            completion.success();
        }
    }

    protected void deleteL2Bridge(List<String> clusterUuids, Completion completion) {
        if (clusterUuids.isEmpty()) {
            logger.debug(String.format("no need to delete l2 bridge ,because l2 network[uuid:%s] is not added to any cluster",
                    getSelfInventory().getUuid()));
            completion.success();
            return;
        }

        List<HostVO> hosts = Q.New(HostVO.class).in(HostVO_.clusterUuid, clusterUuids).list();

        deleteL2BridgeFromHosts(hosts, completion);
    }

    protected void deleteL2BridgeFromHosts(List<HostVO> hostss, Completion completion) {
        Map<String, String> hostProviderTypeMap = new HashMap<>();
        for (HostVO vo : hostss) {
            for (L2NetworkClusterRefVO ref : self.getAttachedClusterRefs()) {
                if (vo.getClusterUuid().equals(ref.getClusterUuid())) {
                    hostProviderTypeMap.put(vo.getUuid(), ref.getL2ProviderType());
                    break;
                }
            }
        }

        new While<>(hostss).step((host, compl) -> {
            HypervisorType hvType = HypervisorType.valueOf(host.getHypervisorType());
            L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());
            String providerType = hostProviderTypeMap.get(host.getUuid());

            L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType, providerType);

            ext.delete(getSelfInventory(), host.getUuid(), new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }

            });
        }, 10).run((new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    logger.debug(String.format("delete bridge fail [error is %s ], but ignore", errorCodeList.getCauses().get(0).toString()));
                }
                completion.success();

            }
        }));
    }
}
