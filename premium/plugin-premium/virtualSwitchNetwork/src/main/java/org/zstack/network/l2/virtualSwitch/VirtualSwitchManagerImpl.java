package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.candidate.CandidateDecision;
import org.zstack.header.candidate.CandidateDecisionEntry;
import org.zstack.header.candidate.CandidateResult;
import org.zstack.header.cluster.ClusterConstant;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.network.l2.L2NetworkHostHelper;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.network.service.flat.IpStatisticConstants.ResourceType;
import org.zstack.network.service.flat.L3NetworkGetIpStatisticExtensionPoint;
import org.zstack.query.QueryFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

public class VirtualSwitchManagerImpl extends AbstractService implements VirtualSwitchManager,
        PostHostConnectExtensionPoint, L3NetworkGetIpStatisticExtensionPoint, HostAfterUpdatedExtensionPoint,
        HostNetworkCandidateFilterExtensionPoint, FilterAttachableL3NetworkExtensionPoint, L2NetworkCandidateFilterExtensionPoint {

    private static CLogger logger = Utils.getLogger(VirtualSwitchManagerImpl.class);
    private static final L2NetworkHostHelper l2NetworkHostHelper = new L2NetworkHostHelper();
    private static final VirtualSwitchHelper virtualSwitchHelper = new VirtualSwitchHelper();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private QueryFacade qf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;

    private String getL2NetworkOnHostThreadName(String l2NetworkUuid, String hostUuid) {
        return String.format("l2Network-%s-on-host-%s", l2NetworkUuid, hostUuid);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APICreateHostKernelInterfaceMsg) {
            handle((APICreateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIBatchCreateHostKernelInterfaceMsg) {
            handle((APIBatchCreateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIUpdateHostKernelInterfaceMsg) {
            handle((APIUpdateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIDeleteHostKernelInterfaceMsg) {
            handle((APIDeleteHostKernelInterfaceMsg) msg);
        } else if (msg instanceof APIGetCandidateHostKernelInterfacesMsg) {
            handle((APIGetCandidateHostKernelInterfacesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateHostKernelInterfaceMsg) {
            handle((CreateHostKernelInterfaceMsg) msg);
        } else if (msg instanceof RefreshHostKernelInterfaceOnHostMsg) {
            handle((RefreshHostKernelInterfaceOnHostMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public Long countUsedIp(String l3NetworkUuid, String ip) {
        if (l3NetworkUuid == null) {
            return 0L;
        }
        String sql = "select count(*) from HostKernelInterfaceUsedIpVO where l3NetworkUuid = :l3Uuid";
        if (StringUtils.isNotEmpty(ip)) {
            sql += " and ip like '" + ip + '\'';
        }
        return SQL.New(sql, Long.class)
                .param("l3Uuid", l3NetworkUuid)
                .find();
    }

    @Override
    public String getType() {
        return ResourceType.KERNEL_INTERFACE;
    }

    @Override
    public String getResourceOwnerUuid(String usedIpUuid) {
        return Q.New(HostKernelInterfaceUsedIpVO.class).select(HostKernelInterfaceUsedIpVO_.hostKernelInterfaceUuid).eq(HostKernelInterfaceUsedIpVO_.uuid, usedIpUuid).findValue();
    }

    @Override
    public List<String> getParentUuid(String uuid, String vipUuid) {
        return null;
    }


    private MevocoKVMAgentCommands.SetHostKernelInterfaceCmd buildHostKernelInterfaceCmd(List<String> l2NetworkUuids, String hostUuid, boolean isDeleteAllInterfaces) {
        MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = new MevocoKVMAgentCommands.SetHostKernelInterfaceCmd();
        if (l2NetworkUuids.isEmpty()) {
            return cmd;
        }

        List<L2NetworkVO> l2vos = Q.New(L2NetworkVO.class).in(L2NetworkVO_.uuid, l2NetworkUuids).list();
        List<HostKernelInterfaceVO> hkvos = Q.New(HostKernelInterfaceVO.class).in(HostKernelInterfaceVO_.l2NetworkUuid, l2NetworkUuids).eq(HostKernelInterfaceVO_.hostUuid, hostUuid).list();

        for (String l2Uuid : l2NetworkUuids) {
            L2NetworkVO l2vo = l2vos.stream().filter(l2 -> l2.getUuid().equals(l2Uuid)).findFirst().orElse(null);
            if (l2vo == null) {
                continue;
            }

            HostKernelInterfaceTO to = new HostKernelInterfaceTO();
            to.setInterfaceName(VirtualSwitchUtils.getInterfaceNameOfL2PortGroupOnHost(L2NetworkInventory.valueOf(l2vo), hostUuid));
            to.setVlanId(l2vo.getVirtualNetworkId());
            to.setIps(new ArrayList<>());

            List<String> hostKernelInterfaceUuids = hkvos.stream().filter(hk -> hk.getL2NetworkUuid().equals(l2Uuid)).map(HostKernelInterfaceVO::getUuid).collect(Collectors.toList());
            if (hostKernelInterfaceUuids.isEmpty()) {
                cmd.getInterfaces().add(to);
                continue;
            }

            if (isDeleteAllInterfaces) {
                List<String> defaultHostKernelInterfaceUuids = VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE
                        .filterResourceHasTag(hostKernelInterfaceUuids);
                hostKernelInterfaceUuids = hostKernelInterfaceUuids.stream().filter(uuid ->
                        !defaultHostKernelInterfaceUuids.contains(uuid)).collect(Collectors.toList());
                if (hostKernelInterfaceUuids.isEmpty()) {
                    cmd.getInterfaces().add(to);
                    continue;
                }
            }

            List<HostKernelInterfaceUsedIpVO> usedIps = Q.New(HostKernelInterfaceUsedIpVO.class).in(HostKernelInterfaceUsedIpVO_.hostKernelInterfaceUuid, hostKernelInterfaceUuids).list();

            usedIps.forEach(ip -> {
                UsedIpTO ito = new UsedIpTO();
                ito.setIp(ip.getIp());
                ito.setNetmask(ip.getNetmask());
                ito.setIpVersion(ip.getIpVersion());
                ito.setActionCode(isDeleteAllInterfaces ? UsedIpTO.ACTION_CODE_REMOVE : UsedIpTO.ACTION_CODE_ADD);
                to.getIps().add(ito);
            });
            cmd.getInterfaces().add(to);
        }

        return cmd;
    }

    private MevocoKVMAgentCommands.SetHostKernelInterfaceCmd buildHostKernelInterfaceCmd(String l2Uuid, String hostUuid,
                                                                                         List<UsedIpInventory> addedIps,
                                                                                         List<UsedIpInventory> removedIps) {
        MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = new MevocoKVMAgentCommands.SetHostKernelInterfaceCmd();
        L2NetworkVO l2vo = dbf.findByUuid(l2Uuid, L2NetworkVO.class);

        HostKernelInterfaceTO to = new HostKernelInterfaceTO();
        to.setInterfaceName(VirtualSwitchUtils.getInterfaceNameOfL2PortGroupOnHost(L2NetworkInventory.valueOf(l2vo), hostUuid));
        to.setVlanId(l2vo.getVirtualNetworkId());
        to.setIps(new ArrayList<>());

        addedIps.forEach(ip -> {
            UsedIpTO ito = new UsedIpTO();
            ito.setIp(ip.getIp());
            ito.setNetmask(ip.getNetmask());
            ito.setIpVersion(ip.getIpVersion());
            ito.setActionCode(UsedIpTO.ACTION_CODE_ADD);
            to.getIps().add(ito);
        });
        removedIps.forEach(ip -> {
            UsedIpTO ito = new UsedIpTO();
            ito.setIp(ip.getIp());
            ito.setNetmask(ip.getNetmask());
            ito.setIpVersion(ip.getIpVersion());
            ito.setActionCode(UsedIpTO.ACTION_CODE_REMOVE);
            to.getIps().add(ito);
        });
        cmd.getInterfaces().add(to);
        return cmd;
    }

    private void doCreateHostKernelInterface(final CreateHostKernelInterfaceMsg msg, ReturnValueCompletion<HostKernelInterfaceVO> completion) {
        class Context {
            HostKernelInterfaceVO vo;
            final List<UsedIpInventory> usedIps = new ArrayList<>();
        }
        final Context ctx = new Context();

        FlowChain chain = new SimpleFlowChain();
        chain.then(new NoRollbackFlow() {
            String __name__ = "allocate-usedIp-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                AllocateIpMsg amsg = new AllocateIpMsg();
                amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                amsg.setRequiredIp(msg.getRequiredIp());
                amsg.setNetmask(msg.getNetmask());
                amsg.setIpVersion(IPv6Constants.IPv4);
                bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        AllocateIpReply r = reply.castReply();
                        UsedIpInventory usedIp = r.getIpInventory();
                        ctx.usedIps.add(usedIp);
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (!ctx.usedIps.isEmpty()) {
                    List<ReturnIpMsg> rmsgs = new ArrayList<>();
                    for (UsedIpInventory ip : ctx.usedIps) {
                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                        rmsgs.add(rmsg);
                    }

                    new While<>(rmsgs).step((rmsg, wcomp) -> {
                        bus.send(rmsg, new CloudBusCallBack(wcomp) {
                            @Override
                            public void run(MessageReply reply) {
                                wcomp.done();

                            }
                        });
                    }, 2).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.rollback();
                        }
                    });
                } else {
                    trigger.rollback();
                }
            }
        }).then(new Flow() {
            String __name__ = "create-host-kernel-interface-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostKernelInterfaceVO vo = new HostKernelInterfaceVO();
                vo.setUuid(Platform.getUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setHostUuid(msg.getHostUuid());
                vo.setL2NetworkUuid(msg.getL2NetworkUuid());
                vo.setL3NetworkUuid(msg.getL3NetworkUuid());
                vo.setAccountUuid(msg.getAccountUuid());
                ctx.vo = dbf.persistAndRefresh(vo);

                if (msg.getTrafficTypes() != null && !msg.getTrafficTypes().isEmpty()) {
                    List<HostKernelInterfaceTrafficTypeVO> tvos = new ArrayList<>();
                    for (String t : msg.getTrafficTypes()) {
                        HostKernelInterfaceTrafficTypeVO tvo = new HostKernelInterfaceTrafficTypeVO();
                        tvo.setHostKernelInterfaceUuid(ctx.vo.getUuid());
                        tvo.setTrafficType(HostKernelInterfaceTrafficType.valueOf(t));
                        tvos.add(tvo);
                    }

                    dbf.persistCollection(tvos);
                }

                if (!ctx.usedIps.isEmpty()) {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            for (UsedIpInventory ip : ctx.usedIps) {
                                dbf.getEntityManager().createNativeQuery(
                                        String.format("insert into HostKernelInterfaceUsedIpVO (uuid, hostKernelInterfaceUuid)" +
                                                " values ('%s', '%s')", ip.getUuid(), ctx.vo.getUuid())).executeUpdate();
                            }
                        }
                    }.execute();
                }

                ctx.vo = dbf.reload(ctx.vo);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.vo != null) {
                    dbf.remove(ctx.vo);
                }

                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("apply-ip-to-host-%s", msg.getHostUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostStatus hostStatus = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, msg.getHostUuid()).findValue();
                if (ctx.usedIps.isEmpty() || msg.isDbOnly() || !hostStatus.equals(HostStatus.Connected)) {
                    trigger.next();
                    return;
                }

                MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = buildHostKernelInterfaceCmd(
                        msg.getL2NetworkUuid(), msg.getHostUuid(), ctx.usedIps, new ArrayList<>());
                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                kmsg.setHostUuid(msg.getHostUuid());
                kmsg.setCommand(cmd);
                kmsg.setPath(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH);
                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(kmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply r = reply.castReply();
                        MevocoKVMAgentCommands.SetHostKernelInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetHostKernelInterfaceRsp.class);
                        if (!rsp.isSuccess()) {
                            trigger.fail(operr("failed to create hostKernelInterface[name:%s] on the host[uuid:%s], %s",
                                    msg.getName(), msg.getHostUuid(), rsp.getError()));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                if (ctx.vo == null) {
                    completion.fail(inerr("cannot find hostKernelInterface"));
                } else {
                    completion.success(ctx.vo);
                }
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(CreateHostKernelInterfaceMsg msg) {
        CreateHostKernelInterfaceReply reply = new CreateHostKernelInterfaceReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getL2NetworkOnHostThreadName(msg.getL2NetworkUuid(), msg.getHostUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCreateHostKernelInterface(msg, new ReturnValueCompletion<HostKernelInterfaceVO>(msg, chain) {
                    @Override
                    public void success(HostKernelInterfaceVO vo) {
                        HostKernelInterfaceInventory inv = HostKernelInterfaceInventory.valueOf(vo);
                        logger.debug(String.format("successfully create hostKernelInterface[uuid:%s]", inv.getUuid()));
                        reply.setInventory(inv);
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
                return String.format("create-host-kernel-interface-on-l2[uuid:%s]-host[uuid:%s]", msg.getL2NetworkUuid(), msg.getHostUuid());
            }

        });
    }

    private void handle(APICreateHostKernelInterfaceMsg msg) {
        final APICreateHostKernelInterfaceEvent evt = new APICreateHostKernelInterfaceEvent(msg.getId());

        CreateHostKernelInterfaceMsg cmsg = new CreateHostKernelInterfaceMsg();
        cmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
        cmsg.setL3NetworkUuid(msg.getL3NetworkUuid());
        cmsg.setHostUuid(msg.getHostUuid());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setRequiredIp(msg.getRequiredIp());
        cmsg.setNetmask(msg.getNetmask());
        cmsg.setTrafficTypes(msg.getTrafficTypes());
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, msg.getL2NetworkUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                } else {
                    CreateHostKernelInterfaceReply r = reply.castReply();
                    evt.setInventory(r.getInventory());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIBatchCreateHostKernelInterfaceMsg msg) {
        final APIBatchCreateHostKernelInterfaceEvent evt = new APIBatchCreateHostKernelInterfaceEvent(msg.getId());

        List<CreateHostKernelInterfaceMsg> cmsgs = new ArrayList<>();
        for (HostKernelInterfaceStruct struct : msg.getStructs()) {
            CreateHostKernelInterfaceMsg cmsg = new CreateHostKernelInterfaceMsg();
            cmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
            cmsg.setL3NetworkUuid(msg.getL3NetworkUuid());
            cmsg.setHostUuid(struct.getHostUuid());
            cmsg.setName(struct.getName());
            cmsg.setDescription(struct.getDescription());
            cmsg.setRequiredIp(struct.getIp());
            cmsg.setNetmask(struct.getNetmask());
            cmsg.setTrafficTypes(msg.getTrafficTypes());
            cmsg.setAccountUuid(msg.getSession().getAccountUuid());
            bus.makeTargetServiceIdByResourceUuid(cmsg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, msg.getL2NetworkUuid());
            cmsgs.add(cmsg);
        }

        List<HostKernelInterfaceResult> results = Collections.synchronizedList(new ArrayList<>());
        new While<>(cmsgs).step((cmsg, whileCompletion) -> {
            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    HostKernelInterfaceResult result = new HostKernelInterfaceResult();
                    if (reply.isSuccess()) {
                        CreateHostKernelInterfaceReply r = reply.castReply();
                        result.setInventory(r.getInventory());
                    } else {
                        result.setError(reply.getError());
                    }
                    results.add(result);
                    whileCompletion.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                evt.setResults(results);
                bus.publish(evt);
            }
        });
    }

    private void doUpdateHostKernelInterface(APIUpdateHostKernelInterfaceMsg msg, Completion completion) {
        doUpdateHostKernelInterface(msg, false, completion);
    }

    private void doUpdateHostKernelInterface(APIUpdateHostKernelInterfaceMsg msg, boolean dbOnly, Completion completion) {
        class Context {
            HostKernelInterfaceVO vo;
            UsedIpInventory usedIpInv;
            final List<HostKernelInterfaceUsedIpVO> ips = new ArrayList<>();
        }
        final Context ctx = new Context();
        ctx.vo = Q.New(HostKernelInterfaceVO.class).eq(HostKernelInterfaceVO_.uuid, msg.getUuid()).find();
        if (ctx.vo == null) {
            completion.fail(inerr("cannot find hostKernelInterface[uuid:%s]", msg.getUuid()));
            return;
        }
        ctx.ips.addAll(Q.New(HostKernelInterfaceUsedIpVO.class).eq(HostKernelInterfaceUsedIpVO_.hostKernelInterfaceUuid, msg.getUuid()).list());

        FlowChain chain = new SimpleFlowChain();
        chain.then(new Flow() {
            String __name__ = "update-ip-in-db-if-needed";

            @Override
            public boolean skip(Map data) {
                return msg.getRequiredIp() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostKernelInterfaceUsedIpVO> existIps = ctx.ips.stream().filter(ip ->
                        Objects.equals(ip.getIp(), msg.getRequiredIp()) && Objects.equals(ip.getNetmask(), msg.getNetmask()))
                        .collect(Collectors.toList());
                ctx.ips.removeAll(existIps);

                if (!existIps.isEmpty()) {
                    trigger.next();
                    return;
                }

                AllocateIpMsg amsg = new AllocateIpMsg();
                amsg.setL3NetworkUuid(ctx.vo.getL3NetworkUuid());
                amsg.setRequiredIp(msg.getRequiredIp());
                amsg.setNetmask(msg.getNetmask());
                amsg.setIpVersion(IPv6Constants.IPv4);
                bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, ctx.vo.getL3NetworkUuid());
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            AllocateIpReply r = reply.castReply();
                            ctx.usedIpInv = r.getIpInventory();

                            new SQLBatch() {
                                @Override
                                protected void scripts() {
                                    dbf.getEntityManager().createNativeQuery(
                                            String.format("insert into HostKernelInterfaceUsedIpVO (uuid, hostKernelInterfaceUuid)" +
                                                    " values ('%s', '%s')", ctx.usedIpInv.getUuid(), ctx.vo.getUuid())).executeUpdate();
                                }
                            }.execute();

                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.usedIpInv != null) {
                    ReturnIpMsg rmsg = new ReturnIpMsg();
                    rmsg.setL3NetworkUuid(ctx.usedIpInv.getL3NetworkUuid());
                    rmsg.setUsedIpUuid(ctx.usedIpInv.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ctx.usedIpInv.getL3NetworkUuid());
                    bus.send(rmsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.rollback();
                        }
                    });
                } else {
                    trigger.rollback();
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "return-old-ip-in-db";

            @Override
            public boolean skip(Map data) {
                return ctx.ips.isEmpty() || msg.getRequiredIp() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ReturnIpMsg> rmsgs = new ArrayList<>();
                for (HostKernelInterfaceUsedIpVO ip : ctx.ips) {
                    ReturnIpMsg rmsg = new ReturnIpMsg();
                    rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                    rmsg.setUsedIpUuid(ip.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                    rmsgs.add(rmsg);
                }

                new While<>(rmsgs).step((rmsg, wcomp) -> {
                    bus.send(rmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            wcomp.done();

                        }
                    });
                }, 2).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        ctx.vo = dbf.reload(ctx.vo);
                        trigger.next();
                    }
                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "update-host-kernel-interface-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostKernelInterfaceVO vo = ctx.vo;
                vo.setName(!StringUtils.isEmpty(msg.getName()) ? msg.getName() : vo.getName());
                vo.setDescription(msg.getDescription() != null ? msg.getDescription() : vo.getDescription());
                vo = dbf.updateAndRefresh(vo);

                if (msg.getTrafficTypes() != null) {
                    List<String> newTrafficTypes = new ArrayList<>(msg.getTrafficTypes());
                    List<Long> toDelete = new ArrayList<>();
                    List<HostKernelInterfaceTrafficTypeVO> newTrafficTypeVOs = new ArrayList<>();

                    List<HostKernelInterfaceTrafficTypeVO> oldTrafficTypes = Q.New(HostKernelInterfaceTrafficTypeVO.class)
                            .eq(HostKernelInterfaceTrafficTypeVO_.hostKernelInterfaceUuid, vo.getUuid())
                            .list();

                    for (HostKernelInterfaceTrafficTypeVO tvo : oldTrafficTypes) {
                        if (!msg.getTrafficTypes().contains(tvo.getTrafficType().toString())) {
                            toDelete.add(tvo.getId());
                        } else {
                            newTrafficTypes.remove(tvo.getTrafficType().toString());
                        }
                    }

                    for (String t : newTrafficTypes) {
                        HostKernelInterfaceTrafficTypeVO tvo = new HostKernelInterfaceTrafficTypeVO();
                        tvo.setHostKernelInterfaceUuid(vo.getUuid());
                        tvo.setTrafficType(HostKernelInterfaceTrafficType.valueOf(t));
                        newTrafficTypeVOs.add(tvo);
                    }

                    dbf.removeByPrimaryKeys(toDelete, HostKernelInterfaceTrafficTypeVO.class);
                    dbf.persistCollection(newTrafficTypeVOs);
                }

                ctx.vo = dbf.reload(vo);
                trigger.next();
            }

        }).then(new NoRollbackFlow() {
            String __name__ = String.format("apply-ip-to-host");

            @Override
            public boolean skip(Map data) {
                return dbOnly || msg.getRequiredIp() == null || ctx.usedIpInv == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2NetworkVO l2vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, ctx.vo.getL2NetworkUuid()).find();
                MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = buildHostKernelInterfaceCmd(l2vo.getUuid(), ctx.vo.getHostUuid(),
                        Collections.singletonList(ctx.usedIpInv), ctx.ips.stream().map(UsedIpInventory::valueOf).collect(Collectors.toList()));
                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                kmsg.setHostUuid(ctx.vo.getHostUuid());
                kmsg.setCommand(cmd);
                kmsg.setPath(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH);
                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, ctx.vo.getHostUuid());
                bus.send(kmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
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

    private void handle(APIUpdateHostKernelInterfaceMsg msg) {
        APIUpdateHostKernelInterfaceEvent evt = new APIUpdateHostKernelInterfaceEvent(msg.getId());
        HostKernelInterfaceVO vo = dbf.findByUuid(msg.getUuid(), HostKernelInterfaceVO.class);

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getL2NetworkOnHostThreadName(vo.getL2NetworkUuid(), vo.getHostUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doUpdateHostKernelInterface(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        evt.setInventory(HostKernelInterfaceInventory.valueOf(dbf.findByUuid(msg.getUuid(), HostKernelInterfaceVO.class)));
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
                return String.format("update-host-kernel-interface-%s", msg.getUuid());
            }

        });
    }

    private void doDeleteHostKernelInterface(APIDeleteHostKernelInterfaceMsg msg, boolean dbOnly, Completion completion) {
        class Context {
            final List<UsedIpInventory> ips = new ArrayList<>();
        }
        final Context ctx = new Context();
        HostKernelInterfaceVO vo = Q.New(HostKernelInterfaceVO.class).eq(HostKernelInterfaceVO_.uuid, msg.getUuid()).find();
        String hostUuid = vo.getHostUuid();
        String l2Uuid = vo.getL2NetworkUuid();

        FlowChain chain = new SimpleFlowChain();

        chain.then(new NoRollbackFlow() {
            String __name__ = "return-used-ip-and-delete-interface-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HostKernelInterfaceUsedIpVO> ips = Q.New(HostKernelInterfaceUsedIpVO.class)
                        .eq(HostKernelInterfaceUsedIpVO_.hostKernelInterfaceUuid, msg.getUuid())
                        .list();
                if (ips.isEmpty()) {
                    dbf.removeByPrimaryKey(msg.getUuid(), HostKernelInterfaceVO.class);
                    trigger.next();
                    return;
                }

                ctx.ips.addAll(ips.stream().map(UsedIpInventory::valueOf).collect(Collectors.toList()));
                List<ReturnIpMsg> rmsgs = new ArrayList<>();
                for (UsedIpVO ip : ips) {
                    ReturnIpMsg rmsg = new ReturnIpMsg();
                    rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                    rmsg.setUsedIpUuid(ip.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                    rmsgs.add(rmsg);
                }

                new While<>(rmsgs).step((rmsg, wcomp) -> {
                    bus.send(rmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            wcomp.done();

                        }
                    });
                }, 2).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        dbf.removeByPrimaryKey(msg.getUuid(), HostKernelInterfaceVO.class);
                        trigger.next();
                    }
                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = String.format("apply-ip-to-host-%s", hostUuid);

            @Override
            public boolean skip(Map data) {
                return dbOnly;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2NetworkVO l2vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();

                MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = buildHostKernelInterfaceCmd(l2vo.getUuid(), hostUuid,
                        Collections.emptyList(), ctx.ips);
                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                kmsg.setHostUuid(hostUuid);
                kmsg.setCommand(cmd);
                kmsg.setPath(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH);
                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(kmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply r = reply.castReply();
                        MevocoKVMAgentCommands.SetHostKernelInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetHostKernelInterfaceRsp.class);
                        if (!rsp.isSuccess()) {
                            trigger.fail(operr("failed to delete hostKernelInterface[uuid:%s] on the host[uuid:%s], %s",
                                    msg.getUuid(), hostUuid, rsp.getError()));
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

    private void handle(APIDeleteHostKernelInterfaceMsg msg) {
        APIDeleteHostKernelInterfaceEvent evt = new APIDeleteHostKernelInterfaceEvent(msg.getId());
        HostKernelInterfaceVO vo = dbf.findByUuid(msg.getUuid(), HostKernelInterfaceVO.class);

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getL2NetworkOnHostThreadName(vo.getL2NetworkUuid(), vo.getHostUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeleteHostKernelInterface(msg, false, new Completion(chain) {
                    @Override
                    public void success() {
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
                return String.format("delete-host-kernel-interface-%s", msg.getUuid());
            }

        });
    }

    private void handle(APIGetCandidateHostKernelInterfacesMsg msg) {
        APIGetCandidateHostKernelInterfacesReply reply = new APIGetCandidateHostKernelInterfacesReply();
        List<CandidateResult<HostKernelInterfaceInventory>> results = new ArrayList<>();
        List<HostKernelInterfaceVO> vos = Q.New(HostKernelInterfaceVO.class)
                .in(HostKernelInterfaceVO_.hostUuid, msg.getHostUuids())
                .list();
        if (vos.isEmpty()) {
            reply.setResults(results);
            bus.reply(msg, reply);
            return;
        }

        for (HostKernelInterfaceVO vo : vos) {
            results.add(new CandidateResult<>(HostKernelInterfaceInventory.valueOf(vo)));
        }

        if (msg.getCidr() != null) {
            filterHostKernelInterfaceByCidr(results, msg.getCidr());
        }

        if (msg.getTrafficTypes() != null) {
            filterHostKernelInterfaceByTrafficTypes(results, msg.getTrafficTypes());
        }

        if (!msg.isContainsRejected()) {
            reply.setResults(CandidateResult.getNotRejectedCandidates(results));
        } else {
            reply.setResults(results);
        }

        bus.reply(msg, reply);
    }

    private void filterHostKernelInterfaceByCidr(List<CandidateResult<HostKernelInterfaceInventory>> results, String cidr) {
        String reason = "no ip in cidr";
        String name = "filter-host-kernel-interface-by-cidr";

        for (CandidateResult<HostKernelInterfaceInventory> result : CandidateResult.getNotRejectedCandidates(results)) {
            List<HostKernelInterfaceUsedIpInventory> usedIps = result.getCandidate().getUsedIps();
            boolean hasIpInCidr = false;
            for (HostKernelInterfaceUsedIpInventory ipInv : usedIps) {
                if (NetworkUtils.isIpInCidr(ipInv.getIp(), cidr)) {
                    hasIpInCidr = true;
                    break;
                }
            }
            if (!hasIpInCidr) {
                CandidateDecisionEntry entry = new CandidateDecisionEntry();
                entry.setDecision(CandidateDecision.REJECTED);
                entry.setReason(reason);
                entry.setDecisionMaker(name);
                result.addFinalDecision(entry);
            }
        }
    }

    private void filterHostKernelInterfaceByTrafficTypes(List<CandidateResult<HostKernelInterfaceInventory>> results, List<String> trafficTypes) {
        String reason = "missing traffic type[%s]";
        String name = "filter-host-kernel-interface-by-traffic-types";

        for (CandidateResult<HostKernelInterfaceInventory> result : CandidateResult.getNotRejectedCandidates(results)) {
            Set<String> interfaceTypes = new HashSet<>(result.getCandidate().getTrafficTypes());
            for (String t : trafficTypes) {
                if (!interfaceTypes.contains(t)) {
                    CandidateDecisionEntry entry = new CandidateDecisionEntry();
                    entry.setDecision(CandidateDecision.REJECTED);
                    entry.setReason(String.format(reason, t));
                    entry.setDecisionMaker(name);
                    result.addFinalDecision(entry);
                    break;
                }
            }
        }
    }

    private void handle(RefreshHostKernelInterfaceOnHostMsg msg) {
        RefreshHostKernelInterfaceOnHostReply reply = new RefreshHostKernelInterfaceOnHostReply();

        List<String> l2Uuids;
        if (!CollectionUtils.isEmpty(msg.getL2NetworkUuids())) {
            l2Uuids = new ArrayList<>(msg.getL2NetworkUuids());
        } else {
            l2Uuids = Q.New(HostKernelInterfaceVO.class).select(HostKernelInterfaceVO_.l2NetworkUuid).eq(HostKernelInterfaceVO_.hostUuid, msg.getHostUuid()).listValues();
        }

        if (l2Uuids.isEmpty()) {
            logger.debug(String.format("no host kernel interface need to be refreshed on host[uuid:%s]", msg.getHostUuid()));
            bus.reply(msg, reply);
            return;
        }

        MevocoKVMAgentCommands.SetHostKernelInterfaceCmd cmd = buildHostKernelInterfaceCmd(l2Uuids.stream().distinct().collect(Collectors.toList()),
                msg.getHostUuid(), msg.isDeleteAllInterfaces());
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.SET_KERNEL_INTERFACE_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                }
                KVMHostAsyncHttpCallReply hReply = r.castReply();
                MevocoKVMAgentCommands.SetHostKernelInterfaceRsp rsp =
                        hReply.toResponse(MevocoKVMAgentCommands.SetHostKernelInterfaceRsp.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("failed to refresh host kernel interface on host[uuid:%s], %s",
                            msg.getHostUuid(), rsp.getError()));
                    bus.reply(msg, reply);
                    return;
                }

                logger.debug(String.format("successfully refresh host kernel interface on host[uuid:%s]", msg.getHostUuid()));
                bus.reply(msg, reply);
            }
        });
    }

    private String getDefaultPortGroupName() {
        String namePattern = VirtualSwitchConstant.DEFAULT_PORT_GROUP_NAME_PREFIX;
        int index = namePattern.length();
        List<String> pgNames = Q.New(PortGroupVO.class).select(PortGroupVO_.name).like(PortGroupVO_.name, namePattern + "%").listValues();
        if (pgNames.isEmpty()) {
            return String.format("%s 0", namePattern);
        }

        List<Integer> numbers = new ArrayList<>();
        for (String name : pgNames) {
            if (name.length() <= index) {
                continue;
            }
            String tmp = name.substring(index).trim();
            if (tmp.isEmpty() || !tmp.matches("\\d+")) {
                continue;
            }
            numbers.add(Integer.valueOf(tmp));
        }
        if (numbers.isEmpty()) {
            return String.format("%s 0", namePattern);
        }

        Collections.sort(numbers);
        int lastNumber = numbers.get(numbers.size() - 1);
        return String.format("%s %s", namePattern, lastNumber + 1);
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        class Context {
            boolean isPhysicalInterfaceChanged = false;
            boolean attachFailed = false;
            String createdVSwitchUuid;
            String createdPortGroupUuid;
            HostKernelInterfaceTO interfaceTO;
            L2VirtualSwitchNetworkVO vSwitchVO;
            PortGroupVO pvo;

            public ErrorCode initL2NetworkHostRefAndUpdateBridgeName() {
                if (interfaceTO == null || pvo == null || vSwitchVO == null) {
                    return null;
                }

                if (L2NetworkHostUtils.checkIfL2AttachedToHost(pvo.getL2NetworkUuid(), host.getUuid())) {
                    logger.debug(String.format("skip checking bridge name for host[uuid:%s] because the virtual switch[uuid:%s] already attached to host",
                            host.getUuid(), vSwitchVO.getUuid()));
                } else if (KVMSystemTags.L2_BRIDGE_NAME.hasTag(pvo.getL2NetworkUuid(), L2NetworkVO.class)) {
                    String tagBridgeName = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(pvo.getL2NetworkUuid(), KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
                    if (interfaceTO.getBridgeName() != null &&
                            !interfaceTO.getBridgeName().equals(tagBridgeName)) {
                        return operr("failed to create default port group, because the bridge name[%s] of managementIp[%s]" +
                                        " must be the same as the bridge name[%s] of vlanId[%s] on default virtual switch[%s]",
                                interfaceTO.getBridgeName(), host.getManagementIp(), tagBridgeName, pvo.getVlanId(), vSwitchVO.getUuid());
                    }
                }

                if (attachFailed) {
                    L2NetworkHostUtils.deleteL2NetworkHostRef(pvo.getL2NetworkUuid(), host.getUuid());
                    return null;
                }

                String bridgeName = interfaceTO.getBridgeName();
                boolean skipDeletion = false;
                if (bridgeName != null) {
                    String generatedBridgeName = VirtualSwitchUtils.makeBridgeName(vSwitchVO.getUuid(),
                            pvo.getL2NetworkUuid(), pvo.getVlanId(), true);
                    skipDeletion = !bridgeName.equals(generatedBridgeName);
                    if (skipDeletion) {
                        logger.debug(String.format("set bridge deletion skipped for l2Network[uuid:%s] on host[uuid:%s]," +
                                        " bridge from agent[%s], generated bridge[%s]",
                                pvo.getL2NetworkUuid(), host.getUuid(), bridgeName, generatedBridgeName));
                    }
                } else {
                    bridgeName = VirtualSwitchUtils.makeBridgeName(vSwitchVO.getUuid(),
                            pvo.getL2NetworkUuid(), pvo.getVlanId());
                }

                l2NetworkHostHelper.initL2NetworkHostRef(pvo.getL2NetworkUuid(), host.getUuid(),
                        KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE, bridgeName);
                L2NetworkHostUtils.updateBridgeNameAndSkipDeletion(pvo.getL2NetworkUuid(), host.getUuid(), bridgeName, skipDeletion);

                return null;
            }
        }

        return new NoRollbackFlow() {
            String __name__ = "create-default-host-kernel-interface-on-host";
            final Context ctx = new Context();

            @Override
            public void run(FlowTrigger extTrigger, Map data) {
                FlowChain chain = new SimpleFlowChain();
                chain.setName(String.format("create-host-kernel-interface-for-host[uuid:%s]", host.getUuid()));

                chain.then(new NoRollbackFlow() {
                    String __name__ = String.format("get-host-kernel-interfaces-for-managetment-ip-%s", host.getManagementIp());

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {

                        MevocoKVMAgentCommands.GetHostKernelInterfaceCmd cmd = new MevocoKVMAgentCommands.GetHostKernelInterfaceCmd();
                        cmd.setHostUuid(host.getUuid());
                        cmd.setTargetIp(host.getManagementIp());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setHostUuid(host.getUuid());
                        kmsg.setCommand(cmd);
                        kmsg.setNoStatusCheck(true);
                        kmsg.setPath(MevocoKVMConstant.GET_KERNEL_INTERFACE_PATH);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(kmsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    innerTrigger.fail(reply.getError());
                                } else {
                                    KVMHostAsyncHttpCallReply r = reply.castReply();
                                    MevocoKVMAgentCommands.GetHostKernelInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.GetHostKernelInterfaceRsp.class);
                                    if (CollectionUtils.isEmpty(rsp.getInterfaces())
                                            || StringUtils.isEmpty(rsp.getInterfaces().get(0).getInterfaceName())) {
                                        innerTrigger.fail(operr("failed to get the host interface for the managementIp[%s]", host.getManagementIp()));
                                        return;
                                    }

                                    ctx.interfaceTO = rsp.getInterfaces().get(0);
                                    innerTrigger.next();
                                }
                            }
                        });
                    }
                }).then(new Flow() {
                    String __name__ = String.format("create-default-virtual-switch-for-host-%s", host.getManagementIp());

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {

                        if (ctx.interfaceTO == null) {
                            innerTrigger.fail(inerr("failed to get the host interface for host[%s]", host.getManagementIp()));
                            return;
                        }

                        HostNetworkBondingVO bond = Q.New(HostNetworkBondingVO.class)
                                .eq(HostNetworkBondingVO_.bondingName, ctx.interfaceTO.getInterfaceName())
                                .eq(HostNetworkBondingVO_.hostUuid, host.getUuid()).find();
                        boolean isBonding = (bond != null);

                        List<L2VirtualSwitchNetworkVO> defaultSwitches = SQL.New("select vs from L2VirtualSwitchNetworkVO vs, SystemTagVO sysTag" +
                                        " where vs.zoneUuid = :zoneUuid" +
                                        " and sysTag.resourceUuid = vs.uuid" +
                                        " and sysTag.tag = :tag", L2VirtualSwitchNetworkVO.class)
                                .param("zoneUuid", host.getZoneUuid())
                                .param("tag", VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.getTagFormat())
                                .list();
                        L2VirtualSwitchNetworkVO vsvo = null;
                        for (L2VirtualSwitchNetworkVO vs : defaultSwitches) {
                            boolean isAttached = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, vs.getUuid()).eq(L2NetworkClusterRefVO_.clusterUuid, host.getClusterUuid()).isExists();
                            if (isAttached) {
                                vsvo = vs;
                                break;
                            }
                        }
                        if (vsvo != null) {
                            if (isBonding && !vsvo.getPhysicalInterface().isEmpty() &&
                                    !Objects.equals(vsvo.getPhysicalInterface(), ctx.interfaceTO.getInterfaceName())) {
                                innerTrigger.fail(operr("failed to create default kernel interface," +
                                                "because the uplink bonding[name:%s] of managementIp[%s] must be the same as cluster[uuid:%s] default uplink bonding[name:%s]",
                                        bond.getBondingName(), host.getManagementIp(), host.getClusterUuid(), vsvo.getPhysicalInterface()));
                                return;
                            }

                            if (isBonding && !vsvo.getPhysicalInterface().isEmpty() &&
                                    !VirtualSwitchUtils.isUpLinkBondingExist(vsvo.getUuid(), host.getUuid(), ctx.interfaceTO.getInterfaceName())) {
                                // bonding mode is different, skip attaching
                                ctx.attachFailed = true;
                                ctx.vSwitchVO = vsvo;
                                if (VirtualSwitchUtils.isUplinkGroupExist(vsvo.getUuid(), host.getUuid())) {
                                    VirtualSwitchUtils.deleteUplinkGroup(vsvo.getUuid(), host.getUuid());
                                }
                                innerTrigger.next();
                                return;
                            }

                            HostParam hostParam = new HostParam();
                            if (!isBonding) {
                                hostParam.setHostUuid(host.getUuid());
                                hostParam.setPhysicalInterface(ctx.interfaceTO.getInterfaceName());
                            } else if (StringUtils.isEmpty(vsvo.getPhysicalInterface())) {
                                VirtualSwitchUtils.changeVirtualSwitchUplinkBondingName(vsvo.getUuid(), ctx.interfaceTO.getInterfaceName());
                                vsvo = dbf.reload(vsvo);
                                ctx.isPhysicalInterfaceChanged = true;

                                VirtualSwitchUtils.createUplinkBondingSystemTag(vsvo.getUuid(), bond.getMode(), bond.getXmitHashPolicy());
                            }
                            // init uplink group and attach to host after connected
                            virtualSwitchHelper.initOrOverrideUplinkGroup(vsvo, host.getUuid(),
                                    KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE, hostParam);

                            ctx.vSwitchVO = vsvo;
                            innerTrigger.next();
                            return;
                        }

                        vsvo = defaultSwitches.stream().filter(vs -> vs.getPhysicalInterface().equals(ctx.interfaceTO.getInterfaceName())
                                || !isBonding && vs.getPhysicalInterface().isEmpty()).findFirst().orElse(null);
                        if (vsvo == null) {
                            vsvo = new L2VirtualSwitchNetworkVO();
                            vsvo.setUuid(Platform.getUuid());
                            vsvo.setName(String.format(VirtualSwitchConstant.DEFAULT_VIRTUAL_SWITCH_NAME_FORMAT,
                                    VirtualSwitchUtils.getVirtualSwitchIndexOfZone(host.getZoneUuid())));
                            vsvo.setPhysicalInterface(isBonding ? ctx.interfaceTO.getInterfaceName() : L2NetworkConstant.PHYSICAL_INTERFACE_EMPTY);
                            vsvo.setType(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE);
                            vsvo.setvSwitchType(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
                            vsvo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                            vsvo.setZoneUuid(host.getZoneUuid());

                            vsvo.setDistributed(true);
                            vsvo.setVSwitchIndex(VirtualSwitchUtils.getVirtualSwitchIndexOfZone(host.getZoneUuid()));
                            vsvo = dbf.persistAndRefresh(vsvo);

                            tagMgr.createNonInherentSystemTag(vsvo.getUuid(),
                                    VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.getTagFormat(),
                                    L2VirtualSwitchNetworkVO.class.getSimpleName());

                            ctx.createdVSwitchUuid = vsvo.getUuid();
                            VirtualSwitchUtils.increaseVirtualSwitchIndexOfZone(host.getZoneUuid());
                        }

                        // new bonding or physical interface to bonding
                        if (isBonding) {
                            VirtualSwitchUtils.createUplinkBondingSystemTag(vsvo.getUuid(), bond.getMode(), bond.getXmitHashPolicy());
                        }

                        HostParam hostParam = new HostParam();
                        if (!isBonding) {
                            hostParam.setHostUuid(host.getUuid());
                            hostParam.setPhysicalInterface(ctx.interfaceTO.getInterfaceName());
                        } else if (StringUtils.isEmpty(vsvo.getPhysicalInterface())) {
                            VirtualSwitchUtils.changeVirtualSwitchUplinkBondingName(vsvo.getUuid(), ctx.interfaceTO.getInterfaceName());
                            ctx.isPhysicalInterfaceChanged = true;
                        }
                        ctx.vSwitchVO = vsvo;

                        // only create l2 network cluster ref, because hosts are not connected
                        AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
                        amsg.setClusterUuid(host.getClusterUuid());
                        amsg.setL2NetworkUuid(ctx.vSwitchVO.getUuid());
                        amsg.setL2ProviderType(KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE);
                        bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, ctx.vSwitchVO.getUuid());
                        bus.send(amsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    if (ctx.createdVSwitchUuid != null) {
                                        dbf.removeByPrimaryKey(ctx.createdVSwitchUuid, L2VirtualSwitchNetworkVO.class);
                                    }
                                    logger.warn(String.format("failed to create default virtual switch for cluster[uuid:%s]", host.getClusterUuid()));
                                    innerTrigger.fail(reply.getError());
                                } else {
                                    // init uplink group and attach to host after connected
                                    virtualSwitchHelper.initOrOverrideUplinkGroup(ctx.vSwitchVO, host.getUuid(),
                                            KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE, hostParam);
                                    innerTrigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback innerTrigger, Map data) {
                        if (ctx.createdVSwitchUuid != null) {
                            dbf.removeByPrimaryKey(ctx.createdVSwitchUuid, L2VirtualSwitchNetworkVO.class);
                            VirtualSwitchUtils.rollbackVirtualSwitchIndexOfZone(host.getZoneUuid());
                        }

                        if (ctx.vSwitchVO != null) {
                            VirtualSwitchUtils.deleteUplinkGroup(ctx.vSwitchVO.getUuid(), host.getUuid());
                            if (ctx.isPhysicalInterfaceChanged) {
                                VirtualSwitchUtils.changeVirtualSwitchUplinkBondingName(ctx.vSwitchVO.getUuid(), L2NetworkConstant.PHYSICAL_INTERFACE_EMPTY);
                                VirtualSwitchUtils.deleteUplinkBondingSystemTag(ctx.vSwitchVO.getUuid());
                            }
                        }

                        innerTrigger.rollback();
                    }
                }).then(new Flow() {
                    String __name__ = "attach-port-group-of-default-virtual-switch";

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {

                        if (ctx.interfaceTO == null) {
                            innerTrigger.fail(inerr("failed to get the host interface for host[%s]", host.getManagementIp()));
                            return;
                        }
                        if (ctx.vSwitchVO == null) {
                            innerTrigger.fail(inerr("failed to create default virtual switch for cluster[uuid:%s]", host.getClusterUuid()));
                            return;
                        }

                        List<PortGroupVO> defaultPortGroups = SQL.New("select pg from PortGroupVO pg, SystemTagVO sysTag" +
                                        " where pg.vSwitchUuid = :vSwitchUuid" +
                                        " and sysTag.resourceUuid = pg.uuid" +
                                        " and sysTag.tag = :tag", PortGroupVO.class)
                                .param("vSwitchUuid", ctx.vSwitchVO.getUuid())
                                .param("tag", VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.getTagFormat())
                                .list();
                        for (PortGroupVO pg : defaultPortGroups) {
                            boolean isAttached = (Long) SQL.New("select count(i) from HostKernelInterfaceVO i, HostVO host, SystemTagVO sysTag" +
                                            " where i.l3NetworkUuid = :l3Uuid" +
                                            " and i.hostUuid = host.uuid" +
                                            " and host.clusterUuid = :clusterUuid" +
                                            " and sysTag.resourceUuid = i.uuid" +
                                            " and sysTag.tag = :tag", Long.class)
                                    .param("l3Uuid", pg.getUuid())
                                    .param("clusterUuid", host.getClusterUuid())
                                    .param("tag", VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.getTagFormat())
                                    .find() > 0;
                            if (isAttached) {
                                ctx.pvo = pg;
                                break;
                            }
                        }
                        if (ctx.pvo != null) {
                            if (ctx.pvo.getVlanId() != ctx.interfaceTO.getVlanId()) {
                                innerTrigger.fail(operr("failed to create default port group," +
                                                " because the vlanId[%s] of managementIp[%s] must be the same as cluster[uuid:%s] default vlanId[%s]",
                                        ctx.interfaceTO.getVlanId(), host.getManagementIp(), host.getClusterUuid(), ctx.pvo.getVlanId()));
                                return;
                            }

                            ErrorCode err = ctx.initL2NetworkHostRefAndUpdateBridgeName();
                            if (err != null) {
                                innerTrigger.fail(err);
                                return;
                            }
                            innerTrigger.next();
                            return;
                        }

                        ctx.pvo = defaultPortGroups.stream().filter(pg -> pg.getVlanId() == ctx.interfaceTO.getVlanId()).findFirst().orElse(null);
                        if (ctx.pvo != null) {
                            ErrorCode err = ctx.initL2NetworkHostRefAndUpdateBridgeName();
                            if (err != null) {
                                innerTrigger.fail(err);
                                return;
                            }
                        }

                        List<String> l2PgUuids = Q.New(PortGroupVO.class)
                                .select(PortGroupVO_.l2NetworkUuid)
                                .eq(PortGroupVO_.vSwitchUuid, ctx.vSwitchVO.getUuid())
                                .listValues();

                        List<AttachL2NetworkToClusterMsg> amsgs = new ArrayList<>();
                        for (String l2PgUuid : l2PgUuids) {
                            AttachL2NetworkToClusterMsg amsg = new AttachL2NetworkToClusterMsg();
                            amsg.setClusterUuid(host.getClusterUuid());
                            amsg.setL2NetworkUuid(l2PgUuid);
                            amsg.setL2ProviderType(KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE);
                            bus.makeTargetServiceIdByResourceUuid(amsg, L2NetworkConstant.SERVICE_ID, l2PgUuid);
                            amsgs.add(amsg);
                        }

                        new While<>(amsgs).step((msg, whileCompletion) -> {
                            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        whileCompletion.done();
                                    } else {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                    }
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(innerTrigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.hasError()) {
                                    innerTrigger.fail(multiErr(errorCodeList));
                                } else {
                                    innerTrigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback innerTrigger, Map data) {
                        if (ctx.pvo != null) {
                            L2NetworkHostUtils.deleteL2NetworkHostRef(ctx.pvo.getL2NetworkUuid(), host.getUuid());
                        }

                        innerTrigger.rollback();
                    }
                }).then(new Flow() {
                    String __name__ = "create-default-port-group-if-not-exist";

                    @Override
                    public boolean skip(Map data) {
                        return ctx.pvo != null;
                    }

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {

                        if (ctx.interfaceTO == null) {
                            innerTrigger.fail(inerr("failed to get the host interface for host[%s]", host.getManagementIp()));
                            return;
                        }
                        if (ctx.vSwitchVO == null) {
                            innerTrigger.fail(inerr("failed to create default virtual switch for cluster[uuid:%s]", host.getClusterUuid()));
                            return;
                        }

                        // fake API message, only use for factory to create PortGroupVO
                        APICreatePortGroupMsg cmsg = new APICreatePortGroupMsg();
                        cmsg.setvSwitchUuid(ctx.vSwitchVO.getUuid());
                        cmsg.setVlan(ctx.interfaceTO.getVlanId());
                        cmsg.setVlanMode(PortGroupVlanMode.ACCESS.toString());
                        cmsg.setSystemTags(Collections.singletonList(VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.getTagFormat()));

                        PortGroupFactory factory = null;
                        for (L3NetworkFactory f : pluginRgty.getExtensionList(L3NetworkFactory.class)) {
                            if (f.getType().equals(L3NetworkType.valueOf(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE))) {
                                factory = (PortGroupFactory) f;
                            }
                        }

                        if (factory == null) {
                            innerTrigger.fail(inerr("there is no PortGroupFactory, please check!"));
                            return;
                        }

                        PortGroupVO pvo = new PortGroupVO();
                        pvo.setUuid(Platform.getUuid());
                        pvo.setName(getDefaultPortGroupName());
                        pvo.setType(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);
                        pvo.setSystem(true);
                        pvo.setCategory(L3NetworkCategory.Private);
                        pvo.setState(L3NetworkState.Enabled);
                        pvo.setEnableIPAM(false);
                        pvo.setIpVersion(IPv6Constants.IPv4);
                        pvo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        pvo.setZoneUuid(host.getZoneUuid());

                        factory.createL3Network(pvo, cmsg, new ReturnValueCompletion<L3NetworkInventory>(cmsg) {
                            @Override
                            public void success(L3NetworkInventory returnValue) {
                                tagMgr.createTagsFromAPICreateMessage(cmsg, returnValue.getUuid(), PortGroupVO.class.getSimpleName());

                                ctx.createdPortGroupUuid = returnValue.getUuid();
                                ctx.pvo = dbf.findByUuid(returnValue.getUuid(), PortGroupVO.class);

                                ErrorCode err = ctx.initL2NetworkHostRefAndUpdateBridgeName();
                                if (err != null) {
                                    innerTrigger.fail(err);
                                    return;
                                }
                                logger.debug(String.format("Successfully created port group[name:%s, uuid:%s]",
                                        returnValue.getName(), returnValue.getUuid()));
                                innerTrigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                innerTrigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback innerTrigger, Map data) {
                        if (ctx.createdPortGroupUuid != null) {
                            DeleteL3NetworkMsg dmsg = new DeleteL3NetworkMsg();
                            dmsg.setForceDelete(true);
                            dmsg.setUuid(ctx.createdPortGroupUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, L3NetworkConstant.SERVICE_ID, ctx.createdPortGroupUuid);
                            bus.send(dmsg, new CloudBusCallBack(innerTrigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    innerTrigger.rollback();
                                }
                            });
                        } else {
                            innerTrigger.rollback();
                        }
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "create-default-host-kernel-interface";

                    @Override
                    public boolean skip(Map data) {
                        return ctx.attachFailed || VirtualSwitchUtils.getDefaultKernelInterface(ctx.pvo.getL2NetworkUuid(), host.getUuid()) != null;
                    }

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {

                        if (ctx.interfaceTO == null) {
                            innerTrigger.fail(inerr("failed to get the host interface for host[%s]", host.getManagementIp()));
                            return;
                        }
                        if (ctx.pvo == null) {
                            innerTrigger.fail(inerr("failed to get default port group for cluster[uuid:%s]", host.getClusterUuid()));
                            return;
                        }

                        UsedIpTO ip = new UsedIpTO();
                        ctx.interfaceTO.getIps().forEach(ipTo -> {
                            if (ipTo.getIp().equals(host.getManagementIp())) {
                                ip.setIp(ipTo.getIp());
                                ip.setNetmask(ipTo.getNetmask());
                                ip.setGateway(ipTo.getGateway());
                            }
                        });

                        CreateHostKernelInterfaceMsg cmsg = new CreateHostKernelInterfaceMsg();
                        cmsg.setHostUuid(host.getUuid());
                        cmsg.setL2NetworkUuid(ctx.pvo.getL2NetworkUuid());
                        cmsg.setL3NetworkUuid(ctx.pvo.getUuid());
                        cmsg.setName(String.format("Kernel-%s", host.getManagementIp()));
                        cmsg.setTrafficTypes(Collections.singletonList(HostKernelInterfaceTrafficType.Management.toString()));
                        cmsg.setRequiredIp(ip.getIp());
                        cmsg.setNetmask(ip.getNetmask());
                        cmsg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        cmsg.setDbOnly(true);
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, ctx.pvo.getUuid());
                        bus.send(cmsg, new CloudBusCallBack(innerTrigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to create host kernel interface on port group[uuid:%s]", ctx.pvo.getUuid()));
                                    innerTrigger.fail(reply.getError());
                                } else {
                                    HostKernelInterfaceInventory inv = ((CreateHostKernelInterfaceReply) reply).getInventory();
                                    tagMgr.createNonInherentSystemTag(inv.getUuid(),
                                            VirtualSwitchSystemTags.HOST_KERNEL_DEFAULT_INTERFACE.getTagFormat(),
                                            HostKernelInterfaceVO.class.getSimpleName());
                                    innerTrigger.next();
                                }
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "delete-default-host-kernel-interface-if-attach-failed";

                    @Override
                    public boolean skip(Map data) {
                        return !ctx.attachFailed;
                    }

                    @Override
                    public void run(FlowTrigger innerTrigger, Map data) {
                        if (ctx.pvo == null) {
                            innerTrigger.fail(inerr("failed to get default port group for cluster[uuid:%s]", host.getClusterUuid()));
                            return;
                        }

                        HostKernelInterfaceVO vo = VirtualSwitchUtils.getDefaultKernelInterface(ctx.pvo.getL2NetworkUuid(), host.getUuid());
                        if (vo == null) {
                            innerTrigger.next();
                            return;
                        }

                        APIDeleteHostKernelInterfaceMsg fakeMsg = new APIDeleteHostKernelInterfaceMsg();
                        fakeMsg.setUuid(vo.getUuid());
                        thdf.chainSubmit(new ChainTask(fakeMsg) {
                            @Override
                            public String getSyncSignature() {
                                return getL2NetworkOnHostThreadName(ctx.pvo.getL2NetworkUuid(), host.getUuid());
                            }

                            @Override
                            public void run(SyncTaskChain chain) {
                                doDeleteHostKernelInterface(fakeMsg, true, new Completion(chain) {
                                    @Override
                                    public void success() {
                                        innerTrigger.next();
                                        chain.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        innerTrigger.fail(errorCode);
                                        chain.next();
                                    }
                                });
                            }

                            @Override
                            public String getName() {
                                return String.format("delete-host-kernel-interface-%s", fakeMsg.getUuid());
                            }

                        });
                    }
                }).done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        extTrigger.next();
                    }
                }).error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        extTrigger.fail(errCode);
                    }
                }).start();
            }
        };
    }

    @Override
    public Flow afterHostUpdated(HostInventory oldHost, HostInventory newHost) {
        return new NoRollbackFlow() {
            String __name__ = "update-host-kernel-interface-after-host-updated";

            @Override
            public boolean skip(Map data) {
                return Objects.equals(oldHost.getManagementIp(), newHost.getManagementIp());
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostKernelInterfaceVO vo = SQL.New("select iface from HostKernelInterfaceVO iface, HostKernelInterfaceUsedIpVO ip" +
                                " where iface.uuid = ip.hostKernelInterfaceUuid" +
                                " and iface.hostUuid = :hostUuid" +
                                " and ip.ip = :ip", HostKernelInterfaceVO.class)
                        .param("hostUuid", oldHost.getUuid())
                        .param("ip", oldHost.getManagementIp())
                        .find();
                if (vo == null) {
                    logger.warn(String.format("no host kernel interface for ip[%s] found on host[uuid:%s]", oldHost.getManagementIp(), oldHost.getUuid()));
                    trigger.next();
                    return;
                }

                HostKernelInterfaceUsedIpVO ipVO = vo.getUsedIps()
                        .stream().filter(it -> Objects.equals(oldHost.getManagementIp(), it.getIp())).findFirst().orElse(null);
                if (ipVO == null) {
                    trigger.next();
                    return;
                }

                APIUpdateHostKernelInterfaceMsg fakeMsg = new APIUpdateHostKernelInterfaceMsg();
                fakeMsg.setUuid(vo.getUuid());
                fakeMsg.setName(vo.getName());
                fakeMsg.setRequiredIp(newHost.getManagementIp());
                fakeMsg.setNetmask(ipVO.getNetmask());

                thdf.chainSubmit(new ChainTask(fakeMsg) {
                    @Override
                    public String getSyncSignature() {
                        return getL2NetworkOnHostThreadName(vo.getL2NetworkUuid(), vo.getHostUuid());
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        doUpdateHostKernelInterface(fakeMsg, true, new Completion(chain) {
                            @Override
                            public void success() {
                                trigger.next();
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return String.format("update-host-kernel-interface-%s", fakeMsg.getUuid());
                    }
                });
            }
        };
    }

    @Override
    public List<HostNetworkBondingVO> filterBondingCandidates(List<HostNetworkBondingVO> candidates, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return candidates;
        }

        List<String> excludedUuids = Q.New(UplinkGroupVO.class)
                .select(UplinkGroupVO_.bondingUuid)
                .in(UplinkGroupVO_.hostUuid, hostUuids)
                .notNull(UplinkGroupVO_.bondingUuid)
                .listValues();

        return candidates.stream().filter(it -> !excludedUuids.contains(it.getUuid())).collect(Collectors.toList());
    }

    @Override
    public List<HostNetworkInterfaceVO> filterInterfaceCandidates(List<HostNetworkInterfaceVO> candidates, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return candidates;
        }

        List<String> excludedUuids = Q.New(UplinkGroupVO.class)
                .select(UplinkGroupVO_.interfaceUuid)
                .in(UplinkGroupVO_.hostUuid, hostUuids)
                .notNull(UplinkGroupVO_.interfaceUuid)
                .listValues();

        return candidates.stream().filter(it -> !excludedUuids.contains(it.getUuid())).collect(Collectors.toList());
    }

    @Override
    public List<L3NetworkInventory> filterAttachableL3Network(VmInstanceInventory vm, List<L3NetworkInventory> l3s) {
        List<String> l3Uuids = l3s.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList());
        List<L3NetworkInventory> rets = new ArrayList<>(l3s);
        if (l3Uuids.isEmpty()) {
            return rets;
        }

        List<String> defaultPortGroups = VirtualSwitchSystemTags.PORT_GROUP_DEFAULT.filterResourceHasTag(l3Uuids);
        rets.removeIf(l3 -> defaultPortGroups.contains(l3.getUuid()));
        return rets;
    }

    @Override
    public void filterL2NetworkCandidates(List<L2NetworkVO> candidates, ClusterVO clusterVO) {
        List<L2NetworkVO> l2Pgs = candidates.stream()
                .filter(it -> VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(it.getType()))
                .collect(Collectors.toList());
        candidates.removeAll(l2Pgs);

        if ((ClusterConstant.ZSTACK_CLUSTER_TYPE.equals(clusterVO.getType())
                && VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(clusterVO.getHypervisorType()))) {
            List<String> defaultVSwitchUuids = VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.filterResourceHasTag(candidates
                    .stream().map(L2NetworkVO::getUuid).collect(Collectors.toList()));
            candidates.removeIf(it -> defaultVSwitchUuids.contains(it.getUuid()));

            List<String> occupiedBonding = SQL.New("select distinct l2.physicalInterface from L2VirtualSwitchNetworkVO l2, L2NetworkClusterRefVO ref" +
                            " where l2.uuid = ref.l2NetworkUuid" +
                            " and ref.clusterUuid = :clusterUuid")
                    .param("clusterUuid", clusterVO.getUuid())
                    .list();
            candidates.removeIf(it -> !StringUtils.isEmpty(it.getPhysicalInterface()) && occupiedBonding.contains(it.getPhysicalInterface()));
        }
    }

    @Override
    public void filterClusterCandidates(List<ClusterVO> candidates, L2NetworkVO l2NetworkVO) {
        if (VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE.equals(l2NetworkVO.getType())) {
            candidates.clear();
        } else if (!VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE.equals(l2NetworkVO.getType())) {
            return;
        }

        List<String> KVMClusterUuids = candidates.stream()
                .filter(it -> ClusterConstant.ZSTACK_CLUSTER_TYPE.equals(it.getType())
                        && VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(it.getHypervisorType()))
                .map(ClusterVO::getUuid).collect(Collectors.toList());

        if (KVMClusterUuids.isEmpty()) {
            return;
        }

        // filter out KVM clusters for default vSwitch
        if (VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(l2NetworkVO.getUuid())) {
            candidates.removeIf(it -> KVMClusterUuids.contains(it.getUuid()));

        } else if (!StringUtils.isEmpty(l2NetworkVO.getPhysicalInterface())) {
            List<String> KVMClusterUuidsWithBondingUsed = SQL.New("select distinct ref.clusterUuid from L2VirtualSwitchNetworkVO l2, L2NetworkClusterRefVO ref" +
                            " where l2.uuid = ref.l2NetworkUuid" +
                            " and l2.physicalInterface = :physicalInterface" +
                            " and ref.clusterUuid in :clusterUuids")
                    .param("physicalInterface", l2NetworkVO.getPhysicalInterface())
                    .param("clusterUuids", KVMClusterUuids)
                    .list();

            candidates.removeIf(it -> KVMClusterUuidsWithBondingUsed.contains(it.getUuid()));
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
