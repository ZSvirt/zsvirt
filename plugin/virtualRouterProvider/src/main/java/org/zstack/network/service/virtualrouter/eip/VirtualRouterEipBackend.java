package org.zstack.network.service.virtualrouter.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmFacade;
import org.zstack.appliancevm.ApplianceVmFirewallProtocol;
import org.zstack.appliancevm.ApplianceVmFirewallRuleInventory;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.CreateEipRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveEipRsp;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 */
public class VirtualRouterEipBackend extends AbstractVirtualRouterBackend implements VirtualRouterHaGetCallbackExtensionPoint,
        EipBackend, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterEipBackend.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private ApplianceVmFacade asf;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private EipConfigProxy proxy;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private EventFacade evtf;

    private final String APPLY_EIP_TASK = "applyEip";
    private final String REVOKE_EIP_TASK = "revokeEip";

    private List<ApplianceVmFirewallRuleInventory> getFirewallRules(EipStruct struct) {
        ApplianceVmFirewallRuleInventory tcp = new ApplianceVmFirewallRuleInventory();
        tcp.setProtocol(ApplianceVmFirewallProtocol.tcp.toString());
        tcp.setDestIp(struct.getVip().getIp());
        tcp.setStartPort(0);
        tcp.setEndPort(65535);

        ApplianceVmFirewallRuleInventory udp = new ApplianceVmFirewallRuleInventory();
        udp.setProtocol(ApplianceVmFirewallProtocol.udp.toString());
        udp.setDestIp(struct.getVip().getIp());
        udp.setStartPort(0);
        udp.setEndPort(65535);

        return Arrays.asList(tcp, udp);
    }

    private void applyEip(final VirtualRouterVmInventory vr, final EipStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("apply-eip-%s-vr-%s", struct.getEip().getUuid(), vr.getUuid()));
        chain.then(new Flow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                asf.openFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
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

            @Override
            public void rollback(final FlowRollback trigger, Map data) {
                asf.removeFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to remove firewall rules on virtual router[uuid:%s, l3Network uuid:%s], %s",
                                vr.getUuid(), struct.getVip().getL3NetworkUuid(), errorCode));
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                EipTO to = new EipTO();
                final VmNicInventory selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                        arg -> arg.getL3NetworkUuid().equals(struct.getNic().getL3NetworkUuid()));
                String priMac = selected == null ? null : selected.getMac();
                to.setPrivateMac(priMac);
                to.setPublicMac(vr.getVmNics().stream()
                        .filter(nic -> nic.getL3NetworkUuid().equals(struct.getVip().getL3NetworkUuid()))
                        .findFirst()
                        .map(VmNicInventory::getMac)
                        .orElse(null));
                to.setVipIp(struct.getVip().getIp());
                to.setGuestIp(struct.getNic().getIp());
                to.setSnatInboundTraffic(struct.isSnatInboundTraffic());

                VirtualRouterCommands.CreateEipCmd cmd = new VirtualRouterCommands.CreateEipCmd();
                cmd.setEip(to);
                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setCheckStatus(true);
                msg.setPath(VirtualRouterConstant.VR_CREATE_EIP);
                msg.setCommand(cmd);
                msg.setVmInstanceUuid(vr.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        CreateEipRsp ret = re.toResponse(CreateEipRsp.class);
                        if (ret.isSuccess()) {
                            fireFirewallEvent(vr.getUuid());
                            trigger.next();
                        } else {
                            trigger.fail(operr("failed to create eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s], %s",
                                            struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                                            vr.getUuid(), ret.getError()));
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                String info = String.format("successfully created eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s]",
                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                        vr.getUuid());
                new VirtualRouterRoleManager().makeEipRole(vr.getUuid());
                logger.debug(info);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();

    }

    private void fireFirewallEvent(String vRouterUuid) {
        FirewallCanonicalEvents.FirewallRuleChangedData data = new FirewallCanonicalEvents.FirewallRuleChangedData();
        data.setVirtualRouterUuid(vRouterUuid);
        evtf.fire(FirewallCanonicalEvents.FIREWALL_RULE_CHANGED_PATH, data);
    }

    protected void applyEipOnHaRouter(String vrUuid, EipStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(APPLY_EIP_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void revokeEipOnHaRouter(String vrUuid, EipStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REVOKE_EIP_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    public void applyEip(final EipStruct struct, final Completion completion) {
        L3NetworkVO l3vo = dbf.findByUuid(struct.getNic().getL3NetworkUuid(), L3NetworkVO.class);
        final L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3vo);

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (!offering.getPublicNetworkUuid().equals(struct.getVip().getL3NetworkUuid())) {
                    throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to EIP[uuid:%s]'s; you may need to use system tag" +
                                    " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network", offering.getUuid(), l3inv.getUuid(), l3inv.getZoneUuid(), struct.getVip().getL3NetworkUuid(), struct.getEip().getUuid()));
                }
            }
        });
        s.setL3Network(l3inv);

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                applyEip(vr, struct, new Completion(completion) {
                    @Override
                    public void success() {
                        proxy.attachNetworkService(vr.getUuid(), EipVO.class.getSimpleName(), asList(struct.getEip().getUuid()));
                        applyEipOnHaRouter(vr.getUuid(), struct, completion);
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

    public void revokeEip(String vrUuid, final EipStruct struct, final Completion completion) {
        VirtualRouterVmVO vrvo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (vrvo.getState() != VmInstanceState.Running) {
            // rule will be synced when vr state changes to Running
            proxy.detachNetworkService(vrUuid,EipVO.class.getSimpleName(), asList(struct.getEip().getUuid()));
            completion.success();
            return;
        }

        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrvo);

        //TODO: add GC
        final FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("revoke-eip-%s-vr-%s", struct.getEip().getUuid(), vr.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                VirtualRouterCommands.RemoveEipCmd cmd = new VirtualRouterCommands.RemoveEipCmd();
                EipTO to = new EipTO();
                final VmNicInventory selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                        arg -> arg.getL3NetworkUuid().equals(struct.getNic().getL3NetworkUuid()));
                String priMac = selected == null ? null : selected.getMac();

                to.setPublicMac(vr.getVmNics().stream()
                        .filter(nic -> nic.getL3NetworkUuid().equals(struct.getVip().getL3NetworkUuid()))
                        .findFirst()
                        .map(VmNicInventory::getMac)
                        .orElse(null));

                to.setPrivateMac(priMac);
                to.setSnatInboundTraffic(struct.isSnatInboundTraffic());
                to.setVipIp(struct.getVip().getIp());
                to.setGuestIp(struct.getNic().getIp());
                to.setNeedCleanGuestIp(!Q.New(EipVO.class)
                        .eq(EipVO_.guestIp, struct.getEip().getGuestIp())
                        .eq(EipVO_.vmNicUuid, struct.getEip().getVmNicUuid())
                        .notEq(EipVO_.vipIp, struct.getEip().getVipIp())
                        .isExists());
                cmd.setEip(to);

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setCommand(cmd);
                msg.setCheckStatus(true);
                msg.setPath(VirtualRouterConstant.VR_REMOVE_EIP);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.setError(reply.getError());
                        } else {
                            VirtualRouterAsyncHttpCallReply re = reply.castReply();
                            RemoveEipRsp ret = re.toResponse(RemoveEipRsp.class);
                            if (!ret.isSuccess()) {
                                ErrorCode err = operr("failed to remove eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s], %s",
                                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                                        vr.getUuid(), ret.getError());
                                trigger.setError(err);
                            }
                        }

                        fireFirewallEvent(vr.getUuid());
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                asf.removeFirewall(vr.getUuid(), struct.getVip().getL3NetworkUuid(), getFirewallRules(struct), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to remove firewall rules on virtual router[uuid:%s, l3Network uuid:%s], %s",
                                vr.getUuid(), struct.getVip().getL3NetworkUuid(), errorCode));
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                String info = String.format("successfully removed eip[uuid:%s, name:%s, ip:%s] for vm nic[uuid:%s] on virtual router[uuid:%s]",
                        struct.getEip().getUuid(), struct.getEip().getName(), struct.getVip().getIp(), struct.getNic().getUuid(),
                        vr.getUuid());
                logger.debug(info);
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
    public void revokeEip(final EipStruct struct, final Completion completion) {
        List<String> vrs = proxy.getVrUuidsByNetworkService(EipVO.class.getSimpleName(), struct.getEip().getUuid());
        if (vrs == null || vrs.isEmpty()) {
            logger.debug(String.format("can not find virtual router uuid when revoking eip [uuid:%s]", struct.getEip().getUuid()));
            completion.success();
            return;
        }

        String vrUuid = vrs.get(0);
        revokeEip(vrUuid, struct, new Completion(completion) {
            @Override
            public void success() {
                revokeEipOnHaRouter(vrUuid, struct, new Completion(completion) {
                    @Override
                    public void success() {
                        proxy.detachNetworkService(vrUuid, EipVO.class.getSimpleName(), asList(struct.getEip().getUuid()));
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });

            }

            @Override
            public void fail(ErrorCode errorCode) {
                // We need to remove the 'ref' record, otherwise the next time when
                // deleting EIP is requested, we will get ConstraintViolationException.
                revokeEipOnHaRouter(vrUuid, struct, new Completion(completion) {
                    @Override
                    public void success() {
                        proxy.detachNetworkService(vrUuid, EipVO.class.getSimpleName(), asList(struct.getEip().getUuid()));
                        completion.fail(errorCode);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        syncEipsOnVirtualRouter(nic, true, completion);
    }

    private void syncEipsOnVirtualRouter(VmNicInventory nic, boolean attach, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vr != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));

        List<EipTO> eips;
        try {
            eips = findEipsOnVirtualRouter(nic, attach);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        if (attach && (eips == null || eips.isEmpty())) {
            completion.success();
            return;
        } else if (!attach && eips == null) {
            eips = new ArrayList<>();
        }

        VirtualRouterCommands.SyncEipCmd cmd = new VirtualRouterCommands.SyncEipCmd();
        cmd.setEips(eips);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_EIP);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
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
                    ErrorCode err = operr("failed to sync eip on virtual router[uuid:%s], %s",
                            vr.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    String info = String.format("sync eip on virtual router[uuid:%s] successfully",
                            vr.getUuid());

                    logger.debug(info);
                    completion.success();
                }
            }
        });
    }

    private List<EipTO> findEipsOnVirtualRouter(VmNicInventory nic, boolean attach) throws OperationFailureException {
        List<Tuple> eips = findEipOnVmNic(nic);
        if (attach && (eips == null || eips.isEmpty())) {
            return null;
        }

        List<Tuple> existsEips = null;
        List<String> existsEipUuids = proxy.getServiceUuidsByRouterUuid(nic.getVmInstanceUuid(), EipVO.class.getSimpleName());
        if (existsEipUuids != null && !existsEipUuids.isEmpty()) {
            existsEips = SQL.New("select eip.vipIp, eip.guestIp, nic.l3NetworkUuid, nic.mac, vip.l3NetworkUuid, eip.uuid " +
                    "from EipVO eip, VmNicVO nic, VipVO vip " +
                    "where eip.vmNicUuid = nic.uuid " +
                    "and eip.uuid in (:eipUuids) " +
                    "and eip.vipUuid = vip.uuid ", Tuple.class)
                    .param("eipUuids", existsEipUuids)
                    .list();
        }

        if (existsEips != null && !existsEips.isEmpty() && attach) {
            eips.addAll(existsEips);
        } else if (existsEips != null && !existsEips.isEmpty() && !attach) {
            // TODO(WeiW) Optimize it
            List<Tuple> remainEips = new ArrayList<>();
            for (Tuple e : existsEips) {
                boolean add = true;
                for (Tuple i : eips) {
                    if (e.get(0, String.class).equals(i.get(0, String.class)) &&
                            e.get(1, String.class).equals(i.get(1, String.class))) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    remainEips.add(e);
                }
            }
            eips.clear();
            eips.addAll(remainEips);
        }


        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

        List<EipTO> ret = new ArrayList<EipTO>();
        for (Tuple t : eips) {
            if (ret.stream().anyMatch(
                    r -> r.getVipIp().equals(t.get(0, String.class)) &&
                            r.getGuestIp().equals(t.get(1, String.class)))) {
                continue;
            }
            EipTO to = new EipTO();
            to.setVipIp(t.get(0, String.class));
            to.setGuestIp(t.get(1, String.class));
            Optional<VmNicInventory> priNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(t.get(2, String.class)))
                    .findFirst();
            if (!priNic.isPresent()) {
                continue;
            }
            to.setPrivateMac(priNic.get().getMac());
            to.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            Optional<VmNicInventory> pubNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(t.get(4, String.class)))
                    .findFirst();
            if (!pubNic.isPresent()) {
                continue;
            }
            to.setPublicMac(pubNic.get().getMac());
            ret.add(to);
        }

        return ret;
    }

    private List<Tuple> findEipTuplesOnVmNic(VmNicInventory nic) {
        List<Tuple> eips = SQL.New("select eip.vipIp, eip.guestIp, nic.l3NetworkUuid, nic.mac, vip.l3NetworkUuid, eip.uuid " +
                "from EipVO eip, VmNicVO nic, VipVO vip " +
                "where eip.vmNicUuid = nic.uuid " +
                "and nic.l3NetworkUuid = :l3Uuid " +
                "and eip.vipUuid = vip.uuid " +
                "and eip.state = :enabledState", Tuple.class)
                .param("l3Uuid", nic.getL3NetworkUuid())
                .param("enabledState", EipState.Enabled)
                .list();

        return eips;
    }

    private List<Tuple> findEipOnVmNic(VmNicInventory nic) throws OperationFailureException {
        List<Tuple> eips = findEipTuplesOnVmNic(nic);

        if (eips == null || eips.isEmpty()) {
            return new ArrayList<>();
        }

        proxy.attachNetworkService(nic.getVmInstanceUuid(), EipVO.class.getSimpleName(), eips.stream().map(e -> e.get(5, String.class)).collect(Collectors.toList()));
        return eips;
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        syncEipsOnVirtualRouter(nic, false, new Completion(completion) {
            @Override
            public void success() {
                List<Tuple> eips = findEipTuplesOnVmNic(nic);
                if (eips != null && !eips.isEmpty()) {
                    proxy.detachNetworkService(nic.getVmInstanceUuid(), EipVO.class.getSimpleName(), eips.stream().map(t -> t.get(5, String.class)).collect(Collectors.toList()));
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct applyEip = new VirtualRouterHaCallbackStruct();
        applyEip.type = APPLY_EIP_TASK;
        applyEip.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need apply Eip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);
                EipStruct s = JSONObjectUtil.toObject(task.getJsonData(), EipStruct.class);
                applyEip(vr, s, completion);
            }
        };
        structs.add(applyEip);

        VirtualRouterHaCallbackStruct revokeEip = new VirtualRouterHaCallbackStruct();
        revokeEip.type = REVOKE_EIP_TASK;
        revokeEip.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need revoke Eip on backend", vrUuid));
                    completion.success();
                    return;
                }

                EipStruct s = JSONObjectUtil.toObject(task.getJsonData(), EipStruct.class);
                revokeEip(vrUuid, s, completion);
            }
        };
        structs.add(revokeEip);

        return structs;
    }
}
