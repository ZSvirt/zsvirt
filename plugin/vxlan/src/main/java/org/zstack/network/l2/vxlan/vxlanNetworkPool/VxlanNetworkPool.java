package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.*;
import org.zstack.header.errorcode.SysErrors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.vxlan.vtep.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetworkPool extends L2NoVlanNetwork implements L2VxlanNetworkPoolManager {
    private static final CLogger logger = Utils.getLogger(VxlanNetworkPool.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2Mgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private Map<String, VniAllocatorStrategy> vniAllocatorStrategies = Collections.synchronizedMap(new HashMap<String, VniAllocatorStrategy>());

    public VxlanNetworkPool(L2NetworkVO vo) {
        super(vo);
    }

    public VxlanNetworkPool(){
        super(null);
    }

    private VxlanNetworkPoolVO getSelf() {
        return dbf.findByUuid(self.getUuid(), VxlanNetworkPoolVO.class);
    }

    protected L2NetworkInventory getSelfInventory() {
        return L2VxlanNetworkPoolInventory.valueOf(getSelf());
    }

    @Override
    public void deleteHook(Completion completion) {
        completion.success();
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
        if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof AllocateVniMsg) {
            handle((AllocateVniMsg) msg);
        } else if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CreateVtepMsg) {
            handle((CreateVtepMsg) msg);
        } else if (msg instanceof DeleteVtepMsg) {
            handle((DeleteVtepMsg) msg);
        } else if (msg instanceof PopulateVtepPeersMsg){
            handle((PopulateVtepPeersMsg) msg);
        } else if (msg instanceof PopulateRemoteVtepPeersMsg){
            handle((PopulateRemoteVtepPeersMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        }  else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(final PopulateRemoteVtepPeersMsg msg) {
        final PopulateRemoteVtepPeersReply reply = new PopulateRemoteVtepPeersReply();

        List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, msg.getPoolUuid()).list();
        if (vteps == null || vteps.size() < 1) {
            logger.debug("no need to populate fdb since there are only one vtep or less");
            bus.reply(msg, reply);
            return;
        }

        List<HostInventory> targets = msg.getHosts();
        if (targets.isEmpty()) {
            List<String> hostUuids = vteps.stream().map(VtepVO::getHostUuid).collect(Collectors.toList());
            Set<String> clusterUuids = vteps.stream().map(VtepVO::getClusterUuid).collect(Collectors.toSet());
            targets = HostInventory.valueOf(Q.New(HostVO.class)
                    .in(HostVO_.uuid, hostUuids).in(HostVO_.clusterUuid, clusterUuids).list());
        }

        List<String> vxlanNetworkUuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid)
                .eq(VxlanNetworkVO_.poolUuid, msg.getPoolUuid())
                .listValues();
        if(vxlanNetworkUuids.isEmpty()){
            logger.debug("no need to populate fdb because there is no vxlannetwork");
            bus.reply(msg, reply);
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(targets).step((host, completion1) -> {
            List<String> peers = new ArrayList<>();
            peers.add(msg.getVtepIp());

            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd();
            cmd.setPeers(peers);
            cmd.setNetworkUuids(vxlanNetworkUuids);

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setHostUuid(host.getUuid());
            kmsg.setCommand(cmd);
            kmsg.setPath(VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH);
            kmsg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    }
                    completion1.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    reply.setError(errList.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final PopulateVtepPeersMsg msg) {
        final PopulateVtepPeersReply reply = new PopulateVtepPeersReply();

        List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, msg.getPoolUuid()).list();
        if (vteps == null || vteps.size() <= 1) {
            logger.debug("no need to populate fdb since there are only one vtep or less");
            bus.reply(msg, reply);
            return;
        }

        List<HostInventory> targets = msg.getHosts();
        if (targets.isEmpty()) {
            List<String> hostUuids = vteps.stream().map(VtepVO::getHostUuid).collect(Collectors.toList());
            Set<String> clusterUuids = vteps.stream().map(VtepVO::getClusterUuid).collect(Collectors.toSet());
            targets = HostInventory.valueOf(Q.New(HostVO.class)
                    .in(HostVO_.uuid, hostUuids).in(HostVO_.clusterUuid, clusterUuids).list());
        }

        List<String> vxlanNetworkUuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid)
                .eq(VxlanNetworkVO_.poolUuid, msg.getPoolUuid())
                .listValues();
        if(vxlanNetworkUuids.isEmpty()){
            logger.debug("no need to populate fdb because there is no vxlannetwork");
            bus.reply(msg, reply);
            return;
        }

        new While<>(targets).all((host, completion1) -> {
            List<VtepVO> peerVteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, msg.getPoolUuid())
                    .notEq(VtepVO_.hostUuid, host.getUuid()).list();
            Set<String> peers = peerVteps.stream()
                    .map(v -> v.getVtepIp())
                    .collect(Collectors.toSet());

            logger.info(String.format("populate fdb to host[ip:%s] for vxlan network pool %s with vxlan network[uuids:%s] to host[uuid:%s]",
                    host.getManagementIp(), msg.getPoolUuid(), vxlanNetworkUuids, host.getUuid()));

            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd();
            // if remote vtep ip exist ,add it
            String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, host.getUuid()).findValue();
            List<String> gwpeers = Q.New(RemoteVtepVO.class).select(RemoteVtepVO_.vtepIp).eq(RemoteVtepVO_.poolUuid, msg.getPoolUuid()).eq(RemoteVtepVO_.clusterUuid, clusterUuid).listValues();
            if (gwpeers.size() > 0) {
                Set<String> gwp = new HashSet<String>(gwpeers);
                peers.addAll(gwp);
            }

            cmd.setPeers(new ArrayList<>(peers));
            cmd.setNetworkUuids(vxlanNetworkUuids);

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setHostUuid(host.getUuid());
            kmsg.setCommand(cmd);
            kmsg.setPath(VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH);
            kmsg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }
                    completion1.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
        final PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        prepareL2NetworkOnHosts(msg.getL2NetworkUuid(), Arrays.asList(msg.getHost()), new Completion(msg) {
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

    private void handle(AllocateVniMsg msg) {
        VniAllocatorType strategyType = msg.getAllocateStrategy() == null ? RandomVniAllocatorStrategy.type : VniAllocatorType.valueOf(msg.getAllocateStrategy());
        VniAllocatorStrategy vas = getVniAllocatorStrategy(strategyType);
        AllocateVniReply reply = new AllocateVniReply();
        Integer vni = vas.allocateVni(msg);
        if (vni == null) {
            reply.setError(err(L2Errors.ALLOCATE_VNI_ERROR, "Vni allocator strategy[%s] returns nothing, because no vni is available in this VxlanNetwork[name:%s, uuid:%s]", strategyType, self.getName(), self.getUuid()));
        } else {
            logger.debug(String.format("Vni allocator strategy[%s] successfully allocates an vni[%s]", strategyType, vni));
            reply.setVni(vni);
        }

        bus.reply(msg, reply);
    }

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        deleteHook(new Completion(msg) {
            @Override
            public void success() {
                deleteL2Network(msg.getL2NetworkUuid(), msg.isForceDelete(), new Completion(msg) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
                        extpEmitter.afterDelete(inv);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
                        extpEmitter.afterDelete(inv);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    protected void handle(CreateVtepMsg msg) {
        List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, msg.getPoolUuid()).eq(VtepVO_.vtepIp, msg.getVtepIp()).list();
        if (vteps.size() != 0) {
            VtepVO vtep = vteps.get(0);
            if (!vtep.getHostUuid().equals(msg.getHostUuid())) {
                logger.warn(String.format("same vtepip[%s] in host[%s] and host[%s], which in same cluster[%s]",
                        msg.getVtepIp(), vtep.getHostUuid(), msg.getHostUuid(), msg.getClusterUuid()));
            } else {
                logger.debug(String.format("get duplicate vtep create msg for ip [%s] in host [%s]",
                        msg.getVtepIp(), vtep.getHostUuid()));
            }

            CreateVtepReply reply = new CreateVtepReply();
            reply.setInventory(VtepInventory.valueOf(vtep));
            bus.reply(msg, reply);

            return;
        }

        VtepVO vo = new VtepVO();

        vo.setUuid(Platform.getUuid());
        vo.setClusterUuid(msg.getClusterUuid());
        vo.setHostUuid(msg.getHostUuid());
        vo.setPort(msg.getPort());
        vo.setType(msg.getType());
        vo.setVtepIp(msg.getVtepIp());
        vo.setPhysicalInterface(msg.getPhysicalInterface());
        vo.setPoolUuid(msg.getPoolUuid());
        try {
            vo = dbf.persistAndRefresh(vo);
        } catch (Throwable t) {
            if (!ExceptionDSL.isCausedBy(t, ConstraintViolationException.class)) {
                throw t;
            }

            // the vtep is already attached
        }

        VtepInventory inv = VtepInventory.valueOf(vo);
        String info = String.format("successfully create Vtep, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);

        CreateVtepReply reply = new CreateVtepReply();
        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    protected void handle(DeleteVtepMsg msg) {
        DeleteVtepReply reply = new DeleteVtepReply();
        VtepVO vo = dbf.findByUuid(msg.getVtepUuid(), VtepVO.class);
        dbf.remove(vo);
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateVniRangeMsg) {
            handle((APICreateVniRangeMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDeleteVniRangeMsg) {
            handle((APIDeleteVniRangeMsg) msg);
        } else if (msg instanceof APIUpdateVniRangeMsg) {
            handle((APIUpdateVniRangeMsg) msg);
        } else if (msg instanceof APICreateVxlanVtepMsg) {
            handle((APICreateVxlanVtepMsg) msg);
        } else if (msg instanceof APICreateVxlanPoolRemoteVtepMsg) {
            handle((APICreateVxlanPoolRemoteVtepMsg) msg);
        } else if (msg instanceof APIDeleteVxlanPoolRemoteVtepMsg) {
            handle((APIDeleteVxlanPoolRemoteVtepMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    private void handle(final APIDeleteVxlanPoolRemoteVtepMsg msg) {
        APIDeleteVxlanPoolRemoteVtepEvent evt = new APIDeleteVxlanPoolRemoteVtepEvent(msg.getId());

        long count = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .count();
        if (count == 0) {
            bus.publish(evt);
            return;
        }


        List<String> ips = Q.New(RemoteVtepVO.class)
                .select(RemoteVtepVO_.vtepIp).eq(RemoteVtepVO_.poolUuid, msg.getL2NetworkUuid())
                .eq(RemoteVtepVO_.clusterUuid, msg.getClusterUuid())
                .eq(RemoteVtepVO_.vtepIp, msg.getRemoteVtepIp()).listValues();

        if (ips.isEmpty()) {
            logger.info(String.format("there are no remote vtep ip for vxlanpool:[%s]", msg.getL2NetworkUuid()));
            bus.publish(evt);
            return;
        }
        logger.info(String.format("delete remote vtep ip for vxlanpool:[%s]", msg.getL2NetworkUuid()));
        RemoteVtepVO vo = Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, msg.getL2NetworkUuid())
                    .eq(RemoteVtepVO_.clusterUuid, msg.getClusterUuid())
                    .eq(RemoteVtepVO_.vtepIp, msg.getRemoteVtepIp()).find();

        List<String> vxlanNetworkUuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid)
                .eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid())
                .listValues();
        // no br based vxlan, delete db only
        if (vxlanNetworkUuids.size() == 0) {
            dbf.remove(vo);
            bus.publish(evt);
            return;
        }
        // delete based vxlan bridge remote vtep ip info
        final List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .notIn(HostVO_.state, list(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);
        // cluster no hosts, only remove db
        if (hvinvs == null || hvinvs.isEmpty()) {
            dbf.remove(vo);
            bus.publish(evt);
            logger.debug(String.format("clusterUuid:[%s] no host", msg.getClusterUuid()));
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(hvinvs).step((host, completion1) -> {

            VxlanKvmAgentCommands.DeleteVxlanNetworksFdbCmd cmd = new VxlanKvmAgentCommands.DeleteVxlanNetworksFdbCmd();
            List<String> peers = new ArrayList<>();
            peers.add(msg.getRemoteVtepIp());

            cmd.setPeers(peers);
            cmd.setNetworkUuids(vxlanNetworkUuids);

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setHostUuid(host.getUuid());
            kmsg.setCommand(cmd);
            kmsg.setPath(VXLAN_KVM_DELETE_FDB_L2VXLAN_NETWORKS_PATH);
            kmsg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    }
                    completion1.done();
                }
            });
       }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    evt.setError(errList.getCauses().get(0));
                } else {
                    dbf.remove(vo);
                }
            }
        });

        bus.publish(evt);
    }

    private void syncVxlanPoolRemoteVtepDataBase(final APICreateVxlanPoolRemoteVtepMsg msg, APICreateVxlanPoolRemoteVtepEvent evt) {
        String uuid;
        if (msg.getResourceUuid() != null) {
            uuid = msg.getResourceUuid();
        } else {
            uuid = Platform.getUuid();
        }

        RemoteVtepInventory inv;
        RemoteVtepVO vvo = new RemoteVtepVO();

        vvo.setUuid(uuid);;
        vvo.setVtepIp(msg.getRemoteVtepIp());
        vvo.setClusterUuid(msg.getClusterUuid());
        vvo.setPoolUuid(msg.getL2NetworkUuid());
        vvo.setPort(VXLAN_PORT);
        vvo.setType(KVM_VXLAN_TYPE);
        vvo = dbf.persistAndRefresh(vvo);

        inv = RemoteVtepInventory.valueOf(vvo);
        evt.setInventory(inv);
        bus.publish(evt);
        return;
    }

    private void handle(final APICreateVxlanPoolRemoteVtepMsg msg) {
        APICreateVxlanPoolRemoteVtepEvent evt = new APICreateVxlanPoolRemoteVtepEvent(msg.getId());

        long count = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .count();
        if (count == 0) {
            evt.setError(err(SysErrors.RESOURCE_NOT_FOUND, "Cannot find L2NetworkClusterRefVO item for l2NetworkUuid[%s] clusterUuid[%s]", msg.getL2NetworkUuid(), msg.getClusterUuid()));
            bus.publish(evt);
            return;
        }

        count = Q.New(RemoteVtepVO.class)
                .eq(RemoteVtepVO_.clusterUuid, msg.getClusterUuid())
                .eq(RemoteVtepVO_.poolUuid, msg.getL2NetworkUuid())
                .eq(RemoteVtepVO_.vtepIp, msg.getRemoteVtepIp())
                .count();
        if (count > 0) {
            evt.setError(err(SysErrors.OPERATION_ERROR, "ip[%s] l2NetworkUuid[%s] clusterUuid[%s] exist", msg.getRemoteVtepIp(), msg.getL2NetworkUuid(), msg.getClusterUuid()));
            bus.publish(evt);
            return;
        }

        List<VxlanNetworkVO> vxlanNetworkVOS;
        vxlanNetworkVOS = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).list();
        // no br based vxlan, add db only
        // when add br based vxlan, will add gw ip to bridge fdb
        if (vxlanNetworkVOS == null || vxlanNetworkVOS.isEmpty()) {
            syncVxlanPoolRemoteVtepDataBase(msg, evt);
            return;
        }
        //based vxlan br already exist, add gw ip to bridge fdb
        final List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .notIn(HostVO_.state, list(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);
        // cluster no hosts, only add db
        if (hvinvs == null || hvinvs.isEmpty()) {
            logger.debug(String.format("clusterUuid:[%s] no host", msg.getClusterUuid()));
            syncVxlanPoolRemoteVtepDataBase(msg, evt);
            return;
        }

        prepareL2NetworkVxlanRemoteVtepOnHosts(msg, hvinvs, new Completion(msg) {
            @Override
            public void success() {
                syncVxlanPoolRemoteVtepDataBase(msg, evt);
                logger.debug(String.format("successfully attached L2VxlanNetworkPool[uuid:%s] to cluster [uuid:%s]", msg.getL2NetworkUuid(), msg.getClusterUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }


    private void prepareL2NetworkVxlanRemoteVtepOnHosts(final APICreateVxlanPoolRemoteVtepMsg msg, final List<HostInventory> hosts, final Completion completion) {
        final String l2NetworkUuid = msg.getL2NetworkUuid();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-vxlan-gateway-%s-on-hosts", l2NetworkUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();

                new While<>(hosts).step((h, completion1) -> {
                    CheckL2NetworkOnHostMsg cmsg = new CheckL2NetworkOnHostMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setL2NetworkUuid(l2NetworkUuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2NetworkUuid);
                    bus.send(cmsg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                            }
                            completion1.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("populate-remote-vtep-for-l2-vxlan-pool-%s", l2NetworkUuid);
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PopulateRemoteVtepPeersMsg pmsg = new PopulateRemoteVtepPeersMsg();
                pmsg.setHosts(hosts);
                pmsg.setVtepIp(msg.getRemoteVtepIp());
                pmsg.setPoolUuid(l2NetworkUuid);
                bus.makeTargetServiceIdByResourceUuid(pmsg, L2NetworkConstant.SERVICE_ID, l2NetworkUuid);
                bus.send(pmsg, new CloudBusCallBack(pmsg){
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.debug(String.format("fail to populate remote vtep for l2 vxlan pool %s", l2NetworkUuid));
                            trigger.fail(reply.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
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


    protected void handle(final APICreateVxlanVtepMsg msg) {
        APICreateVxlanVtepEvent evt = new APICreateVxlanVtepEvent(msg.getId());
        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();

        VtepVO vtep = new VtepVO();
        vtep.setUuid(Platform.getUuid());
        vtep.setClusterUuid(host.getClusterUuid());
        vtep.setHostUuid(msg.getHostUuid());
        vtep.setPoolUuid(msg.getPoolUuid());
        vtep.setPort(VxlanNetworkPoolConstant.VXLAN_PORT);
        vtep.setType(VxlanNetworkPoolConstant.KVM_VXLAN_TYPE);
        vtep.setVtepIp(msg.getVtepIp());

        vtep = dbf.persistAndRefresh(vtep);
        evt.setInventory(VtepInventory.valueOf(vtep));

        PopulateVtepPeersMsg pmsg = new PopulateVtepPeersMsg();
        pmsg.setPoolUuid(msg.getPoolUuid());
        bus.makeTargetServiceIdByResourceUuid(pmsg, L2NetworkConstant.SERVICE_ID, msg.getPoolUuid());
        bus.send(pmsg, new CloudBusCallBack(msg){
            @Override
            public void run(MessageReply reply) {
                bus.publish(evt);
            }
        });
    }

    protected void afterDetachVxlanPoolFromCluster(APIDetachL2NetworkFromClusterMsg msg) {
        String tag = TagUtils.tagPatternToSqlPattern(VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.instantiateTag(map(
                e(VxlanSystemTags.VXLAN_POOL_UUID_TOKEN, msg.getL2NetworkUuid()),
                e(VxlanSystemTags.CLUSTER_UUID_TOKEN, msg.getClusterUuid()))
        ));

        VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.delete(msg.getL2NetworkUuid(), tag);
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        final APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-l2-vxlan-pool-%s", msg.getL2NetworkUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "detach-l2-vxlan-network-in-pool";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuids = Q.New(VxlanNetworkVO.class)
                        .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).listValues();

                if (uuids.isEmpty()) {
                    logger.info(String.format("There are no vxlan networks for vxlan pool %s", msg.getL2NetworkUuid()));
                    trigger.next();
                    return;
                }

                logger.info(String.format("Detach l2 vxlan networks %s for vxlan pool %s", uuids, msg.getL2NetworkUuid()));

                new While<>(uuids).all((uuid, completion) -> {
                    L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setClusterUuid(msg.getClusterUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            } else {
                                logger.debug(String.format("Detach l2 vxlan network %s for vxlan pool %s success", uuid, msg.getL2NetworkUuid()));
                            }
                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "detach-l2-vxlan-pool";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                dmsg.setClusterUuid(msg.getClusterUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                bus.send(dmsg, new CloudBusCallBack(dmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                afterDetachVxlanPoolFromCluster(msg);

                trigger.next();
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                evt.setInventory(self.toInventory());
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

    private void handle(final APICreateVniRangeMsg msg) {
        VniRangeVO vo = new VniRangeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setStartVni(msg.getStartVni());
        vo.setEndVni(msg.getEndVni());
        vo.setL2NetworkUuid(msg.getL2NetworkUuid());
        vo = dbf.persistAndRefresh(vo);
        VniRangeInventory inv = VniRangeInventory.valueOf(vo);
        String info = String.format("successfully create VniRange, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);

        APICreateVniRangeEvent evt = new APICreateVniRangeEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    protected void afterAttachVxlanPoolFromClusterFailed(APIAttachL2NetworkToClusterMsg msg) {
        for (String tag : msg.getSystemTags()) {
            VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.delete(msg.getL2NetworkUuid(), tag);
        }
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());
        long count = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .count();
        if (count != 0) {
            evt.setInventory(self.toInventory());
            bus.publish(evt);
            return;
        }

        if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
            for (String tag : msg.getSystemTags()) {
                tagMgr.createNonInherentSystemTag(msg.getL2NetworkUuid(), tag, L2NetworkVO.class.getSimpleName());
            }
        }

        final List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .notIn(HostVO_.state, list(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        prepareL2NetworkOnHosts(msg.getL2NetworkUuid(), hvinvs, new Completion(msg) {
            @Override
            public void success() {
                L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                rvo.setClusterUuid(msg.getClusterUuid());
                rvo.setL2NetworkUuid(self.getUuid());
                dbf.persist(rvo);

                List<String> uuids = Q.New(VxlanNetworkVO.class)
                        .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).listValues();
                for (String uuid : uuids) {
                    rvo = new L2NetworkClusterRefVO();
                    rvo.setClusterUuid(msg.getClusterUuid());
                    rvo.setL2NetworkUuid(uuid);
                    dbf.persist(rvo);
                }


                logger.debug(String.format("successfully attached L2VxlanNetworkPool[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                evt.setInventory(self.toInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                afterAttachVxlanPoolFromClusterFailed(msg);
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    void deleteL2Network(String poolUuid, Boolean forceDelete, Completion completion1){
        List<String> uuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, poolUuid).listValues();
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(uuids).each((String uuid, WhileCompletion completion) -> {
            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
            dmsg.setUuid(uuid);
            dmsg.setForceDelete(forceDelete);
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    }
                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion1) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion1.fail(errList.getCauses().get(0));
                } else {
                    completion1.success();
                }
            }
        });
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
        deleteL2Network(msg.getUuid(), msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Enforcing, new Completion(msg){
            @Override
            public void success() {
                superHandle((L2NetworkMessage) msg);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
                bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIDeleteVniRangeMsg msg) {
        APIDeleteVniRangeEvent evt = new APIDeleteVniRangeEvent(msg.getId());
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();

        List<String> uuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, vo.getL2NetworkUuid())
                .gte(VxlanNetworkVO_.vni, vo.getStartVni()).lte(VxlanNetworkVO_.vni, vo.getEndVni()).listValues();

        if (uuids.isEmpty()) {
            logger.info(String.format("there are no vxlan networks for vni range[%s] and delete vni range directly", msg.getUuid()));
            dbf.remove(vo);
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
            bus.publish(evt);
        }

        logger.info(String.format("delete l2 vxlan networks[%s] for vni range[%s]", uuids, msg.getUuid()));

        new While<>(uuids).all((uuid, completion) -> {
            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
            dmsg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    } else {
                        logger.debug(String.format("delete l2 vxlan network %s for vni range %s success", uuid, msg.getL2NetworkUuid()));
                    }
                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                dbf.remove(vo);
                bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIUpdateVniRangeMsg msg) {
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();
        vo.setName(msg.getName());
        vo = dbf.updateAndRefresh(vo);

        APIUpdateVniRangeEvent event = new APIUpdateVniRangeEvent(msg.getId());
        event.setInventory(new VniRangeInventory(vo));
        bus.publish(event);

        logger.info(String.format("update l2 vxlan vni range[%s] name[%s]", msg.getUuid(), msg.getName()));
    }

    private void prepareL2NetworkOnHosts(final String l2NetworkUuid, final List<HostInventory> hosts, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        List<String> vtepIpChanged = new ArrayList<>();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();

                new While<>(hosts).all((h, completion1) -> {
                    VtepVO oldvtep = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2NetworkUuid).eq(VtepVO_.hostUuid, h.getUuid()).find();
                    CheckL2NetworkOnHostMsg cmsg = new CheckL2NetworkOnHostMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setL2NetworkUuid(l2NetworkUuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2NetworkUuid);
                    bus.send(cmsg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                            }
                            VtepVO newvtep = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2NetworkUuid).eq(VtepVO_.hostUuid, h.getUuid()).find();
                            if (oldvtep == null ||!oldvtep.getVtepIp().equals(newvtep.getVtepIp())) {
                                vtepIpChanged.add(h.getUuid());
                            }
                            completion1.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            // clean Vtep if faild attach vxlanPool to cluster by ZSTAC-41263
                            if(vtepIpChanged != null && !vtepIpChanged.isEmpty()) {
                                SQL.New(VtepVO.class).eq(VtepVO_.poolUuid, l2NetworkUuid).in(VtepVO_.hostUuid, vtepIpChanged).delete();
                            }
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("populate-vtep-for-l2-vxlan-pool-%s", l2NetworkUuid);
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PopulateVtepPeersMsg pmsg = new PopulateVtepPeersMsg();
                if (vtepIpChanged.isEmpty()) {
                    /* if some vtepip of some host changed, new vtep ip address need to be update to all host */
                    pmsg.setHosts(hosts);
                }
                pmsg.setPoolUuid(l2NetworkUuid);
                bus.makeTargetServiceIdByResourceUuid(pmsg, L2NetworkConstant.SERVICE_ID, l2NetworkUuid);
                bus.send(pmsg, new CloudBusCallBack(pmsg){
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.debug(String.format("fail to populate vtep for l2 vxlan pool %s", l2NetworkUuid));
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            private void realize(final Iterator<HostInventory> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostInventory host = it.next();
                realizeNetwork(host.getUuid(), host.getHypervisorType(), new Completion(trigger) {
                    @Override
                    public void success() {
                        realize(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                realize(hosts.iterator(), trigger);
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

    protected void realizeNetwork(String hostUuid, String htype, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.realize(getSelfInventory(), hostUuid, completion);
    }

    private void superHandle(L2NetworkMessage msg) {
        super.handleMessage((Message) msg);
    }

    @Override
    public VniAllocatorStrategy getVniAllocatorStrategy(VniAllocatorType type) {
        for (VniAllocatorStrategy f : pluginRgty.getExtensionList(VniAllocatorStrategy.class)) {
            VniAllocatorStrategy old = vniAllocatorStrategies.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VniAllocatorStrategy[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            vniAllocatorStrategies.put(f.getType().toString(), f);
        }

        VniAllocatorStrategy factory = vniAllocatorStrategies.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find VniAllocatorStrategy for type(%s)", type));
        }

        return factory;
    }

    public Map<String, String> getAttachedCidrs(String l2NetworkUuid) {
        List<Map<String, String>> tokenList = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(l2NetworkUuid);

        Map<String, String> attachedClusters = new HashMap<>();
        for (Map<String, String> tokens : tokenList) {
            attachedClusters.put(tokens.get(VxlanSystemTags.CLUSTER_UUID_TOKEN),
                    tokens.get(VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]")[1]);
        }
        return attachedClusters;
    }

}
