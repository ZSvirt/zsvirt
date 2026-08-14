package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

public class VirtualRouterPortForwardingBackend extends AbstractVirtualRouterBackend implements
        PortForwardingBackend, Component, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint,
        VirtualRouterHaGetCallbackExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterPortForwardingBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private PortForwardingConfigProxy proxy;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private EventFacade evtf;


    private final String APPLY_PF_TASK = "applyPF";
    private final String REVOKE_PF_TASK = "revokePF";

    private List<String> applyPortForwardingRuleElements;
    private FlowChainBuilder applyRuleChainBuilder;
    private FlowChainBuilder releaseRuleChainBuilder;
    private List<String> releasePortForwardingRuleElements;

    private PortForwardingRuleTO makePortForwardingRuleTO(final PortForwardingStruct struct, VirtualRouterVmInventory vr) {
        VmNicInventory selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                arg -> arg.getL3NetworkUuid().equals(struct.getGuestL3Network().getUuid()));
        String privateMac = selected == null ? null : selected.getMac();

        selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                arg -> arg.getL3NetworkUuid().equals(struct.getVipL3Network().getUuid()));
        String publicMac = selected == null ? null : selected.getMac();

        DebugUtils.Assert(privateMac!=null, String.format("cannot find guest nic[l3NetworkUuid:%s] on virtual router[uuid:%s, name:%s]",
                struct.getGuestL3Network().getUuid(), vr.getUuid(), vr.getName()));

        PortForwardingRuleTO to = new PortForwardingRuleTO();
        to.setUuid(struct.getRule().getUuid());
        to.setAllowedCidr(struct.getRule().getAllowedCidr());
        to.setPrivateIp(struct.getGuestIp());
        to.setPrivateMac(privateMac);
        to.setPrivatePortEnd(struct.getRule().getPrivatePortEnd());
        to.setPrivatePortStart(struct.getRule().getPrivatePortStart());
        to.setVipIp(struct.getVip().getIp());
        to.setVipPortEnd(struct.getRule().getVipPortEnd());
        to.setSnatInboundTraffic(struct.isSnatInboundTraffic());
        to.setVipPortStart(struct.getRule().getVipPortStart());
        to.setProtocolType(struct.getRule().getProtocolType());
        to.setPublicMac(publicMac);
        return to;
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findRunningVirtualRouterForRule(String ruleUuid) {
        List<String> vrUuids = proxy.getVrUuidsByNetworkService(PortForwardingRuleVO.class.getSimpleName(), ruleUuid);
        if (vrUuids == null || vrUuids.isEmpty()) {
            return null;
        }

        String sql = "select vr from VirtualRouterVmVO vr where vr.uuid in (:vrUuids) and vr.state = :vrState";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("vrUuids", vrUuids);
        q.setParameter("vrState", VmInstanceState.Running);
        q.setMaxResults(1);
        List<VirtualRouterVmVO> vrs = q.getResultList();
        if (vrs.isEmpty()) {
            return null;
        } else {
            return VirtualRouterVmInventory.valueOf(vrs.get(0));
        }
    }

    private void buildWorkFlowsBuilder() {
        try{
            applyRuleChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(applyPortForwardingRuleElements).construct();
            releaseRuleChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(releasePortForwardingRuleElements).construct();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void setApplyPortForwardingRuleElements(List<String> applyPortForwardingRuleElements) {
        this.applyPortForwardingRuleElements = applyPortForwardingRuleElements;
    }

    public void setReleasePortForwardingRuleElements(List<String> releasePortForwardingRuleElements) {
        this.releasePortForwardingRuleElements = releasePortForwardingRuleElements;
    }

    @Override
    public boolean start() {
        buildWorkFlowsBuilder();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applyRule(final Iterator<PortForwardingStruct> it, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final PortForwardingStruct struct = it.next();
        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(struct.getGuestL3Network());
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (!offering.getPublicNetworkUuid().equals(struct.getVip().getL3NetworkUuid())) {
                    throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to PortForwarding rule[uuid:%s]'s; you may need to use system tag" +
                                    " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network", offering.getUuid(), struct.getGuestL3Network().getUuid(), struct.getGuestL3Network().getZoneUuid(), struct.getVip().getL3NetworkUuid(), struct.getRule().getUuid()));
                }
            }
        });

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                applyRuleToVirtualRouter(struct, vr, new Completion(completion) {
                    @Override
                    public void success() {
                        new VirtualRouterRoleManager().makePortForwardingRole(vr.getUuid());
                        proxy.attachNetworkService(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName(), asList(struct.getRule().getUuid()));
                        applyRuleOnHaVirtualRouter(struct, vr, new Completion(completion) {
                            @Override
                            public void success() {
                                applyRule(it, completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void applyRuleToVirtualRouter(final PortForwardingStruct struct, final VirtualRouterVmInventory vr, final Completion completion) {
        final PortForwardingRuleTO to = makePortForwardingRuleTO(struct, vr);
        FlowChain chain = applyRuleChainBuilder.build();

        chain.setName(String.format("vr-apply-port-forwarding-rule-%s", struct.getRule().getUuid()));
        chain.getData().put(VirtualRouterConstant.VR_RESULT_VM, vr);
        chain.getData().put(VirtualRouterConstant.VR_PORT_FORWARDING_RULE, to);
        chain.getData().put(VirtualRouterConstant.VR_VIP, struct.getVip());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void applyPortForwardingRule(PortForwardingStruct struct, Completion completion) {
        PortForwardingRuleInventory rule = struct.getRule();
        if ((rule.getVipPortStart() != rule.getPrivatePortStart() || rule.getVipPortEnd() != rule.getPrivatePortEnd()) && (rule.getVipPortStart() != rule.getVipPortEnd()) && (rule.getPrivatePortStart() != rule.getPrivatePortEnd())) {
            throw new OperationFailureException(operr("virtual router doesn't support port forwarding range redirection, the vipPortStart must be equals to privatePortStart and vipPortEnd must be equals to privatePortEnd;" +
                            "but this rule rule has a mismatching range: vip port[%s, %s], private port[%s, %s]", rule.getVipPortStart(), rule.getVipPortEnd(), rule.getPrivatePortStart(), rule.getPrivatePortEnd()));
        }

        applyRule(Arrays.asList(struct).iterator(), completion);
    }

    public void revokeRuleOnVirtualRouter(final PortForwardingStruct struct, VirtualRouterVmInventory vr, final Completion completion) {
        PortForwardingRuleTO to = makePortForwardingRuleTO(struct, vr);

        FlowChain chain = releaseRuleChainBuilder.build();
        chain.setName(String.format("vr-release-port-forwarding-rule-%s", struct.getRule().getUuid()));
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(VirtualRouterConstant.VR_RESULT_VM, vr);
        ctx.put(VirtualRouterConstant.VR_PORT_FORWARDING_RULE, to);
        ctx.put(VirtualRouterConstant.VR_VIP_L3NETWORK, struct.getVip().getL3NetworkUuid());

        chain.setData(ctx).done(new FlowDoneHandler(completion) {
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
    public void revokePortForwardingRule(PortForwardingStruct struct, Completion completion) {
        VirtualRouterVmInventory vr = findRunningVirtualRouterForRule(struct.getRule().getUuid());
        if (vr == null) {
            // vr is either destroyed or not running
            // no need to do anything in each case, if vr is not running, rules will get synced once it gets running
            completion.success();
            return;
        }

        revokeRuleOnVirtualRouter(struct, vr, new Completion(completion) {
            @Override
            public void success() {
                proxy.detachNetworkService(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName(), asList(struct.getRule().getUuid()));
                revokeRuleOnHaVirtualRouter(struct, vr, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private List<PortForwardingRuleTO> findPortforwardingsOnVmNic(VmNicInventory nic, VirtualRouterVmInventory vr) {
        List<Tuple> pfs = findPortForwardingTuplesOnVmNic(nic);

        if (pfs == null || pfs.isEmpty()) {
            return null;
        }

        List<PortForwardingRuleTO> tos = new ArrayList<>();
        for (Tuple t : pfs) {
            PortForwardingRuleVO pf = t.get(0, PortForwardingRuleVO.class);
            VipVO vipVO = dbf.findByUuid(pf.getVipUuid(), VipVO.class);
            PortForwardingRuleTO to = new PortForwardingRuleTO();
            to.setUuid(pf.getUuid());
            to.setAllowedCidr(pf.getAllowedCidr());
            to.setPrivateIp(t.get(1, String.class));

            Optional<VmNicInventory> priNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(nic.getL3NetworkUuid()))
                    .findFirst();
            if (!priNic.isPresent()) {
                continue;
            }
            to.setPrivateMac(priNic.get().getMac());

            Optional<VmNicInventory> publicNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(vipVO.getL3NetworkUuid()))
                    .findFirst();
            if (!publicNic.isPresent()) {
                continue;
            }
            to.setPublicMac(publicNic.get().getMac());
            to.setPrivatePortStart(pf.getPrivatePortStart());
            to.setPrivatePortEnd(pf.getPrivatePortEnd());
            to.setProtocolType(pf.getProtocolType().toString());
            to.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            to.setVipIp(pf.getVipIp());
            to.setVipPortStart(pf.getVipPortStart());
            to.setVipPortEnd(pf.getVipPortEnd());
            tos.add(to);
        }

        return tos;
    }

    private List<Tuple> findPortForwardingTuplesOnVmNic(VmNicInventory nic) {
        return SQL.New("select pf, nic.ip, nic.mac " +
                    "from PortForwardingRuleVO pf, VmNicVO nic " +
                    "where pf.vmNicUuid = nic.uuid " +
                    "and nic.l3NetworkUuid = :l3Uuid " +
                    "and pf.state = :enabledState", Tuple.class)
                    .param("l3Uuid", nic.getL3NetworkUuid())
                    .param("enabledState", PortForwardingRuleState.Enabled)
                    .list();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), PortForwardingConstant.PORTFORWARDING_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<PortForwardingRuleTO> pfs = findPortforwardingsOnVmNic(nic, vr);
        if (pfs == null || pfs.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.CreatePortForwardingRuleCmd cmd = new VirtualRouterCommands.CreatePortForwardingRuleCmd();
        cmd.setRules(pfs);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrVO.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.SyncEipRsp ret = re.toResponse(VirtualRouterCommands.SyncEipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to add portforwardings on virtual router[uuid:%s], %s",
                            vrVO.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    String info = String.format("sync port forwardings on virtual router[uuid:%s] successfully",
                            vrVO.getUuid());
                    logger.debug(info);
                    proxy.attachNetworkService(vrVO.getUuid(), PortForwardingRuleVO.class.getSimpleName(),
                            pfs.stream().map(PortForwardingRuleTO::getUuid).collect(Collectors.toList()));
                    fireFirewallEvent(vr.getUuid());
                    completion.success();
                }
            }
        });
    }

    private void fireFirewallEvent(String vRouterUuid) {
        FirewallCanonicalEvents.FirewallRuleChangedData data = new FirewallCanonicalEvents.FirewallRuleChangedData();
        data.setVirtualRouterUuid(vRouterUuid);
        evtf.fire(FirewallCanonicalEvents.FIREWALL_RULE_CHANGED_PATH, data);
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), PortForwardingConstant.PORTFORWARDING_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<PortForwardingRuleTO> pfs = findPortforwardingsOnVmNic(nic, vr);
        if (pfs == null || pfs.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RevokePortForwardingRuleCmd cmd = new VirtualRouterCommands.RevokePortForwardingRuleCmd();
        cmd.setRules(pfs);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrVO.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RevokePortForwardingRuleRsp ret = re.toResponse(VirtualRouterCommands.RevokePortForwardingRuleRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to revoke port forwardings on virtual router[uuid:%s], %s",
                            vrVO.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    List<Tuple> pfs = findPortForwardingTuplesOnVmNic(nic);
                    List<String> ruleUuids = pfs.stream().map(p -> p.get(0, PortForwardingRuleVO.class).getUuid()).collect(Collectors.toList());
                    proxy.detachNetworkService(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName(), ruleUuids);
                    for (Tuple t : pfs) {
                        PortForwardingRuleVO rule = t.get(0, PortForwardingRuleVO.class);
                        new SQLBatch(){
                            @Override
                            protected void scripts() {
                                sql(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, rule.getUuid())
                                        .set(PortForwardingRuleVO_.guestIp, null).set(PortForwardingRuleVO_.vmNicUuid, null).update();
                            }
                        }.execute();
                    }

                    String info = String.format("sync port forwardings on virtual router[uuid:%s] successfully",
                            vrVO.getUuid());
                    logger.debug(info);
                    fireFirewallEvent(vr.getUuid());
                    completion.success();
                }
            }
        });
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    protected void revokeRuleOnHaVirtualRouter(final PortForwardingStruct struct, VirtualRouterVmInventory vrInv, Completion completion)  {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REVOKE_PF_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void applyRuleOnHaVirtualRouter(final PortForwardingStruct struct, VirtualRouterVmInventory vrInv, Completion completion)  {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(APPLY_PF_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct applyPF = new VirtualRouterHaCallbackStruct();
        applyPF.type = APPLY_PF_TASK;
        applyPF.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need applyVip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVO);
                PortForwardingStruct s = JSONObjectUtil.toObject(task.getJsonData(), PortForwardingStruct.class);
                applyRuleToVirtualRouter(s, vrInv, completion);
            }
        };
        structs.add(applyPF);

        VirtualRouterHaCallbackStruct revokePF = new VirtualRouterHaCallbackStruct();
        revokePF.type = REVOKE_PF_TASK;
        revokePF.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need revokePF on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVO);
                PortForwardingStruct s = JSONObjectUtil.toObject(task.getJsonData(), PortForwardingStruct.class);
                revokeRuleOnVirtualRouter(s, vrInv, completion);
            }
        };
        structs.add(revokePF);

        return structs;
    }
}
