package org.zstack.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.cluster.MevocoClusterGlobalConfig;
import org.zstack.compute.cpuPinning.CpuRangeSet;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;
import org.zstack.header.tpm.TpmConstants;
import org.zstack.header.vm.ChangeVmPasswordMsg;
import org.zstack.header.vm.ChangeVmPasswordReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.vmfiles.message.CloneVmHostFileMsg;
import org.zstack.kvm.tpm.message.CloneVmTpmMsg;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoGlobalProperty;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.mevoco.VolumeQosHelper;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.volume.VolumeQosMode.OVERWRITE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by mingjian.deng on 16/10/29.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MevocoVmInstanceBase extends VmInstanceBase {
    protected static final CLogger logger = Utils.getLogger(MevocoVmInstanceBase.class);

    @Autowired
    MevocoVmInstanceBaseFactory mimpl;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmInstanceManager vmMgr;
    @Autowired
    private VmTpmManager vmTpmManager;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    public MevocoVmInstanceBase(VmInstanceVO vo) {
        super(vo);
        allowedOperations.addState(VmInstanceState.Running,
                APIChangeVmPasswordMsg.class.getName(),
                ChangeVmPasswordMsg.class.getName(),
                APIGetVmInstanceFirstBootDeviceMsg.class.getName(),
                CloneVmInstanceMsg.class.getName(),
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class.getName(),
                APICreateTemplatedVmInstanceFromVmInstanceMsg.class.getName(),
                APICloneVmInstanceMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Stopped,
                APIChangeVmImageMsg.class.getName(),
                CloneVmInstanceMsg.class.getName(),
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class.getName(),
                APICreateTemplatedVmInstanceFromVmInstanceMsg.class.getName(),
                APICloneVmInstanceMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Paused,
                CloneVmInstanceMsg.class.getName(),
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class.getName(),
                APICreateTemplatedVmInstanceFromVmInstanceMsg.class.getName(),
                APICloneVmInstanceMsg.class.getName());
    }

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleApiMessage(APIMessage msg){
        if (msg instanceof APIChangeVmPasswordMsg) {
            handle((APIChangeVmPasswordMsg) msg);
        } else if (msg instanceof APIDeleteNicQosMsg) {
            handle((APIDeleteNicQosMsg) msg);
        } else if (msg instanceof APISetNicQosMsg) {
            handle((APISetNicQosMsg) msg);
        } else if (msg instanceof APIGetNicQosMsg) {
            handle((APIGetNicQosMsg) msg);
        } else if (msg instanceof APISetVmQgaMsg) {
            handle((APISetVmQgaMsg) msg);
        } else if (msg instanceof APIGetVmQgaMsg) {
            handle((APIGetVmQgaMsg) msg);
        } else if (msg instanceof APISetVmUsbRedirectMsg) {
            handle((APISetVmUsbRedirectMsg) msg);
        } else if (msg instanceof APIGetVmUsbRedirectMsg) {
            handle((APIGetVmUsbRedirectMsg) msg);
        } else if (msg instanceof APISetVmRDPMsg) {
            handle((APISetVmRDPMsg) msg);
        } else if (msg instanceof APIGetVmRDPMsg) {
            handle((APIGetVmRDPMsg) msg);
        } else if (msg instanceof APISetVmMonitorNumberMsg) {
            handle((APISetVmMonitorNumberMsg) msg);
        } else if (msg instanceof APIGetVmMonitorNumberMsg) {
            handle((APIGetVmMonitorNumberMsg) msg);
        } else if (msg instanceof APIGetImageCandidatesForVmToChangeMsg) {
            handle((APIGetImageCandidatesForVmToChangeMsg) msg);
        } else if (msg instanceof APIChangeVmImageMsg) {
            handle((APIChangeVmImageMsg) msg);
        } else if (msg instanceof APIUpdateVmNicMacMsg) {
            handle((APIUpdateVmNicMacMsg) msg);
        } else if (msg instanceof APISetVmConsoleModeMsg) {
            handle((APISetVmConsoleModeMsg) msg);
        } else if (msg instanceof APISetVmCleanTrafficMsg) {
            handle((APISetVmCleanTrafficMsg) msg);
        } else if (msg instanceof APIGetVmInstanceFirstBootDeviceMsg) {
            handle((APIGetVmInstanceFirstBootDeviceMsg) msg);
        } else if (msg instanceof APISetVmSecurityLevelMsg) {
            handle((APISetVmSecurityLevelMsg) msg);
        } else if (msg instanceof APIDeleteVmUserDefinedXmlMsg) {
            handle((APIDeleteVmUserDefinedXmlMsg) msg);
        } else if (msg instanceof APISetVmUserDefinedXmlMsg) {
            handle((APISetVmUserDefinedXmlMsg) msg);
        } else if (msg instanceof APIGetVmXmlMsg) {
            handle((APIGetVmXmlMsg) msg);
        } else if (msg instanceof APIDeleteVmUserDefinedXmlHookScriptMsg) {
            handle((APIDeleteVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APISetVmUserDefinedXmlHookScriptMsg) {
            handle((APISetVmUserDefinedXmlHookScriptMsg) msg);
        } else if (msg instanceof APIGetVmXmlHookScriptMsg) {
            handle((APIGetVmXmlHookScriptMsg) msg);
        } else if (msg instanceof APISetVmNumaMsg) {
            handle((APISetVmNumaMsg) msg);
        } else if (msg instanceof APIGetVmNumaMsg) {
            handle((APIGetVmNumaMsg) msg);
        } else if (msg instanceof APISetVmEmulatorPinningMsg) {
            handle((APISetVmEmulatorPinningMsg) msg);
        } else if (msg instanceof APIGetVmEmulatorPinningMsg) {
            handle((APIGetVmEmulatorPinningMsg) msg);
        } else if (msg instanceof APIGetVmvNUMATopologyMsg) {
            handle((APIGetVmvNUMATopologyMsg) msg);
        } else if (msg instanceof APISyncVmClockMsg) {
            handle((APISyncVmClockMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CloneVmSyncQosMsg) {
            handle((CloneVmSyncQosMsg) msg);
        } else if (msg instanceof GetImageCandidatesForVmToChangeMsg) {
            handle((GetImageCandidatesForVmToChangeMsg) msg);
        } else if (msg instanceof ChangeVmImageMsg) {
            handle((ChangeVmImageMsg) msg);
        } else if (msg instanceof CloneVmInstanceMsg) {
            handle((CloneVmInstanceMsg) msg);
        } else if (msg instanceof ChangeVmPasswordMsg) {
            handle((ChangeVmPasswordMsg) msg);
        } else if (msg instanceof SyncVmClockMsg) {
            handle((SyncVmClockMsg) msg);
        } else if (msg instanceof SetVmQgaSyncClockTaskMsg) {
            handle((SetVmQgaSyncClockTaskMsg) msg);
        } else if (msg instanceof GetVmNicQosMsg) {
            handle((GetVmNicQosMsg) msg);
        } else if (msg instanceof SetVmNicQosMsg) {
            handle((SetVmNicQosMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APISyncVmClockMsg msg) {
        APISyncVmClockEvent event = new APISyncVmClockEvent(msg.getId());
        SyncVmClockMsg innerMsg = new SyncVmClockMsg();
        innerMsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, MevocoConstants.SERVICE_ID, innerMsg.getVmInstanceUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(SyncVmClockMsg msg) {
        final SyncVmClockReply reply = new SyncVmClockReply();
        if (!VmInstanceState.Running.equals(self.getState())) {
            reply.setError(operr("state of vm[uuid:%s] is not in Running state, can not sync clock"));
            bus.reply(msg, reply);
            return;
        }

        thdf.singleFlightSubmit(new SingleFlightTask(msg)
                .setSyncSignature(String.format("sync-vm-%s-clock-single-flight", msg.getUuid()))
                .run(this::syncVmClockNow)
                .done(((result) -> {
                    if (!result.isSuccess()) {
                        reply.setError(result.getErrorCode());
                    }
                    bus.reply(msg, reply);
                }))
        );
    }

    private void syncVmClockNow(ReturnValueCompletion<Object> completion) {
        MevocoKVMAgentCommands.SyncVmClockCmd cmd = new MevocoKVMAgentCommands.SyncVmClockCmd();
        cmd.setVmUuid(self.getUuid());
        String hostUuid = self.getLastHostUuid();

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(MevocoKVMConstant.SYNC_VM_CLOCK_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        kmsg.setNoStatusCheck(false);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply asyncReply= reply.castReply();
                MevocoKVMAgentCommands.SyncVmClockRsp rsp = asyncReply.toResponse(MevocoKVMAgentCommands.SyncVmClockRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr(rsp.getError()));
                    return;
                }

                completion.success(null);
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void handle(SetVmQgaSyncClockTaskMsg msg) {
        final SetVmQgaSyncClockTaskReply reply = new SetVmQgaSyncClockTaskReply();
        final ResourceConfig syncAfterResumeConfig = rcf.getResourceConfig(VmGlobalConfig.VM_CLOCK_SYNC_AFTER_VM_RESUME.getIdentity());
        final ResourceConfig intervalConfig = rcf.getResourceConfig(VmGlobalConfig.VM_CLOCK_SYNC_INTERVAL_IN_SECONDS.getIdentity());
        final String vmUuid = msg.getVmInstanceUuid();

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("set-vm-%s-qga-sync-clock", vmUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "refresh-qga-task-in-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // sync clock by QGA
                String hostUuid = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.hostUuid)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .findValue();
                if (StringUtils.isEmpty(hostUuid)) {
                    trigger.next();
                    return;
                }

                Map<String, Integer> vmIntervalMap = KvmUserVmClockSyncExtension.buildNeedSyncVmMap(hostUuid);
                final Integer intervalInSeconds = msg.getIntervalInSeconds();
                if (intervalInSeconds != null && !VmClockSyncConstant.DISABLE_VM_CLOCK_SYNC.equals(intervalInSeconds)) {
                    vmIntervalMap.put(msg.getVmInstanceUuid(), intervalInSeconds);
                } else {
                    vmIntervalMap.remove(msg.getVmInstanceUuid());
                }

                UpdateHostClockSyncVmMsg updateMsg = new UpdateHostClockSyncVmMsg();
                updateMsg.setHostUuid(hostUuid);
                // NOTE: empty interval map also need to synchronize data with the host
                updateMsg.setVmIntervalMap(vmIntervalMap);
                bus.makeTargetServiceIdByResourceUuid(updateMsg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(updateMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(reply.getError());
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-vm-qga-sync-clock-resource-config";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.getSyncAfterVMResume() != null) {
                    syncAfterResumeConfig.updateValue(vmUuid, msg.getSyncAfterVMResume().toString());
                }
                if (msg.getIntervalInSeconds() != null) {
                    intervalConfig.updateValue(vmUuid, msg.getIntervalInSeconds().toString());
                }
                trigger.next();
            }
        });

        chain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errorCode, Map data) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(APISetVmEmulatorPinningMsg msg) {
        APISetVmEmulatorPinningEvent evt = new APISetVmEmulatorPinningEvent(msg.getId());
        if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).isExists() ||Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue()==null){
            setEmulatorPinningSystemTag(msg.getEmulatorPinning());
            bus.publish(evt);
            return;
        }
        ChangeVmEmulatorPinningMsg cmsg = new ChangeVmEmulatorPinningMsg();
        cmsg.setEmulatorPinning(msg.getEmulatorPinning());
        cmsg.setUuid(msg.getUuid());
        ChangeVmEmulatorPinningReply lmsg = new ChangeVmEmulatorPinningReply();
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, cmsg.getHostUuid());
        bus.send(cmsg, new CloudBusCallBack(lmsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(err(VmErrors.NOT_IN_CORRECT_STATE, reply.getError().getDetails()).withCause(reply.getError()));
                    bus.publish(evt);
                    return;
                }
                setEmulatorPinningSystemTag(msg.getEmulatorPinning());
                bus.publish(evt);
            }
        });
    }

    private void setEmulatorPinningSystemTag(String emulatorPinningValue){
        if (Objects.equals(emulatorPinningValue, "")){
            MevocoVmSystemTags.VM_EMULATOR_PINNING.delete(self.getUuid());
        }else {
            String cpus = StringUtils.join(CpuRangeSet.originValueOf(emulatorPinningValue), ",");
            SystemTagCreator creator = MevocoVmSystemTags.VM_EMULATOR_PINNING.newSystemTagCreator(self.getUuid());
            creator.setTagByTokens(map(e(MevocoVmSystemTags.VM_EMULATOR_PINNING_TOKEN, cpus)));
            creator.recreate = true;
            creator.create();
        }
    }

    private void handle(APIGetVmEmulatorPinningMsg msg) {
        String emulatorPinning = "";
        APIGetVmEmulatorPinningReply reply = new APIGetVmEmulatorPinningReply() ;
        if (MevocoVmSystemTags.VM_EMULATOR_PINNING.hasTag(self.getUuid())){
            emulatorPinning = MevocoVmSystemTags.VM_EMULATOR_PINNING.getTokenByResourceUuid(self.getUuid(),
                    MevocoVmSystemTags.VM_EMULATOR_PINNING_TOKEN);
        }
        reply.setEmulatorPinning(emulatorPinning);
        bus.reply(msg, reply);
    }

    private void handle(APISetVmNumaMsg msg) {
        APISetVmNumaEvent evt = new APISetVmNumaEvent(msg.getId());
        SystemTagCreator creator = MevocoVmSystemTags.VM_NUMA_ENABLE.newSystemTagCreator(self.getUuid());
        if (!msg.isEnable()) {
            dbf.removeCollection(Q.New(VmInstanceNumaNodeVO.class)
                    .eq(VmInstanceNumaNodeVO_.vmUuid,msg.getUuid()).list(), VmInstanceNumaNodeVO.class);
            String clusterUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.clusterUuid)
                    .eq(VmInstanceVO_.uuid,msg.getUuid()).findValue();
            boolean isOvsDpdkSup = rcf.getResourceConfigValue(MevocoClusterGlobalConfig.OVS_DPDK_SUPPORT, clusterUuid, Boolean.class);
            if (isOvsDpdkSup) {
                ResourceConfig numaConfig = rcf.getResourceConfig(VmGlobalConfig.NUMA.getIdentity());
                numaConfig.deleteValue(msg.getUuid());
            }
            MevocoVmSystemTags.VM_NUMA_ENABLE.delete(self.getUuid());
        } else {
            boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getUuid(), Boolean.class);
            if (numa) {
                evt.setError(operr("hot plug is not turned off,can not open vm numa"));
                bus.publish(evt);
                return;
            } else {
                ResourceConfig numaConfig = rcf.getResourceConfig(VmGlobalConfig.NUMA.getIdentity());
                numaConfig.updateValue(self.getUuid(),Boolean.FALSE.toString());
            }
            creator.setTagByTokens(map(e(MevocoVmSystemTags.VM_NUMA_ENABLE_TOKEN, Boolean.TRUE.toString())));
            creator.recreate = true;
            creator.create();
        }
        bus.publish(evt);
    }


    private void handle(APIGetVmNumaMsg msg) {
        APIGetVmNumaReply reply = new APIGetVmNumaReply() ;
        reply.setEnable(MevocoVmSystemTags.VM_NUMA_ENABLE.hasTag(self.getUuid()));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVmvNUMATopologyMsg msg) {
        APIGetVmvNUMATopologyReply reply = new APIGetVmvNUMATopologyReply();

        VmInstanceVO vm = dbf.findByUuid(msg.getUuid(), VmInstanceVO.class);

        List<Map<String, Object>> topology = new ArrayList<>();

        SimpleQuery<VmInstanceNumaNodeVO> vmQuery = dbf.createQuery(VmInstanceNumaNodeVO.class);
        vmQuery.add(VmInstanceNumaNodeVO_.vmUuid, SimpleQuery.Op.EQ, msg.getUuid());
        List<VmInstanceNumaNodeVO> tuples = vmQuery.list();
        if (!tuples.isEmpty()) {
            for (VmInstanceNumaNodeVO vNode: tuples) {
                Map<String, Object> node = new HashMap<>();
                node.put("nodeID", vNode.getvNodeID());
                node.put("phyNodeID", vNode.getpNodeID());
                node.put("memSize", vNode.getvNodeMemSize());

                String CPUString = vNode.getvNodeCPUs();
                node.put("CPUsID", Arrays.asList(CPUString.split(",")));

                String distanceString = vNode.getvNodeDistance();
                node.put("distance", Arrays.asList(distanceString.split(",")));

                topology.add(node);
            }
        }

        reply.setHostUuid(vm.getHostUuid());
        reply.setName(vm.getName());
        reply.setTopology(topology);
        reply.setUuid(msg.getUuid());
        bus.reply(msg, reply);
    }


    private void handle(APIDeleteVmUserDefinedXmlMsg msg) {
        APIDeleteVmUserDefinedXmlEvent event = new APIDeleteVmUserDefinedXmlEvent(msg.getId());

        UserDefinedXmlHelper.removeUserDefinedVmXmlIfExists(msg.getVmInstanceUuid());
        bus.publish(event);
    }

    private void handle(APISetVmUserDefinedXmlMsg msg) {
        APISetVmUserDefinedXmlEvent event = new APISetVmUserDefinedXmlEvent(msg.getId());
        UserDefinedXmlHelper.removeUserDefinedVmXmlIfExists(msg.getVmInstanceUuid());

        if (UserDefinedXmlHelper.VmXmlHookScriptExists(msg.getVmInstanceUuid())) {
            throw new CloudRuntimeException("there is a xml hook script on this vm, can not set user defined xml when xml hook script exists");
        }

        try {
            DocumentHelper.parseText(new String(Base64.decodeBase64(msg.getXmlBase64())));
        } catch (DocumentException e) {
            throw new CloudRuntimeException("can not parse xml", e);
        }

        new JsonLabel().create(UserDefinedXmlHelper.getUserDefinedVmXmlLabelKey(msg.getVmInstanceUuid()), msg.getXmlBase64());
        event.setVmUserDefinedXml(new String(Base64.decodeBase64(msg.getXmlBase64())));

        bus.publish(event);
    }

    private void handle(APIGetVmXmlMsg msg) {
        APIGetVmXmlReply reply = new APIGetVmXmlReply();
        JsonLabelInventory label = UserDefinedXmlHelper.getUserDefinedVmXmlBase64(msg.getVmInstanceUuid());
        if (label != null) {
            reply.setUserDefinedXml(new String(Base64.decodeBase64(label.getLabelValue())));
        }

        VmInstanceState state = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.state).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();
        if (Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused).contains(state)) {
            //TODO(weiw) not implemented
            reply.setRunningXml(reply.getUserDefinedXml());
        }

        if (reply.getRunningXml() != null && reply.getUserDefinedXml() != null) {
            reply.setMatch(reply.getRunningXml().trim().equalsIgnoreCase(reply.getUserDefinedXml().trim()));
        }

        bus.reply(msg, reply);
    }

    private void handle(APIDeleteVmUserDefinedXmlHookScriptMsg msg) {
        APIDeleteVmUserDefinedXmlHookScriptEvent event = new APIDeleteVmUserDefinedXmlHookScriptEvent(msg.getId());

        UserDefinedXmlHookScriptHelper.removeUserDefinedVmXmlHookScriptIfExists(msg.getVmInstanceUuid());
        bus.publish(event);
    }

    private void handle(APISetVmUserDefinedXmlHookScriptMsg msg) {
        APISetVmUserDefinedXmlHookScriptEvent event = new APISetVmUserDefinedXmlHookScriptEvent(msg.getId());
        UserDefinedXmlHookScriptHelper.removeUserDefinedVmXmlHookScriptIfExists(msg.getVmInstanceUuid());

        if (UserDefinedXmlHookScriptHelper.VmUserDefinedXmlExists(msg.getVmInstanceUuid())) {
            throw new CloudRuntimeException("there is a user defined xml on this vm, can not set xml hook script when user defined xml exists");
        }

        new JsonLabel().create(UserDefinedXmlHookScriptHelper.getUserDefinedVmXmlHookScriptLabelKey(msg.getVmInstanceUuid()), msg.getXmlHookScriptBase64());
        event.setVmUserDefinedXmlHookScript(new String(Base64.decodeBase64(msg.getXmlHookScriptBase64())));

        bus.publish(event);
    }

    private void handle(APIGetVmXmlHookScriptMsg msg) {
        APIGetVmXmlHookScriptReply reply = new APIGetVmXmlHookScriptReply();
        JsonLabelInventory label = UserDefinedXmlHookScriptHelper.getUserDefinedVmXmlHookScriptBase64(msg.getVmInstanceUuid());
        if (label != null) {
            reply.setUserDefinedXmlHookScript(new String(Base64.decodeBase64(label.getLabelValue())));
        }

        bus.reply(msg, reply);
    }

    private void handle(APISetVmSecurityLevelMsg msg) {
        APISetVmSecurityLevelEvent evt = new APISetVmSecurityLevelEvent(msg.getId());

        if (msg.getSecurityLevel() != null) {
            SystemTagCreator creator = MevocoVmSystemTags.SECURITY_LEVEL.newSystemTagCreator(self.getUuid());
            creator.setTagByTokens(map(e(MevocoVmSystemTags.SECURITY_LEVEL_TOKEN, msg.getSecurityLevel())));
            creator.recreate = true;
            creator.create();
        } else {
            MevocoVmSystemTags.SECURITY_LEVEL.delete(self.getUuid());
        }

        bus.publish(evt);
    }

    private void handle(GetImageCandidatesForVmToChangeMsg msg) {
        GetImageCandidatesForVmToChangeReply reply = new GetImageCandidatesForVmToChangeReply();
        reply.setInventories(getImageCandidatesForVm(ImageConstant.ImageMediaType.RootVolumeTemplate));
        bus.reply(msg, reply);
    }

    // change vm's root volume using user choosed image
    private void doChangeVmImage(final ChangeVmImageMsg msg, final SyncTaskChain chain) {
        ChangeVmImageReply reply = new ChangeVmImageReply();
        self = dbf.reload(self);
        if (self.getState() != VmInstanceState.Stopped) {
            reply.setError(operr("vm[uuid: %s]'s state is not Stopped now, cannot operate 'changevmimage' action", self.getUuid()));
            bus.reply(msg, reply);
            chain.next();
            return;
        }
        // VmSpec to be updated
        VmInstanceInventory vmInv = VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class));
        VmInstanceSpec vmSpec = buildSpecFromInventory(vmInv, VmInstanceConstant.VmOperation.ChangeImage);

        // remove all data volumes from vmSpec
        vmSpec.getVmInventory().setAllVolumes(
                vmSpec.getVmInventory().getAllVolumes().stream().filter(
                        v -> v.getType().equals(VolumeType.Root.toString()))
                        .collect(Collectors.toList())
        );
        vmSpec.getVmInventory().setHostUuid(null);

        // use new image
        VmInstanceSpec.ImageSpec imageSpec = new VmInstanceSpec.ImageSpec();
        imageSpec.setInventory(ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class)));
        vmSpec.setImageSpec(imageSpec);

        // discard these info in vmSpec
        vmSpec.setVolumeSpecs(new ArrayList<>());
        vmSpec.setDestDataVolumes(Collections.emptyList());
        vmSpec.setDestHost(null);

        // use the same primary storage as before changing
        String psUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.uuid, vmSpec.getVmInventory().getRootVolumeUuid())
                .findValue();
        vmSpec.setRequiredPrimaryStorageUuidForRootVolume(psUuid);

        // if root volume on LocalStorage, then allocate last host again
        String psType = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .findValue();
        if (psType.equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
            String hostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, vmInv.getRootVolumeUuid())
                    .findValue();
            HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
            vmSpec.setDestHost(HostInventory.valueOf(hvo));
            vmSpec.getVmInventory().setHostUuid(hostUuid);
            vmSpec.setHostAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
        } else if (vmSpec.getDestHost() == null) {
            // randomly select host in the cluster if last host doesn't exist
            String clusterUuid = vmSpec.getVmInventory().getClusterUuid();
            if (clusterUuid == null) {
                reply.setError(operr("vm[uuid:%s] cluster uuid is null, cannot change image for it", self.getUuid()));
                bus.reply(msg, reply);
                chain.next();
                return;
            }

            HostVO host = Q.New(HostVO.class)
                    .eq(HostVO_.clusterUuid, clusterUuid)
                    .eq(HostVO_.hypervisorType, self.getHypervisorType())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .limit(1).find();
            if (host == null) {
                reply.setError(operr("vm[uuid:%s] is in cluster[uuid:%s], but there is no available host in the cluster, " +
                        "cannot change image for the vm", self.getUuid(), self.getClusterUuid()));
                bus.reply(msg, reply);
                chain.next();
                return;
            }

            vmSpec.setDestHost(HostInventory.valueOf(host));
        }

        // sync guest tools tag based on new image
        String guestTools = ImageSystemTags.IMAGE_GUEST_TOOLS.getTokenByResourceUuid(msg.getImageUuid(), ImageSystemTags.IMAGE_GUEST_TOOLS_VERSION_TOKEN);
        if (guestTools == null) {
            if (VmSystemTags.VM_GUEST_TOOLS.hasTag(msg.getVmInstanceUuid())) {
                VmSystemTags.VM_GUEST_TOOLS.delete(msg.getVmInstanceUuid());
            }
        } else {
            SystemTagCreator creator = VmSystemTags.VM_GUEST_TOOLS.newSystemTagCreator(self.getUuid());
            creator.setTagByTokens(Collections.singletonMap(VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN, guestTools));
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        }

        if (VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(vmSpec.getVmInventory().getRootVolumeUuid(), VolumeVO.class)) {
            if (vmSpec.getRootVolumeSystemTags() == null) {
                vmSpec.setRootVolumeSystemTags(new ArrayList<>());
            }
            vmSpec.getRootVolumeSystemTags().add(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.getTag(vmSpec.getVmInventory().getRootVolumeUuid(), VolumeVO.class));
        }

        FlowChain fchain = getChangeVmImageWorkFlowChain();
        fchain.setName(String.format("change-vm-%s-image-to-%s", msg.getVmInstanceUuid(), msg.getImageUuid()));
        fchain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), vmSpec);
        fchain.getData().put(VmInstanceConstant.Params.DeletionPolicy, VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct);
        fchain.getData().put("uuid", msg.getResourceUuid());
        fchain.done(new FlowDoneHandler(chain) {
            @Override
            public void handle(Map data) {
                logger.info(String.format("successfully changed vm[uuid:%s] image to %s",
                        msg.getVmInstanceUuid(), msg.getImageUuid()));
                extEmitter.cleanUpAfterVmChangeImage(vmInv);
                reply.setInventory(VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class)));
                bus.reply(msg, reply);
                chain.next();
            }
        }).error(new FlowErrorHandler(chain) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.error(String.format("failed to change vm[uuid:%s] to image %s",
                        msg.getVmInstanceUuid(), msg.getImageUuid()));
                reply.setError(errCode);
                bus.reply(msg, reply);
                chain.next();
            }
        }).start();
    }

    private void handle(ChangeVmImageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeVmImage(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("change-vm-%s-root-image", msg.getVmInstanceUuid());
            }
        });
    }

    private static class VolumeImageSpec {
        String volumeQos;
        VolumeType type;
        ImageInventory image;
        VolumeSnapshotInventory snapshot;
        int deviceId;
        Timestamp lastAttachDate;
    }

    private void handle(CloneVmInstanceMsg msg) {
        ErrorCode err = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
        if (err != null) {
            throw new OperationFailureException(err);
        }

        VmInstanceVO vivo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        handleKVMClone(msg, vivo);
    }

    private void handleKVMClone(CloneVmInstanceMsg msg, VmInstanceVO vivo) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("clone-vm-%s", msg.getVmInstanceUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            final CloneVmInstanceResults results = new CloneVmInstanceResults();
            final Set<VolumeVO> volumesToClone = msg.isFull() ? vivo.getAllDiskVolumes() : Collections.singleton(vivo.getRootVolume());
            final Map<String, VolumeImageSpec> volumeImages = new HashMap<>();
            final Map<String, Long> volumeActualSize = new ConcurrentHashMap<>();
            final Map<String, String> selectedBackupStorageUuids = new HashMap<>();
            boolean fastCreate = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch);

            @Override
            public void setup() {
                setupPreAllocateHostFlows();
                setupPrepareCreateImagesFlows();
                boolean cloneDataVolumes = volumesToClone.size() > 1;
                boolean preferSnapshotGroup = msg.isFull();

                if (cloneDataVolumes || preferSnapshotGroup) {
                    setupCreateVolumeSnapshotGroupFlows();
                } else {
                    setupCreateRootVolumeSnapshotFlows();
                }

                setupCreateVolumesImageFlows();

                setupCreateVmAndSyncTagFlows();

                if (cloneDataVolumes) {
                    setupAttachDataVolumesFlows();
                }

                if (!VmCreationStrategy.JustCreate.name().equals(msg.getStrategy())) {
                    setupSyncQosFlows();
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        CloneVmInstanceReply reply = new CloneVmInstanceReply();
                        results.getInventoriesWithoutError().forEach(cloneInv -> {
                            VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                                    .eq(VmInstanceVO_.uuid, cloneInv.getInventory().getUuid())
                                    .find();
                            cloneInv.setInventory(VmInstanceInventory.valueOf(vmInstanceVO));
                        });

                        List<ImageInventory> tempImages = volumeImages.values().stream()
                                .map(it -> it.image).filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        results.resetNumberOfCloneVm();

                        reply.setResults(results);
                        reply.setTempImages(tempImages);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CloneVmInstanceReply reply = new CloneVmInstanceReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            private void setupPreAllocateHostFlows() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "dryRun-allocate-host";

                    @Override
                    public boolean skip(Map data) {
                        return VmCreationStrategy.JustCreate.name().equals(msg.getStrategy());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
                        amsg.setHostUuid(msg.getHostUuid());
                        amsg.setClusterUuid(msg.getClusterUuid());
                        amsg.setCpuCapacity(self.getCpuNum());
                        amsg.setMemoryCapacity(self.getMemorySize());
                        amsg.setL3NetworkUuids(msg.getL3NetworkUuids());
                        amsg.setVmNicParams(msg.getVmNicParams());
                        amsg.setAllowNoL3Networks(true);
                        amsg.setVmInstance(VmInstanceInventory.valueOf(self));
                        amsg.setZoneUuid(self.getZoneUuid());
                        amsg.setDryRun(true);
                        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocateHostDryRunReply reply1 = (AllocateHostDryRunReply) reply;
                                if (reply1.getHosts().isEmpty()) {
                                    trigger.fail(err(HostAllocatorError.NO_AVAILABLE_HOST, "unable to allocate hosts, no host meets the following conditions: " +
                                                    "clusterUuid=%s hostUuid=%s cpu=%d memoryCapacity=%d L3NetworkUuids=%s",
                                            amsg.getClusterUuids(), amsg.getHostUuid(), amsg.getCpuCapacity(), amsg.getMemoryCapacity(), amsg.getL3NetworkUuids()));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });
            }

            private void setupPrepareCreateImagesFlows() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "calculate-all-volumes-actual-size";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(volumesToClone).step((volume, whileCompletion) -> {
                            SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                            smsg.setVolumeUuid(volume.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        errList.getCauses().add(reply.getError());
                                        whileCompletion.done();
                                        return;
                                    }

                                    SyncVolumeSizeReply sr = reply.castReply();
                                    volumeActualSize.put(smsg.getVolumeUuid(), sr.getActualSize());
                                    whileCompletion.done();
                                }
                            });
                        }, 3).run(new WhileDoneCompletion(trigger) {
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
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "allocate-bs";

                    @Override
                    public boolean skip(Map data) {
                        return fastCreate;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(volumesToClone).each((vol, compl) -> {
                            SelectBackupStorageMsg smsg = new SelectBackupStorageMsg();
                            smsg.setVolumeUuid(vol.getUuid());
                            smsg.setRequiredSize(volumeActualSize.getOrDefault(smsg.getVolumeUuid(), 0L));
                            smsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());

                            String requiredPsUuid = null;
                            DiskAO diskAO = msg.getDiskAOsByVolumeUuid().get(vol.getUuid());
                            if (diskAO != null && diskAO.getPrimaryStorageUuid() != null) {
                                requiredPsUuid = diskAO.getPrimaryStorageUuid();
                                smsg.setRequiredBackupStorageTypes(getRequiredBackupStorageTypes(requiredPsUuid));
                            }
                            bus.makeLocalServiceId(smsg, PrimaryStorageConstant.SERVICE_ID);
                            String finalRequiredPsUuid = requiredPsUuid;
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        compl.addError(reply.getError());
                                        compl.allDone();
                                        return;
                                    }

                                    SelectBackupStorageReply sr = reply.castReply();
                                    if (sr.getInventory() == null) {
                                        compl.addError(operr("can not find backup storage, " +
                                                "unable to commit volume snapshot[psUuid:%s] as image, " +
                                                "destination required PS uuid:%s", vol.getPrimaryStorageUuid(), finalRequiredPsUuid
                                        ));

                                        compl.allDone();
                                        return;
                                    }

                                    selectedBackupStorageUuids.put(smsg.getVolumeUuid(), sr.getInventory().getUuid());
                                    compl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });
            }

            private void setupCreateRootVolumeSnapshotFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("create-root-volume-snapshot-for-clone-vm-%s", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setVolumeUuid(vivo.getRootVolumeUuid());
                        cmsg.setName(String.format("for-clone-vm-%s", msg.getNames().get(0)));
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, vivo.getRootVolume().getUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply creply = reply.castReply();
                                handleVolumeSnapshotCreate(creply.getInventory(), 0, Timestamp.from(Instant.EPOCH));

                                CloneVmCanonicalEvents.VmInnerSnapshotCreated createdEvent = new CloneVmCanonicalEvents.VmInnerSnapshotCreated();
                                createdEvent.primaryStorageUuid = vivo.getRootVolume().getPrimaryStorageUuid();
                                createdEvent.vmInstanceUuid = vivo.getUuid();
                                createdEvent.volumeSnapshot = creply.getInventory();
                                createdEvent.fire();
                                trigger.next();
                            }
                        });
                    }
                });
            }

            private void setupCreateVolumeSnapshotGroupFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("create-volume-snapshot-group-for-clone-vm-%s", msg.getVmInstanceUuid());

                    @Override
                    public boolean skip(Map data) {
                        if (msg.getVolumeSnapshotGroup() == null) {
                            return false;
                        }

                        for (VolumeSnapshotGroupRefInventory ref : msg.getVolumeSnapshotGroup().getVolumeSnapshotRefs()) {
                            putVolumeImageSpec(VolumeSnapshotInventory.valueOf(ref), ref.getDeviceId(), ref.getVolumeLastAttachDate());
                        }
                        return true;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotGroupMsg cmsg = new CreateVolumeSnapshotGroupMsg();
                        cmsg.setRootVolumeUuid(vivo.getRootVolumeUuid());
                        cmsg.setVmInstance(VmInstanceInventory.valueOf(vivo));
                        cmsg.setConsistentType(ConsistentType.None);
                        cmsg.setName("snapshot-group-for-clone");
                        cmsg.setDescription(String.format("bulk snapshot for clone vm[%s]", vivo.getUuid()));
                        cmsg.setSession(msg.getSession());

                        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, cmsg.getVolumeUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateVolumeSnapshotGroupReply cr = reply.castReply();
                                VolumeSnapshotGroupInventory group = cr.getInventory();

                                logger.debug(String.format("created volume snapshot group %s", group.getUuid()));
                                for (VolumeSnapshotGroupRefInventory ref : group.getVolumeSnapshotRefs()) {
                                    handleVolumeSnapshotCreate(VolumeSnapshotInventory.valueOf(ref), ref.getDeviceId(), ref.getVolumeLastAttachDate());
                                }

                                CloneVmCanonicalEvents.VmInnerSnapshotCreated createdEvent = new CloneVmCanonicalEvents.VmInnerSnapshotCreated();
                                createdEvent.primaryStorageUuid = vivo.getRootVolume().getPrimaryStorageUuid();
                                createdEvent.vmInstanceUuid = vivo.getUuid();
                                createdEvent.snapshotInventoryList = group.getVolumeSnapshotRefs();
                                createdEvent.fire();

                                trigger.next();
                            }
                        });
                    }
                });
            }

            private void handleVolumeSnapshotCreate(VolumeSnapshotInventory snapshot, int deviceId, Timestamp lastAttachDate) {
                tagMgr.createNonInherentSystemTag(snapshot.getUuid(),
                        VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                        VolumeSnapshotVO.class.getSimpleName());

                putVolumeImageSpec(snapshot, deviceId, lastAttachDate);
            }

            private void putVolumeImageSpec(VolumeSnapshotInventory snapshot, int deviceId, Timestamp lastAttachDate) {
                VolumeImageSpec spec = new VolumeImageSpec();
                spec.snapshot = snapshot;
                spec.type = VolumeType.valueOf(snapshot.getVolumeType());
                spec.volumeQos = vivo.getVolume(it -> it.getUuid().equals(snapshot.getVolumeUuid())).getVolumeQos();
                spec.deviceId = deviceId;
                spec.lastAttachDate = lastAttachDate;
                volumeImages.put(snapshot.getVolumeUuid(), spec);
            }

            private void setupCreateVolumesImageFlows() {
                flow(new Flow() {
                    String __name__ = String.format("create-volumes-image-for-clone-vm-%s", msg.getVmInstanceUuid());

                    final Set<String> rollbackImageUuids = new HashSet<>();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(volumeImages.values()).all((spec, whileCompletion) -> {
                            if (spec.type == VolumeType.Data && fastCreate) {
                                whileCompletion.done();
                                return;
                            }

                            AddImageMessage cmsg = buildMsg(spec.snapshot);
                            if (cmsg.getResourceUuid() != null) {
                                rollbackImageUuids.add(cmsg.getResourceUuid());
                            }

                            bus.send((NeedReplyMessage) cmsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.done();
                                        return;
                                    }

                                    spec.image = ((ImageReply) reply).getInventory();
                                    rollbackImageUuids.add(spec.image.getUuid());
                                    tagMgr.createNonInherentSystemTag(spec.image.getUuid(),
                                            ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                            ImageVO.class.getSimpleName());

                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }

                    private AddImageMessage buildMsg(VolumeSnapshotInventory snap) {
                        AddImageMessage cmsg;
                        if (snap.getVolumeUuid().equals(vivo.getRootVolume().getUuid())) {
                            CreateRootVolumeTemplateMessage csmsg = fastCreate ?
                                    new CreateTemporaryRootVolumeTemplateFromVolumeSnapshotMsg() :
                                    new CreateRootVolumeTemplateFromVolumeSnapshotMsg();

                            csmsg.setName(String.format("for-clone-vm-%s-rootVolume-%s", vivo.getUuid(),
                                    snap.getVolumeUuid()));
                            csmsg.setPlatform(vivo.getPlatform());
                            csmsg.setArchitecture(vivo.getArchitecture());
                            csmsg.setVirtio(VmExtraInfoGetter.New(vivo.getUuid()).isVirtio());
                            csmsg.setGuestOsType(vivo.getGuestOsType());
                            cmsg = csmsg;
                        } else {
                            CreateDataVolumeTemplateFromVolumeSnapshotMsg csmsg = new CreateDataVolumeTemplateFromVolumeSnapshotMsg();
                            csmsg.setName(String.format("for-clone-vm-%s-volume-%s",
                                    vivo.getUuid(), snap.getVolumeUuid()));
                            cmsg = csmsg;
                        }

                        ((CreateTemplateFromSnapshotMessage) cmsg).setSnapshotUuid(snap.getUuid());
                        cmsg.setSession(msg.getSession());
                        cmsg.setResourceUuid(getUuid());
                        cmsg.setBackupStorageUuids(Collections.singletonList(selectedBackupStorageUuids.get(snap.getVolumeUuid())));
                        if (fastCreate) {
                            cmsg.addSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
                        }

                        bus.makeLocalServiceId((NeedReplyMessage) cmsg, ImageConstant.SERVICE_ID);
                        return cmsg;
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new While<>(rollbackImageUuids).each((imageUuid, coml) -> {
                            ImageDeletionMsg dmsg = new ImageDeletionMsg();
                            dmsg.setImageUuid(imageUuid);
                            dmsg.setForceDelete(true);
                            dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
                            bus.send(dmsg, new CloudBusCallBack(coml) {
                                @Override
                                public void run(MessageReply reply) {
                                    coml.done();
                                }
                            });

                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.rollback();
                            }
                        });
                    }
                });
            }

            private void setupCreateVmAndSyncTagFlows() {
                // both full and non-full clone will take the bellow flows
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("cloning-vm-instance-%s", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<CreateVmInstanceMsg> cmsgs = transformAndRemoveNull(msg.getNames(), this::nameToMsg);

                        setNewVmResourceUuid(cmsgs);

                        new While<>(cmsgs).all((cmsg, completion) -> {
                            bus.send(cmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    CloneVmInstanceInventory inv = new CloneVmInstanceInventory();
                                    if (reply.isSuccess()) {
                                        CreateVmInstanceReply vmreply = reply.castReply();
                                        inv.setInventory(vmreply.getInventory());
                                    } else {
                                        inv.setError(reply.getError());
                                    }
                                    results.addVmInstanceInventory(inv);
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }

                    private CreateVmInstanceMsg nameToMsg(String name) {
                        CreateVmInstanceMsg cvmsg = new CreateVmInstanceMsg();

                        cvmsg.setImageUuid(volumeImages.get(vivo.getRootVolumeUuid()).image.getUuid());
                        if (vivo.getAllocatorStrategy() != null) {
                            cvmsg.setAllocatorStrategy(vivo.getAllocatorStrategy());
                        } else {
                            cvmsg.setAllocatorStrategy(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);
                        }
                        cvmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cvmsg.setName(name);

                        cvmsg.setL3NetworkSpecs(NewVmInstanceMsgBuilder.getVmNicSpecsFromVmNicParams(msg.getVmNicParams(), msg.getL3NetworkUuids()));
                        cvmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
                        cvmsg.setClusterUuid(msg.getClusterUuid());
                        cvmsg.setHostUuid(msg.getHostUuid());
                        cvmsg.setType(vivo.getType());
                        cvmsg.setZoneUuid(vivo.getZoneUuid());
                        cvmsg.setInstanceOfferingUuid(vivo.getInstanceOfferingUuid());
                        cvmsg.setCpuNum(msg.getCpuNum() != null ? msg.getCpuNum() : vivo.getCpuNum());
                        cvmsg.setReservedMemorySize(msg.getReservedMemorySize() != null ? msg.getReservedMemorySize() : vivo.getReservedMemorySize());
                        cvmsg.setCpuSpeed(vivo.getCpuSpeed());
                        cvmsg.setMemorySize(msg.getMemorySize() != null ? msg.getMemorySize() : vivo.getMemorySize());
                        cvmsg.setDescription(msg.getDescription());
                        cvmsg.setSystemTags(msg.getSystemTags());
                        cvmsg.setPlatform(msg.getPlatform());
                        cvmsg.setGuestOsType(msg.getGuestOsType());
                        cvmsg.setArchitecture(msg.getArchitecture());
                        cvmsg.setVirtio(cvmsg.hasSystemTag(VmSystemTags.VIRTIO::isMatch));

                        if (!cvmsg.hasSystemTag(VmSystemTags.CREATE_VM_CD_ROM_LIST::isMatch)) {
                        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class)
                                .eq(VmCdRomVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                                .list();
                        if (cdRomVOS.isEmpty()) {
                            cvmsg.getSystemTags().add(VmSystemTags.CREATE_WITHOUT_CD_ROM.instantiateTag(map(e(VmSystemTags.CREATE_WITHOUT_CD_ROM_TOKEN, true))));
                        } else {
                            String[] cdRomConfigs = {
                                    VmInstanceConstant.NONE_CDROM,
                                    VmInstanceConstant.NONE_CDROM,
                                    VmInstanceConstant.NONE_CDROM,
                            };
                            for (VmCdRomVO cdRomVO : cdRomVOS) {
                                String config = cdRomVO.getIsoUuid() != null ? cdRomVO.getIsoUuid() : VmInstanceConstant.EMPTY_CDROM;
                                cdRomConfigs[cdRomVO.getDeviceId()] = config;
                            }
                            cvmsg.getSystemTags().add(VmSystemTags.CREATE_VM_CD_ROM_LIST.instantiateTag(
                                    map(e(VmSystemTags.CD_ROM_0, cdRomConfigs[0]),
                                            e(VmSystemTags.CD_ROM_1, cdRomConfigs[1]),
                                            e(VmSystemTags.CD_ROM_2, cdRomConfigs[2]))
                            ));
                        }}

                        DiskAO rootDiskAO = msg.getDiskAOsByVolumeUuid().get(vivo.getRootVolumeUuid());
                        cvmsg.setRootVolumeSystemTags(rootDiskAO != null ? rootDiskAO.getSystemTags() : null);
                        cvmsg.setRootDisk(rootDiskAO);

                        if (msg.isFull() || fastCreate) {
                            cvmsg.setPrimaryStorageUuidForRootVolume(vivo.getRootVolume().getPrimaryStorageUuid());
                        }
                        if (rootDiskAO != null && rootDiskAO.getPrimaryStorageUuid() != null) {
                            cvmsg.setPrimaryStorageUuidForRootVolume(rootDiskAO.getPrimaryStorageUuid());
                        }

                        cvmsg.setVmCustomSpecification(msg.getVmCustomSpecification());
                        cvmsg.setStrategy(VmCreationStrategy.JustCreate.toString());
                        bus.makeLocalServiceId(cvmsg, VmInstanceConstant.SERVICE_ID);
                        return cvmsg;
                    }

                    private void setNewVmResourceUuid(List<CreateVmInstanceMsg> cmsgs) {
                        if (msg.getResourceUuidByName().isEmpty()) {
                            return;
                        }
                        cmsgs.forEach(cmsg -> {
                            if (msg.getResourceUuidByName().containsKey(cmsg.getName())) {
                                cmsg.setResourceUuid(msg.getResourceUuidByName().get(cmsg.getName()));
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("clone-ssh-key-pairs-from-origin-vm-%s-before-start", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceInventory origin = VmInstanceInventory.valueOf(vivo);
                        for (CloneVmInstanceInventory inventory : results.getInventoriesWithoutError()) {
                            VmInstanceInventory destVm = inventory.getInventory();

                            extEmitter.cloneSshKeyPairsToVm(origin.getUuid(), destVm.getUuid());
                        }
                        trigger.next();
                    }
                });

                final boolean needRegisterNvRam = vmTpmManager.needRegisterNvRam(vivo.getUuid());
                flow(new NoRollbackFlow() {
                    String __name__ = "clone-vm-tpm-and-encryption-key";

                    @Override
                    public boolean skip(Map data) {
                        return !needRegisterNvRam;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloneVmTpmMsg cloneMsg = new CloneVmTpmMsg();
                        cloneMsg.setSrcVmUuid(vivo.getUuid());
                        cloneMsg.setDstVmUuidList(transform(results.getInventoriesWithoutError(), inventory -> inventory.getInventory().getUuid()));
                        cloneMsg.setResetTpm(msg.getResetTpm());
                        bus.makeLocalServiceId(cloneMsg, TpmConstants.SERVICE_ID);
                        bus.send(cloneMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "clone-vm-host-files-includes-tpm-and-nvram";

                    @Override
                    public boolean skip(Map data) {
                        return !needRegisterNvRam;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloneVmHostFileMsg cloneMsg = new CloneVmHostFileMsg();
                        cloneMsg.setSrcVmUuid(vivo.getUuid());
                        cloneMsg.setDstVmUuidList(transform(results.getInventoriesWithoutError(), inventory -> inventory.getInventory().getUuid()));
                        cloneMsg.setResetTpm(msg.getResetTpm());
                        bus.makeLocalServiceId(cloneMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                        bus.send(cloneMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<String> backupUuidList = Q.New(VmHostBackupFileVO.class)
                                .in(VmHostBackupFileVO_.resourceUuid,
                                        transform(results.getInventoriesWithoutError(), inventory -> inventory.getInventory().getUuid()))
                                .select(VmHostBackupFileVO_.uuid)
                                .listValues();
                        if (backupUuidList.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        new While<>(backupUuidList).each((fileUuid, whileCompletion) -> {
                            VmHostBackupFileDeletionMsg deletionMsg = new VmHostBackupFileDeletionMsg();
                            deletionMsg.setUuid(fileUuid);
                            deletionMsg.setForceDelete(true);
                            bus.makeLocalServiceId(deletionMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                            bus.send(deletionMsg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        whileCompletion.done();
                                        return;
                                    }
                                    whileCompletion.addError(reply.getError());
                                    whileCompletion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(null) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.hasError()) {
                                    logger.warn("failed to delete some VmHostBackupFiles:\n" + String.join("\n",
                                            transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails)));
                                }
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start-vm";

                    @Override
                    public boolean skip(Map data) {
                        return VmCreationStrategy.JustCreate.name().equals(msg.getStrategy());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(results.getInventoriesWithoutError()).all((inv, completion) -> {
                            StartVmInstanceMsg startVmInstanceMsg = new StartVmInstanceMsg();
                            startVmInstanceMsg.setAccountUuid(msg.getSession().getAccountUuid());
                            startVmInstanceMsg.setVmInstanceUuid(inv.getInventory().getUuid());
                            startVmInstanceMsg.setStartPaused(VmCreationStrategy.CreateStopped.name().equals(msg.getStrategy()) ||
                                    VmCreationStrategy.CreatedPaused.name().equals(msg.getStrategy()));
                            bus.makeTargetServiceIdByResourceUuid(startVmInstanceMsg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                            bus.send(startVmInstanceMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.debug(String.format("vm %s start failed", startVmInstanceMsg.getVmInstanceUuid()));
                                        inv.setError(reply.getError());
                                        completion.done();
                                        return;
                                    }

                                    StartVmInstanceReply reply1 = reply.castReply();
                                    inv.setInventory(reply1.getInventory());
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });
            }

            private void setupAttachDataVolumesFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("stop-cloning-vm-%s-for-attach-volume", msg.getVmInstanceUuid());

                    @Override
                    public boolean skip(Map data) {
                        return !ImagePlatform.Other.name().equals(volumeImages.get(vivo.getRootVolumeUuid()).image.getPlatform());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(results.getInventoriesWithoutError()).all((inv, completion) -> {
                            String vmUuid = inv.getInventory().getUuid();

                            StopVmInstanceMsg stopMsg = new StopVmInstanceMsg();
                            stopMsg.setVmInstanceUuid(vmUuid);
                            stopMsg.setGcOnFailure(true);
                            stopMsg.setType(StopVmType.cold.toString());
                            stopMsg.setStopHA(true);
                            bus.makeTargetServiceIdByResourceUuid(stopMsg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                            bus.send(stopMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.debug(String.format("vm %s stop failed", inv.getInventory().getUuid()));
                                        inv.setError(reply.getError());
                                        completion.done();
                                        return;
                                    }

                                    StopVmInstanceReply reply1 = reply.castReply();
                                    inv.setInventory(reply1.getInventory());
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("attach-data-volumes-for-cloning-vm-%s", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        int maxDeviceId = Collections.max(volumeImages.values(),
                                Comparator.comparing(it -> it.deviceId)).deviceId;
                        Map<String, String> newVolumeByOriginVolume = new HashMap<>();

                        new While<>(results.getInventoriesWithoutError()).each((vmInstanceInventory, whileCompleteion) -> {
                            logger.debug("volumeImages here: " + JSONObjectUtil.toJsonString(volumeImages));

                            VolumeInventory[] dataVolumesIndexByDeviceId = new VolumeInventory[maxDeviceId + 1];
                            List<VolumeImageSpec> dataVolImageSpecs = volumeImages.values().stream()
                                    .filter(it -> it.type == VolumeType.Data)
                                    .collect(Collectors.toList());

                            new While<>(dataVolImageSpecs).all((spec, whileCompleteion1) -> {
                                NeedReplyMessage cmsg = buildCreateDataVolMsg(spec, vmInstanceInventory.getInventory());
                                bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                                bus.send(cmsg, new CloudBusCallBack(whileCompleteion1) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            errList.getCauses().add(reply.getError());
                                            whileCompleteion1.done();
                                            return;
                                        }

                                        CreateDataVolumeReply cr = reply.castReply();
                                        if (spec.volumeQos != null) {
                                            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, cr.getInventory().getUuid())
                                                    .set(VolumeVO_.volumeQos, spec.volumeQos)
                                                    .update();
                                        }

                                        dataVolumesIndexByDeviceId[spec.deviceId] = cr.getInventory();
                                        newVolumeByOriginVolume.put(spec.snapshot.getVolumeUuid(), cr.getInventory().getUuid());
                                        whileCompleteion1.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(whileCompleteion) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (!errList.getCauses().isEmpty()) {
                                        whileCompleteion.done();
                                        return;
                                    }

                                    List<VolumeInventory> dataVolumes = Arrays.stream(dataVolumesIndexByDeviceId)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                    new While<>(dataVolumes).each((dataVolume, whileCompleteion2) -> {
                                        AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                                        amsg.setVolume(dataVolume);
                                        amsg.setVmInstanceUuid(vmInstanceInventory.getInventory().getUuid());
                                        bus.makeLocalServiceId(amsg, VmInstanceConstant.SERVICE_ID);
                                        bus.send(amsg, new CloudBusCallBack(whileCompleteion2) {
                                            @Override
                                            public void run(MessageReply reply) {
                                                if (!reply.isSuccess()) {
                                                    errList.getCauses().add(reply.getError());
                                                }

                                                whileCompleteion2.done();
                                            }
                                        });
                                    }).run(new WhileDoneCompletion(whileCompleteion) {
                                        @Override
                                        public void done(ErrorCodeList errorCodeList) {
                                            updateVolumeLastAttachDate();
                                            whileCompleteion.done();
                                        }
                                    });
                                }

                                private void updateVolumeLastAttachDate() {
                                    new SQLBatch() {
                                        @Override
                                        protected void scripts() {
                                            List<VolumeImageSpec> originVolumeOrderList = dataVolImageSpecs.stream()
                                                    .sorted(Comparator.comparing(it -> it.lastAttachDate))
                                                    .collect(Collectors.toList());
                                            LocalDateTime now = LocalDateTime.now();
                                            for (int count = originVolumeOrderList.size(); count > 0; count--) {
                                                String originVolumeUuid = originVolumeOrderList.get(originVolumeOrderList.size() - count).snapshot.getVolumeUuid();
                                                sql(VolumeVO.class).eq(VolumeVO_.uuid, newVolumeByOriginVolume.get(originVolumeUuid))
                                                        .set(VolumeVO_.lastAttachDate, Timestamp.valueOf(now.plusSeconds(-count))).update();
                                            }
                                        }
                                    }.execute();
                                }
                            });

                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(errList.getCauses().get(0));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    private NeedReplyMessage buildCreateDataVolMsg(VolumeImageSpec spec, VmInstanceInventory vm) {
                        VolumeVO originVol = vivo.getVolume(it -> it.getUuid().equals(spec.snapshot.getVolumeUuid()));
                        DiskAO diskAO = msg.getDiskAOsByVolumeUuid().get(spec.snapshot.getVolumeUuid());
                        if (fastCreate) {
                            CreateDataVolumeFromVolumeSnapshotMsg cmsg = new CreateDataVolumeFromVolumeSnapshotMsg();
                            cmsg.setName(originVol.getName() + "-clone");
                            cmsg.setDescription(String.format("for-clone-vm-%s-volume-%s", vivo.getUuid(),
                                    spec.snapshot.getVolumeUuid()));
                            cmsg.setVolumeSnapshotUuid(spec.snapshot.getUuid());
                            cmsg.setSystemTags(diskAO != null ? diskAO.getSystemTags() : null);
                            cmsg.addSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
                            cmsg.setSession(msg.getSession());
                            if (diskAO != null && diskAO.getSize() != 0) {
                                cmsg.setSize(diskAO.getSize());
                            }
                            return cmsg;
                        } else {
                            CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                            cmsg.setResourceUuid(getUuid());
                            cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                            cmsg.setHostUuid(vm.getHostUuid() != null ? vm.getHostUuid() : vm.getLastHostUuid());
                            cmsg.setImageUuid(spec.image.getUuid());
                            cmsg.setName(originVol.getName() + "-clone");
                            cmsg.setDescription(String.format("for-clone-vm-%s-volume-%s", vivo.getUuid(),
                                    spec.snapshot.getVolumeUuid()));
                            if (diskAO != null) {
                                cmsg.setSystemTags(diskAO.getSystemTags() != null ? diskAO.getSystemTags() : null);
                                cmsg.setPrimaryStorageUuid(diskAO.getPrimaryStorageUuid() != null ?
                                        diskAO.getPrimaryStorageUuid() : originVol.getPrimaryStorageUuid());
                            }
                            return cmsg;
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("start-cloning-vm-%s-after-attach-volume", msg.getVmInstanceUuid());

                    @Override
                    public boolean skip(Map data) {
                        return !ImagePlatform.Other.name().equals(volumeImages.get(vivo.getRootVolumeUuid()).image.getPlatform())
                                || VmCreationStrategy.CreateStopped.toString().equals(msg.getStrategy());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(results.getInventoriesWithoutError()).all((inv, completion) -> {
                            StartVmInstanceMsg startVmInstanceMsg = new StartVmInstanceMsg();
                            startVmInstanceMsg.setAccountUuid(msg.getSession().getAccountUuid());
                            startVmInstanceMsg.setVmInstanceUuid(inv.getInventory().getUuid());
                            startVmInstanceMsg.setStartPaused(VmCreationStrategy.CreateStopped.name().equals(msg.getStrategy()) ||
                                    VmCreationStrategy.CreatedPaused.name().equals(msg.getStrategy()));
                            bus.makeTargetServiceIdByResourceUuid(startVmInstanceMsg, VmInstanceConstant.SERVICE_ID, inv.getInventory().getUuid());
                            bus.send(startVmInstanceMsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.debug(String.format("vm %s start failed", startVmInstanceMsg.getVmInstanceUuid()));
                                        inv.setError(reply.getError());
                                        completion.done();
                                        return;
                                    }

                                    StartVmInstanceReply reply1 = reply.castReply();
                                    inv.setInventory(reply1.getInventory());
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });
            }

            private void setupSyncQosFlows() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-qos-from-origin-vm-%s", msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceInventory origin = VmInstanceInventory.valueOf(dbf.reload(vivo));
                        new While<>(results.getInventoriesWithoutError()).all((inventory, completion) -> {
                            CloneVmSyncQosMsg cvmsg = new CloneVmSyncQosMsg();
                            cvmsg.setSrcVm(origin);
                            cvmsg.setDstVmUuid(inventory.getInventory().getUuid());
                            cvmsg.setFull(msg.isFull());
                            bus.makeLocalServiceId(cvmsg, VmInstanceConstant.SERVICE_ID);
                            bus.send(cvmsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        inventory.setError(reply.getError());
                                    }
                                    completion.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private List<String> getRequiredBackupStorageTypes(String psUuid) {
        String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, psUuid)
                .select(PrimaryStorageVO_.type)
                .findValue();
        return hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(psType);
    }

    // copy SystemTags from rootVolume/Nic which attached on origin vm

    /**
     * Step 1: find qos on rootVolume or Nic(notice: nics, not nic), copy it
     * Step 2: delete qos on vm(if user delete it, but clone will take it with instanceOffering, we need drop it)
     * Step 3: find qos on origin vm, copy it
     * Step 4: Anyway, sync it on kvm
     * @param msg
     */
    protected void handle(final CloneVmSyncQosMsg msg) {
        Map<String, Long> inbound = new HashMap<>();
        Map<String, Long> outbound = new HashMap<>();
        VmInstanceInventory destVm = VmInstanceInventory.valueOf(self);
        VmInstanceInventory srcVm = msg.getSrcVm();
        MessageReply reply = new MessageReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("copy-system-tags-from-old-vm-%s-and-sync", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "find-qos-on-volumes-or-nics";
                    @Override
                    public void run(FlowTrigger trigger, Map data){
                        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(srcVm.getType());
                        for (VmNicInventory nic: srcVm.getVmNics()) {
                            VmNicQosStruct struct = backend.getNicQos(srcVm.getUuid(), nic.getUuid());
                            if (struct.inboundBandwidth != -1L || struct.outboundBandwidth != -1L) {
                                // make sure copy systemTag to the same nick(same l3)
                                for (VmNicInventory vmni: destVm.getVmNics()) {
                                    if (vmni.getL3NetworkUuid().equals(nic.getL3NetworkUuid())) {
                                        backend.addNicQos(destVm.getUuid(), vmni.getUuid(), struct.outboundBandwidth, struct.inboundBandwidth);
                                        if (struct.inboundBandwidth != -1L) {
                                            inbound.put(vmni.getUuid(), struct.inboundBandwidth);
                                        }
                                        if (struct.outboundBandwidth != -1L) {
                                            outbound.put(vmni.getUuid(), struct.outboundBandwidth);
                                        }

                                        break;
                                    }
                                }
                            }
                        }

                        List<VolumeInventory> srcVols = srcVm.getAllDiskVolumes();
                        srcVols.sort(Comparator.comparing(VolumeInventory::getDeviceId));

                        List<VolumeInventory> dstVols = destVm.getAllDiskVolumes();
                        dstVols.sort(Comparator.comparing(VolumeInventory::getDeviceId));

                        for (VolumeInventory dstVol : dstVols) {
                            int index = dstVols.indexOf(dstVol);
                            VolumeInventory srcVol = srcVols.get(index);
                            String vUpThresholdQos = VolumeQosHelper.copyUpThresholdQosTagFromVolume(srcVol.getUuid(), dstVol.getUuid());
                            String iopsUpThresholdQos = VolumeQosHelper.copyUpThresholdIopsTagFromVolume(srcVol.getUuid(),
                                    dstVol.getUuid());
                            if (vUpThresholdQos == null && srcVol.getVolumeQos() == null) {
                                continue;
                            }

                            VolumeVO vvo = dbf.findByUuid(dstVol.getUuid(), VolumeVO.class);
                            String qos = srcVol.getVolumeQos() != null ? srcVol.getVolumeQos() :
                                    VolumeQosHelper.getVolumeQosString(VolumeQosHelper.getVolumeQosFromSystemTag(vUpThresholdQos, iopsUpThresholdQos));
                            vvo.setVolumeQos(qos);
                            dbf.updateAndRefresh(vvo);
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-qos-on-vm";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(srcVm.getType());
                        VmNicQosStruct struct = backend.getVmQos(srcVm.getUuid());
                        backend.deleteVmQos(destVm.getUuid(), "in");
                        backend.deleteVmQos(destVm.getUuid(), "out");
                        backend.addVmQos(destVm.getUuid(), struct.outboundBandwidthUpthreshold, struct.inboundBandwidthUpthreshold);

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {

                    @Override
                    public boolean skip(Map data) {
                        return destVm.getState().equals(VmInstanceState.Stopped.toString());
                    }

                    String __name__ = "sync-qos-on-volumes";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceInventory reloadVm = VmInstanceInventory.valueOf(dbf.findByUuid(destVm.getUuid(), VmInstanceVO.class));

                        ErrorCodeList errList = new ErrorCodeList();

                        new While<>(reloadVm.getAllDiskVolumes()).all((volume, coml) -> {
                            if (volume.getVolumeQos() == null || volume.getVolumeQos().isEmpty()) {
                                coml.done();
                                return;
                            }
                            SetVolumeQosOnKVMHostMsg hmsg = new SetVolumeQosOnKVMHostMsg();
                            VolumeQos qos = VolumeQosHelper.getVolumeQos(volume.getVolumeQos());

                            if (!VolumeQosHelper.hasQosLimit(qos)) {
                                coml.done();
                                return;
                            }

                            hmsg.setInstallPath(volume.getInstallPath());
                            hmsg.setVmUuid(volume.getVmInstanceUuid());
                            hmsg.setHostUuid(destVm.getHostUuid());
                            hmsg.setMode(OVERWRITE.getMode());
                            hmsg.setVolume(volume);
                            hmsg.setMsgQos(qos);

                            bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
                            bus.send(hmsg, new CloudBusCallBack(msg) {
                                @Override
                                public void run(MessageReply reply) {
                                    coml.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
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
                });

                flow(new NoRollbackFlow() {

                    @Override
                    public boolean skip(Map data) {
                        return destVm.getState().equals(VmInstanceState.Stopped.toString());
                    }

                    String __name__ = "sync-it-on-nic";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(srcVm.getType());

                        if (CollectionUtils.isEmpty(destVm.getVmNics())) {
                            trigger.next();
                        }

                        for (VmNicInventory vmni: destVm.getVmNics()) {
                            VmNicQosStruct struct = backend.getNicQos(srcVm.getUuid(), vmni.getUuid());
                            VmNicVO vmNicVO = dbf.findByUuid(vmni.getUuid(), VmNicVO.class);
                            fireVmNicQosChangedEvent(vmNicVO);

                            SetNicQosOnKVMHostMsg hmsg2 = new SetNicQosOnKVMHostMsg();
                            hmsg2.setInternalName(vmni.getInternalName());
                            Long in = struct.inboundBandwidth;
                            Long out = struct.outboundBandwidth;
                            hmsg2.setInboundBandwidth(in);
                            hmsg2.setOutboundBandwidth(out);
                            hmsg2.setVmUuid(destVm.getUuid());
                            hmsg2.setHostUuid(destVm.getHostUuid());

                            bus.makeTargetServiceIdByResourceUuid(hmsg2, HostConstant.SERVICE_ID, hmsg2.getHostUuid());
                            bus.send(hmsg2, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply rpl) {
                                    if (!rpl.isSuccess()) {
                                        logger.warn(String.format("sync qos on vmNic[%s] qos failed, due to: %s",
                                                vmni.getUuid(), rpl.getError().getDetails()));
                                        trigger.fail(rpl.getError());
                                    }else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    }
                });
                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
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

    private void setVmQga(final APISetVmQgaMsg msg, Completion completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.then(new Flow() {
            String __name__ = "set-vm-qga";

            final String originQgaTag = VmSystemTags.VM_INJECT_QEMUGA.getTokenByResourceUuid(msg.getUuid(), VmSystemTags.VM_INJECT_QEMUGA_TOKEN);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!msg.getEnable()) {
                    VmSystemTags.VM_INJECT_QEMUGA.delete(msg.getUuid());
                }else{
                    SystemTagCreator creator = VmSystemTags.VM_INJECT_QEMUGA.newSystemTagCreator(msg.getUuid());
                    creator.inherent = false;
                    creator.recreate = true;
                    creator.create();
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (!StringUtils.isEmpty(originQgaTag) && !msg.getEnable())  {
                    SystemTagCreator creator = VmSystemTags.VM_INJECT_QEMUGA.newSystemTagCreator(msg.getUuid());
                    creator.inherent = false;
                    creator.recreate = true;
                    creator.create();
                } else {
                    VmSystemTags.VM_INJECT_QEMUGA.delete(msg.getUuid());
                }

                trigger.rollback();
            }
        });
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(getSelfInventory());
        spec.setCurrentVmOperation(VmInstanceConstant.VmOperation.SetVmQga);
        setAdditionalFlow(chain, spec);

        chain.error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void handle(final APISetVmQgaMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("set-vm-%s-qga", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APISetVmQgaEvent evt = new APISetVmQgaEvent(msg.getId());
                setVmQga(msg, new Completion(chain) {
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
                return getSyncSignature();
            }
        });
    }

    protected void handle(final APIGetVmQgaMsg msg) {
        APIGetVmQgaReply reply = new APIGetVmQgaReply();
        String qemuga = VmSystemTags.VM_INJECT_QEMUGA.getTag(msg.getUuid());
        if (qemuga == null) {
            reply.setEnable(false);
        } else {
            reply.setEnable(true);
        }
        bus.reply(msg, reply);
    }

    private void setNicQosSystemTag(final Long outboundBandWidth, final Long inboundBandWidth, String nicUuid) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
        backend.addNicQos(self.getUuid(), nicUuid, outboundBandWidth, inboundBandWidth);
        VmNicVO vmNicVO = dbf.findByUuid(nicUuid, VmNicVO.class);
        fireVmNicQosChangedEvent(vmNicVO);
    }

    private void deleteNicQosSystemTag(final APIDeleteNicQosMsg msg, String vmUuid) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
        backend.deleteNicQos(vmUuid, msg.getUuid(), msg.getDirection());

        VmNicVO vmNicVO = dbf.findByUuid(msg.getUuid(), VmNicVO.class);
        fireVmNicQosChangedEvent(vmNicVO);
    }

    protected void handle(final APIDeleteNicQosMsg msg) {
        APIDeleteNicQosEvent evt = new APIDeleteNicQosEvent(msg.getId());
        VmNicVO nvo = dbf.findByUuid(msg.getUuid(), VmNicVO.class);

        if (nvo.getVmInstanceUuid() == null) {
            logger.debug("exit here: nvo.getVmInstanceUuid() == null");
            deleteNicQosSystemTag(msg, null);
            bus.publish(evt);
            return;
        }

        /*no primate to delete the Qos when the offering with Qos miao zhanyong*/
        VmInstanceVO ivo = dbf.findByUuid(nvo.getVmInstanceUuid(), VmInstanceVO.class);
        if (!acntMgr.isAdmin(msg.getSession())) {
            VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
            VmNicQosStruct struct = backend.getVmQos(self.getUuid());
            if (struct.inboundBandwidthUpthreshold != null || struct.outboundBandwidthUpthreshold != null) {

                //evt.setSuccess(false);
                //evt.setError(operr("No privilege to delete NicQos %s.", msg.getUuid()));
                bus.publish(evt);
                logger.debug(String.format("DeleteNicQos [%s] ignored because of account privilege.", msg.getUuid()));
                return;
            }
        }

        if (ivo.getHostUuid() == null
                || (ivo.getState() != VmInstanceState.Running && ivo.getState() != VmInstanceState.Paused)) {
            deleteNicQosSystemTag(msg, ivo.getUuid());
            bus.publish(evt);
        } else {
            SetNicQosOnKVMHostMsg hmsg = new SetNicQosOnKVMHostMsg();
            hmsg.setInternalName(nvo.getInternalName());
            if (msg.getDirection().equals("in")) {
                hmsg.setInboundBandwidth(Long.valueOf("0"));
            } else if (msg.getDirection().equals("out")) {
                hmsg.setOutboundBandwidth(Long.valueOf("0"));
            } else {
                evt.setSuccess(false);
                evt.setError(argerr("direction must be set to in or out"));
                bus.publish(evt);
                return;
            }
            hmsg.setVmUuid(nvo.getVmInstanceUuid());
            hmsg.setHostUuid(ivo.getHostUuid());

            bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
            bus.send(hmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        evt.setSuccess(false);
                        evt.setError(reply.getError());
                    } else {
                        deleteNicQosSystemTag(msg, ivo.getUuid());
                    }
                    bus.publish(evt);
                }
            });
        }
    }

    protected void handle(final SetVmNicQosMsg msg) {
        SetVmNicQosReply reply = new SetVmNicQosReply();
        VmNicVO nvo = dbf.findByUuid(msg.getUuid(), VmNicVO.class);

        if (nvo.getVmInstanceUuid() == null) {
            setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
            bus.reply(msg, reply);
            return;
        }
        VmInstanceVO ivo = self;

        if (ivo.getHostUuid() == null
                || (ivo.getState() != VmInstanceState.Running && ivo.getState() != VmInstanceState.Paused)) {
            setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
            bus.reply(msg, reply);
        } else {
            SetNicQosOnKVMHostMsg hmsg = new SetNicQosOnKVMHostMsg();
            hmsg.setInternalName(nvo.getInternalName());
            hmsg.setInboundBandwidth(msg.getInboundBandwidth());
            hmsg.setOutboundBandwidth(msg.getOutboundBandwidth());
            hmsg.setVmUuid(nvo.getVmInstanceUuid());
            hmsg.setHostUuid(ivo.getHostUuid());

            bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
            bus.send(hmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        reply.setError(reply.getError());
                    } else {
                        setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
                    }
                    bus.reply(msg, reply);
                }
            });
        }
    }

    protected void handle(final APISetNicQosMsg msg) {
        APISetNicQosEvent evt = new APISetNicQosEvent(msg.getId());
        VmNicVO nvo = dbf.findByUuid(msg.getUuid(), VmNicVO.class);

        if (nvo.getVmInstanceUuid() == null) {
            setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
            bus.publish(evt);
            return;
        }
        VmInstanceVO ivo = self;

        if (!acntMgr.isAdmin(msg.getSession())) {
            VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
            VmNicQosStruct struct = backend.getVmQos(self.getUuid());
            if (msg.getInboundBandwidth() != null) {
                //get the bandwidth from vmInstance's tag copy from instanceOffering during creating vm
                if (struct.inboundBandwidthUpthreshold != null) {
                    if (struct.inboundBandwidthUpthreshold < msg.getInboundBandwidth()) {
                        evt.setSuccess(false);
                        evt.setError(argerr("inboundBandwidth must be set no more than %s.", struct.inboundBandwidthUpthreshold));
                        bus.publish(evt);
                        return;
                    }
                }
            }
            if (msg.getOutboundBandwidth() != null) {
                if (struct.outboundBandwidthUpthreshold != null) {
                    if (struct.outboundBandwidthUpthreshold < msg.getOutboundBandwidth()) {
                        evt.setSuccess(false);
                        evt.setError(argerr("outboundBandwidth must be set no more than %s.", struct.outboundBandwidthUpthreshold));
                        bus.publish(evt);
                        return;
                    }
                }
            }
        }
        if (ivo.getHostUuid() == null
                || (ivo.getState() != VmInstanceState.Running && ivo.getState() != VmInstanceState.Paused)) {
            setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
            bus.publish(evt);
        } else {
            SetNicQosOnKVMHostMsg hmsg = new SetNicQosOnKVMHostMsg();
            hmsg.setInternalName(nvo.getInternalName());
            hmsg.setInboundBandwidth(msg.getInboundBandwidth());
            hmsg.setOutboundBandwidth(msg.getOutboundBandwidth());
            hmsg.setVmUuid(nvo.getVmInstanceUuid());
            hmsg.setHostUuid(ivo.getHostUuid());

            bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
            bus.send(hmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        evt.setSuccess(false);
                        evt.setError(reply.getError());
                    } else {
                        setNicQosSystemTag(msg.getOutboundBandwidth(), msg.getInboundBandwidth(), msg.getUuid());
                    }
                    bus.publish(evt);
                }
            });
        }
    }

    private void fireVmNicQosChangedEvent(VmNicVO vmNicVO) {
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
        VmNicQosStruct struct = backend.getNicQos(self.getUuid(), vmNicVO.getUuid());
        VmInstanceVO vmInstanceVO = self;

        VmNicQosCanonicalEvents.VmNicQosEventData vmNicQosEventData = new VmNicQosCanonicalEvents.VmNicQosEventData();
        vmNicQosEventData.setInventory(VmNicInventory.valueOf(vmNicVO));
        vmNicQosEventData.setCurrentStatus(vmInstanceVO.getState().toString());
        vmNicQosEventData.setDate(new Date());
        Long inbound = struct.inboundBandwidth;
        Long outbound = struct.outboundBandwidth;
        if (outbound != null) {
            vmNicQosEventData.setBandwidthOut(outbound);
        }
        if (inbound != null) {
            vmNicQosEventData.setBandwidthIn(inbound);
        }
        evtf.fire(VmNicQosCanonicalEvents.VM_NIC_QOS_CHANGE_PATH, vmNicQosEventData);
    }

    private void getNiqQosOnHost(String nicUuid, final ReturnValueCompletion<GetNicQosOnKVMHostReply> completion) {
        if (self.getState() != VmInstanceState.Running && self.getState() != VmInstanceState.Paused) {
            completion.fail(operr("vm [%s]' state must be Running or Paused to sync nic qos", self.getUuid()));
            return;
        }

        if (self.getHostUuid() == null) {
            completion.fail(operr("vm [%s]'s HostUuid is null, cannot sync nic qos"));
            return;
        }

        VmNicVO nvo = dbf.findByUuid(nicUuid, VmNicVO.class);
        GetNicQosOnKVMHostMsg gmsg = new GetNicQosOnKVMHostMsg();
        gmsg.setInternalName(nvo.getInternalName());
        gmsg.setVmUuid(self.getUuid());
        gmsg.setHostUuid(self.getHostUuid());

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, self.getHostUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    GetNicQosOnKVMHostReply r = reply.castReply();
                    completion.success(r);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    protected void handle(final GetVmNicQosMsg msg) {
        GetVmNicQosReply reply = new GetVmNicQosReply();
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
        VmNicQosStruct struct = backend.getNicQos(self.getUuid(), msg.getUuid());

        if (msg.getForceSync()) {
            getNiqQosOnHost(msg.getUuid(), new ReturnValueCompletion<GetNicQosOnKVMHostReply>(msg) {
                @Override
                public void success(GetNicQosOnKVMHostReply r) {
                    if (r.getInbound() != null) {
                        reply.setInboundBandwidth(r.getInbound());
                    }

                    if (r.getOutbound() != null) {
                        reply.setOutboundBandwidth(r.getOutbound());
                    }

                    setNicQosSystemTag(r.getOutbound(), r.getInbound(), msg.getUuid());
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            reply.setInboundBandwidth(struct.inboundBandwidth);
            reply.setOutboundBandwidth(struct.outboundBandwidth);
            bus.reply(msg, reply);
        }
    }

    protected void handle(final APIGetNicQosMsg msg) {
        APIGetNicQosReply reply = new APIGetNicQosReply();

        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(self.getType());
        VmNicQosStruct struct = backend.getNicQos(self.getUuid(), msg.getUuid());

        if (!acntMgr.isAdmin(msg.getSession())) {
            if (struct.inboundBandwidthUpthreshold != null && struct.inboundBandwidthUpthreshold != -1L) {
                reply.setInboundBandwidthUpthreshold(struct.inboundBandwidthUpthreshold);
            }
            if (struct.outboundBandwidthUpthreshold != null && struct.outboundBandwidthUpthreshold != -1L) {
                reply.setOutboundBandwidthUpthreshold(struct.outboundBandwidthUpthreshold);
            }
        }

        if (msg.getForceSync()) {
            getNiqQosOnHost(msg.getUuid(), new ReturnValueCompletion<GetNicQosOnKVMHostReply>(msg){
                @Override
                public void success(GetNicQosOnKVMHostReply r) {
                    if (r.getInbound() != null) {
                        reply.setInboundBandwidth(r.getInbound());
                    }

                    if (r.getOutbound() != null) {
                        reply.setOutboundBandwidth(r.getOutbound());
                    }

                    setNicQosSystemTag(r.getOutbound(), r.getInbound(), msg.getUuid());
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            reply.setInboundBandwidth(struct.inboundBandwidth);
            reply.setOutboundBandwidth(struct.outboundBandwidth);
            bus.reply(msg, reply);
        }
    }

    private void handle(final ChangeVmPasswordMsg msg) {
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            bus.replyErrorByMessageType(msg, allowed);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                ChangeVmPasswordReply reply = new ChangeVmPasswordReply();
                changepasswd(msg, new Completion(chain) {
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
                return String.format("change-vm-password-%s", self.getUuid());
            }
        });
    }

    protected void handle(final APIChangeVmPasswordMsg msg) {
        APIChangeVmPasswordEvent evt = new APIChangeVmPasswordEvent(msg.getId());

        ChangeVmPasswordMsg cmsg = new ChangeVmPasswordMsg();
        cmsg.setAccount(msg.getAccount());
        cmsg.setPassword(msg.getPassword());
        cmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, MevocoConstants.SERVICE_ID, cmsg.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(err(VmErrors.CHANGE_VM_PASSWORD_ERROR, reply.getError().getDetails()).withCause(reply.getError()));
                    bus.publish(evt);
                    return;
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(APISetVmUsbRedirectMsg msg) {
        APISetVmUsbRedirectEvent evt = new APISetVmUsbRedirectEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.USB_REDIRECT.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(VmSystemTags.USB_REDIRECT_TOKEN, String.valueOf(msg.isEnable()))));
        creator.recreate = true;
        creator.create();
        bus.publish(evt);
    }

    private void handle(APIGetVmUsbRedirectMsg msg) {
        APIGetVmUsbRedirectReply reply = new APIGetVmUsbRedirectReply();
        String usbRedirect = VmSystemTags.USB_REDIRECT.getTokenByResourceUuid(self.getUuid(),
                VmSystemTags.USB_REDIRECT_TOKEN);
        reply.setEnable(Boolean.parseBoolean(usbRedirect));
        bus.reply(msg, reply);
    }

    private void handle(APISetVmRDPMsg msg) {
        APISetVmRDPEvent evt = new APISetVmRDPEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.RDP_ENABLE.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(VmSystemTags.RDP_ENABLE_TOKEN, String.valueOf(msg.isEnable()))));
        creator.recreate = true;
        creator.create();
        bus.publish(evt);
    }

    private void handle(APIGetVmRDPMsg msg) {
        APIGetVmRDPReply reply = new APIGetVmRDPReply() ;
        String rdpEnable = VmSystemTags.RDP_ENABLE.getTokenByResourceUuid(self.getUuid(),
                VmSystemTags.RDP_ENABLE_TOKEN);
        reply.setEnable(Boolean.parseBoolean(rdpEnable));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVmMonitorNumberMsg msg) {
        APIGetVmMonitorNumberReply reply = new APIGetVmMonitorNumberReply() ;
        String VDIMonitorNumber = VmSystemTags.VDI_MONITOR_NUMBER.getTokenByResourceUuid(self.getUuid(),
                VmSystemTags.VDI_MONITOR_NUMBER_TOKEN);
        if (VDIMonitorNumber == null) {
            VDIMonitorNumber = VmInstanceConstant.VM_MONITOR_NUMBER.toString();
        }
        reply.setMonitorNumber(Integer.valueOf(VDIMonitorNumber));
        bus.reply(msg, reply);
    }

    private void handle(APISetVmMonitorNumberMsg msg) {
        APISetVmMonitorNumberEvent evt = new APISetVmMonitorNumberEvent(msg.getId());
        SystemTagCreator creator = VmSystemTags.VDI_MONITOR_NUMBER.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(VmSystemTags.VDI_MONITOR_NUMBER_TOKEN, String.valueOf(msg.getMonitorNumber()))));
        creator.recreate = true;
        creator.create();
        bus.publish(evt);
    }

    private void handle(APIGetImageCandidatesForVmToChangeMsg msg) {
        APIGetImageCandidatesForVmToChangeReply apiReply = new APIGetImageCandidatesForVmToChangeReply();

        GetImageCandidatesForVmToChangeMsg gmsg = new GetImageCandidatesForVmToChangeMsg();
        gmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(gmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                GetImageCandidatesForVmToChangeReply rly = reply.castReply();
                if (rly.getInventories() == null || rly.getInventories().isEmpty()) {
                    apiReply.setInventories(null);
                    bus.reply(msg, apiReply);
                    return;
                }

                List<ImageInventory> candidates = new ArrayList<>();
                if (acntMgr.isAdmin(msg.getSession())) {
                    apiReply.setInventories(rly.getInventories());
                    bus.reply(msg, apiReply);
                    return;
                }

                List<String> myImages = acntMgr.getResourceUuidsCanAccessByAccount(msg.getSession().getAccountUuid(), ImageVO.class);
                if (myImages == null || myImages.isEmpty()) {
                    apiReply.setInventories(null);
                    bus.reply(msg, apiReply);
                    return;
                }

                // get intersection
                for (ImageInventory inv : rly.getInventories()) {
                    for (String myImage : myImages) {
                        if (myImage.equals(inv.getUuid())) {
                            candidates.add(inv);
                            break;
                        }
                    }
                }
                apiReply.setInventories(candidates);
                bus.reply(msg, apiReply);
            }
        });
    }

    private void handle(final APISetVmConsoleModeMsg msg) {
        APISetVmConsoleModeEvent evt = new APISetVmConsoleModeEvent(msg.getId());
        SystemTagCreator creator = MevocoSystemTags.VM_CONSOLE_MODE.newSystemTagCreator(self.getUuid());
        creator.setTagByTokens(map(e(MevocoSystemTags.VM_CONSOLE_MODE_TOKEN, msg.getMode())));
        creator.recreate = true;
        creator.create();
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(final APIUpdateVmNicMacMsg msg) {
        APIUpdateVmNicMacEvent event = new APIUpdateVmNicMacEvent(msg.getId());
        VmNicVO vo = new SQLBatchWithReturn<VmNicVO>() {
            @Override
            protected VmNicVO scripts() {
                VmNicVO vo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
                vo.setMac(msg.getMac());
                vo = dbf.updateAndRefresh(vo);
                return vo;
            }
        }.execute();
        event.setInventory(VmNicInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIGetVmInstanceFirstBootDeviceMsg msg) {
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            bus.replyErrorByMessageType(msg, allowed);
            return;
        }

        String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .select(VmInstanceVO_.hostUuid)
                .findValue();

        APIGetVmInstanceFirstBootDeviceReply reply = new APIGetVmInstanceFirstBootDeviceReply();
        GetVmFirstBootDeviceOnHypervisorMsg gmsg = new GetVmFirstBootDeviceOnHypervisorMsg();
        gmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        gmsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(gmsg, new CloudBusCallBack(reply) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    reply.setError(rly.getError());
                }

                GetVmFirstBootDeviceOnHypervisorReply grly = rly.castReply();
                if (!grly.isSuccess()) {
                    reply.setError(grly.getError());
                }

                if (reply.isSuccess()) {
                    reply.setFirstBootDevice(grly.getFirstBootDevice());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APISetVmCleanTrafficMsg msg) {
        APISetVmCleanTrafficEvent event = new APISetVmCleanTrafficEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            private boolean changeTag(APISetVmCleanTrafficMsg msg){
                String currentValue = VmSystemTags.CLEAN_TRAFFIC.getTokenByResourceUuid(msg.getUuid(), VmSystemTags.CLEAN_TRAFFIC_TOKEN);
                boolean needChange = currentValue == null || msg.isEnable() != Boolean.valueOf(currentValue);
                if (needChange) {
                    doUpdateTag(msg.getUuid(), msg.isEnable());
                }
                return needChange;
            }

            private void doUpdateTag(String vmUuid, boolean enable){
                SystemTagCreator creator = VmSystemTags.CLEAN_TRAFFIC.newSystemTagCreator(vmUuid);
                creator.setTagByTokens(map(e(VmSystemTags.CLEAN_TRAFFIC_TOKEN, String.valueOf(enable))));
                creator.recreate = true;
                creator.create();
            }

            @Override
            public void run(SyncTaskChain chain) {
                boolean changed = changeTag(msg);
                String hostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid()).select(VmInstanceVO_.hostUuid).findValue();
                if (!changed || hostUuid == null) {
                    bus.publish(event);
                    chain.next();
                    return;
                }

                VmUpdateNicOnHypervisorMsg cmsg = new VmUpdateNicOnHypervisorMsg();
                cmsg.setVmInstanceUuid(msg.getUuid());
                cmsg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, msg.getUuid());
                bus.send(cmsg, new CloudBusCallBack(chain, event) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            doUpdateTag(msg.getUuid(), !msg.isEnable());
                            event.setError(reply.getError());
                        }

                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("change-vm-clean-traffic-state-%s", self.getUuid());
            }
        });
    }

    private void handle(APIChangeVmImageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIChangeVmImageEvent evt = new APIChangeVmImageEvent(msg.getId());

                ChangeVmImageMsg cmsg = new ChangeVmImageMsg();
                cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                cmsg.setImageUuid(msg.getImageUuid());
                cmsg.setResourceUuid(msg.getResourceUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());

                ChangeVmImageOverlayMsg imsg = new ChangeVmImageOverlayMsg();
                imsg.setMessage(cmsg);
                imsg.setImageUuid(msg.getImageUuid());
                bus.makeTargetServiceIdByResourceUuid(imsg, ImageConstant.SERVICE_ID, msg.getImageUuid());
                bus.send(imsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            evt.setError(reply.getError());
                        } else {
                            String imageUuid = msg.getImageUuid();
                            String vmUuid = msg.getVmInstanceUuid();
                            if (VmSystemTags.VM_INJECT_QEMUGA.hasTag(imageUuid, ImageVO.class)) {
                                SystemTagCreator creator = VmSystemTags.VM_INJECT_QEMUGA.newSystemTagCreator(vmUuid);
                                creator.inherent = false;
                                creator.recreate = true;
                                creator.create();
                            } else {
                                VmSystemTags.VM_INJECT_QEMUGA.delete(vmUuid);
                            }

                            ChangeVmImageReply rly = reply.castReply();
                            evt.setInventory(rly.getInventory());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("api-change-vm-%s-image", msg.getVmInstanceUuid());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void changepasswd(final Message msg, final Completion completion){
        ErrorCode allowed = validateOperationByState(msg, self.getState(), null);
        if (allowed != null) {
            completion.fail(allowed);
            return;
        }

        ChangeVmPasswordMsg amsg = (ChangeVmPasswordMsg)msg;
        VmAccountPreference account = new VmAccountPreference(amsg.getVmInstanceUuid(),
                amsg.getAccount(), amsg.getPassword());

        logger.debug("vm instance is " + amsg.getVmInstanceUuid());

        final VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(VmInstanceInventory.valueOf(dbf.findByUuid(amsg.getUuid(), VmInstanceVO.class)));
        spec.setCurrentVmOperation(VmInstanceConstant.VmOperation.ChangePassword);
        spec.setAccountPerference(account);

        ErrorCode noHostErr = operr("not dest host found in db by uuid: %s, can't" +
                " send change password cmd to the host!", amsg.getVmInstanceUuid());

        VmInstanceVO viVo = dbf.findByUuid(amsg.getVmInstanceUuid(), VmInstanceVO.class);

        if (viVo == null) {
            completion.fail(noHostErr);
            return;
        }
        VmInstanceState vmState = viVo.getState();

        VmInstanceInventory inv = VmInstanceInventory.valueOf(viVo);

        String hostid = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        HostVO hvo = dbf.findByUuid(hostid, HostVO.class);
        if (hvo != null) {
            spec.setDestHost(HostInventory.valueOf(hvo));
            spec.setDestRootVolume(inv.getRootVolume());
        } else {
            completion.fail(noHostErr);
            return;
        }

        FlowChain chain;
        if(vmState.equals(VmInstanceState.Running)) {
            chain = getChangeVmPasswordWorkFlowChain();
            setAdditionalFlow(chain, spec);

            logger.debug("flow numbers are " + chain.getFlows().size());
        } else {
            completion.fail(operr("state is not correct while change password."));
            return;
        }

        chain.setName(String.format("change-vm-password-%s", amsg.getVmInstanceUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    protected FlowChain getChangeVmPasswordWorkFlowChain() {
        return mimpl.getChangeVmPasswordWorkFlowChain();
    }

    private FlowChain getChangeVmImageWorkFlowChain() {
        return mimpl.getChangeVmImageWorkFlowChain();
    }

    private Boolean isLocalStorage(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .isExists();
    }
}
