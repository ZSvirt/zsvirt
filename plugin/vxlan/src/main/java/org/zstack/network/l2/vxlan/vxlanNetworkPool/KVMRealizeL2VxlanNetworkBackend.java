package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.InstantiateResourceOnAttachingNicExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.network.l2.vxlan.vtep.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.network.service.MtuGetter;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 17/04/2017.
 */
public class KVMRealizeL2VxlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint, InstantiateResourceOnAttachingNicExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static String VTEP_IP = "vtepIp";
    private static String PHYSICAL_INTERFACE = "physicalInterface";
    private static String NEED_POPULATE = "needPopulate";

    public static String makeBridgeName(int vxlan) {
        return String.format("br_vx_%s", vxlan);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;
        final List<String> vtepIps = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.hostUuid, hostUuid).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
        if (vtepIps.size() > 1) {
            throw new OperationFailureException(operr("find multiple vtep ips[%s] for one host[uuid:%s], need to delete host and add again",
                    vtepIps, hostUuid));
        }

        if (vtepIps.size() == 0) {
            ErrorCode err = operr("failed to find vtep on host[uuid: %s], please re-attach vxlanpool[uuid: %s] to cluster.",
                    hostUuid, l2vxlan.getPoolUuid());
            completion.fail(err);
            return;
        }

        String vtepIp = vtepIps.get(0);
        List<String> peers = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
        Set<String> p = new HashSet<String>(peers);
        p.remove(vtepIp);
        peers.clear();
        peers.addAll(p);

        //add remote vtep ip
        String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();

        List<String> gwpeers = Q.New(RemoteVtepVO.class).select(RemoteVtepVO_.vtepIp).eq(RemoteVtepVO_.poolUuid, l2vxlan.getPoolUuid()).eq(RemoteVtepVO_.clusterUuid, clusterUuid).listValues();
        if (gwpeers.size() > 0) {
            Set<String> gwp = new HashSet<String>(gwpeers);
            peers.addAll(gwp);
        }

        String info = String.format(
                "get vtep peers [%s] and vtep ip [%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s]", peers,
                vtepIp, l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid);
        logger.debug(info);

        List<Integer> dstports = Q.New(VtepVO.class).select(VtepVO_.port)
                .eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid())
                .eq(VtepVO_.hostUuid, hostUuid)
                .eq(VtepVO_.vtepIp, vtepIp)
                .listValues();
        Integer dstport = dstports.get(0);

        final VxlanKvmAgentCommands.CreateVxlanBridgeCmd cmd = new VxlanKvmAgentCommands.CreateVxlanBridgeCmd();
        cmd.setVtepIp(vtepIp);
        cmd.setBridgeName(getBridgeName(l2vxlan));
        cmd.setVni(l2vxlan.getVni());
        cmd.setL2NetworkUuid(l2Network.getUuid());
        cmd.setPeers(peers);
        cmd.setDstport(dstport);
        cmd.setMtu(new MtuGetter().getL2Mtu(l2Network));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setNoStatusCheck(noStatusCheck);
        msg.setPath(VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                VxlanKvmAgentCommands.CreateVxlanBridgeResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CreateVxlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to create bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s], because %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format(
                        "successfully realize bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid);
                logger.debug(info);

                SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                creator.inherent = true;
                creator.ignoreIfExisting = true;
                creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                creator.create();

                completion.success();
            }
        });
    }

    @Override
    public void update(final L2NetworkInventory oldL2Network, final L2NetworkInventory newL2Network,
                       final String hostUuid, final Completion completion) {
        final KVMAgentCommands.UpdateBridgeCmd cmd = new KVMAgentCommands.UpdateBridgeCmd();

        String vxlanPoolUuid = Q.New(VxlanNetworkVO.class).select(VxlanNetworkVO_.poolUuid)
                .eq(VxlanNetworkVO_.uuid, oldL2Network.getUuid()).findValue();
        List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, vxlanPoolUuid).list();
        List<String> peers = vteps.stream()
                .filter(v -> !v.getHostUuid().equals(hostUuid))
                .map(VtepVO::getVtepIp)
                .collect(Collectors.toList());
        cmd.setPhysicalInterfaceName(newL2Network.getPhysicalInterface());
        cmd.setBridgeName(getBridgeName(oldL2Network));
        cmd.setOldVirtualNetworkId(oldL2Network.getVirtualNetworkId());
        cmd.setNewVirtualNetworkId(newL2Network.getVirtualNetworkId());
        cmd.setL2NetworkUuid(newL2Network.getUuid());
        cmd.setPeers(peers);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(false);
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(KVMConstant.KVM_UPDATE_L2VXLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.UpdateL2NetworkResponse rsp = hreply.toResponse(KVMAgentCommands.UpdateL2NetworkResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to update bridge[%s] for l2Network[uuid:%s, name:%s] on kvm host[uuid: %s], %s",
                            cmd.getBridgeName(), newL2Network.getUuid(), newL2Network.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully update bridge[%s] for l2Network[uuid:%s, name:%s] on kvm host[uuid: %s]",
                        cmd.getBridgeName(), newL2Network.getUuid(), newL2Network.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });

    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, false, completion);
    }

    public void check(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        final VxlanNetworkPoolVO poolVO = Q.New(VxlanNetworkPoolVO.class).eq(VxlanNetworkPoolVO_.uuid, l2vxlan.getPoolUuid()).find();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("check-l2-vxlan-%s-on-host-%s", l2Network.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VxlanKvmAgentCommands.CheckVxlanCidrCmd cmd = new VxlanKvmAgentCommands.CheckVxlanCidrCmd();
                cmd.setCidr(getAttachedCidrs(l2vxlan.getPoolUuid()).get(clusterUuid));
                if (!poolVO.getPhysicalInterface().isEmpty()) {
                    cmd.setPhysicalInterfaceName(poolVO.getPhysicalInterface());
                }
                VtepVO vtep = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).eq(VtepVO_.hostUuid, hostUuid).find();
                if (vtep != null) {
                    cmd.setVtepip(vtep.getVtepIp());
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setPath(VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH);
                msg.setNoStatusCheck(noStatusCheck);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CheckVxlanCidrResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CheckVxlanCidrResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                                    cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid, rsp.getError());
                            trigger.fail(err);
                            return;
                        }

                        String info = String.format("successfully checked cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                                cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid);
                        logger.debug(info);
                        data.put(VTEP_IP, rsp.getVtepIp());
                        data.put(PHYSICAL_INTERFACE, rsp.getPhysicalInterfaceName());

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VtepVO> vtepVOS = Q.New(VtepVO.class)
                        .eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid())
                        .eq(VtepVO_.hostUuid, hostUuid)
                        .list();

                if (vtepVOS == null || vtepVOS.isEmpty()) {
                    /* vtep is not created, no action here */
                } else if (vtepVOS.size() > 1) {
                    /* more than 1 vtep, shoult not happen */
                    throw new CloudRuntimeException(String.format("multiple vteps[ips: %s] found on host[uuid: %s]",
                            vtepVOS.stream().map(v -> v.getVtepIp()).collect(Collectors.toSet()), hostUuid));
                } else if (vtepVOS.get(0).getVtepIp().equals(data.get(VTEP_IP))) {
                    /* vtep is already created */
                    if (data.get(PHYSICAL_INTERFACE) != null &&
                            !data.get(PHYSICAL_INTERFACE).equals(vtepVOS.get(0).getPhysicalInterface())) {
                        vtepVOS.get(0).setPhysicalInterface((String) data.get(PHYSICAL_INTERFACE));
                        dbf.update(vtepVOS.get(0));
                    }
                    logger.debug(String.format(
                            "vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s] checks successfully",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    data.put(NEED_POPULATE, false);
                    trigger.next();
                    return;
                } else {
                    /* remove old vtep */
                    logger.debug(String.format(
                            "remove deprecated vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s]",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    dbf.remove(vtepVOS.get(0));
                }

                data.put(NEED_POPULATE, true);
                CreateVtepMsg cmsg = new CreateVtepMsg();
                cmsg.setPoolUuid(l2vxlan.getPoolUuid());
                cmsg.setClusterUuid(clusterUuid);
                cmsg.setHostUuid(hostUuid);
                cmsg.setPort(VXLAN_PORT);
                cmsg.setVtepIp((String) data.get(VTEP_IP));
                cmsg.setPhysicalInterface((String) data.get(PHYSICAL_INTERFACE));
                cmsg.setType(KVM_VXLAN_TYPE);

                bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2vxlan.getPoolUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                            trigger.fail(reply.getError());
                            return;
                        }
                        logger.debug(String.format("created new vtep [%s] on vxlan network pool [%s]", cmsg.getVtepIp(), ((L2VxlanNetworkInventory) l2Network).getPoolUuid()));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (data.get(NEED_POPULATE).equals(false)) {
                    trigger.next();
                    return;
                }

                List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).list();

                if (vteps.size() == 1) {
                    logger.debug("no need to populate fdb since there are only one vtep");
                    trigger.next();
                    return;
                }

                new While<>(vteps).all((vtep, completion1) -> {
                    List<String> peers = new ArrayList<>();
                    for (VtepVO vo : vteps) {
                        if (peers.contains(vo.getVtepIp()) || vo.getVtepIp().equals(vtep.getVtepIp())) {
                            continue;
                        } else {
                            peers.add(vo.getVtepIp());
                        }
                    }

                    logger.info(String.format("populate fdb for vtep %s in vxlan network %s", vtep.getVtepIp(), l2vxlan.getUuid()));

                    VxlanKvmAgentCommands.PopulateVxlanFdbCmd cmd = new VxlanKvmAgentCommands.PopulateVxlanFdbCmd();
                    cmd.setPeers(peers);
                    cmd.setVni(l2vxlan.getVni());

                    KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                    msg.setHostUuid(vtep.getHostUuid());
                    msg.setCommand(cmd);
                    msg.setPath(VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH);
                    msg.setNoStatusCheck(noStatusCheck);
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                    bus.send(msg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            }
                            completion1.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
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

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(VxlanNetworkConstant.VXLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return getSupportedL2NetworkType();
    }

    @Override
    public KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        final Integer vni = getVni(l2Network.getUuid());
        KVMAgentCommands.NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
        to.setBridgeName(getBridgeName(l2Network));
        to.setMetaData(String.valueOf(vni));
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
        return to;
    }

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        if (KVMSystemTags.L2_BRIDGE_NAME.hasTag(l2Network.getUuid(), L2NetworkVO.class)) {
            return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Network.getUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }
        final Integer vni = getVni(l2Network.getUuid());
        return makeBridgeName(vni);
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

    private Integer getVni(String l2NetworkUuid) {
        return Q.New(VxlanNetworkVO.class)
                .eq(VxlanNetworkVO_.uuid, l2NetworkUuid)
                .select(VxlanNetworkVO_.vni)
                .findValue();
    }

    @Override
    public void instantiateResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, Completion completion) {
        L2NetworkVO vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).find();
        if (!vo.getType().equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE)) {
            completion.success();
            return;
        } else {
            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            L2VxlanNetworkInventory l2 = L2VxlanNetworkInventory.valueOf((VxlanNetworkVO) Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.uuid, vo.getUuid()).find());
            chain.setName(String.format("attach-l2-vxlan-%s-on-host-%s", l2.getUuid(), spec.getDestHost().getUuid()));
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    check(l2, spec.getDestHost().getUuid(), new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("check l2 vxlan failed:%n%s", errorCode.getReadableDetails()));
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    realize(l2, spec.getDestHost().getUuid(), new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("realize l2 vxlan failed:%n%s", errorCode.getReadableDetails()));
                            trigger.fail(errorCode);
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
    }

    @Override
    public void releaseResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, NoErrorCompletion completion) {
        completion.done();
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;

        final VxlanKvmAgentCommands.DeleteVxlanBridgeCmd cmd = new VxlanKvmAgentCommands.DeleteVxlanBridgeCmd();
        cmd.setBridgeName(getBridgeName(l2vxlan));
        cmd.setVni(l2vxlan.getVni());
        cmd.setL2NetworkUuid(l2Network.getUuid());

        final List<String> vtepIps = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.hostUuid, hostUuid).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
        String vtepIp = vtepIps.get(0);
        cmd.setVtepIp(vtepIp);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                VxlanKvmAgentCommands.DeleteVxlanBridgeResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.DeleteVxlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to delete bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s], because %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String message = String.format(
                        "successfully delete bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid);
                logger.debug(message);

                completion.success();
            }
        });
    }
}
