package org.zstack.compute.host;

import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.host.MevocoKVMAgentCommands.SetSyncVmClockTaskCmd;
import org.zstack.compute.host.MevocoKVMAgentCommands.SetSyncVmClockTaskRsp;
import org.zstack.compute.sriov.VfPciDeviceUtils;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.PowerOffHardwareResult;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.host.ChangeVmPasswordMsg;
import org.zstack.header.host.ChangeVmPasswordReply;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicManager;
import org.zstack.header.storage.primary.PrimaryStorageErrors;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmMsg;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmReply;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.*;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.pciDevice.PciDeviceType;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by mingjian.deng on 16/12/1.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MevocoHostBase implements Host {
    protected static final CLogger logger = Utils.getLogger(MevocoHostBase.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();
    private static final int HOST_NIC_PCI_NAME_MAX_LENGTH = 255;

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmVfNicManager vfMgr;

    private KVMHostContext context;
    private String id;
    private String changeVmPasswordPath;
    private String checkVmVolumesPath;
    private String setVolumeBandwidth;
    private String deleteVolumeBandwidth;
    private String getVolumeBandwidth;
    private String setNicQos;
    private String getNicQos;
    private String checkMountPath;
    private String resizeVolume;
    private String takeVolumesSnaphost;
    private String blockStreamVolume;
    private String setBridgeRouterPort;
    private String setVMEmulatorPinning;
    private String guestToolsStatePath;
    private String vmPortsConfigSyncPath;
    private String setVmHostnamePath;
    private String setVmDnsPath;
    private String vmSpecificationConfigSyncPath;
    private String applyMemoryBalloonPath;
    private String setVmIoThreadPin;
    private String getVmIoThreadPin;
    private String delVmIoThreadPin;
    private String setVmScsiController;
    private String delVmScsiController;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ChangeVmPasswordMsg) {
            handle((ChangeVmPasswordMsg) msg);
        } else if (msg instanceof CheckVmVolumesMsg) {
            handle((CheckVmVolumesMsg) msg);
        } else if (msg instanceof SetVolumeQosOnKVMHostMsg) {
            handle((SetVolumeQosOnKVMHostMsg) msg);
        } else if (msg instanceof DeleteVolumeQosOnKVMHostMsg) {
            handle((DeleteVolumeQosOnKVMHostMsg) msg);
        } else if (msg instanceof SetNicQosOnKVMHostMsg) {
            handle((SetNicQosOnKVMHostMsg) msg);
        } else if (msg instanceof GetVolumeQosOnKVMHostMsg) {
            handle((GetVolumeQosOnKVMHostMsg) msg);
        } else if (msg instanceof GetNicQosOnKVMHostMsg) {
            handle((GetNicQosOnKVMHostMsg) msg);
        } else if (msg instanceof CheckMountDomainMsg) {
            handle((CheckMountDomainMsg) msg);
        } else if (msg instanceof ResizeVolumeOnKvmMsg) {
            handle((ResizeVolumeOnKvmMsg) msg);
        } else if (msg instanceof BlockStreamVolumeMsg) {
            handle((BlockStreamVolumeMsg) msg);
        } else if (msg instanceof TakeVolumesSnapshotOnKvmMsg) {
            handle((TakeVolumesSnapshotOnKvmMsg) msg);
        } else if (msg instanceof IdentifyHostMsg) {
            handle((IdentifyHostMsg) msg);
        } else if (msg instanceof ChangeHostPasswordMsg) {
            handle((ChangeHostPasswordMsg) msg);
        } else if (msg instanceof GetHostNetworkFactsMsg) {
            handle((GetHostNetworkFactsMsg) msg);
        } else if (msg instanceof LocateHostNetworkInterfaceMsg) {
            handle((LocateHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof GetHostPhysicalMemoryFactsMsg) {
            handle((GetHostPhysicalMemoryFactsMsg) msg);
        } else if (msg instanceof GetHostPhysicalCpuFactsMsg) {
            handle((GetHostPhysicalCpuFactsMsg) msg);
        } else if (msg instanceof PowerOffHostMsg) {
            handle((PowerOffHostMsg) msg);
        } else if (msg instanceof SetBridgeRouterPortMsg) {
            handle((SetBridgeRouterPortMsg) msg);
        } else if (msg instanceof ChangeVmEmulatorPinningMsg) {
            handle( (ChangeVmEmulatorPinningMsg) msg);
        } else if (msg instanceof SetHostPhysicalNicMonitorMsg) {
            handle((SetHostPhysicalNicMonitorMsg) msg);
        } else if (msg instanceof InitQgaZWatchMonitorMsg) {
            handle((InitQgaZWatchMonitorMsg) msg);
        } else if (msg instanceof ReserveEthernetVfMsg) {
            handle((ReserveEthernetVfMsg) msg);
        } else if (msg instanceof UpdateHostClockSyncVmMsg) {
            handle((UpdateHostClockSyncVmMsg) msg);
        } else if (msg instanceof VmPortsConfigSyncOnHypervisorMsg) {
            handle((VmPortsConfigSyncOnHypervisorMsg) msg);
        } else if (msg instanceof VmSpecificationConfigSyncOnHypervisorMsg) {
            handle((VmSpecificationConfigSyncOnHypervisorMsg) msg);
        } else if (msg instanceof SetVmHostnameOnHypervisorMsg) {
            handle((SetVmHostnameOnHypervisorMsg) msg);
        } else if (msg instanceof SetVmDnsOnHypervisorMsg) {
            handle((SetVmDnsOnHypervisorMsg) msg);
        } else if (msg instanceof ApplyMemoryBalloonDecisionMsg) {
            handle((ApplyMemoryBalloonDecisionMsg) msg);
        } else if (msg instanceof StartHostPhysicalMemoryMonitorMsg) {
            handle((StartHostPhysicalMemoryMonitorMsg) msg);
        } else if (msg instanceof UpdateHostIscsiInitiatorNameMsg) {
            handle((UpdateHostIscsiInitiatorNameMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(UpdateHostIscsiInitiatorNameMsg msg) {
        UpdateHostIscsiInitiatorNameReply ureply = new UpdateHostIscsiInitiatorNameReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameCmd cmd = new MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameCmd();
                cmd.iscsiInitiatorName = msg.getIscsiInitiatorName();

                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                kmsg.setCommand(cmd);
                kmsg.setPath(MevocoKVMConstant.KVM_UPDATE_HOST_ISCSI_INITIATOR_NAME_PATH);
                kmsg.setHostUuid(msg.getHostUuid());
                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(kmsg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            ureply.setError(operr("fail to update iscsi initiator name of host[uuid:%s]", msg.getHostUuid())
                                    .withCause(reply.getError()));
                        } else {
                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameRsp rsp = r.toResponse(MevocoKVMAgentCommands.UpdateHostIscsiInitiatorNameRsp.class);
                            if (!rsp.isSuccess()) {
                                ureply.setError(operr("fail to update iscsi initiator name of host[uuid:%s]", msg.getHostUuid())
                                        .withOpaque("response.error", rsp.getError()));
                            } else {
                                SQL.New(KVMHostVO.class).eq(KVMHostVO_.uuid, msg.getHostUuid())
                                        .set(KVMHostVO_.iscsiInitiatorName, msg.getIscsiInitiatorName())
                                        .update();
                                KVMHostVO vo = dbf.findByUuid(msg.getHostUuid(), KVMHostVO.class);
                                ureply.setInventory(KVMHostInventory.valueOf(vo));
                            }
                        }
                        bus.reply(msg, ureply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-iscsi-initiator-name-of-host-%s", msg.getHostUuid());
            }
        });
    }

    private void handle(StartHostPhysicalMemoryMonitorMsg msg) {
        StartHostPhysicalMemoryMonitorReply reply = new StartHostPhysicalMemoryMonitorReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                kmsg.setPath(MevocoKVMConstant.SET_HOST_PHYSICAL_MEMORY_MONITOR);
                kmsg.setHostUuid(msg.getHostUuid());
                kmsg.setCommand(new KVMAgentCommands.HardwareMonitorCmd());
                kmsg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(kmsg, new CloudBusCallBack(kmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to set hardware monitor initial at host[uuid:%s] after host connected: %s", msg.getHostUuid(), reply.getError()));
                        } else {
                            logger.debug(String.format("successfully to set hardware monitor initial at host[uuid:%s] after host connected", msg.getHostUuid()));
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getId();
            }
        });
    }

    @Override
    public String getId() {
        return id;
    }

    protected MevocoHostBase(KVMHostContext context) {
        this.context = context;
        this.id = "Host-" + context.getInventory().getUuid();

        String baseUrl = context.getBaseUrl();
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.KVM_VM_CHANGE_PASSWORD_PATH);
        changeVmPasswordPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.KVM_VM_CHECK_VOLUME_PATH);
        checkVmVolumesPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VOLUME_BANDWIDTH);
        setVolumeBandwidth = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.DELETE_VOLUME_BANDWIDTH);
        deleteVolumeBandwidth = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.GET_VOLUME_BANDWIDTH);
        getVolumeBandwidth = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_NIC_QOS);
        setNicQos = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.GET_NIC_QOS);
        getNicQos = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.CHECK_MOUNT_DOMAIN);
        checkMountPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.RESIZE_VOLUME);
        resizeVolume = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.TAKE_VOLUMES_SNAPSHOT);
        takeVolumesSnaphost = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.BLOCK_STREAM);
        blockStreamVolume = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_BRIDGE_ROUTER_PORT);
        setBridgeRouterPort = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VM_EMULATOR_PINNING);
        setVMEmulatorPinning = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.KVM_VM_GUESTTOOLS_STATE_PATH);
        guestToolsStatePath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_PORTS_PATH);
        vmPortsConfigSyncPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.KVM_VM_CONFIG_SYNC_SPECIFICATION_PATH);
        vmSpecificationConfigSyncPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VM_HOSTNAME_PATH);
        setVmHostnamePath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VM_DNS_PATH);
        setVmDnsPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.APPLY_MEMORY_BALLOON_PATH);
        applyMemoryBalloonPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VM_IOTHREAD_PIN_PATH);
        setVmIoThreadPin = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.GET_VM_IOTHREAD_PIN_PATH);
        getVmIoThreadPin = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.DEL_VM_IOTHREAD_PIN_PATH);
        delVmIoThreadPin = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.SET_VM_SCSI_CONTROLLER_PATH);
        setVmScsiController = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(MevocoKVMConstant.DEL_VM_SCSI_CONTROLLER_PATH);
        delVmScsiController = ub.build().toString();
    }

    protected void checkStateAndStatus(String hostUuid) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        if (hostVO == null) {
            throw new OperationFailureException(operr("host[uuid:%s] can not find", hostUuid));
        }
        checkStateAndStatus(hostVO);
    }

    protected void checkStateAndStatus(HostVO host) {
        checkState(host);
        checkStatus(host);
    }

    protected void checkStatus(HostVO host) {
        if (HostStatus.Connected != host.getStatus()) {
            ErrorCode cause = err(HostErrors.HOST_IS_DISCONNECTED, "host[uuid:%s, name:%s] is in status[%s], cannot perform required operation", host.getUuid(), host.getName(), host.getStatus());
            throw err(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE, "unable to do the operation because the host is in status of Disconnected")
                    .withCause(cause)
                    .toException();
        }
    }

    protected void checkState(HostVO host) {
        if (HostState.PreMaintenance == host.getState() || HostState.Maintenance == host.getState()) {
            throw new OperationFailureException(operr("host[uuid:%s, name:%s] is in state[%s], cannot perform required operation", host.getUuid(), host.getName(), host.getState()));
        }
    }
    
    private void handle(UpdateHostClockSyncVmMsg msg) {
        final UpdateHostClockSyncVmReply reply = new UpdateHostClockSyncVmReply();
        final String hostUuid = msg.getHostUuid();

        SetSyncVmClockTaskCmd cmd = new SetSyncVmClockTaskCmd();
        cmd.setIntervalMap(msg.getVmIntervalMap());

        new KvmCommandSender(msg.getHostUuid()).send(cmd, MevocoKVMConstant.SET_SYNC_VM_CLOCK_TASK_PATH,
                wrapper -> {
                    SetSyncVmClockTaskRsp rsp = wrapper.getResponse(SetSyncVmClockTaskRsp.class);
                    return rsp.isSuccess() ? null :
                            operr(rsp.getError(), "failed to set sync clock task on host[uuid:%s]", hostUuid);
                },
                new ReturnValueCompletion<KvmResponseWrapper>(msg) {
                    @Override
                    public void success(KvmResponseWrapper ret) {
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void handle(InitQgaZWatchMonitorMsg msg){
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.INIT_ZWATCH_METRIC_MONITOR);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setNoStatusCheck(msg.isNoStatusCheck());
        kmsg.setCommand(new KVMAgentCommands.AgentCommand());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to init qga zwatch monitor, %s", reply.getError()));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SetHostPhysicalNicMonitorMsg msg) {
        MessageReply greply = new MessageReply();
        MevocoKVMAgentCommands.SetHostPhysicalNicMonitorCmd cmd = new MevocoKVMAgentCommands.SetHostPhysicalNicMonitorCmd();
        cmd.nics = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid()).list();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.SET_HOST_PHYSICAL_NIC_MONITOR);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(msg.isNoStatusCheck());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    MevocoKVMAgentCommands.SetHostPhysicalNicMonitorRsp rsp = r.toResponse(MevocoKVMAgentCommands.SetHostPhysicalNicMonitorRsp.class);
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    }
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final ReserveEthernetVfMsg msg) {
        ReserveHostPciDeviceReply reply = new ReserveHostPciDeviceReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<PciDeviceInventory> devices = new ArrayList<>();
                for (String l3Uuid : msg.getL3Uuids()) {
                    String vfParentUuid = msg.getL3PciDeviceMap().get(l3Uuid);
                    PciDeviceInventory device = vfPciDeviceUtils.reserveVfDevice
                            (msg.getHostUuid(), msg.getVmUuid(), l3Uuid, vfParentUuid, msg.isReleaseOldVf(), msg.getStatus());
                    if (device != null) {
                        logger.debug(String.format("reserve pci device [%s] for vm [uuid:%s] l3 network[uuid:%s]",
                                JSONObjectUtil.toJsonString(device), msg.getVmUuid(), l3Uuid));
                        devices.add(device);
                    } else {
                        logger.debug(String.format("failed to allocate pci device for l3[uuid:%s] on host[uuid:%s]", l3Uuid, msg.getHostUuid()));
                        break;
                    }
                }

                /* reserve failed */
                if (devices.size() < msg.getL3Uuids().size()) {
                    for (PciDeviceInventory pci : devices) {
                        vfPciDeviceUtils.releaseVfDevice(pci);
                    }
                    reply.setError(operr("failed to allocate pci device on host[uuid:%s]," +
                            " because there are not enough pci devices available", msg.getHostUuid()));
                } else {
                    reply.setPciDevices(devices);
                }
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("reserve-pci-device-on-host-%s", msg.getHostUuid());
            }
        });
    }

    private void handle(final ChangeVmEmulatorPinningMsg msg) {
        ChangeVmEmulatorPinningReply reply = new ChangeVmEmulatorPinningReply();

        MevocoKVMAgentCommands.ChangeEmulatorPinnningCmd cmd = new MevocoKVMAgentCommands.ChangeEmulatorPinnningCmd();
        cmd.setEmulatorPinning(msg.getEmulatorPinning());
        cmd.setUuid(msg.getUuid());
        restf.asyncJsonPost(setVMEmulatorPinning, cmd, new JsonAsyncRESTCallback<KVMAgentCommands.AgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setSuccess(false);
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(KVMAgentCommands.AgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<KVMAgentCommands.AgentResponse> getReturnClass() {
                return KVMAgentCommands.AgentResponse.class;
            }
        });
    }

    private void handle(final SetBridgeRouterPortMsg msg) {
        SetBridgeRouterPortReply reply = new SetBridgeRouterPortReply();
        MevocoKVMAgentCommands.BridgeRouterPortCmd cmd = new MevocoKVMAgentCommands.BridgeRouterPortCmd();
        cmd.setNicNames(msg.getNicNames());
        cmd.setEnable(msg.getEnable());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.SET_BRIDGE_ROUTER_PORT);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply mreply) {
                if (!mreply.isSuccess()) {
                    reply.setError(mreply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = mreply.castReply();
                    MevocoKVMAgentCommands.BridgeRouterPortResponse rsp = r.toResponse(MevocoKVMAgentCommands.BridgeRouterPortResponse.class);
                    if (!rsp.isSuccess()) {
                        reply.setError(operr(rsp.getError()));
                    }
                }

                bus.reply(msg, reply);
            }
        });
    }

    private synchronized void syncHostNetworkFactsInDb(String hostUuid,
                                          List<HostNetworkBondingInventory> bondings,
                                          List<HostNetworkInterfaceInventory> nics) {
        Map<String, HostNetworkBondingVO> slaveBondingMap = new ConcurrentHashMap<>();
        // update bondings in host
        List<HostNetworkInterfaceVO> newInterfaceVOs = new ArrayList<>();
        List<HostNetworkBondingVO> newBondingVOS = new ArrayList<>();

        if (bondings != null && !bondings.isEmpty()) {
            List<HostNetworkBondingVO> oldBondingVOs = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, hostUuid).list();
            List<HostNetworkBondingVO> bondingVOs = transformAndRemoveNull(bondings, inv -> {
                HostNetworkBondingVO vo;
                Optional<HostNetworkBondingVO> optionalVo = oldBondingVOs.stream().filter(bondingVO -> bondingVO.getBondingName().equals(inv.getBondingName())).findFirst();
                if (optionalVo.isPresent()) {
                    vo = optionalVo.get();
                } else {
                    vo = new HostNetworkBondingVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                    newBondingVOS.add(vo);
                }
                vo.setHostUuid(hostUuid);
                if (inv.getMode().contains(HostNetworkBondingConstant.BONDING_MODE_LACP)) {
                    vo.setMode(HostNetworkBondingConstant.BONDING_MODE_LACP);
                } else {
                    vo.setMode(HostNetworkBondingConstant.BONDING_MODE_AB);
                    inv.setXmitHashPolicy(null);
                }
                vo.setBondingName(inv.getBondingName());
                vo.setBondingType(inv.getBondingType());
                vo.setSpeed(inv.getSpeed());
                vo.setCallBackIp(inv.getCallBackIp());
                if (inv.getIpAddresses() != null && !inv.getIpAddresses().isEmpty()) {
                    vo.setIpAddresses(String.join(",", inv.getIpAddresses()));
                } else {
                    vo.setIpAddresses(null);
                }
                if (NetworkInterfaceType.bridgeSlave.toString().equals(inv.getBondingType())) {
                    vo.setIpAddresses(null);
                }
                vo.setMac(inv.getMac());
                vo.setMiimon(inv.getMiimon() == null ? 0 : inv.getMiimon());
                vo.setMiiStatus(inv.getMiiStatus());
                vo.setType(inv.getType());
                if (inv.getXmitHashPolicy() != null) {
                    if (inv.getXmitHashPolicy().contains("0")) {
                        vo.setXmitHashPolicy(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO);
                    } else if (inv.getXmitHashPolicy().contains("1")) {
                        vo.setXmitHashPolicy(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_THREE_AND_FOUR);
                    } else {
                        vo.setXmitHashPolicy(HostNetworkBondingConstant.BONDING_XMIT_HASH_POLICY_LAYER_TWO_AND_THREE);
                    }
                } else {
                    vo.setXmitHashPolicy(null);
                }
                vo.setAllSlavesActive(inv.getAllSlavesActive() == Boolean.TRUE);
                if (inv.getSlaves() != null) {
                    for (HostNetworkInterfaceInventory i : inv.getSlaves()) {
                        slaveBondingMap.put(i.getInterfaceName(), vo);
                    }
                }
                return vo;
            });
            dbf.updateCollection(bondingVOs);

            Set<String> nameSet = bondingVOs.stream().map(HostNetworkBondingVO::getBondingName).collect(Collectors.toSet());
            Set<String> deleteSet = oldBondingVOs.stream().map(HostNetworkBondingVO::getBondingName).filter(name -> !nameSet.contains(name)).collect(Collectors.toSet());
            if (!deleteSet.isEmpty()) {
                List<String> bondingUuids = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.uuid)
                        .eq(HostNetworkBondingVO_.hostUuid, hostUuid).in(HostNetworkBondingVO_.bondingName, deleteSet)
                        .listValues();
                SQL.New(HostNetworkBondingVO.class).in(HostNetworkBondingVO_.uuid, bondingUuids)
                        .hardDelete();
            }
        } else {
            // delete old bondings in host
            SQL.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, hostUuid).hardDelete();
        }
        // update nics in host
        if (nics != null && !nics.isEmpty()) {
            List<PciDeviceVO> pciDeviceVOs = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, hostUuid)
                    .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller).list();
            List<HostNetworkInterfaceVO> oldNicVOs = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid).list();
            List<HostNetworkInterfaceVO> nicVOs = transformAndRemoveNull(nics, inv -> {
                HostNetworkInterfaceVO vo;
                Optional<HostNetworkInterfaceVO> optionalVo = oldNicVOs.stream().filter(interfaceVO -> interfaceVO.getInterfaceName().equals(inv.getInterfaceName())).findFirst();
                if (optionalVo.isPresent()) {
                    vo = optionalVo.get();
                } else {
                    vo = new HostNetworkInterfaceVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                    newInterfaceVOs.add(vo);
                }
                vo.setHostUuid(hostUuid);
                vo.setInterfaceName(inv.getInterfaceName());
                vo.setInterfaceModel(truncateHostNicPciName(hostUuid, inv.getInterfaceName(), "interfaceModel", inv.getInterfaceModel()));
                vo.setDeviceId(inv.getDeviceId());
                vo.setVendorId(inv.getVendorId());
                vo.setDeviceName(truncateHostNicPciName(hostUuid, inv.getInterfaceName(), "deviceName", inv.getDeviceName()));
                vo.setVendorName(truncateHostNicPciName(hostUuid, inv.getInterfaceName(), "vendorName", inv.getVendorName()));
                vo.setSubvendorId(inv.getSubvendorId());
                vo.setSubdeviceId(inv.getSubdeviceId());
                vo.setSubvendorName(truncateHostNicPciName(hostUuid, inv.getInterfaceName(), "subvendorName", inv.getSubvendorName()));
                vo.setInterfaceType(inv.getInterfaceType());
                if (inv.getIpAddresses() != null && !inv.getIpAddresses().isEmpty()) {
                    vo.setIpAddresses(String.join(",", inv.getIpAddresses()));
                } else {
                    vo.setIpAddresses(null);
                }
                if (NetworkInterfaceType.bridgeSlave.toString().equals(inv.getInterfaceType())) {
                    vo.setIpAddresses(null);
                }
                if (NetworkInterfaceType.bondingSlave.toString().equals(inv.getInterfaceType())) {
                    vo.setBondingUuid(slaveBondingMap.get(inv.getInterfaceName()).getUuid());
                    vo.setIpAddresses(null);
                } else {
                    vo.setBondingUuid(null);
                }
                vo.setMac(inv.getMac());
                vo.setSpeed(inv.getSpeed());
                vo.setCallBackIp(inv.getCallBackIp());
                vo.setPciDeviceAddress(inv.getPciDeviceAddress());
                vo.setSlaveActive(inv.getSlaveActive() == Boolean.TRUE);
                vo.setCarrierActive(inv.getCarrierActive() == Boolean.TRUE);
                vo.setOffloadStatus(inv.getOffloadStatus());
                PciDeviceVirtStatus virtStatus = pciDeviceVOs.stream()
                        .filter(i -> i.getPciDeviceAddress().equals(inv.getPciDeviceAddress()))
                        .findFirst()
                        .map(PciDeviceVO::getVirtStatus)
                        .orElse(PciDeviceVirtStatus.UNKNOWN);
                vo.setVirtStatus(String.valueOf(virtStatus));
                return vo;
            });
            dbf.updateCollection(nicVOs);

            Set<String> nameSet = nicVOs.stream().map(HostNetworkInterfaceVO::getInterfaceName).collect(Collectors.toSet());
            Set<String> deleteSet = oldNicVOs.stream().map(HostNetworkInterfaceVO::getInterfaceName).filter(name -> !nameSet.contains(name)).collect(Collectors.toSet());
            if (!deleteSet.isEmpty()) {
                List<String> interfaceUuids = Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.uuid)
                        .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid).in(HostNetworkInterfaceVO_.interfaceName, deleteSet)
                        .listValues();
                SQL.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, interfaceUuids)
                        .hardDelete();
            }
        } else {
            // delete old nics in host
            SQL.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid).hardDelete();
        }

        for (HostNetworkInterfaceUpdateExtensionPoint exps :
                pluginRgty.getExtensionList(HostNetworkInterfaceUpdateExtensionPoint.class)) {
            exps.afterCreated(hostUuid, newInterfaceVOs, newBondingVOS);
        }

        logger.debug(String.format("successfully synced network facts of host[uuid:%s] in database", hostUuid));
    }

    private String truncateHostNicPciName(String hostUuid, String interfaceName, String fieldName, String value) {
        if (value == null) {
            return null;
        }

        if (value.length() <= HOST_NIC_PCI_NAME_MAX_LENGTH) {
            return value;
        }

        logger.warn(String.format(
                "truncate HostNetworkInterfaceVO.%s for host[uuid:%s], interface[%s], length[%d] exceeds max[%d]",
                fieldName, hostUuid, interfaceName, value.length(), HOST_NIC_PCI_NAME_MAX_LENGTH
        ));
        return value.substring(0, HOST_NIC_PCI_NAME_MAX_LENGTH);
    }

    private void handle(final GetHostNetworkFactsMsg msg) {
        GetHostNetworkFactsReply greply = new GetHostNetworkFactsReply();

        MevocoKVMAgentCommands.GetHostNetworkBondingCmd cmd = new MevocoKVMAgentCommands.GetHostNetworkBondingCmd();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.GET_HOST_NETWORK_FACTS);
        kmsg.setHostUuid(msg.getHostUuid());
        cmd.setManagementServerIp(getManagementServerIp());
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(msg.isNoStatusCheck());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    MevocoKVMAgentCommands.GetHostNetworkBondingResponse rsp = r.toResponse(MevocoKVMAgentCommands.GetHostNetworkBondingResponse.class);
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    } else {
                        syncHostNetworkFactsInDb(msg.getHostUuid(), rsp.bondings, rsp.nics);
                        greply.setBondings(HostNetworkBondingInventory.valueOf(
                                Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid()).list()));
                        greply.setNics(HostNetworkInterfaceInventory.valueOf(
                                Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid()).list()));
                    }
                }

                bus.reply(msg, greply);
            }
        });
    }

    private void handle(final LocateHostNetworkInterfaceMsg msg){
        LocateHostNetworkInterfaceReply reply1 = new LocateHostNetworkInterfaceReply();

        boolean isInterface = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.interfaceName, msg.getNetworkInterfaceName())
                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                .isExists();
        if (!isInterface) {
            throw new OperationFailureException(operr("networkInterface[name:%s] of host[uuid:%s] can not find", msg.getNetworkInterfaceName(), msg.getHostUuid()));
        }

        MevocoKVMAgentCommands.LocateHostNetworkInterfaceCmd cmd = new MevocoKVMAgentCommands.LocateHostNetworkInterfaceCmd();
        cmd.interval=msg.getInterval();
        cmd.networkInterface = msg.getNetworkInterfaceName();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.LOCATE_HOST_NETWORK_INTERFACE);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg);
        bus.reply(msg, reply1);
    }

    @Transactional
    private synchronized void syncHostPhysicalMemoryFactsInDb(String hostUuid, List<HostPhysicalMemoryInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            SQL.New(HostPhysicalMemoryVO.class).eq(HostPhysicalMemoryVO_.hostUuid, hostUuid).hardDelete();
            return;
        }
        
        List<HostPhysicalMemoryInventory> oldHostPhysicalMemoryInventories = HostPhysicalMemoryInventory.valueOf(Q.New(HostPhysicalMemoryVO.class)
                .eq(HostPhysicalMemoryVO_.hostUuid, hostUuid)
                .list());

        oldHostPhysicalMemoryInventories.stream()
                .filter(vo -> inventories.stream().noneMatch(inventory -> inventory.getSerialNumber().equals(vo.getSerialNumber())))
                .forEach(vo -> dbf.removeByPrimaryKey(vo.getUuid(), HostPhysicalMemoryVO.class));

        List<HostPhysicalMemoryInventory> newHostPhysicalMemoryInventories = inventories.stream()
                .filter(vo -> oldHostPhysicalMemoryInventories.stream().noneMatch(inventory -> vo.getSerialNumber().equals(inventory.getSerialNumber())))
                .collect(Collectors.toList());
        
        if (!newHostPhysicalMemoryInventories.isEmpty()) {
            List<HostPhysicalMemoryVO> hostPhysicalMemoryVOS = transformAndRemoveNull(newHostPhysicalMemoryInventories, inv -> {
                HostPhysicalMemoryVO vo = new HostPhysicalMemoryVO();
                vo.setUuid(Platform.getUuid());
                vo.setHostUuid(hostUuid);
                vo.setManufacturer(inv.getManufacturer());
                vo.setSize(inv.getSize());
                vo.setSpeed(inv.getSpeed());
                vo.setClockSpeed(inv.getClockSpeed());
                vo.setLocator(inv.getLocator());
                vo.setSerialNumber(inv.getSerialNumber());
                vo.setRank(inv.getRank());
                vo.setVoltage(inv.getVoltage());
                vo.setType(inv.getType());
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                return vo;
            });
            dbf.persistCollection(hostPhysicalMemoryVOS);
        }
    }

    private void handle(final GetHostPhysicalMemoryFactsMsg msg) {
        GetHostPhysicalMemoryFactsReply greply = new GetHostPhysicalMemoryFactsReply();

        MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsCmd cmd = new MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsCmd();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.GET_HOST_PHYSICAL_MEMORY_FACTS);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(msg.isNoStatusCheck());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsResponse rsp = r.toResponse(MevocoKVMAgentCommands.GetHostPhysicalMemoryFactsResponse.class);
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    } else {
                        syncHostPhysicalMemoryFactsInDb(msg.getHostUuid(), rsp.physicalMemoryFacts);
                        greply.setInventories(HostPhysicalMemoryInventory.valueOf(
                                Q.New(HostPhysicalMemoryVO.class).eq(HostPhysicalMemoryVO_.hostUuid, msg.getHostUuid()).list()));
                    }
                }

                bus.reply(msg, greply);
            }
        });
    }

    private void handle(final GetHostPhysicalCpuFactsMsg msg) {
        GetHostPhysicalCpuFactsReply greply = new GetHostPhysicalCpuFactsReply();

        MevocoKVMAgentCommands.GetHostPhysicalCpuFactsCmd cmd = new MevocoKVMAgentCommands.GetHostPhysicalCpuFactsCmd();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.GET_HOST_PHYSICAL_CPU_FACTS);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(msg.isNoStatusCheck());
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    MevocoKVMAgentCommands.GetHostPhysicalCpuFactsResponse rsp = r.toResponse(MevocoKVMAgentCommands.GetHostPhysicalCpuFactsResponse.class);
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    } else {
                        syncHostPhysicalCpuFactsInDb(msg.getHostUuid(), rsp.physicalCpuFacts);
                        greply.setInventories(HostPhysicalCpuInventory.valueOf(
                                Q.New(HostPhysicalCpuVO.class).eq(HostPhysicalCpuVO_.hostUuid, msg.getHostUuid()).list()));
                    }
                }

                bus.reply(msg, greply);
            }
        });
    }

    private void syncHostPhysicalCpuFactsInDb(String hostUuid, List<HostPhysicalCpuInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            SQL.New(HostPhysicalCpuVO.class).eq(HostPhysicalCpuVO_.hostUuid, hostUuid).hardDelete();
            return;
        }

        List<HostPhysicalCpuInventory> oldHostPhysicalCpuInventories = HostPhysicalCpuInventory.valueOf(Q.New(HostPhysicalCpuVO.class)
                .eq(HostPhysicalCpuVO_.hostUuid, hostUuid)
                .list());

        oldHostPhysicalCpuInventories.stream()
                .filter(vo -> inventories.stream().noneMatch(inventory -> inventory.getSerialNumber().equals(vo.getSerialNumber())))
                .forEach(vo -> dbf.removeByPrimaryKey(vo.getUuid(), HostPhysicalCpuVO.class));

        List<HostPhysicalCpuInventory> newHostPhysicalCpuInventories = inventories.stream()
                .filter(vo -> oldHostPhysicalCpuInventories.stream().noneMatch(inventory -> vo.getSerialNumber().equals(inventory.getSerialNumber())))
                .collect(Collectors.toList());

        if (!newHostPhysicalCpuInventories.isEmpty()) {
            List<HostPhysicalCpuVO> hostPhysicalCpuVOS = transformAndRemoveNull(newHostPhysicalCpuInventories, inv -> {
                HostPhysicalCpuVO vo = new HostPhysicalCpuVO();
                vo.setUuid(Platform.getUuid());
                vo.setHostUuid(hostUuid);
                vo.setSocketDesignation(inv.getSocketDesignation());
                vo.setSerialNumber(inv.getSerialNumber());
                vo.setVersion(inv.getVersion());
                vo.setCurrentSpeed(inv.getCurrentSpeed());
                vo.setCoreCount(inv.getCoreCount());
                vo.setThreadCount(inv.getThreadCount());
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                return vo;
            });
            dbf.persistCollection(hostPhysicalCpuVOS);
        }
    }

    private void handle(final ChangeHostPasswordMsg msg) {
        ChangeHostPasswordReply reply = new ChangeHostPasswordReply();

        MevocoKVMAgentCommands.ChangeHostPasswordCmd cmd = new MevocoKVMAgentCommands.ChangeHostPasswordCmd();
        cmd.password = msg.getPassword();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.CHANGE_PASSWORD);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rpy) {
                if (!rpy.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = rpy.castReply();
                    KVMAgentCommands.AgentResponse rsp = r.toResponse(KVMAgentCommands.AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        reply.setSuccess(false);
                        reply.setError(operr("operation error, because %s", rsp.getError()));
                    }
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final IdentifyHostMsg msg) {
        IdentifyHostReply ireply = new IdentifyHostReply();

        MevocoKVMAgentCommands.IdentifyHostCmd cmd = new MevocoKVMAgentCommands.IdentifyHostCmd();
        cmd.interval = msg.getInterval();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.IDENTIFY_HOST);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    ireply.setSuccess(false);
                    ireply.setError(reply.getError());
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    KVMAgentCommands.AgentResponse rsp = r.toResponse(KVMAgentCommands.AgentResponse.class);
                    if (!rsp.isSuccess()) {
                        ireply.setSuccess(false);
                        ireply.setError(operr("operation error, because %s", rsp.getError()));
                    }
                }

                bus.reply(msg, ireply);
            }
        });
    }

    private void handle(final BlockStreamVolumeMsg msg) {
        checkStateAndStatus(msg.getHostUuid());

        BlockStreamVolumeReply breply = new BlockStreamVolumeReply();
        MevocoKVMAgentCommands.BlockStreamVolumeCmd cmd = new MevocoKVMAgentCommands.BlockStreamVolumeCmd();
        cmd.vmUuid = msg.getVmInstanceUuid();
        cmd.volume = VolumeTO.valueOf(msg.getVolume(), context.getInventory());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.BLOCK_STREAM);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    breply.setError(reply.getError());
                    bus.reply(msg, breply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                KVMAgentCommands.AgentResponse rsp = r.toResponse(KVMAgentCommands.AgentResponse.class);
                if (!rsp.isSuccess()) {
                    breply.setError(operr("%s", rsp.getError()));
                    bus.reply(msg, breply);
                    return;
                }

                bus.reply(msg, breply);
            }
        });
    }

    private void handle(final TakeVolumesSnapshotOnKvmMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        if (hostVO == null) {
            throw new OperationFailureException(operr("host[uuid:%s] can not find", msg.getHostUuid()));
        }
        checkStateAndStatus(hostVO);

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid,
                msg.getSnapshotJobs().get(0).getVmInstanceUuid()).find();

        if (!MevocoConstants.supportedVmStatesForLiveSnapshotOnVolumes.contains(vmInstanceVO.getState())) {
            throw new OperationFailureException(operr("only support do live snapshot on vm state[%s], " +
                    "but vm is on [%s] state", vmInstanceVO.getUuid(), vmInstanceVO.getState()));
        }

        if (!HostSystemTags.LIVE_SNAPSHOT.hasTag(msg.getHostUuid())) {
            throw new OperationFailureException(err(SysErrors.NO_CAPABILITY_ERROR,
                    "kvm host[uuid:%s, name:%s, ip:%s] doesn't not support live snapshot. please stop vm[uuid:%s] and try again",
                    hostVO.getUuid(), hostVO.getName(), hostVO.getManagementIp(), vmInstanceVO.getUuid()
            ));
        }

        TakeVolumesSnapshotOnKvmReply treply = new TakeVolumesSnapshotOnKvmReply();
        MevocoKVMAgentCommands.TakeSnapshotsCmd cmd = new MevocoKVMAgentCommands.TakeSnapshotsCmd();
        cmd.snapshotJobs = msg.getSnapshotJobs();
        cmd.vmHostFileBackupJobs = msg.getVmHostFileBackupJobs();
        cmd.timeout = timeoutManager.getTimeoutSeconds();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.TAKE_VOLUMES_SNAPSHOT);
        kmsg.setHostUuid(msg.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    treply.setSuccess(false);
                    treply.setError(reply.getError());
                    bus.reply(msg, treply);
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                if (!r.isSuccess()) {
                    treply.setSuccess(false);
                    treply.setError(r.getError());
                    bus.reply(msg, treply);
                    return;
                }

                MevocoKVMAgentCommands.TakeSnapshotsResponse rsp = r.toResponse(MevocoKVMAgentCommands.TakeSnapshotsResponse.class);
                if (!rsp.isSuccess()) {
                    treply.setSuccess(false);
                    treply.setError(operr("%s", rsp.getError()));
                    bus.reply(msg, treply);
                    return;
                }

                treply.setSnapshotsResults(rsp.snapshots);
                if (treply.getSnapshotsResults() == null || treply.getSnapshotsResults().isEmpty()) {
                    treply.setSuccess(false);
                }
                bus.reply(msg, treply);
            }
        });
    }

    private void handle(final CheckMountDomainMsg msg) {
        CheckMountDomainReply reply = new CheckMountDomainReply();
        MevocoKVMAgentCommands.CheckMountDomainCmd cmd = new MevocoKVMAgentCommands.CheckMountDomainCmd();
        cmd.setTimeout(msg.getWait());
        cmd.setUrl(msg.getMountDomain());

        restf.asyncJsonPost(checkMountPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.CheckMountDomainResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);

            }

            @Override
            public void success(MevocoKVMAgentCommands.CheckMountDomainResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                reply.setActive(ret.active);
                bus.reply(msg, reply);

            }

            @Override
            public Class<MevocoKVMAgentCommands.CheckMountDomainResponse> getReturnClass() {
                return MevocoKVMAgentCommands.CheckMountDomainResponse.class;
            }
        });
    }

    private void handle(ResizeVolumeOnKvmMsg msg) {
        ResizeVolumeOnHypervisorReply reply = new ResizeVolumeOnHypervisorReply();
        VolumeInventory volume = msg.getVolume();
        volume.setSize(msg.getSize());

        MevocoKVMAgentCommands.ResizeVolumeCmd cmd = new MevocoKVMAgentCommands.ResizeVolumeCmd();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setSize(msg.getSize());
        cmd.setVolume(VolumeTO.valueOf(msg.getVolume(), context.getInventory()));
        cmd.setInstallPath(msg.getVolume().getInstallPath());
        beforeKvmHostResizeVolume(cmd, msg.getVolume(), msg.getHostUuid());
        restf.asyncJsonPost(resizeVolume, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.ResizeVolumeResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(MevocoKVMAgentCommands.ResizeVolumeResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because %s", ret.getError()));
                    bus.reply(msg, reply);
                    return;
                }

                reply.setVolume(volume);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.ResizeVolumeResponse> getReturnClass() {
                return MevocoKVMAgentCommands.ResizeVolumeResponse.class;
            }
        });
    }

    private void beforeKvmHostResizeVolume(MevocoKVMAgentCommands.ResizeVolumeCmd cmd, VolumeInventory volume, String hostUuid) {
        HostResizeVolumeStruct struct = new HostResizeVolumeStruct();
        struct.setSize(cmd.getSize());
        struct.setInstallPath(cmd.getInstallPath());
        struct.setDeviceType(cmd.getDeviceType());

        for (HostResizeVolumeExtensionPoint ext : pluginRgty.getExtensionList(HostResizeVolumeExtensionPoint.class)) {
            struct = ext.beforeKvmHostResizeVolume(struct, volume, hostUuid);
        }

        cmd.setSize(struct.getSize());
        cmd.setInstallPath(struct.getInstallPath());
        cmd.setDeviceType(struct.getDeviceType());
    }

    private void handle(final GetVolumeQosOnKVMHostMsg msg) {
        GetVolumeQosOnKVMHostReply reply = new GetVolumeQosOnKVMHostReply();
        MevocoKVMAgentCommands.VolumeQosCmd cmd = new MevocoKVMAgentCommands.VolumeQosCmd();
        cmd.setVmUuid(msg.getVmUuid());
        cmd.volume = VolumeTO.valueOf(msg.getVolume(), context.getInventory());

        for (KVMHostSetVolumeQosExtensionPoint ext : pluginRgty.getExtensionList(KVMHostSetVolumeQosExtensionPoint.class)) {
            if (ext.forbbidenSetVolumeQos(msg.getVolume())) {
                reply.setError(err(PrimaryStorageErrors.FORBIDDENQOS, "primary storage type doesn't support sync qos from host"));
                bus.reply(msg, reply);
                return;
            }
        }

        restf.asyncJsonPost(getVolumeBandwidth, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.VolumeAgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setSuccess(false);
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(MevocoKVMAgentCommands.VolumeAgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                if (NumberUtils.isNumber(ret.bandWidth)) {
                    reply.setVolumeBandwidth(Long.valueOf(ret.bandWidth));
                }

                if (NumberUtils.isNumber(ret.bandWidthWrite)) {
                    reply.setVolumeBandwidthWrite(Long.valueOf(ret.bandWidthWrite));
                }

                if (NumberUtils.isNumber(ret.bandWidthRead)) {
                    reply.setVolumeBandwidthRead(Long.valueOf(ret.bandWidthRead));
                }

                if (NumberUtils.isNumber(ret.iopsTotal)) {
                    reply.setIopsTotal(Long.valueOf(ret.iopsTotal));
                }

                if (NumberUtils.isNumber(ret.iopsRead)) {
                    reply.setIopsRead(Long.valueOf(ret.iopsRead));
                }

                if (NumberUtils.isNumber(ret.iopsWrite)) {
                    reply.setIopsWrite(Long.valueOf(ret.iopsWrite));
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.VolumeAgentResponse> getReturnClass() {
                return MevocoKVMAgentCommands.VolumeAgentResponse.class;
            }
        });
    }

    private void handle(final GetNicQosOnKVMHostMsg msg) {
        GetNicQosOnKVMHostReply reply = new GetNicQosOnKVMHostReply();
        MevocoKVMAgentCommands.NicQosCmd cmd = new MevocoKVMAgentCommands.NicQosCmd();
        cmd.setVmUuid(msg.getVmUuid());
        cmd.setInternalName(msg.getInternalName());

        restf.asyncJsonPost(getNicQos, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.NicAgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setSuccess(false);
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(MevocoKVMAgentCommands.NicAgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                if (ret.inbound != null) {
                    reply.setInbound(Long.valueOf(ret.inbound));
                }
                if (ret.outbound != null) {
                    reply.setOutbound(Long.valueOf(ret.outbound));
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.NicAgentResponse> getReturnClass() {
                return MevocoKVMAgentCommands.NicAgentResponse.class;
            }
        });
    }

    private void handle(final SetNicQosOnKVMHostMsg msg) {
        SetNicQosOnKvmHostReply reply = new SetNicQosOnKvmHostReply();

        // FIX ME: VF NIC QoS
        VmNicVO vnic = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmUuid())
                .eq(VmNicVO_.internalName, msg.getInternalName())
                .limit(1)
                .find();
        if (vnic != null && vnic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
            logger.debug("vf nic qos is not supported for now, so skip");
            bus.reply(msg, reply);
            return;
        }

        MevocoKVMAgentCommands.NicQosCmd cmd = new MevocoKVMAgentCommands.NicQosCmd();
        if (msg.getInboundBandwidth() != null) {
            cmd.setInboundBandwidth(msg.getInboundBandwidth());
        }
        if (msg.getOutboundBandwidth() != null) {
            cmd.setOutboundBandwidth(msg.getOutboundBandwidth());
        }
        cmd.setVmUuid(msg.getVmUuid());
        cmd.setInternalName(msg.getInternalName());

        restf.asyncJsonPost(setNicQos, cmd, new JsonAsyncRESTCallback<KVMAgentCommands.AgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setSuccess(false);
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(KVMAgentCommands.AgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setSuccess(false);
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<KVMAgentCommands.AgentResponse> getReturnClass() {
                return KVMAgentCommands.AgentResponse.class;
            }
        });
    }

    private void handle(final DeleteVolumeQosOnKVMHostMsg msg) {
        MessageReply reply = new MessageReply();
        MevocoKVMAgentCommands.VolumeQosCmd cmd = new MevocoKVMAgentCommands.VolumeQosCmd();
        cmd.setVmUuid(msg.getVmUuid());
        cmd.setMode(msg.getMode());
        cmd.volume = VolumeTO.valueOf(msg.getVolume(), context.getInventory());

        restf.asyncJsonPost(deleteVolumeBandwidth, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.VolumeAgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(MevocoKVMAgentCommands.VolumeAgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.VolumeAgentResponse> getReturnClass() {
                return MevocoKVMAgentCommands.VolumeAgentResponse.class;
            }
        });
    }

    private void setVolumeQos(final SetVolumeQosOnKVMHostMsg msg, final Completion completion) {
        for (KVMHostSetVolumeQosExtensionPoint ext : pluginRgty.getExtensionList(KVMHostSetVolumeQosExtensionPoint.class)) {
            if (ext.forbbidenSetVolumeQos(msg.getVolume())) {
                completion.fail(err(PrimaryStorageErrors.FORBIDDENQOS, "primary storage type doesn't support set qos"));
                return;
            }
        }

        MevocoKVMAgentCommands.VolumeQosCmd cmd = new MevocoKVMAgentCommands.VolumeQosCmd();
        cmd.setTotalBandwidth(msg.getTotalBandWidth() < 0 ? 0 : msg.getTotalBandWidth());
        cmd.setReadBandwidth(msg.getReadBandwidth() < 0 ? 0 : msg.getReadBandwidth());
        cmd.setWriteBandwidth(msg.getWriteBandwidth() < 0 ? 0 : msg.getWriteBandwidth());
        cmd.setTotalIOPS(msg.getTotalIOPS() < 0 ? 0 : msg.getTotalIOPS());
        cmd.setReadIOPS(msg.getReadIOPS() < 0 ? 0 : msg.getReadIOPS());
        cmd.setWriteIOPS(msg.getWriteIOPS() < 0 ? 0 : msg.getWriteIOPS());
        cmd.setVmUuid(msg.getVmUuid());
        cmd.setMode(msg.getMode());
        cmd.volume = VolumeTO.valueOf(msg.getVolume(), context.getInventory());

        restf.asyncJsonPost(setVolumeBandwidth, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.VolumeAgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(MevocoKVMAgentCommands.VolumeAgentResponse ret) {
                if (!ret.isSuccess()) {
                    completion.fail(operr("%s", ret.getError()));
                } else {
                    completion.success();
                }
            }

            @Override
            public Class<MevocoKVMAgentCommands.VolumeAgentResponse> getReturnClass() {
                return MevocoKVMAgentCommands.VolumeAgentResponse.class;
            }
        });
    }

    private void handle(final SetVolumeQosOnKVMHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                SetVolumeQosOnKvmHostReply reply = new SetVolumeQosOnKvmHostReply();
                setVolumeQos(msg, new Completion(chain) {
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
                return String.format("set-volume-qos-%s", msg.getVolume().getUuid());
            }
        });
    }

    private void handle(final ChangeVmPasswordMsg msg) {
        final ChangeVmPasswordReply reply = new ChangeVmPasswordReply();

        MevocoKVMAgentCommands.ChangeVmPasswordCmd cmd = new MevocoKVMAgentCommands.ChangeVmPasswordCmd();
        cmd.setAccountPerference(msg.getAccountPerference());
        cmd.setTimeout(timeoutManager.getTimeout());

        restf.asyncJsonPost(changeVmPasswordPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.ChangeVmPasswordResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(MevocoKVMAgentCommands.ChangeVmPasswordResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                } else {
                    reply.setVmAccountPreference(ret.getVmAccountPerference());
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.ChangeVmPasswordResponse> getReturnClass() {
                return MevocoKVMAgentCommands.ChangeVmPasswordResponse.class;
            }
        });
    }

    private void handle(CheckVmVolumesMsg msg) {
        final CheckVmVolumesReply reply = new CheckVmVolumesReply();

        MevocoKVMAgentCommands.CheckVmVolumesCmd cmd = new MevocoKVMAgentCommands.CheckVmVolumesCmd();
        cmd.setUuid(msg.getVmInstance().getUuid());
        cmd.setVolumes(VolumeTO.valueOf(msg.getVmInstance().getAllDiskVolumes(), context.getInventory()));

        restf.asyncJsonPost(checkVmVolumesPath, cmd, new JsonAsyncRESTCallback<KVMAgentCommands.AgentResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(KVMAgentCommands.AgentResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<KVMAgentCommands.AgentResponse> getReturnClass() {
                return KVMAgentCommands.AgentResponse.class;
            }
        });
    }

    private void handle(PowerOffHostMsg msg) {
        final PowerOffHostReply reply = new PowerOffHostReply();

        HostState originState = HostState.valueOf(msg.getHost().getState());
        String powerOffMnId =  SQL.New("select m.uuid from HostVO h, ManagementNodeVO m" +
                " where m.hostName = h.managementIp and h.uuid = :huuid", String.class)
                .param("huuid", msg.getHostUuid()).find();

        boolean shutdownOurself = Platform.getManagementServerId().equals(powerOffMnId);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("power-off-host-%s", msg.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "disable-host";

                    @Override
                    public boolean skip(Map data) {
                        return originState != HostState.Enabled;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        changeHostState(HostStateEvent.disable, trigger);
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        recoverHostStateInDb();
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "maintain-host";

                    @Override
                    public boolean skip(Map data) {
                        return originState == HostState.Maintenance;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        changeHostState(HostStateEvent.preMaintain, trigger);
                    }
                });

                if (shutdownOurself) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "recover-host-state-ahead";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            recoverHostStateInDb();
                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "power-off-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ShutdownHostMsg smsg = new ShutdownHostMsg();
                        smsg.setMaxWaitTime(msg.getMaxWaitTime());
                        smsg.setWaitTaskCompleted(msg.isWaitTaskCompleted());
                        smsg.setUuid(msg.getHostUuid());
                        smsg.setMethod(HostPowerManagementMethod.AGENT);
                        bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (powerOffMnId != null && !shutdownOurself
                                        && reply.getError().isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR) ) {
                                    trigger.next();
                                    return;
                                }

                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });
                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        recoverHostStateInDb();
                        new HostBase.HostDisconnectedCanonicalEvent(msg.getHostUuid(), operr("host[uuid:%s] becomes power off, send notify",
                                msg.getHostUuid())).fire();
                        reply.addResult(PowerOffHardwareResult.valueOf(msg.getHostUuid(), null));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        reply.addResult(PowerOffHardwareResult.valueOf(msg.getHostUuid(), errCode));
                        bus.reply(msg, reply);
                    }
                });
            }

            private void changeHostState(HostStateEvent stateEvent, FlowTrigger trigger) {
                ChangeHostStateMsg cmsg = new ChangeHostStateMsg();
                cmsg.setUuid(msg.getHostUuid());
                cmsg.setStateEvent(stateEvent.toString());
                cmsg.setJustChangeState(false);
                bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
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

            private void recoverHostStateInDb() {
                SQL.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).set(HostVO_.state, originState).update();
            }
        }).start();
    }

    private void handle(final VmPortsConfigSyncOnHypervisorMsg msg) {

        final VmPortsConfigSyncOnHypervisorReply reply = new VmPortsConfigSyncOnHypervisorReply();
        MevocoKVMAgentCommands.SyncVmPortsCommand cmd = new MevocoKVMAgentCommands.SyncVmPortsCommand();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setPorts(msg.getPorts());
        cmd.setHostname(msg.getHostname());
        cmd.setDefaultIP(msg.getDefaultIP());

        restf.asyncJsonPost(vmPortsConfigSyncPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.SyncVmPortsResponse>(msg) {
            @Override
            public void success(MevocoKVMAgentCommands.SyncVmPortsResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode code = new ErrorCode();
                    code.setCause(operr("sync vm port config failed: %s", ret.getError()));
                    reply.setError(code);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ErrorCode code = new ErrorCode();
                code.setCause(errorCode);
                reply.setError(code);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.SyncVmPortsResponse> getReturnClass() {
                return MevocoKVMAgentCommands.SyncVmPortsResponse.class;
            }
        });
    }

    private void handle(final VmSpecificationConfigSyncOnHypervisorMsg msg) {
        final VmSpecificationConfigSyncOnHypervisorReply reply = new VmSpecificationConfigSyncOnHypervisorReply();
        MevocoKVMAgentCommands.SyncVmSpecificationCommand cmd = new MevocoKVMAgentCommands.SyncVmSpecificationCommand();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setSpec(msg.getSpec());

        restf.asyncJsonPost(vmSpecificationConfigSyncPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.SyncVmSpecificationResponse>(msg) {
            @Override
            public void success(MevocoKVMAgentCommands.SyncVmSpecificationResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode code = new ErrorCode();
                    code.setCause(operr("sync vm specification config failed: %s", ret.getError()));
                    reply.setError(code);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ErrorCode code = new ErrorCode();
                code.setCause(errorCode);
                reply.setError(code);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.SyncVmSpecificationResponse> getReturnClass() {
                return MevocoKVMAgentCommands.SyncVmSpecificationResponse.class;
            }
        });
    }

    private void handle(SetVmHostnameOnHypervisorMsg msg) {
        final SetVmHostnameOnHypervisorReply reply = new SetVmHostnameOnHypervisorReply();
        MevocoKVMAgentCommands.SetVmHostnameCmd cmd = new MevocoKVMAgentCommands.SetVmHostnameCmd();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setHostname(msg.getHostname());
        cmd.setDefaultIP(msg.getDefaultIP());

        restf.asyncJsonPost(setVmHostnamePath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.SetVmHostnameResponse>(msg) {
            @Override
            public void success(MevocoKVMAgentCommands.SetVmHostnameResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode code = new ErrorCode();
                    code.setCause(operr("set vm hostname failed: %s", ret.getError()));
                    reply.setError(code);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ErrorCode code = new ErrorCode();
                code.setCause(errorCode);
                reply.setError(code);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.SetVmHostnameResponse> getReturnClass() {
                return MevocoKVMAgentCommands.SetVmHostnameResponse.class;
            }
        });
    }

    private void handle(SetVmDnsOnHypervisorMsg msg) {
        final SetVmDnsOnHypervisorReply reply = new SetVmDnsOnHypervisorReply();
        MevocoKVMAgentCommands.SetVmDnsCmd cmd = new MevocoKVMAgentCommands.SetVmDnsCmd();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setDns(msg.getDns());

        restf.asyncJsonPost(setVmDnsPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.SetVmDnsResponse>(msg) {
            @Override
            public void success(MevocoKVMAgentCommands.SetVmDnsResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode code = new ErrorCode();
                    code.setCause(operr("set vm dns failed: %s", ret.getError()));
                    reply.setError(code);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ErrorCode code = new ErrorCode();
                code.setCause(errorCode);
                reply.setError(code);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.SetVmDnsResponse> getReturnClass() {
                return MevocoKVMAgentCommands.SetVmDnsResponse.class;
            }
        });
    }

    private void handle(ApplyMemoryBalloonDecisionMsg msg) {
        final ApplyMemoryBalloonDecisionReply reply = new ApplyMemoryBalloonDecisionReply();
        MevocoKVMAgentCommands.ApplyMemoryBalloonCmd cmd = new MevocoKVMAgentCommands.ApplyMemoryBalloonCmd();
        cmd.setVmUuids(msg.getVmInstanceUuids());
        cmd.setDirection(msg.getDirection());
        cmd.setAdjustPercent(msg.getAdjustPercent());
        cmd.setVmReservedMemory(msg.getVmReservedMemory());

        restf.asyncJsonPost(applyMemoryBalloonPath, cmd, new JsonAsyncRESTCallback<MevocoKVMAgentCommands.ApplyMemoryBalloonResponse>(msg) {
            @Override
            public void success(MevocoKVMAgentCommands.ApplyMemoryBalloonResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode code = new ErrorCode();
                    code.setCause(operr("apply memory balloon failed: %s", ret.getError()));
                    reply.setError(code);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ErrorCode code = new ErrorCode();
                code.setCause(errorCode);
                reply.setError(code);
                bus.reply(msg, reply);
            }

            @Override
            public Class<MevocoKVMAgentCommands.ApplyMemoryBalloonResponse> getReturnClass() {
                return MevocoKVMAgentCommands.ApplyMemoryBalloonResponse.class;
            }
        });
    }
}
