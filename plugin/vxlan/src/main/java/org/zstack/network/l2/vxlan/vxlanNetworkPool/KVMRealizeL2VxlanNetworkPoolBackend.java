package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.network.l2.vxlan.vtep.CreateVtepMsg;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.*;
import org.zstack.network.service.MtuGetter;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 20/03/2017.
 */
public class KVMRealizeL2VxlanNetworkPoolBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkPoolBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VxlanNetworkFactory VxlanNetworkFactory;

    private static String VTEP_IP = "vtepIp";
    private static String PHYSICAL_INTERFACE = "physicalInterface";
    private static String NEED_POPULATE = "needPopulate";

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        completion.success();
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, true, completion);
    }

    public void check(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VxlanNetworkPoolInventory vxlanPool = (L2VxlanNetworkPoolInventory) l2Network;
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("check-l2-vxlan-pool-%s-on-host-%s", l2Network.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("check-vtep-ip-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VxlanKvmAgentCommands.CheckVxlanCidrCmd cmd = new VxlanKvmAgentCommands.CheckVxlanCidrCmd();
                cmd.setCidr(getAttachedCidrs(vxlanPool.getUuid()).get(clusterUuid));
                if (!l2Network.getPhysicalInterface().isEmpty()) {
                    cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
                }
                VtepVO vtep = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, vxlanPool.getUuid()).eq(VtepVO_.hostUuid, hostUuid).find();
                if (vtep != null) {
                    cmd.setVtepip(vtep.getVtepIp());
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setPath(VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH);
                msg.setNoStatusCheck(noStatusCheck);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CheckVxlanCidrResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CheckVxlanCidrResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetworkPool[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                                    cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid, rsp.getError());
                            completion.fail(err);
                            return;
                        }

                        data.put(VTEP_IP, rsp.getVtepIp());
                        data.put(PHYSICAL_INTERFACE, rsp.getPhysicalInterfaceName());
                        String info = String.format("successfully checked cidr[%s] for l2VxlanNetworkPool[uuid:%s, name:%s] on kvm host[uuid:%s]",
                                cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid);
                        logger.debug(info);
                        trigger.next();
                    }
                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = String.format("create-vtep-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                data.put(NEED_POPULATE, false);

                if (data.get(VTEP_IP) == null) {
                    trigger.next();
                    return;
                }

                List<VtepVO> vtepVOS = Q.New(VtepVO.class)
                        .eq(VtepVO_.poolUuid, l2Network.getUuid())
                        .eq(VtepVO_.hostUuid, hostUuid)
                        .list();

                if (vtepVOS == null || vtepVOS.isEmpty()) {
                    /* vtep is not created */
                } else if (vtepVOS.size() > 1) {
                    throw new CloudRuntimeException(String.format("multiple vteps[ips: %s] found on host[uuid: %s]",
                            vtepVOS.stream().map(VtepVO::getVtepIp).collect(Collectors.toSet()), hostUuid));

                } else if (vtepVOS.get(0).getVtepIp().equals(data.get(VTEP_IP))) {
                    if (data.get(PHYSICAL_INTERFACE) != null &&
                            !data.get(PHYSICAL_INTERFACE).equals(vtepVOS.get(0).getPhysicalInterface())) {
                        vtepVOS.get(0).setPhysicalInterface((String) data.get(PHYSICAL_INTERFACE));
                        dbf.update(vtepVOS.get(0));
                    }
                    logger.debug(String.format(
                            "vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s] checks successfully",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    trigger.next();
                    return;
                } else {
                    logger.debug(String.format(
                            "remove deprecated vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s]",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    dbf.remove(vtepVOS.get(0));
                }

                logger.debug(String.format(
                        "creating vtep[ip:%s] on host[uuid:%s] for l2 vxlan network pool[uuid:%s]",
                        data.get(VTEP_IP), hostUuid, l2Network.getUuid()));

                CreateVtepMsg cmsg = new CreateVtepMsg();
                cmsg.setPoolUuid(l2Network.getUuid());
                cmsg.setClusterUuid(clusterUuid);
                cmsg.setHostUuid(hostUuid);
                cmsg.setPort(VXLAN_PORT);
                cmsg.setVtepIp((String) data.get(VTEP_IP));
                cmsg.setPhysicalInterface((String) data.get(PHYSICAL_INTERFACE));
                cmsg.setType(KVM_VXLAN_TYPE);

                bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2Network.getUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                            trigger.fail(reply.getError());
                            return;
                        }
                        logger.debug(String.format("created new vtep [%s] on vxlan network pool [%s]", cmsg.getVtepIp(), l2Network.getUuid()));
                        data.put(NEED_POPULATE, true);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("realize-vtep-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (data.get(NEED_POPULATE).equals(false) && VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(Boolean.class)) {
                    logger.debug("no need populate for vxlan networks");
                    trigger.next();
                    return;
                }
                List<VxlanNetworkVO> vxlanNetworkVOS;
                if (VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(Boolean.class)) {
                    vxlanNetworkVOS = SQL.New("select vxlan from VxlanNetworkVO vxlan, VmNicVO nic, L3NetworkVO l3 " +
                                    "where vxlan.uuid = l3.l2NetworkUuid " +
                                    "and nic.l3NetworkUuid = l3.uuid " +
                                    "and vxlan.poolUuid = :poolUuid " +
                                    "group by vxlan.uuid")
                            .param("poolUuid", l2Network.getUuid())
                            .list();
                } else {
                    vxlanNetworkVOS = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, l2Network.getUuid()).list();
                }

                if (vxlanNetworkVOS == null || vxlanNetworkVOS.isEmpty()) {
                    trigger.next();
                    return;
                }

                final List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2Network.getUuid()).list();

                if (vteps == null || vteps.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<Integer> dstports = Q.New(VtepVO.class).select(VtepVO_.port)
                        .eq(VtepVO_.poolUuid, l2Network.getUuid())
                        .eq(VtepVO_.hostUuid, hostUuid)
                        .eq(VtepVO_.vtepIp, (String) data.get(VTEP_IP))
                        .listValues();
                Integer dstport = dstports.get(0);

                final VxlanKvmAgentCommands.CreateVxlanBridgesCmd cmd = new VxlanKvmAgentCommands.CreateVxlanBridgesCmd();
                cmd.setBridgeCmds(new ArrayList<>());
                for (VxlanNetworkVO vo : vxlanNetworkVOS) {
                    VxlanKvmAgentCommands.CreateVxlanBridgeCmd bridgeCmd = new VxlanKvmAgentCommands.CreateVxlanBridgeCmd();
                    bridgeCmd.setVtepIp((String) data.get(VTEP_IP));
                    bridgeCmd.setBridgeName(getBridgeName(vo.toInventory()));
                    bridgeCmd.setVni(vo.getVni());
                    bridgeCmd.setDstport(dstport);
                    bridgeCmd.setL2NetworkUuid(vo.getUuid());
                    bridgeCmd.setMtu(new MtuGetter().getL2Mtu(L2VxlanNetworkInventory.valueOf(vo)));
                    cmd.getBridgeCmds().add(bridgeCmd);
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setNoStatusCheck(noStatusCheck);
                msg.setPath(VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CreateVxlanBridgeResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CreateVxlanBridgeResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to realize vxlan network pool[uuid:%s, type:%s] on kvm host[uuid:%s], because %s",
                                    l2Network.getUuid(), l2Network.getType(), hostUuid, rsp.getError());
                            trigger.fail(err);
                            return;
                        }

                        String info = String.format(
                                "successfully realize vxlan network pool[uuid:%s, type:%s] on kvm host[uuid:%s]",
                                l2Network.getUuid(), l2Network.getType(), hostUuid);
                        logger.debug(info);

                        for (VxlanNetworkVO vo : vxlanNetworkVOS) {
                            SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(vo.getUuid());
                            creator.inherent = true;
                            creator.ignoreIfExisting = true;
                            creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN,
                                    getBridgeName(vo.toInventory()))));
                            creator.create();
                        }
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
        return L2NetworkType.valueOf(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf("KVM");
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return getSupportedL2NetworkType();
    }

    @Override
    public KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        KVMAgentCommands.NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
        return to;
    }

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), VxlanNetworkVO.class);
        if (KVMSystemTags.L2_BRIDGE_NAME.hasTag(l2Network.getUuid(), L2NetworkVO.class)) {
            return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Network.getUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }
        return KVMRealizeL2VxlanNetworkBackend.makeBridgeName(vo.getVni());
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

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}
