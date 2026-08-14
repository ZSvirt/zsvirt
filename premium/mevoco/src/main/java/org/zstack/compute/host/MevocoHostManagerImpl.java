package org.zstack.compute.host;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.cluster.MevocoClusterGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.RunInQueue;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.cluster.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.host.ServiceTypeStatisticConstants.InterfaceType;
import org.zstack.header.host.ServiceTypeStatisticConstants.SortBy;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceNumaNodeVO;
import org.zstack.header.vm.VmInstanceNumaNodeVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.network.l3.ServiceTypeExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.verify.ParamValidator;
import org.zstack.utils.verify.Verifiable;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.buildErrIfCanceled;
import static org.zstack.longjob.LongJobUtils.jobCanceled;

public class MevocoHostManagerImpl implements HostExtensionManager, PostHostConnectExtensionPoint, HostAfterConnectedExtensionPoint, Component, ServiceTypeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoHostManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    private final String queueId = "MevocoHostImpl-" + Platform.getUuid();

    private Map<String, HypervisorMessageFactory> hypervisorMessageFactories = Collections.synchronizedMap(new HashMap<String, HypervisorMessageFactory>());

    public boolean start() {
        for (HypervisorMessageFactory f : pluginRgty.getExtensionList(HypervisorMessageFactory.class)) {
            HypervisorMessageFactory old = hypervisorMessageFactories.get(f.getHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HypervisorMessageFactory[%s, %s] for hypervisor type[%s]",
                        old.getClass().getName(), f.getClass().getName(), f.getHypervisorType()));
            }
            hypervisorMessageFactories.put(f.getHypervisorType().toString(), f);
        }
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public HypervisorMessageFactory getHypervisorMessageFactory(HypervisorType type) {
        HypervisorMessageFactory factory = hypervisorMessageFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException("No messageFactory for hypervisor: " + type + " found, check your HypervisorManager.xml");
        }

        return factory;
    }

    protected RunInQueue inQueue() {
        return new RunInQueue(queueId, thdf, 1);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    @Override
    public List<Class> getMessageClasses() {
        return Arrays.asList(APIAddHostFromConfigFileMsg.class,
                APICheckHostConfigFileMsg.class,
                AddKVMHostFromConfigFileMsg.class,
                ChangeHugePageStateMsg.class,
                UpdateHostOvsPmdPinningMsg.class,
                ChangeHostNetworkInterfaceStateMsg.class,
                APIIdentifyHostMsg.class,
                APIPowerOffHostMsg.class,
                APIGetHostNetworkFactsMsg.class,
                APIGetClusterHostNetworkFactsMsg.class,
                APIGetCandidateNetworkBondingsMsg.class,
                APIGetCandidateNetworkInterfacesMsg.class,
                APIChangeHostPasswordMsg.class,
                ChangeZeroCopyStateMsg.class,
                APILocateHostNetworkInterfaceMsg.class,
                APIGetHostPhysicalMemoryFactsMsg.class,
                APIGetHostResourceAllocationMsg.class,
                APIGetHostNUMATopologyMsg.class,
                APISetIpOnHostNetworkInterfaceMsg.class,
                SetIpOnHostNetworkInterfaceMsg.class,
                APISetIpOnHostNetworkBondingMsg.class,
                SetIpOnHostNetworkBondingMsg.class,
                APIUpdateHostNetworkInterfaceMsg.class,
                APISetServiceTypeOnHostNetworkInterfaceMsg.class,
                SetServiceTypeOnHostNetworkInterfaceMsg.class,
                APISetServiceTypeOnHostNetworkBondingMsg.class,
                SetServiceTypeOnHostNetworkBondingMsg.class,
                APIGetCandidateInterfaceVlanIdsMsg.class,
                GetHostNetworkInterfaceByIpMsg.class,
                APIGetInterfaceServiceTypeStatisticMsg.class,
                APIUpdateHostNetworkInterfaceMsg.class,
                APIAllocateHostResourceMsg.class,
                AllocateHostComputeResourceMsg.class,
                APIUpdateHostIscsiInitiatorNameMsg.class
        );
    }


    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddKVMHostFromConfigFileMsg) {
            handle((AddKVMHostFromConfigFileMsg) msg);
        } else if (msg instanceof ChangeHugePageStateMsg) {
            handle((ChangeHugePageStateMsg) msg);
        } else if (msg instanceof ChangeZeroCopyStateMsg) {
            handle((ChangeZeroCopyStateMsg) msg);
        } else if (msg instanceof UpdateHostOvsPmdPinningMsg) {
            handle((UpdateHostOvsPmdPinningMsg) msg);
        } else if (msg instanceof ChangeHostNetworkInterfaceStateMsg) {
            handle((ChangeHostNetworkInterfaceStateMsg) msg);
        } else if (msg instanceof SetIpOnHostNetworkInterfaceMsg) {
            handle((SetIpOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof SetIpOnHostNetworkBondingMsg) {
            handle((SetIpOnHostNetworkBondingMsg) msg);
        } else if (msg instanceof SetServiceTypeOnHostNetworkBondingMsg) {
            handle((SetServiceTypeOnHostNetworkBondingMsg) msg);
        } else if (msg instanceof SetServiceTypeOnHostNetworkInterfaceMsg) {
            handle((SetServiceTypeOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof GetHostNetworkInterfaceByIpMsg) {
            handle((GetHostNetworkInterfaceByIpMsg) msg);
        } else if (msg instanceof AllocateHostComputeResourceMsg) {
            handle((AllocateHostComputeResourceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(AllocateHostComputeResourceMsg msg) {
        inQueue().name(String.format("Allocate-resource-on-host[%s]", msg.getHostUuid()))
                .asyncBackup(msg)
                .run(chain -> allocateHostComputeResource(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));

    }

    private void handle(AddKVMHostFromConfigFileMsg msg) {
        AddHostFromConfigFileReply reply = new AddHostFromConfigFileReply();
        addHostFromFile(msg, new ReturnValueCompletion<AddHostsFromFileResult>(msg) {
            @Override
            public void success(AddHostsFromFileResult result) {
                Optional.ofNullable(buildErrIfCanceled()).ifPresent(reply::setCanceled);
                reply.setResults(result.getResults());
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
        if (msg instanceof APIAddHostFromConfigFileMsg) {
            handle((APIAddHostFromConfigFileMsg) msg);
        } else if (msg instanceof APICheckHostConfigFileMsg) {
            handle((APICheckHostConfigFileMsg) msg);
        } else if (msg instanceof APIIdentifyHostMsg) {
            handle((APIIdentifyHostMsg) msg);
        } else if (msg instanceof APIGetHostNetworkFactsMsg) {
            handle((APIGetHostNetworkFactsMsg) msg);
        } else if (msg instanceof APIGetClusterHostNetworkFactsMsg) {
            handle((APIGetClusterHostNetworkFactsMsg) msg);
        } else if (msg instanceof APIGetCandidateNetworkBondingsMsg) {
            handle((APIGetCandidateNetworkBondingsMsg) msg);
        } else if (msg instanceof APIGetCandidateNetworkInterfacesMsg) {
            handle((APIGetCandidateNetworkInterfacesMsg) msg);
        } else if (msg instanceof APIGetCandidateInterfaceVlanIdsMsg) {
            handle((APIGetCandidateInterfaceVlanIdsMsg) msg);
        } else if (msg instanceof APILocateHostNetworkInterfaceMsg) {
            handle((APILocateHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APIGetHostPhysicalMemoryFactsMsg) {
            handle((APIGetHostPhysicalMemoryFactsMsg) msg);
        } else if (msg instanceof APIPowerOffHostMsg) {
            handle((APIPowerOffHostMsg) msg);
        } else if (msg instanceof APIChangeHostPasswordMsg) {
            handle((APIChangeHostPasswordMsg) msg);
        } else if (msg instanceof APIGetHostNUMATopologyMsg) {
            handle((APIGetHostNUMATopologyMsg) msg);
        } else if (msg instanceof APIGetHostResourceAllocationMsg) {
            handle((APIGetHostResourceAllocationMsg) msg);
        } else if (msg instanceof APISetIpOnHostNetworkInterfaceMsg) {
            handle((APISetIpOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APISetIpOnHostNetworkBondingMsg) {
            handle((APISetIpOnHostNetworkBondingMsg) msg);
        } else if (msg instanceof APIUpdateHostNetworkInterfaceMsg) {
            handle((APIUpdateHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APISetServiceTypeOnHostNetworkBondingMsg) {
            handle((APISetServiceTypeOnHostNetworkBondingMsg) msg);
        } else if (msg instanceof APISetServiceTypeOnHostNetworkInterfaceMsg) {
            handle((APISetServiceTypeOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APIGetInterfaceServiceTypeStatisticMsg) {
            handle((APIGetInterfaceServiceTypeStatisticMsg) msg);
        } else if (msg instanceof APIAllocateHostResourceMsg) {
            handle((APIAllocateHostResourceMsg) msg);
        } else if (msg instanceof APIUpdateHostIscsiInitiatorNameMsg) {
            handle((APIUpdateHostIscsiInitiatorNameMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateHostIscsiInitiatorNameMsg msg) {
        APIUpdateHostIscsiInitiatorNameEvent event = new APIUpdateHostIscsiInitiatorNameEvent(msg.getId());
        UpdateHostIscsiInitiatorNameMsg umsg = new UpdateHostIscsiInitiatorNameMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setIscsiInitiatorName(msg.getIscsiInitiatorName());
        bus.makeTargetServiceIdByResourceUuid(umsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                UpdateHostIscsiInitiatorNameReply r = reply.castReply();
                if (!r.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(r.getError());
                } else {
                    event.setInventory(r.getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIAllocateHostResourceMsg msg) {
        APIAllocateHostResourceEvent evt = new APIAllocateHostResourceEvent(msg.getId());

        AllocateHostComputeResourceMsg kMsg = new AllocateHostComputeResourceMsg();
        kMsg.setHostUuid(msg.getHostUuid());
        kMsg.setScene(msg.getScene());
        kMsg.setVcpu(msg.getVcpu());
        kMsg.setStrategy(msg.getStrategy());
        kMsg.setMemSize(msg.getMemSize());

        bus.makeTargetServiceIdByResourceUuid(kMsg, HostConstant.SERVICE_ID, kMsg.getHostUuid());
        bus.send(kMsg, new CloudBusCallBack(kMsg) {

            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply.getError());
                } else {
                    AllocateHostComputeResourceReply rpy = (AllocateHostComputeResourceReply) reply;
                    evt.setName(rpy.getHostName());
                    evt.setUuid(rpy.getHostUuid());
                    evt.setvCPUPin(rpy.getPins());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(SetServiceTypeOnHostNetworkBondingMsg msg) {
        SetServiceTypeOnHostNetworkBondingReply reply = new SetServiceTypeOnHostNetworkBondingReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "Bonding-" + msg.getBondingUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                fchain.setName(String.format("set-service-type-on-bonding-%s", msg.getBondingUuid()));
                fchain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        if (msg.getVlanId() != null && msg.getVlanId() != 0) {
                            flow(new NoRollbackFlow() {
                                String __name__ = "check-interface-vlan";

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    HostNetworkBondingVO vo = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
                                    final MevocoKVMAgentCommands.CheckInterfaceVlanCmd cmd = new MevocoKVMAgentCommands.CheckInterfaceVlanCmd();
                                    cmd.setInterfaceName(vo.getBondingName());
                                    cmd.setVlanId(msg.getVlanId());

                                    KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                                    kmsg.setHostUuid(vo.getHostUuid());
                                    kmsg.setCommand(cmd);
                                    kmsg.setPath(MevocoKVMConstant.CHECK_INTERFACE_VLAN);
                                    bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, vo.getHostUuid());
                                    bus.send(kmsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply mreply) {
                                            if (!mreply.isSuccess()) {
                                                SQL.New(HostNetworkBondingServiceRefVO.class)
                                                        .eq(HostNetworkBondingServiceRefVO_.bondingUuid, msg.getBondingUuid())
                                                        .eq(HostNetworkBondingServiceRefVO_.vlanId, msg.getVlanId())
                                                        .hardDelete();
                                                trigger.fail(mreply.getError());
                                                return;
                                            } else {
                                                KVMHostAsyncHttpCallReply hreply = mreply.castReply();
                                                MevocoKVMAgentCommands.CheckInterfaceVlanRsp rsp = hreply.toResponse(MevocoKVMAgentCommands.CheckInterfaceVlanRsp.class);
                                                if (!rsp.isSuccess()) {
                                                    trigger.fail(operr("failed to check vlan id[%s] for network bonding[%s] on kvm host[uuid: %s], %s",
                                                            cmd.getVlanId(), cmd.getInterfaceName(), vo.getHostUuid(), rsp.getError()));
                                                    return;
                                                }
                                                String info = String.format("successfully check vlan id[%s] for network bonding[%s] on kvm host[uuid: %s], %s",
                                                        cmd.getVlanId(), cmd.getInterfaceName(), vo.getHostUuid(), rsp.getError());
                                                logger.debug(info);
                                            }
                                            trigger.next();
                                        }
                                    });
                                }
                            });
                        }

                        flow(new NoRollbackFlow() {
                            String __name__ = "apply-to-backend";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                HostNetworkBondingVO vo = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);

                                final MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd();
                                cmd.setInterfaceName(vo.getBondingName());
                                cmd.setVlanId(msg.getVlanId());
                                cmd.setServiceType(msg.getServiceType());

                                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                                kmsg.setHostUuid(vo.getHostUuid());
                                kmsg.setCommand(cmd);
                                kmsg.setPath(MevocoKVMConstant.SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE);
                                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, vo.getHostUuid());
                                bus.send(kmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply mreply) {
                                        if (!mreply.isSuccess()) {
                                            trigger.fail(mreply.getError());
                                            return;
                                        } else {
                                            KVMHostAsyncHttpCallReply hreply = mreply.castReply();
                                            MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp rsp = hreply.toResponse(MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp.class);
                                            if (!rsp.isSuccess()) {
                                                trigger.fail(operr("failed to set service type[%s] for network bonding[%s] on kvm host[uuid: %s], %s",
                                                        cmd.getServiceType(), cmd.getInterfaceName(), vo.getHostUuid(), rsp.getError()));
                                                return;
                                            }
                                            String info = String.format("successfully to set service type[%s] for network bonding[%s] on kvm host[uuid: %s]",
                                                    cmd.getServiceType(), cmd.getInterfaceName(), vo.getHostUuid());
                                            logger.debug(info);
                                        }
                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "write-to-db";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                if (msg.getVlanId() == null) {
                                    msg.setVlanId(0);
                                }
                                SQL.New(HostNetworkBondingServiceRefVO.class)
                                        .eq(HostNetworkBondingServiceRefVO_.bondingUuid, msg.getBondingUuid())
                                        .eq(HostNetworkBondingServiceRefVO_.vlanId, msg.getVlanId())
                                        .hardDelete();

                                if (msg.getServiceType() != null && !msg.getServiceType().isEmpty()) {
                                    List<HostNetworkBondingServiceRefVO> list = new ArrayList<>();
                                    for (String serviceType : msg.getServiceType()) {
                                        HostNetworkBondingServiceRefVO refVO = new HostNetworkBondingServiceRefVO();
                                        refVO.setBondingUuid(msg.getBondingUuid());
                                        refVO.setVlanId(msg.getVlanId());
                                        refVO.setServiceType(HostNetworkInterfaceServiceType.valueOf(serviceType));
                                        refVO.setCreateDate(new Timestamp(new Date().getTime()));
                                        list.add(refVO);
                                    }
                                    dbf.persistCollection(list);
                                }
                                trigger.next();
                            }
                        });

                        done(new FlowDoneHandler(reply) {
                            @Override
                            public void handle(Map data) {
                                List<HostNetworkBondingServiceRefVO> refVOS = Q.New(HostNetworkBondingServiceRefVO.class).eq(HostNetworkBondingServiceRefVO_.bondingUuid, msg.getBondingUuid())
                                        .eq(HostNetworkBondingServiceRefVO_.vlanId, msg.getVlanId() == null ? 0 : msg.getVlanId())
                                        .list();
                                reply.setInventory(HostNetworkBondingServiceRefInventory.valueOf(refVOS));
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });

                        error(new FlowErrorHandler(reply) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                reply.setError(errCode);
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APISetServiceTypeOnHostNetworkBondingMsg msg) {
        APISetServiceTypeOnHostNetworkBondingEvent event = new APISetServiceTypeOnHostNetworkBondingEvent(msg.getId());

        List<SetServiceTypeOnHostNetworkBondingMsg> msgs = new ArrayList<>();
        List<Integer> vlanIds = msg.getVlanIds() != null ? msg.getVlanIds() : Collections.singletonList(null);
        for (String bondingUuid : msg.getBondingUuids()) {
            for (Integer vlanId : vlanIds) {
                SetServiceTypeOnHostNetworkBondingMsg setServiceTypeMsg = new SetServiceTypeOnHostNetworkBondingMsg();
                setServiceTypeMsg.setBondingUuid(bondingUuid);
                setServiceTypeMsg.setVlanId(vlanId);
                setServiceTypeMsg.setServiceType(msg.getServiceTypes());

                bus.makeLocalServiceId(setServiceTypeMsg, HostConstant.SERVICE_ID);
                msgs.add(setServiceTypeMsg);
            }
        }

        List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());
        List<HostNetworkBondingServiceRefInventory> inventories = new ArrayList<>();
        new While<>(msgs).step((smsg, whileCompletion) ->
                bus.send(smsg, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            SetServiceTypeOnHostNetworkBondingReply aReply = reply.castReply();
                            inventories.addAll(aReply.getInventory());
                            whileCompletion.done();
                        } else {
                            errors.add(reply.getError());
                            whileCompletion.allDone();
                        }
                    }
                }), 10).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errors.size() > 0) {
                    event.setError(errors.get(0));
                    bus.publish(event);
                    return;
                }
                event.setInventory(inventories);
                bus.publish(event);
            }
        });
    }

    private void handle(SetServiceTypeOnHostNetworkInterfaceMsg msg) {
        SetServiceTypeOnHostNetworkInterfaceReply reply = new SetServiceTypeOnHostNetworkInterfaceReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "Interface-" + msg.getInterfaceUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                FlowChain fchain = FlowChainBuilder.newShareFlowChain();
                fchain.setName(String.format("set-service-type-on-interface-%s", msg.getInterfaceUuid()));
                fchain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        if (msg.getVlanId() != null && msg.getVlanId() != 0) {
                            flow(new NoRollbackFlow() {
                                String __name__ = "check-interface-vlan";

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    HostNetworkInterfaceVO vo = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
                                    final MevocoKVMAgentCommands.CheckInterfaceVlanCmd cmd = new MevocoKVMAgentCommands.CheckInterfaceVlanCmd();
                                    cmd.setInterfaceName(vo.getInterfaceName());
                                    cmd.setVlanId(msg.getVlanId());

                                    KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                                    kmsg.setHostUuid(vo.getHostUuid());
                                    kmsg.setCommand(cmd);
                                    kmsg.setPath(MevocoKVMConstant.CHECK_INTERFACE_VLAN);
                                    bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, vo.getHostUuid());
                                    bus.send(kmsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply mreply) {
                                            if (!mreply.isSuccess()) {
                                                SQL.New(HostNetworkInterfaceServiceRefVO.class)
                                                        .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, msg.getInterfaceUuid())
                                                        .eq(HostNetworkInterfaceServiceRefVO_.vlanId, msg.getVlanId())
                                                        .delete();
                                                trigger.fail(mreply.getError());
                                                return;
                                            } else {
                                                KVMHostAsyncHttpCallReply hreply = mreply.castReply();
                                                MevocoKVMAgentCommands.CheckInterfaceVlanRsp rsp = hreply.toResponse(MevocoKVMAgentCommands.CheckInterfaceVlanRsp.class);
                                                if (!rsp.isSuccess()) {
                                                    trigger.fail(operr("failed to check vlan id[%s] for network interface[%s] on kvm host[uuid: %s], %s",
                                                            cmd.getVlanId(), cmd.getInterfaceName(), vo.getHostUuid(), rsp.getError()));
                                                    return;
                                                }
                                                String info = String.format("successfully check vlan id[%s] for network interface[%s] on kvm host[uuid: %s]",
                                                        cmd.getVlanId(), cmd.getInterfaceName(), vo.getHostUuid());
                                                logger.debug(info);
                                            }
                                            trigger.next();
                                        }
                                    });
                                }
                            });
                        }

                        flow(new NoRollbackFlow() {
                            String __name__ = "apply-to-backend";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                HostNetworkInterfaceVO vo = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);

                                final MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd();
                                cmd.setInterfaceName(vo.getInterfaceName());
                                cmd.setVlanId(msg.getVlanId());
                                cmd.setServiceType(msg.getServiceType());

                                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                                kmsg.setHostUuid(vo.getHostUuid());
                                kmsg.setCommand(cmd);
                                kmsg.setPath(MevocoKVMConstant.SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE);
                                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, vo.getHostUuid());
                                bus.send(kmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply mreply) {
                                        if (!mreply.isSuccess()) {
                                            trigger.fail(mreply.getError());
                                            return;
                                        } else {
                                            KVMHostAsyncHttpCallReply hreply = mreply.castReply();
                                            MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp rsp = hreply.toResponse(MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp.class);
                                            if (!rsp.isSuccess()) {
                                                trigger.fail(operr("failed to set service type[%s] for network bonding[%s] on kvm host[uuid: %s], %s",
                                                        cmd.getServiceType(), cmd.getInterfaceName(), vo.getHostUuid(), rsp.getError()));
                                                return;
                                            }
                                            String info = String.format("successfully to set service type[%s] for network interface[%s] on kvm host[uuid: %s]",
                                                    cmd.getServiceType(), cmd.getInterfaceName(), vo.getHostUuid());
                                            logger.debug(info);
                                        }
                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "write-to-db";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                if (msg.getVlanId() == null) {
                                    msg.setVlanId(0);
                                }
                                SQL.New(HostNetworkInterfaceServiceRefVO.class)
                                        .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, msg.getInterfaceUuid())
                                        .eq(HostNetworkInterfaceServiceRefVO_.vlanId, msg.getVlanId())
                                        .delete();

                                if (msg.getServiceType() != null && !msg.getServiceType().isEmpty()) {
                                    List<HostNetworkInterfaceServiceRefVO> list = new ArrayList<>();
                                    for (String serviceType : msg.getServiceType()) {
                                        HostNetworkInterfaceServiceRefVO refVO = new HostNetworkInterfaceServiceRefVO();
                                        refVO.setInterfaceUuid(msg.getInterfaceUuid());
                                        refVO.setVlanId(msg.getVlanId());
                                        refVO.setServiceType(HostNetworkInterfaceServiceType.valueOf(serviceType));
                                        refVO.setCreateDate(new Timestamp(new Date().getTime()));
                                        list.add(refVO);
                                    }
                                    dbf.persistCollection(list);
                                }
                                trigger.next();
                            }
                        });

                        done(new FlowDoneHandler(reply) {
                            @Override
                            public void handle(Map data) {
                                List<HostNetworkInterfaceServiceRefVO> refVOS = Q.New(HostNetworkInterfaceServiceRefVO.class).eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, msg.getInterfaceUuid())
                                        .eq(HostNetworkInterfaceServiceRefVO_.vlanId, msg.getVlanId() == null ? 0 : msg.getVlanId())
                                        .list();
                                reply.setInventory(HostNetworkInterfaceServiceRefInventory.valueOf(refVOS));
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });

                        error(new FlowErrorHandler(reply) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                reply.setError(errCode);
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APISetServiceTypeOnHostNetworkInterfaceMsg msg) {
        APISetServiceTypeOnHostNetworkInterfaceEvent event = new APISetServiceTypeOnHostNetworkInterfaceEvent(msg.getId());

        List<SetServiceTypeOnHostNetworkInterfaceMsg> msgs = new ArrayList<>();
        List<Integer> vlanIds = msg.getVlanIds() != null ? msg.getVlanIds() : Collections.singletonList(null);
        for (String interfaceUuid : msg.getInterfaceUuids()) {
            for (Integer vlanId : vlanIds) {
                SetServiceTypeOnHostNetworkInterfaceMsg setServiceTypeMsg = new SetServiceTypeOnHostNetworkInterfaceMsg();
                setServiceTypeMsg.setInterfaceUuid(interfaceUuid);
                setServiceTypeMsg.setVlanId(vlanId);
                setServiceTypeMsg.setServiceType(msg.getServiceTypes());

                bus.makeLocalServiceId(setServiceTypeMsg, HostConstant.SERVICE_ID);
                msgs.add(setServiceTypeMsg);
            }
        }

        List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());
        List<HostNetworkInterfaceServiceRefInventory> inventories = new ArrayList<>();
        new While<>(msgs).step((smsg, whileCompletion) ->
                bus.send(smsg, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            SetServiceTypeOnHostNetworkInterfaceReply aReply = reply.castReply();
                            inventories.addAll(aReply.getInventory());
                            whileCompletion.done();
                        } else {
                            errors.add(reply.getError());
                            whileCompletion.allDone();
                        }
                    }
                }), 10).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errors.size() > 0) {
                    event.setError(errors.get(0));
                    bus.publish(event);
                    return;
                }
                event.setInventory(inventories);
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetInterfaceServiceTypeStatisticMsg msg) {
        APIGetInterfaceServiceTypeStatisticReply reply = new APIGetInterfaceServiceTypeStatisticReply();
        List<ServiceTypeStatisticData> serviceTypeStatistics = filterStatistic(msg);
        int totalCount = serviceTypeStatistics.size();
        int start = msg.getStart();
        int end = start + msg.getLimit();
        if (end > totalCount) {
            end = totalCount;
        }
        List<ServiceTypeStatisticData> paginatedResults = serviceTypeStatistics.subList(start, end);
        reply.setServiceTypeStatistics(paginatedResults);
        if (msg.isReplyWithCount()) {
            reply.setTotal((long) totalCount);
        }

        bus.reply(msg, reply);
    }

    private List<ServiceTypeStatisticData> filterStatistic(APIGetInterfaceServiceTypeStatisticMsg msg) {
        /*
        select ref.interfaceUuid, ref.interfaceName, ref.vlanId, ref.serviceType, host.uuid as hostUuid, host.name as hostName, host.managementIp as hostIp, cluster.uuid as clusterUuid, cluster.name as clusterName, host.zoneUuid, ref.createDate
        from (
            select bondingUuid as interfaceUuid, bondingName as interfaceName, vlanId, serviceType, hostUuid, ref.createDate
            from (select * from HostNetworkBondingServiceRefVO) ref
            left join HostNetworkBondingVO interface ON ref.bondingUuid = interface.uuid [where bondingUuid = '{bondingUuid}'] [where bondingUuid = '{bondingUuid}'] [and vlanId = '{vlanId}'] [and bondingUuid is null]
            union
            select interfaceUuid, interfaceName, vlanId, serviceType, hostUuid, ref.createDate
            from (select * from HostNetworkInterfaceServiceRefVO) ref
            left join HostNetworkInterfaceVO interface ON ref.interfaceUuid = interface.uuid  [where interfaceUuid = '{interfaceUuid}'] [and vlanId = '{vlanId}'] [and interfaceUuid is null]
        ) ref
        inner join (select uuid, name, managementIp, clusterUuid, zoneUuid from HostVO [where uuid = '{hostUuid}']) host on ref.hostUuid = host.uuid
        inner join (select uuid, zoneUuid, name from ClusterVO [where uuid = '{clusterUuid}']) cluster on host.clusterUuid = cluster.uuid
        inner join (select uuid from ZoneVO [where uuid = '{zoneUuid}']) zone on host.zoneUuid = zone.uuid
        order by {sortBy} {direction}
        //limit {limit} offset {start};
         */
        if (msg.getSortBy().equals(SortBy.VLAN_ID)) {
            msg.setSortBy("interfaceName, vlanId");
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ref.interfaceUuid, ref.interfaceName, ref.vlanId, ref.serviceType, host.uuid AS hostUuid, host.name AS hostName, host.managementIp AS hostIp, cluster.uuid AS clusterUuid, cluster.name AS clusterName, host.zoneUuid, ref.createDate ")
                .append("FROM (")
                .append("SELECT bondingUuid AS interfaceUuid, bondingName AS interfaceName, vlanId, serviceType, hostUuid, ref.createDate ")
                .append("FROM HostNetworkBondingServiceRefVO ref ")
                .append("LEFT JOIN HostNetworkBondingVO interface ON ref.bondingUuid = interface.uuid ")
                .append("WHERE (:vlanId IS NULL OR vlanId = :vlanId) AND (:bondingUuid IS NULL OR bondingUuid = :bondingUuid) AND (:interfaceOnly != true OR ref.bondingUuid IS NULL)")
                .append("UNION ")
                .append("SELECT interfaceUuid, interfaceName, vlanId, serviceType, hostUuid, ref.createDate ")
                .append("FROM HostNetworkInterfaceServiceRefVO ref ")
                .append("LEFT JOIN HostNetworkInterfaceVO interface ON ref.interfaceUuid = interface.uuid ")
                .append("WHERE (:vlanId IS NULL OR vlanId = :vlanId) AND (:interfaceUuid IS NULL OR interfaceUuid = :interfaceUuid) AND (:bondingOnly != true OR ref.interfaceUuid IS NULL) ")
                .append(") ref ")
                .append("INNER JOIN HostVO host ON ref.hostUuid = host.uuid ")
                .append("INNER JOIN ClusterVO cluster ON host.clusterUuid = cluster.uuid ")
                .append("INNER JOIN ZoneVO zone ON host.zoneUuid = zone.uuid ")
                .append("WHERE (:hostUuid IS NULL OR host.uuid = :hostUuid) AND (:clusterUuid IS NULL OR cluster.uuid = :clusterUuid) AND (:zoneUuid IS NULL OR zone.uuid = :zoneUuid) ")
                .append("ORDER BY ").append(msg.getSortBy()).append(' ').append(msg.getSortDirection());
        //.append(" LIMIT ").append(msg.getLimit()).append(" OFFSET ").append(msg.getStart());

        Query q = dbf.getEntityManager().createNativeQuery(sqlBuilder.toString())
                .setParameter("vlanId", msg.getVlanId())
                .setParameter("bondingUuid", msg.getInterfaceUuid())
                .setParameter("bondingOnly", InterfaceType.BONDING.equals(msg.getInterfaceType()))
                .setParameter("interfaceUuid", msg.getInterfaceUuid())
                .setParameter("interfaceOnly", InterfaceType.INTERFACE.equals(msg.getInterfaceType()))
                .setParameter("hostUuid", msg.getHostUuid())
                .setParameter("clusterUuid", msg.getClusterUuid())
                .setParameter("zoneUuid", msg.getZoneUuid());
        List<Object[]> results = q.getResultList();

        List<ServiceTypeStatisticData> serviceTypeStatistics = new ArrayList<>();
        for (Object[] result : results) {
            ServiceTypeStatisticData element = new ServiceTypeStatisticData();
            serviceTypeStatistics.add(element);
            element.setInterfaceUuid((String) result[0]);
            element.setInterfaceName((String) result[1]);
            element.setVlanId((Integer) result[2]);
            element.setHostUuid((String) result[4]);
            element.setHostName((String) result[5]);
            element.setHostIp((String) result[6]);
            element.setClusterUuid((String) result[7]);
            element.setClusterName((String) result[8]);
            element.setZoneUuid((String) result[9]);
            element.setCreateDate((Timestamp) result[10]);
        }

        Map<Map<String, Integer>, List<String>> serviceTypeMap = new HashMap<>();
        for (Object[] result : results) {
            String interfaceUuid = (String) result[0];
            Integer vlanId = (Integer) result[2];
            String serviceType = (String) result[3];
            Map<String, Integer> key = new HashMap<>();
            key.put(interfaceUuid, vlanId);
            serviceTypeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(serviceType);
        }

        for (ServiceTypeStatisticData element : serviceTypeStatistics) {
            String interfaceUuid = element.getInterfaceUuid();
            Integer vlanId = element.getVlanId();
            Map<String, Integer> key = new HashMap<>();
            key.put(interfaceUuid, vlanId);
            List<String> serviceTypes = serviceTypeMap.getOrDefault(key, Collections.emptyList());
            element.setServiceTypes(serviceTypes);
        }

        List<ServiceTypeStatisticData> distinctServiceTypeStatistics = new ArrayList<>();
        for (ServiceTypeStatisticData element : serviceTypeStatistics) {
            List<String> serviceTypes = element.getServiceTypes();
            if (msg.getServiceType() != null) {
                if (serviceTypes.containsAll(msg.getServiceType())) {
                    distinctServiceTypeStatistics.add(element);
                }
            } else {
                distinctServiceTypeStatistics.add(element);
            }
        }

        Set<String> uniqueCombinations = new HashSet<>();
        List<ServiceTypeStatisticData> distinctInterfaceNameVlanIdStatistics = new ArrayList<>();
        for (ServiceTypeStatisticData element : distinctServiceTypeStatistics) {
            String interfaceUuid = element.getInterfaceUuid();
            Integer vlanId = element.getVlanId();
            String combination = interfaceUuid + "-" + vlanId;

            if (!uniqueCombinations.contains(combination)) {
                uniqueCombinations.add(combination);
                distinctInterfaceNameVlanIdStatistics.add(element);
            }
        }

        return distinctInterfaceNameVlanIdStatistics;
    }

    private void handle(final GetHostNetworkInterfaceByIpMsg msg) {
        GetHostNetworkInterfaceByIpReply reply = new GetHostNetworkInterfaceByIpReply();

        final MevocoKVMAgentCommands.GetInterfaceNameCmd cmd = new MevocoKVMAgentCommands.GetInterfaceNameCmd();
        cmd.setIpAddresses(msg.getIpAddresses());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.GET_INTERFACE_NAME);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply re) {
                if (!re.isSuccess()) {
                    reply.setError(re.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = re.castReply();
                MevocoKVMAgentCommands.GetInterfaceNameRsp rsp = r.toResponse(MevocoKVMAgentCommands.GetInterfaceNameRsp.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setInterfaceNames(rsp.getInterfaceNames());
                    logger.info(String.format("successfully get interface name for network interface on kvm host[uuid: %s]", kmsg.getHostUuid()));
                }
                bus.reply(msg, reply);
            }
        });

    }

    private void changeManagementServiceTypeOnInterface(String hostUuid, String interfaceName, Integer vlanId, boolean isDelete, final Completion completion) {
        final Set<HostNetworkInterfaceServiceType> serviceTypes = new HashSet<>();

        HostNetworkInterfaceVO interfaceVO = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .eq(HostNetworkInterfaceVO_.interfaceName, interfaceName).find();
        HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, interfaceName).find();

        if (interfaceVO == null && bondingVO == null) {
            logger.debug(String.format("interface[%s] can not be found", interfaceName));
            completion.success();
            return;
        }

        if (interfaceVO != null) {
            serviceTypes.addAll(Q.New(HostNetworkInterfaceServiceRefVO.class).select(HostNetworkInterfaceServiceRefVO_.serviceType)
                    .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, interfaceVO.getUuid())
                    .eq(HostNetworkInterfaceServiceRefVO_.vlanId, vlanId).listValues());
            boolean isExistMnServiceType = Q.New(HostNetworkInterfaceServiceRefVO.class)
                    .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, interfaceVO.getUuid())
                    .eq(HostNetworkInterfaceServiceRefVO_.vlanId, vlanId)
                    .eq(HostNetworkInterfaceServiceRefVO_.serviceType, HostNetworkInterfaceServiceType.ManagementNetwork)
                    .isExists();
            if (!isExistMnServiceType && isDelete) {
                completion.success();
                return;
            }
            if (isExistMnServiceType && !isDelete) {
                completion.success();
                return;
            }
            if (isDelete) {
                serviceTypes.remove(HostNetworkInterfaceServiceType.ManagementNetwork);
            } else {
                serviceTypes.add(HostNetworkInterfaceServiceType.ManagementNetwork);
            }
        }

        if (bondingVO != null) {
            serviceTypes.addAll(Q.New(HostNetworkBondingServiceRefVO.class).select(HostNetworkBondingServiceRefVO_.serviceType)
                    .eq(HostNetworkBondingServiceRefVO_.bondingUuid, bondingVO.getUuid())
                    .eq(HostNetworkBondingServiceRefVO_.vlanId, vlanId).listValues());
            boolean isExistMnServiceType = Q.New(HostNetworkBondingServiceRefVO.class)
                    .eq(HostNetworkBondingServiceRefVO_.bondingUuid, bondingVO.getUuid())
                    .eq(HostNetworkBondingServiceRefVO_.vlanId, vlanId)
                    .eq(HostNetworkBondingServiceRefVO_.serviceType, HostNetworkInterfaceServiceType.ManagementNetwork)
                    .isExists();
            if (!isExistMnServiceType && isDelete) {
                completion.success();
                return;
            }
            if (isExistMnServiceType && !isDelete) {
                completion.success();
                return;
            }
            if (isDelete) {
                serviceTypes.remove(HostNetworkInterfaceServiceType.ManagementNetwork);
            } else {
                serviceTypes.add(HostNetworkInterfaceServiceType.ManagementNetwork);
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("set-management-service-type-on-interface-%s-with-virtual-id-%s-of-host-%s", interfaceName, vlanId, hostUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceCmd();
                        cmd.setInterfaceName(interfaceName);
                        cmd.setVlanId(vlanId != 0 ? vlanId : null);
                        cmd.setServiceType(serviceTypes.stream()
                                .map(HostNetworkInterfaceServiceType::toString)
                                .collect(Collectors.toList()));
                        //attach service type for bridge port or vxlan port
                        if (interfaceVO == null && bondingVO == null) {
                            cmd.setServiceType((Collections.singletonList(HostNetworkInterfaceServiceType.ManagementNetwork.toString())));
                        }

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setHostUuid(hostUuid);
                        kmsg.setCommand(cmd);
                        kmsg.setPath(MevocoKVMConstant.SET_SERVICE_TYPE_ON_HOST_NETWORK_INTERFACE);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(kmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetServiceTypeOnHostNetworkInterfaceRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr("failed to set service type[%s] for network interface[%s] on kvm host[uuid: %s], %s",
                                            cmd.getServiceType(), cmd.getInterfaceName(), hostUuid, rsp.getError()));
                                    return;
                                }
                                String info = String.format("successfully set service type[%s] for network interface[%s] on kvm host[uuid: %s]",
                                        cmd.getServiceType(), cmd.getInterfaceName(), hostUuid);
                                logger.debug(info);
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "write-service-type-to-db";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (interfaceVO != null) {
                                    if (!isDelete) {
                                        HostNetworkInterfaceServiceRefVO refVO = new HostNetworkInterfaceServiceRefVO();
                                        refVO.setInterfaceUuid(interfaceVO.getUuid());
                                        refVO.setVlanId(vlanId);
                                        refVO.setServiceType(HostNetworkInterfaceServiceType.ManagementNetwork);
                                        dbf.persistAndRefresh(refVO);
                                    } else {
                                        SQL.New(HostNetworkInterfaceServiceRefVO.class)
                                                .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, interfaceVO.getUuid())
                                                .eq(HostNetworkInterfaceServiceRefVO_.vlanId, vlanId)
                                                .eq(HostNetworkInterfaceServiceRefVO_.serviceType, HostNetworkInterfaceServiceType.ManagementNetwork)
                                                .hardDelete();
                                    }
                                }

                                if (bondingVO != null) {
                                    if (!isDelete) {
                                        HostNetworkBondingServiceRefVO refVO = new HostNetworkBondingServiceRefVO();
                                        refVO.setBondingUuid(bondingVO.getUuid());
                                        refVO.setVlanId(vlanId);
                                        refVO.setServiceType(HostNetworkInterfaceServiceType.ManagementNetwork);
                                        dbf.persistAndRefresh(refVO);
                                    } else {
                                        SQL.New(HostNetworkBondingServiceRefVO.class)
                                                .eq(HostNetworkBondingServiceRefVO_.bondingUuid, bondingVO.getUuid())
                                                .eq(HostNetworkBondingServiceRefVO_.vlanId, vlanId)
                                                .eq(HostNetworkBondingServiceRefVO_.serviceType, HostNetworkInterfaceServiceType.ManagementNetwork)
                                                .hardDelete();
                                    }
                                }
                            }
                        }.execute();

                        trigger.next();
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

    private void handle(APISetIpOnHostNetworkInterfaceMsg msg) {
        APISetIpOnHostNetworkInterfaceEvent event = new APISetIpOnHostNetworkInterfaceEvent(msg.getId());

        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        HostNetworkInterfaceVO interfaceVO = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuid()).find();

        SetIpOnHostNetworkInterfaceMsg msg1 = new SetIpOnHostNetworkInterfaceMsg();
        msg1.setInterfaceUuid(msg.getInterfaceUuid());
        msg1.setIpAddress(msg.getIpAddress());
        msg1.setNetmask(msg.getNetmask());

        boolean ipChanged = true;
        if (interfaceVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");

            if (ipNetmaskArray[0].equals(hostVO.getManagementIp())) {
                ipChanged = false;
            }
        }
        if (interfaceVO.getIpAddresses() == null && msg.getIpAddress() == null) {
            ipChanged = false;
        }
        if (interfaceVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
            if (ipNetmaskArray[0].equals(msg.getIpAddress()) && Objects.equals(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])), msg.getNetmask())) {
                ipChanged = false;
            }
        }

        if (ipChanged) {
            bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
            bus.send(msg1, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply1) {
                    if (!reply1.isSuccess()) {
                        event.setSuccess(false);
                        event.setError(reply1.getError());
                    } else {
                        SetIpOnHostNetworkInterfaceReply reply = reply1.castReply();
                        event.setInventory(reply.getInventory());
                    }
                    bus.publish(event);
                }
            });
            return;
        }

        event.setInventory(HostNetworkInterfaceInventory.valueOf(interfaceVO));
        bus.publish(event);
    }

    private void handle(APISetIpOnHostNetworkBondingMsg msg) {
        APISetIpOnHostNetworkBondingEvent event = new APISetIpOnHostNetworkBondingEvent(msg.getId());

        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, msg.getBondingUuid()).find();

        SetIpOnHostNetworkBondingMsg msg1 = new SetIpOnHostNetworkBondingMsg();
        msg1.setBondingUuid(msg.getBondingUuid());
        msg1.setIpAddress(msg.getIpAddress());
        msg1.setNetmask(msg.getNetmask());

        boolean ipChanged = true;
        if (bondingVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(bondingVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");

            if (ipNetmaskArray[0].equals(hostVO.getManagementIp())) {
                ipChanged = false;
            }
        }
        if (bondingVO.getIpAddresses() == null && msg.getIpAddress() == null) {
            ipChanged = false;
        }
        if (bondingVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(bondingVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
            if (ipNetmaskArray[0].equals(msg.getIpAddress()) && Objects.equals(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])), msg.getNetmask())) {
                ipChanged = false;
            }
        }

        if (ipChanged) {
            bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
            bus.send(msg1, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply1) {
                    if (!reply1.isSuccess()) {
                        event.setSuccess(false);
                        event.setError(reply1.getError());
                    } else {
                        SetIpOnHostNetworkBondingReply reply = reply1.castReply();
                        event.setInventory(reply.getInventory());
                    }
                    bus.publish(event);
                }
            });
            return;
        }

        event.setInventory(HostNetworkBondingInventory.valueOf(bondingVO));
        bus.publish(event);
    }

    private void handle(APIUpdateHostNetworkInterfaceMsg msg) {
        APIUpdateHostNetworkInterfaceEvent event = new APIUpdateHostNetworkInterfaceEvent(msg.getId());

        HostNetworkInterfaceVO interfaceVO = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuid()).find();
        interfaceVO.setDescription(msg.getDescription());
        interfaceVO = dbf.updateAndRefresh(interfaceVO);

        event.setInventory(HostNetworkInterfaceInventory.valueOf(interfaceVO));
        bus.publish(event);
    }

    private void handle(SetIpOnHostNetworkInterfaceMsg msg) {
        SetIpOnHostNetworkInterfaceReply reply = new SetIpOnHostNetworkInterfaceReply();

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-ip-to-interface-%s", msg.getInterfaceUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-ip-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
                        data.put("preIpAddress", interfaceVO.getIpAddresses());

                        if (msg.getIpAddress() != null) {
                            Integer prefix = NetworkUtils.getPrefixLengthFromNetmask(msg.getNetmask());
                            interfaceVO.setIpAddresses(msg.getIpAddress() + '/' + prefix);
                        } else {
                            interfaceVO.setIpAddresses(null);
                        }
                        dbf.updateAndRefresh(interfaceVO);

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        String preIpAddress = (String) data.get("preIpAddress");
                        SQL.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuid())
                                .set(HostNetworkInterfaceVO_.ipAddresses, preIpAddress)
                                .update();

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceCmd();
                        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
                        String preIpAddress = (String) data.get("preIpAddress");
                        if (preIpAddress != null) {
                            List<String> ipAddresses = Arrays.asList(preIpAddress.split(","));
                            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                            cmd.setOldIpAddress(ipNetmaskArray[0]);
                            cmd.setOldNetmask(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])));
                        }
                        cmd.setInterfaceName(interfaceVO.getInterfaceName());
                        cmd.setIpAddress(msg.getIpAddress());
                        cmd.setNetmask(msg.getNetmask());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setHostUuid(msg.getHostUuid());
                        kmsg.setCommand(cmd);
                        kmsg.setPath(MevocoKVMConstant.SET_IP_ON_HOST_NETWORK_INTERFACE);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(kmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply mreply) {
                                if (!mreply.isSuccess()) {
                                    trigger.fail(mreply.getError());
                                    return;
                                } else {
                                    KVMHostAsyncHttpCallReply r = mreply.castReply();
                                    MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp.class);
                                    if (!rsp.isSuccess()) {
                                        trigger.fail(operr("failed to update interface ip, because %s", rsp.getError()));
                                        return;
                                    }
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
                        reply.setInventory(HostNetworkInterfaceInventory.valueOf(interfaceVO));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(SetIpOnHostNetworkBondingMsg msg) {
        SetIpOnHostNetworkBondingReply reply = new SetIpOnHostNetworkBondingReply();

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-ip-to-bonding-%s", msg.getBondingUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-ip-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
                        data.put("preIpAddress", bondingVO.getIpAddresses());

                        if (msg.getIpAddress() != null) {
                            Integer prefix = NetworkUtils.getPrefixLengthFromNetmask(msg.getNetmask());
                            bondingVO.setIpAddresses(msg.getIpAddress() + '/' + prefix);
                        } else {
                            bondingVO.setIpAddresses(null);
                        }
                        dbf.updateAndRefresh(bondingVO);

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        String preIpAddress = (String) data.get("preIpAddress");
                        SQL.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, msg.getBondingUuid())
                                .set(HostNetworkBondingVO_.ipAddresses, preIpAddress)
                                .update();

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceCmd();
                        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
                        String preIpAddress = (String) data.get("preIpAddress");
                        if (preIpAddress != null) {
                            List<String> ipAddresses = Arrays.asList(preIpAddress.split(","));
                            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                            cmd.setOldIpAddress(ipNetmaskArray[0]);
                            cmd.setOldNetmask(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])));
                        }
                        cmd.setInterfaceName(bondingVO.getBondingName());
                        cmd.setIpAddress(msg.getIpAddress());
                        cmd.setNetmask(msg.getNetmask());

                        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                        kmsg.setHostUuid(msg.getHostUuid());
                        kmsg.setCommand(cmd);
                        kmsg.setPath(MevocoKVMConstant.SET_IP_ON_HOST_NETWORK_INTERFACE);
                        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(kmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply mreply) {
                                if (!mreply.isSuccess()) {
                                    trigger.fail(mreply.getError());
                                    return;
                                } else {
                                    KVMHostAsyncHttpCallReply r = mreply.castReply();
                                    MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetIpOnHostNetworkInterfaceRsp.class);
                                    if (!rsp.isSuccess()) {
                                        trigger.fail(operr("failed to update bonding ip, because %s", rsp.getError()));
                                        return;
                                    }
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
                        reply.setInventory(HostNetworkBondingInventory.valueOf(bondingVO));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }

        }).start();
    }


    private void handle(APIGetHostResourceAllocationMsg msg) {
        APIGetHostResourceAllocationEvent evt = new APIGetHostResourceAllocationEvent(msg.getId());

        AllocateHostComputeResourceMsg kMsg = new AllocateHostComputeResourceMsg();
        kMsg.setHostUuid(msg.getHostUuid());
        kMsg.setScene(msg.getScene());
        kMsg.setVcpu(msg.getVcpu());
        kMsg.setStrategy(msg.getStrategy());
        kMsg.setMemSize(msg.getMemSize());

        bus.makeTargetServiceIdByResourceUuid(kMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kMsg, new CloudBusCallBack(kMsg) {

            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply.getError());
                } else {
                    AllocateHostComputeResourceReply rpy = (AllocateHostComputeResourceReply) reply;
                    evt.setName(rpy.getHostName());
                    evt.setUuid(rpy.getHostUuid());
                    evt.setvCPUPin(rpy.getPins());
                }
                bus.publish(evt);
            }
        });
    }

    private void allocateHostComputeResource(AllocateHostComputeResourceMsg msg, NoErrorCompletion completion) {
        AllocateHostComputeResourceReply reply = new AllocateHostComputeResourceReply();

        HostVO host = dbf.findByUuid(msg.getUuid(), HostVO.class);

        GetHostNumaTopologyMsg kmsg = new GetHostNumaTopologyMsg();
        kmsg.setHostUuid(msg.getUuid());

        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, kmsg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(kmsg) {
            @Override
            public void run(MessageReply kreply) {
                if (!kreply.isSuccess()) {
                    reply.setError(kreply.getError());
                } else {
                    GetHostNumaTopologyReply rpy = (GetHostNumaTopologyReply) kreply;
                    if (rpy.getNuma() == null) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, "Temporarily unable to obtain NUMA info on Host[%s], please retry later.", host.getUuid());
                        reply.setError(err);
                        bus.reply(msg, reply);
                        completion.done();
                        return;
                    }

                    HostResourceAllocationStrategy strategyObj;
                    try {
                        strategyObj = HostResourceAllocationStrategyFactory.getInstance(StringUtils.capitalize(msg.getStrategy()));
                    } catch (ClassNotFoundException e) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, "Not found strategy[%s] that you request.", msg.getStrategy());
                        reply.setError(err);
                        bus.reply(msg, reply);
                        completion.done();
                        return;
                    }
                    strategyObj.setHostUuid(msg.getHostUuid());
                    strategyObj.setNumaNodes(rpy.getNuma());
                    strategyObj.setScene(msg.getScene());
                    strategyObj.prepareAllocate();
                    boolean cyclicDistribution = true;
                    if (HostResourceAllocationStrategyConstant.PERFORMANCE_SCENE.equals(msg.getScene())) {
                        cyclicDistribution = false;
                    }
                    strategyObj.allocate(msg.getVcpu(), msg.getMemSize(), cyclicDistribution);
                    List<String> pCPUs = strategyObj.getNewAllocatedCPUs();
                    if (pCPUs.isEmpty()) {
                        ErrorCode err = Platform.err(SysErrors.OPERATION_ERROR, "Not enough resource on Host[%s].", host.getUuid());
                        reply.setError(err);
                        bus.reply(msg, reply);
                        completion.done();
                        return;
                    }

                    List<Map<String, String>> pins = new ArrayList<>();
                    int vCPU = 0;
                    for (String pCPU : pCPUs) {
                        pins.add(ImmutableMap.of("vCPU", String.valueOf(vCPU), "pCPU", pCPU));
                        vCPU += 1;
                    }

                    reply.setPins(pins);
                    reply.setHostName(host.getName());
                    reply.setHostUuid(msg.getUuid());
                }
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(APIGetHostNUMATopologyMsg msg) {
        APIGetHostNUMATopologyEvent evt = new APIGetHostNUMATopologyEvent(msg.getId());

        HostVO host = dbf.findByUuid(msg.getUuid(), HostVO.class);

        GetHostNumaTopologyMsg kmsg = new GetHostNumaTopologyMsg();
        kmsg.setHostUuid(msg.getUuid());

        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, kmsg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(kmsg) {
            @Override
            public void run(MessageReply kreply) {
                if (!kreply.isSuccess()) {
                    evt.setError(kreply.getError());
                } else {
                    GetHostNumaTopologyReply rpy = (GetHostNumaTopologyReply) kreply;
                    evt.setTopology(dealNumaTopologyWithVms(msg.getHostUuid(), rpy.getNuma()));
                    evt.setUuid(msg.getHostUuid());
                    evt.setName(host.getName());
                }
                bus.publish(evt);
            }
        });
    }

    private Map<String, HostNUMANode> dealNumaTopologyWithVms(String hostUuid, Map<String, HostNUMANode> nodes) {
        Map<String, List<String>> pNodeIDMapVmID = new HashMap<>();

        SimpleQuery<VmInstanceVO> vmIDsQuery = dbf.createQuery(VmInstanceVO.class);
        vmIDsQuery.select(VmInstanceVO_.uuid);
        vmIDsQuery.add(VmInstanceVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid);
        List<String> ids = vmIDsQuery.listValue();
        if (ids.isEmpty()) {
            return nodes;
        }

        SimpleQuery<VmInstanceNumaNodeVO> vmQuery = dbf.createQuery(VmInstanceNumaNodeVO.class);
        vmQuery.add(VmInstanceNumaNodeVO_.vmUuid, SimpleQuery.Op.IN, ids);
        List<VmInstanceNumaNodeVO> vNodes = vmQuery.list();
        for (VmInstanceNumaNodeVO vNode : vNodes) {
            String pNodeID = String.valueOf(vNode.getpNodeID());
            if (pNodeIDMapVmID.containsKey(pNodeID)) {
                pNodeIDMapVmID.get(pNodeID).add(vNode.getVmUuid());
            } else {
                pNodeIDMapVmID.put(pNodeID, Stream.of(vNode.getVmUuid()).collect(Collectors.toList()));
            }
        }

        for (String pNodeID : pNodeIDMapVmID.keySet()) {
            if (nodes.containsKey(pNodeID)) {
                nodes.get(pNodeID).setVMsUuid(pNodeIDMapVmID.get(pNodeID));
            }
        }
        return nodes;
    }

    private void handle(APIChangeHostPasswordMsg msg) {
        APIChangeHostPasswordEvent event = new APIChangeHostPasswordEvent(msg.getId());

        ChangeHostPasswordMsg msg1 = new ChangeHostPasswordMsg();
        msg1.setHostUuid(msg.getHostUuid());
        msg1.setPassword(msg.getPassword());
        bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg1, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply1) {
                if (!reply1.isSuccess()) {
                    event.setError(reply1.getError());
                } else {
                    KVMHostVO hostVO = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, msg.getHostUuid()).find();
                    hostVO.setPassword(msg.getPassword());
                    dbf.update(hostVO);

                    for (KVMChangeHostPasswordExtensionPoint ext : pluginRgty.getExtensionList(KVMChangeHostPasswordExtensionPoint.class)) {
                        ext.afterChangeHostPassword(hostVO);
                    }
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetClusterHostNetworkFactsMsg msg) {
        List<String> clusterHostUuids = Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.clusterUuid, msg.getClusterUuid()).eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled).limit(5).listValues();
        List<GetHostNetworkFactsMsg> gmsgs = new ArrayList<>();
        clusterHostUuids.forEach(hostUuid -> {
            GetHostNetworkFactsMsg gmsg = new GetHostNetworkFactsMsg();
            gmsg.setHostUuid(hostUuid);
            bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, hostUuid);
            gmsgs.add(gmsg);
        });
        if (gmsgs.isEmpty()) {
            APIGetClusterHostNetworkFactsReply emptyReply = new APIGetClusterHostNetworkFactsReply();
            bus.reply(msg, emptyReply);
            return;
        }
        Map<String, MessageReply> replies = Collections.synchronizedMap(new HashMap<>(gmsgs.size()));
        new While<>(gmsgs).step((gmsg, compl) -> bus.send(gmsg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                replies.put(gmsg.getId(), reply);
                compl.done();
            }
        }), 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                HashMap<String, Integer> nicMap = new HashMap<>();
                HashMap<String, Integer> bondMap = new HashMap<>();
                List<HostNetworkInterfaceInventory> nicList = new ArrayList<>();
                List<HostNetworkBondingInventory> bondList = new ArrayList<>();
                APIGetClusterHostNetworkFactsReply reply = new APIGetClusterHostNetworkFactsReply();
                gmsgs.forEach(gmsg -> {
                    GetHostNetworkFactsReply rsp = replies.get(gmsg.getId()).castReply();
                    if (!rsp.isSuccess()) {
                        reply.setError(rsp.getError());
                        bus.reply(msg, reply);
                        return;
                    }
                    rsp.getNics().forEach(nic -> {
                        if (nicMap.containsKey(nic.getInterfaceName())) {
                            int num = nicMap.get(nic.getInterfaceName());
                            if (num + 1 == gmsgs.size()) {
                                nicList.add(nic);
                            }
                            nicMap.put(nic.getInterfaceName(), num + 1);
                        } else {
                            nicMap.put(nic.getInterfaceName(), 1);
                            if (gmsgs.size() == 1) {
                                nicList.add(nic);
                            }
                        }
                    });
                    rsp.getBondings().forEach(bond -> {
                        if (bondMap.containsKey(bond.getBondingName())) {
                            int num = bondMap.get(bond.getBondingName());
                            if (num + 1 == gmsgs.size()) {
                                bondList.add(bond);
                            }
                            bondMap.put(bond.getBondingName(), num + 1);
                        } else {
                            bondMap.put(bond.getBondingName(), 1);
                            if (gmsgs.size() == 1) {
                                bondList.add(bond);
                            }
                        }
                    });
                });
                //filter and sort list
                bondList.forEach(bond -> {
                    bond.getSlaves().forEach(slave -> {
                        nicList.removeIf(nic -> nic.getInterfaceName().equals(slave.getInterfaceName()));
                    });
                });
                nicList.sort(Comparator.comparing(HostNetworkInterfaceInventory::getInterfaceName));
                bondList.sort(Comparator.comparing(HostNetworkBondingInventory::getBondingName));

                //pageable list
                int limit = msg.getLimit();
                int start = msg.getStart();
                int bondLimit = Math.min(bondList.size(), start + limit);
                int nicLimit = Math.min(nicList.size(), start + limit);
                if (start >= nicList.size()) {
                    reply.setNics(new ArrayList<>());
                } else {
                    reply.setNics(nicList.subList(start, nicLimit));
                }
                if (start >= bondList.size()) {
                    reply.setBondings(new ArrayList<>());
                } else {
                    reply.setBondings(bondList.subList(start, bondLimit));
                }
                bus.reply(msg, reply);
            }
        });
    }

    // get host network facts from agent and sync with database
    private void handle(APIGetHostNetworkFactsMsg msg) {
        APIGetHostNetworkFactsReply reply = new APIGetHostNetworkFactsReply();
        GetHostNetworkFactsMsg msg1 = new GetHostNetworkFactsMsg();
        msg1.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg1, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply1) {
                if (!reply1.isSuccess()) {
                    reply.setError(reply1.getError());
                } else {
                    GetHostNetworkFactsReply rsp = reply1.castReply();
                    reply.setBondings(rsp.getBondings());
                    reply.setNics(rsp.getNics());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetCandidateNetworkBondingsMsg msg) {
        APIGetCandidateNetworkBondingsReply reply = new APIGetCandidateNetworkBondingsReply();

        List<HostNetworkBondingVO> candidates = Q.New(HostNetworkBondingVO.class)
                .in(HostNetworkBondingVO_.hostUuid, msg.getHostUuids())
                .list();

        for (HostNetworkCandidateFilterExtensionPoint ext : pluginRgty.getExtensionList(HostNetworkCandidateFilterExtensionPoint.class)) {
            candidates = ext.filterBondingCandidates(candidates, msg.getHostUuids());
        }

        Map<String, List<String>> map = new HashMap<>();

        candidates.forEach(vo -> {
            String key = vo.getBondingName() + vo.getMode() + vo.getXmitHashPolicy();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(vo.getUuid());
        });

        List<String> bondingUuids = new ArrayList<>();
        map.values().forEach(uuids -> {
            if (uuids.size() == msg.getHostUuids().size()) {
                bondingUuids.addAll(uuids);
            }
        });

        if (bondingUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
        } else {
            reply.setInventories(HostNetworkBondingInventory.valueOf(
                    Q.New(HostNetworkBondingVO.class).in(HostNetworkBondingVO_.uuid, bondingUuids).list()));
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateNetworkInterfacesMsg msg) {
        APIGetCandidateNetworkInterfacesReply reply = new APIGetCandidateNetworkInterfacesReply();
        List<HostNetworkInterfaceVO> candidates = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.interfaceType, NetworkInterfaceType.noMaster.toString())
                .isNull(HostNetworkInterfaceVO_.bondingUuid)
                .in(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuids())
                .list();

        for (HostNetworkCandidateFilterExtensionPoint ext : pluginRgty.getExtensionList(HostNetworkCandidateFilterExtensionPoint.class)) {
            candidates = ext.filterInterfaceCandidates(candidates, msg.getHostUuids());
        }

        List<String> interfaceNames = new ArrayList<>();

        // msg.getInterfaceType is not used in ZSV
        if (msg.isIntersecting()) {
            Map<String, List<String>> map = new HashMap<>();
            String separator = "/";
            candidates.forEach(vo -> {
                String key = vo.getInterfaceName() + separator + vo.getSpeed().toString();
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(vo.getUuid());

            });

            List<String> interfaceUuids = new ArrayList<>();
            map.values().forEach(uuids -> {
                if (uuids.size() == msg.getHostUuids().size()) {
                    interfaceUuids.addAll(uuids);
                }
            });

            if (interfaceUuids.isEmpty()) {
                reply.setSlaveNames(new ArrayList<>());
                reply.setCandidateNics(new ArrayList<>());
                bus.reply(msg, reply);
                return;
            }

            candidates = candidates.stream().filter(i -> interfaceUuids.contains(i.getUuid())).collect(Collectors.toList());
            interfaceNames.addAll(candidates.stream().map(HostNetworkInterfaceVO::getInterfaceName).distinct().collect(Collectors.toList()));
        }

        List<HostVO> hostVOs = Q.New(HostVO.class).in(HostVO_.uuid, msg.getHostUuids()).list();
        for (HostVO hostVO : hostVOs) {
            List<HostNetworkInterfaceVO> interfaces = candidates.stream().filter(vo ->
                    vo.getHostUuid().equals(hostVO.getUuid())).collect(Collectors.toList());

            for (HostNetworkInterfaceVO i : interfaces){
                if (i.getIpAddresses() == null) {
                    continue;
                }

                List<String> ipAddresses = Arrays.asList(i.getIpAddresses().split(","));
                String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                if (ipNetmaskArray[0].equals(hostVO.getManagementIp()) || ipNetmaskArray[0].equals(i.getCallBackIp())) {
                    interfaceNames.remove(i.getInterfaceName());
                    candidates.remove(i);
                }
            }
        }

        if (!msg.isIntersecting()) {
            reply.setCandidateNics(HostNetworkInterfaceInventory.valueOf(candidates));
        } else {
            reply.setCandidateNics(HostNetworkInterfaceInventory.valueOf(candidates.stream().filter(i ->
                    interfaceNames.contains(i.getInterfaceName())).collect(Collectors.toList())));
        }

        reply.setSlaveNames(interfaceNames);
        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateInterfaceVlanIdsMsg msg) {
        Map<String, List<String>> hostInterfaceMap = new ConcurrentHashMap<>();
        Map<String, List<Integer>> hostVlanIdMap = new ConcurrentHashMap<>();

        HostNetworkBondingVO bondingVO;
        HostNetworkInterfaceVO interfaceVO;
        List<String> interfaceUuids = msg.getInterfaceUuids();
        for (String interfaceUuid : interfaceUuids) {
            bondingVO = dbf.findByUuid(interfaceUuid, HostNetworkBondingVO.class);
            interfaceVO = dbf.findByUuid(interfaceUuid, HostNetworkInterfaceVO.class);
            if (bondingVO != null) {
                hostInterfaceMap.computeIfAbsent(bondingVO.getHostUuid(), k -> new ArrayList<>())
                        .add(bondingVO.getBondingName());
            }
            if (interfaceVO != null) {
                hostInterfaceMap.computeIfAbsent(interfaceVO.getHostUuid(), k -> new ArrayList<>())
                        .add(interfaceVO.getInterfaceName());
            }
        }

        if (hostInterfaceMap.isEmpty()) {
            APIGetCandidateInterfaceVlanIdsReply emptyReply = new APIGetCandidateInterfaceVlanIdsReply();
            bus.reply(msg, emptyReply);
            return;
        }

        List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<>());
        new While<>(hostInterfaceMap.entrySet()).step((entry, whileCompletion) -> {
            getCandidateInterfaceVlanIds(entry.getKey(), entry.getValue(), new ReturnValueCompletion<List<Integer>>(msg) {
                @Override
                public void success(List<Integer> returnValue) {
                    hostVlanIdMap.put(entry.getKey(), returnValue);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errors.add(errorCode);
                    whileCompletion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<Integer> vlanIds = new ArrayList<>();
                APIGetCandidateInterfaceVlanIdsReply reply = new APIGetCandidateInterfaceVlanIdsReply();
                if (!errors.isEmpty()) {
                    reply.setError(errors.get(0));
                    bus.reply(msg, reply);
                    return;
                }
                hostVlanIdMap.values().stream()
                        .filter(Objects::nonNull)
                        .forEach(value -> {
                            if (vlanIds.isEmpty()) {
                                vlanIds.addAll(value);
                            } else {
                                vlanIds.retainAll(value);
                            }
                        });
                reply.setVlanIds(vlanIds);
                bus.reply(msg, reply);
            }
        });
    }

    private void getCandidateInterfaceVlanIds(String hostUuid, List<String> interfaceNames, ReturnValueCompletion<List<Integer>> completion) {
        final MevocoKVMAgentCommands.GetInterfaceVlanCmd cmd = new MevocoKVMAgentCommands.GetInterfaceVlanCmd();
        cmd.setInterfaceNames(interfaceNames);
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.GET_INTERFACE_VLAN);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to get interface vlanIds of host[uuid:%s] : %s",
                            hostUuid, reply.getError()));
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    MevocoKVMAgentCommands.GetInterfaceVlanRsp rsp = r.toResponse(MevocoKVMAgentCommands.GetInterfaceVlanRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to get interface vlanIds of host[uuid:%s] : %s",
                                hostUuid, rsp.getError()));
                    } else {
                        completion.success(rsp.getVlanIds());
                        logger.info(String.format("successfully get interface vlan ids for network interface on kvm host[uuid: %s]", hostUuid));
                    }
                }
            }
        });
    }

    private void handle(APILocateHostNetworkInterfaceMsg msg) {
        APILocateHostNetworkInterfaceEvent event = new APILocateHostNetworkInterfaceEvent(msg.getId());

        LocateHostNetworkInterfaceMsg msg1 = new LocateHostNetworkInterfaceMsg();
        msg1.setHostUuid(msg.getHostUuid());
        msg1.setNetworkInterfaceName(msg.getNetworkInterfaceName());
        msg1.setInterval(msg.getInterval());
        bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg1, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetHostPhysicalMemoryFactsMsg msg) {
        APIGetHostPhysicalMemoryFactsReply reply = new APIGetHostPhysicalMemoryFactsReply();

        GetHostPhysicalMemoryFactsMsg msg1 = new GetHostPhysicalMemoryFactsMsg();
        msg1.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg1, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply1) {
                if (!reply1.isSuccess()) {
                    reply.setError(reply1.getError());
                } else {
                    GetHostPhysicalMemoryFactsReply rsp = reply1.castReply();
                    reply.setInventories(rsp.getInventories());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIPowerOffHostMsg msg) {
        APIPowerOffHostEvent event = new APIPowerOffHostEvent(msg.getId());

        List<String> hostUuids = new ArrayList<>(msg.getHostUuids());
        List<PowerOffHardwareMsg> pmsgs = new ArrayList<>();
        pluginRgty.getExtensionList(PowerOffHostMessageBuilder.class).forEach(it -> {
            List<PowerOffHardwareMsg> msgs = it.buildMsg(hostUuids);
            List<String> buildHostUuids = msgs.stream().map(PowerOffHardwareMsg::getUuids).flatMap(Collection::stream).collect(Collectors.toList());
            hostUuids.removeAll(buildHostUuids);
            pmsgs.addAll(msgs);
        });

        List<HostInventory> hosts = hostUuids.isEmpty() ? Collections.emptyList() :
                HostInventory.valueOf(dbf.listByPrimaryKeys(hostUuids, HostVO.class));
        hosts.forEach(it -> {
            PowerOffHostMsg pmsg = new PowerOffHostMsg();
            pmsg.setHost(it);
            bus.makeLocalServiceId(pmsg, HostConstant.SERVICE_ID);
            pmsgs.add(pmsg);
        });

        PowerOffHardwareExecutor.validate(pmsgs, msg.getHostUuids());
        PowerOffHardwareExecutor.New(msg.isWaitTaskCompleted(), msg.getMaxWaitTime())
                .powerOffHardware(pmsgs, new ReturnValueCompletion<List<PowerOffHardwareResult>>(event) {
                    @Override
                    public void success(List<PowerOffHardwareResult> results) {
                        event.setResults(results);
                        bus.publish(event);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                    }
                });
    }

    private void handle(APIIdentifyHostMsg msg) {
        APIIdentifyHostEvent event = new APIIdentifyHostEvent(msg.getId());
        IdentifyHostMsg imsg = new IdentifyHostMsg();
        imsg.setHostUuid(msg.getUuid());
        imsg.setInterval(msg.getInterval());
        bus.makeTargetServiceIdByResourceUuid(imsg, HostConstant.SERVICE_ID, msg.getUuid());
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APICheckHostConfigFileMsg msg) {
        HypervisorMessageFactory factory = getHypervisorMessageFactory(HypervisorType.valueOf(msg.getHypervisorType()));
        List<AddHostMsg> amsgs = factory.buildMessageFromFile(msg.getHostInfo(), ParamValidator::validate);
        checkAddHostMsg(amsgs, msg.getHypervisorType());
        APICheckHostConfigFileReply reply = new APICheckHostConfigFileReply();
        bus.reply(msg, reply);
    }

    private void handle(APIAddHostFromConfigFileMsg msg) {
        APIAddHostFromConfigFileEvent event = new APIAddHostFromConfigFileEvent(msg.getId());
        addHostFromFile(msg, new ReturnValueCompletion<AddHostsFromFileResult>(event) {
            @Override
            public void success(AddHostsFromFileResult result) {
                event.setResults(result.getResults());
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void addHostFromFile(AddHostFromConfigFileMessage msg, ReturnValueCompletion<AddHostsFromFileResult> completion) {
        AddHostsFromFileResult result = new AddHostsFromFileResult();
        List<AddHostFromFileResult> results = Collections.synchronizedList(new ArrayList<>());

        HypervisorMessageFactory factory = getHypervisorMessageFactory(HypervisorType.valueOf(msg.getHypervisorType()));
        List<AddHostMsg> amsgs = factory.buildMessageFromFile(msg.getHostInfo(), null);

        new While<>(amsgs)
                .enableProgressReport("add-hosts")
                .step((amsg, coml) -> {
            if (jobCanceled()) {
                result.setCanceled(true);
                logger.debug("add host job has been canceled, abort rest hosts");
                coml.allDone();
            }

            ErrorCode err = validate(amsg);
            if (err != null) {
                results.add(new AddHostFromFileResult(amsg.getManagementIp(), err));
                coml.done();
                return;
            }

            bus.makeTargetServiceIdByResourceUuid(amsg, HostConstant.SERVICE_ID, amsg.getClusterUuid());
            bus.send(amsg, new CloudBusCallBack(coml) {
                @Override
                public void run(MessageReply reply) {
                    results.add(new AddHostFromFileResult(amsg.getManagementIp(), reply.getError()));
                    coml.done();
                }
            });
        }, 30).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                results.sort(Comparator.comparing(AddHostFromFileResult::getIp));
                result.setResults(results);
                completion.success(result);
            }
        });
    }


    private ErrorCode validate(Verifiable msg) {
        try {
            ParamValidator.validate(msg);
        } catch (Exception e) {
            return argerr(e.getMessage());
        }
        return null;
    }

    private void checkAddHostMsg(List<AddHostMsg> amsgs, String hypervisorType) {
        List<String> clusterUuids = amsgs.stream().map(AddHostMsg::getClusterUuid).distinct().collect(Collectors.toList());
        List<String> exsitClusterUuid = Q.New(ClusterVO.class).select(ClusterVO_.uuid).eq(ClusterVO_.hypervisorType, hypervisorType).listValues();
        clusterUuids.removeAll(exsitClusterUuid);
        if (!clusterUuids.isEmpty()) {
            throw new OperationFailureException(operr("cluster[uuids:%s, hypervisorType:%s] are not exist!",
                    clusterUuids, hypervisorType));
        }
    }

    private void handle(final ChangeHugePageStateMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        if (hostVO == null) {
            throw new OperationFailureException(operr("host[uuid:%s] can not find", msg.getHostUuid()));
        }

        if (Boolean.valueOf(msg.getValue())) {
            sendEnableHugePageMsg(msg, hostVO);
        } else {
            sendDisableHugePageMsg(msg);
        }
    }

    private void handle(final ChangeZeroCopyStateMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        if (hostVO == null) {
            throw new OperationFailureException(operr("host[uuid:%s] can not find", msg.getHostUuid()));
        }

        if (Boolean.valueOf(msg.getValue())) {
            sendEnableZeroCopyMsg(msg);
        } else {
            sendDisableZeroCopyMsg(msg);
        }
    }

    private void handle(UpdateHostOvsPmdPinningMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        if (hostVO == null) {
            throw new OperationFailureException(operr("host[uuid:%s] can not find", msg.getHostUuid()));
        }

        updateHostOvsCpuPinning(msg, hostVO);
    }

    private void handle(ChangeHostNetworkInterfaceStateMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                //update interface
                HostNetworkInterfaceVO interfaceVO = Q.New(HostNetworkInterfaceVO.class)
                        .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                        .eq(HostNetworkInterfaceVO_.interfaceName, msg.getInterfaceName()).find();
                interfaceVO.setCarrierActive(msg.getInterfaceStatus().equals(HostNetworkBondingConstant.MII_STATUS_UP));
                dbf.updateAndRefresh(interfaceVO);

                //update bonding
                HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, interfaceVO.getBondingUuid()).find();
                Set<HostNetworkInterfaceVO> interfaceVOs = bondingVO.getSlaves();
                if (HostNetworkBondingConstant.MII_STATUS_DOWN.equals(msg.getInterfaceStatus())) {
                    if (interfaceVOs.stream().noneMatch(HostNetworkInterfaceVO::isCarrierActive)) {
                        bondingVO.setMiiStatus(HostNetworkBondingConstant.MII_STATUS_DOWN);
                        dbf.update(bondingVO);
                    }
                } else {
                    bondingVO.setMiiStatus(HostNetworkBondingConstant.MII_STATUS_UP);
                    dbf.update(bondingVO);
                }
            }
        }.execute();
        bus.reply(msg, new ChangeHostNetworkInterfaceStateReply());
    }

    private void sendEnableZeroCopyMsg(ChangeZeroCopyStateMsg msg) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        MevocoKVMAgentCommands.EnableZeroCopyCmd cmd = new MevocoKVMAgentCommands.EnableZeroCopyCmd();
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.ENABLE_ZERO_COPY);
        kmsg.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    reply.setError(reply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                MevocoKVMAgentCommands.EnableZeroCopyResponse rsp = r.toResponse(MevocoKVMAgentCommands.EnableZeroCopyResponse.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    logger.info(String.format("Host:%s has changed zero copy state to %s", msg.getHostUuid(), msg.getValue()));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void sendDisableZeroCopyMsg(ChangeZeroCopyStateMsg msg) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        MevocoKVMAgentCommands.DisableZeroCopyCmd cmd = new MevocoKVMAgentCommands.DisableZeroCopyCmd();
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.DISABLE_ZERO_COPY);
        kmsg.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    reply.setError(reply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                MevocoKVMAgentCommands.DisableZeroCopyResponse rsp = r.toResponse(MevocoKVMAgentCommands.DisableZeroCopyResponse.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    logger.info(String.format("Host:%s has changed zero copystate to %s", msg.getHostUuid(), msg.getValue()));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void sendEnableHugePageMsg(ChangeHugePageStateMsg msg, HostVO hostVO) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        MevocoKVMAgentCommands.EnableHugePageCmd cmd = new MevocoKVMAgentCommands.EnableHugePageCmd();
        cmd.pageSize = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.HUGEPAGE_SIZE, hostVO.getClusterUuid(), Long.class);
        String reserveMem = rcf.getResourceConfigValue(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY, hostVO.getUuid(), String.class);
        cmd.reserveSize = SizeUtils.sizeStringToBytes(reserveMem);
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.ENABLE_HUGEPAGE);
        kmsg.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    reply.setError(reply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                MevocoKVMAgentCommands.EnableHugePageResponse rsp = r.toResponse(MevocoKVMAgentCommands.EnableHugePageResponse.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    logger.info(String.format("Host:%s has changed huge page state to %s", msg.getHostUuid(), msg.getValue()));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void sendDisableHugePageMsg(ChangeHugePageStateMsg msg) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        MevocoKVMAgentCommands.DisableHugePageCmd cmd = new MevocoKVMAgentCommands.DisableHugePageCmd();
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.DISABLE_HUGEPAGE);
        kmsg.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    reply.setError(reply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                MevocoKVMAgentCommands.DisableHugePageResponse rsp = r.toResponse(MevocoKVMAgentCommands.DisableHugePageResponse.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    logger.info(String.format("Host:%s has changed huge page state to %s", msg.getHostUuid(), msg.getValue()));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private String pmdCpuPinningStringToHex(String ovsCpuPinning, int hostCpuNum) {
        if (ovsCpuPinning.isEmpty()) {
            return null;
        }

        String pmdCpuMask = "";
        String cpuPins[] = ovsCpuPinning.split(";");
        int[] cpuNodes = Arrays.stream(cpuPins).mapToInt(Integer::parseInt).toArray();
        int[] hexs = new int[hostCpuNum / Integer.SIZE + 1];

        for (int i : cpuNodes) {
            i = i % hostCpuNum;
            int index = i / Integer.SIZE;
            i = i % Integer.SIZE;
            hexs[index] |= 1 << i;
        }

        for (int h : hexs) {
            pmdCpuMask = Integer.toHexString(h) + pmdCpuMask;
        }
        return "0x" + pmdCpuMask;
    }

    private void updateHostOvsCpuPinning(UpdateHostOvsPmdPinningMsg msg, HostVO hostVO) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        MevocoKVMAgentCommands.UpdateOvsCpuPinningCmd cmd = new MevocoKVMAgentCommands.UpdateOvsCpuPinningCmd();

        int hostCpuNum = Q.New(HostCapacityVO.class).select(HostCapacityVO_.cpuNum)
                .eq(HostCapacityVO_.uuid, hostVO.getUuid()).findValue();

        cmd.setOvsCpuPinning(pmdCpuPinningStringToHex(msg.getPmdCpuPinning(), hostCpuNum));
        kmsg.setHostUuid(hostVO.getUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.UPDATE_HOST_OVS_CPU_PINNING);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    bus.reply(msg, reply);
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                MevocoKVMAgentCommands.UpdateOvsCpuPinningResponse rsp = rly.toResponse(MevocoKVMAgentCommands.UpdateOvsCpuPinningResponse.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    logger.debug(String.format("successfully update ovsCpuPinning on host[uuid:%s]", msg.getHostUuid()));
                }
                bus.reply(msg, reply);
            }
        });

    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName(String.format("get-host-facts-after-host-%s-connected", host.getUuid()));
                chain.then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger1, Map data) {
                        GetHostNetworkFactsMsg gmsg = new GetHostNetworkFactsMsg();
                        gmsg.setHostUuid(host.getUuid());
                        gmsg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger1) {
                            @Override
                            public void run(MessageReply reply) {
                                ErrorCode error = null;
                                if (!reply.isSuccess()) {
                                    error = reply.getError();
                                } else {
                                    GetHostNetworkFactsReply rsp = reply.castReply();
                                    if (!rsp.isSuccess()) {
                                        error = rsp.getError();
                                    }
                                }

                                if (error != null) {
                                    logger.warn(String.format("failed to get network facts after host[uuid:%s] connected", host.getUuid()));
                                }
                                // trigger next anyway
                                trigger1.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger1, Map data) {
                        GetHostPhysicalMemoryFactsMsg gmsg = new GetHostPhysicalMemoryFactsMsg();
                        gmsg.setHostUuid(host.getUuid());
                        gmsg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger1) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to get physical memory facts after host[uuid:%s] connected, error: %s", host.getUuid(), reply.getError().getDetails()));
                                }

                                // trigger next anyway
                                trigger1.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "get-host-physical-cpu-facts-after-host-connected";

                    @Override
                    public void run(FlowTrigger trigger1, Map data) {
                        GetHostPhysicalCpuFactsMsg gmsg = new GetHostPhysicalCpuFactsMsg();
                        gmsg.setHostUuid(host.getUuid());
                        gmsg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger1) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to get physical cpu facts after host[uuid:%s] connected, error: %s", host.getUuid(), reply.getError().getDetails()));
                                }

                                trigger1.next();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.next();
                    }
                }).start();
            }
        };
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(new HashMap<>());
        chain.setName(String.format("sync-interface-data-after-host-%s-connected", host.getUuid()));

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "initial-physicalNic-monitor-data ";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        //initial physicalNic monitor data to connected host
                        SetHostPhysicalNicMonitorMsg msg = new SetHostPhysicalNicMonitorMsg();
                        msg.setHostUuid(host.getUuid());
                        msg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(null) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to set PhysicalNic monitor initial at host[uuid:%s] after host connected: %s", host.getUuid(), reply.getError()));
                                } else {
                                    logger.debug(String.format("successfully to set PhysicalNic monitor initial at host[uuid:%s] after host connected", host.getUuid()));
                                }
                            }
                        });

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reset-interface-service-type-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // reset interface service type from db
                        List<HostNetworkInterfaceServiceRefVO> interfaceServiceRefVOS = Q.New(HostNetworkInterfaceServiceRefVO.class).list();
                        for (HostNetworkInterfaceServiceRefVO refVO : interfaceServiceRefVOS) {
                            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(refVO.getInterfaceUuid(), HostNetworkInterfaceVO.class);
                            if (!host.getUuid().equals(interfaceVO.getHostUuid())) {
                                continue;
                            }
                            List<HostNetworkInterfaceServiceType> serviceTypes = Q.New(HostNetworkInterfaceServiceRefVO.class)
                                    .select(HostNetworkInterfaceServiceRefVO_.serviceType)
                                    .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, refVO.getInterfaceUuid())
                                    .eq(HostNetworkInterfaceServiceRefVO_.vlanId, refVO.getVlanId())
                                    .listValues();

                            SetServiceTypeOnHostNetworkInterfaceMsg msg = new SetServiceTypeOnHostNetworkInterfaceMsg();
                            msg.setInterfaceUuid(refVO.getInterfaceUuid());
                            msg.setVlanId(refVO.getVlanId());
                            msg.setServiceType(serviceTypes.stream().map(HostNetworkInterfaceServiceType::toString).collect(Collectors.toList()));
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                            bus.send(msg, new CloudBusCallBack(msg) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("interface[%s] service type reset failed, because %s", refVO.getInterfaceUuid(), reply.getError()));
                                    } else {
                                        logger.debug(String.format("interface[%s] service type reset successfully", refVO.getInterfaceUuid()));
                                    }
                                }
                            });
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reset-bonding-service-type-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // reset bonding service type from db
                        List<HostNetworkBondingServiceRefVO> bondingServiceRefVOS = Q.New(HostNetworkBondingServiceRefVO.class).list();
                        for (HostNetworkBondingServiceRefVO refVO : bondingServiceRefVOS) {
                            HostNetworkBondingVO bondingVO = dbf.findByUuid(refVO.getBondingUuid(), HostNetworkBondingVO.class);
                            if (!Objects.equals(bondingVO.getHostUuid(), host.getUuid())) {
                                continue;
                            }
                            List<HostNetworkInterfaceServiceType> serviceTypes = Q.New(HostNetworkBondingServiceRefVO.class)
                                    .select(HostNetworkBondingServiceRefVO_.serviceType)
                                    .eq(HostNetworkBondingServiceRefVO_.bondingUuid, refVO.getBondingUuid())
                                    .eq(HostNetworkBondingServiceRefVO_.vlanId, refVO.getVlanId())
                                    .listValues();

                            SetServiceTypeOnHostNetworkBondingMsg msg = new SetServiceTypeOnHostNetworkBondingMsg();
                            msg.setBondingUuid(refVO.getBondingUuid());
                            msg.setVlanId(refVO.getVlanId());
                            msg.setServiceType(serviceTypes.stream().map(HostNetworkInterfaceServiceType::toString).collect(Collectors.toList()));
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                            bus.send(msg, new CloudBusCallBack(msg) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("bonding[%s] service type reset failed, because %s", refVO.getBondingUuid(), reply.getError()));
                                    } else {
                                        logger.debug(String.format("bonding[%s] service type reset successfully", refVO.getBondingUuid()));
                                    }
                                }
                            });
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "add-default-management-service-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // add default management service type
                        List<String> interfaceNames = new ArrayList<>();
                        List<String> l2Uuids = Q.New(L3NetworkVO.class)
                                .select(L3NetworkVO_.l2NetworkUuid)
                                .eq(L3NetworkVO_.category, L3NetworkCategory.System)
                                .listValues();
                        if (l2Uuids != null) {
                            l2Uuids.stream()
                                    .map(l2Uuid -> dbf.findByUuid(l2Uuid, L2NetworkVO.class))
                                    .forEach(l2 -> {
                                        switch (l2.getType()) {
                                            case L2NetworkConstant.VXLAN_NETWORK_TYPE:
                                                interfaceNames.add("vxlan" + l2.getVirtualNetworkId());
                                                break;
                                            case L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE:
                                                interfaceNames.add(l2.getPhysicalInterface());
                                                break;
                                            case L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE:
                                            case L2NetworkConstant.L2_VLAN_NETWORK_TYPE:
                                                interfaceNames.add(l2.getPhysicalInterface() + "." + l2.getVirtualNetworkId());
                                                break;
                                            default:
                                                break;
                                        }
                                    });
                        }

                        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, host.getUuid()).find();

                        GetHostNetworkInterfaceByIpMsg msg1 = new GetHostNetworkInterfaceByIpMsg();
                        msg1.setHostUuid(hostVO.getUuid());
                        List<HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid()).list();
                        if (interfaceVOS != null && !interfaceVOS.isEmpty()) {
                            HostNetworkInterfaceVO interfaceVO = interfaceVOS.get(0);
                            if (interfaceVO.getCallBackIp() != null && !hostVO.getManagementIp().equals(interfaceVO.getCallBackIp())) {
                                msg1.setIpAddresses(Arrays.asList(hostVO.getManagementIp(), interfaceVO.getCallBackIp()));
                            } else {
                                msg1.setIpAddresses(Collections.singletonList(hostVO.getManagementIp()));
                            }
                        }
                        bus.makeTargetServiceIdByResourceUuid(msg1, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg1, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to get nic from management ip at host[uuid:%s] after host connected: %s", host.getUuid(), reply.getError()));
                                    return;
                                } else {
                                    GetHostNetworkInterfaceByIpReply rsp = reply.castReply();
                                    if (!rsp.isSuccess()) {
                                        logger.warn(String.format("failed to get nic from management ip at host at host[uuid:%s] after host connected, because %s", host.getUuid(), rsp.getError()));
                                    } else {
                                        if (rsp.getInterfaceNames() != null) {
                                            interfaceNames.addAll(rsp.getInterfaceNames());
                                        }
                                        logger.debug(String.format("successfully to get nic from management ip at host at host[uuid:%s] after host connected", host.getUuid()));
                                    }
                                }

                                Set<String> interfaceSet = new HashSet<>(interfaceNames);
                                for (String interfaceName : interfaceSet) {
                                    if (interfaceName.contains(".")) {
                                        String[] nameVlanIdArray = interfaceName.split("\\.");
                                        changeManagementServiceTypeOnInterface(host.getUuid(), nameVlanIdArray[0], Integer.parseInt(nameVlanIdArray[1]), false, new Completion(null) {
                                            @Override
                                            public void success() {
                                                // nothing
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                logger.warn(String.format("failed to set management service type on interface[%s] with virtual id[%s] of host[%s]", nameVlanIdArray[0], Integer.parseInt(nameVlanIdArray[1]), host.getUuid()));
                                            }
                                        });
                                    } else {
                                        changeManagementServiceTypeOnInterface(host.getUuid(), interfaceName, 0, false, new Completion(null) {
                                            @Override
                                            public void success() {
                                                // nothing
                                            }

                                            @Override
                                            public void fail(ErrorCode errorCode) {
                                                logger.warn(String.format("failed to set management service type on interface[%s] with virtual id[%s] of host[%s]", interfaceName, 0, host.getUuid()));
                                            }
                                        });
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {

                    }
                });

                error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {

                    }
                });
            }
        }).start();
    }

    @Override
    public void syncManagementServiceTypeExtensionPoint(List<String> hostUuids, String interfaceName, Integer virtualNetworkId, boolean isDelete) {
        new While<>(hostUuids).step((hostUuid, wcomp) -> {
            changeManagementServiceTypeOnInterface(hostUuid, interfaceName, virtualNetworkId, isDelete, new Completion(null) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to set management service type on interface[%s] with virtual id[%s] of host[%s]", interfaceName, virtualNetworkId, hostUuid));
                    wcomp.allDone();
                }
            });
        }, 10).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
            }
        });
    }

    private void setupCanonicalEvents() {
        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_UP, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalNicStatusData d = (HostCanonicalEvents.HostPhysicalNicStatusData) data;
                ChangeHostNetworkInterfaceStateMsg msg = new ChangeHostNetworkInterfaceStateMsg();
                msg.setHostUuid(d.getHostUuid());
                msg.setFromBond(d.getFromBond());
                msg.setInterfaceName(d.getInterfaceName());
                msg.setInterfaceStatus(d.getInterfaceStatus());
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, d.getHostUuid());
                bus.send(msg);
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_DOWN, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalNicStatusData d = (HostCanonicalEvents.HostPhysicalNicStatusData) data;
                ChangeHostNetworkInterfaceStateMsg msg = new ChangeHostNetworkInterfaceStateMsg();
                msg.setHostUuid(d.getHostUuid());
                msg.setFromBond(d.getFromBond());
                msg.setInterfaceName(d.getInterfaceName());
                msg.setInterfaceStatus(d.getInterfaceStatus());
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, d.getHostUuid());
                bus.send(msg);
            }
        });
    }
}
