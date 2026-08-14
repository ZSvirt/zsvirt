package org.zstack.iscsi.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.externalStorage.primary.ExternalStorageConstant;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.HeartbeatVolumeTO;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeProtocolCapability;
import org.zstack.iscsi.kvm.KvmIscsiCommands.AgentRsp;
import org.zstack.iscsi.kvm.KvmIscsiCommands.KvmCancelSelfFencerCmd;
import org.zstack.iscsi.kvm.KvmIscsiCommands.KvmSetupSelfFencerCmd;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class KvmIscsiNodeServer implements Component, KVMStartVmExtensionPoint, VmInstanceMigrateExtensionPoint,
        KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint,
        KVMPreAttachIsoExtensionPoint, KvmSetupSelfFencerExtensionPoint {
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    @Autowired
    private PluginRegistry pluginRgty;

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    private static final VolumeProtocolCapability capability = VolumeProtocolCapability
            .register(VolumeProtocol.iSCSI.name(), KVMConstant.KVM_HYPERVISOR_TYPE);

    static {
        capability.setSupportQosOnHypervisor(true);
        capability.setSupportResizeOnHypervisor(false);
        capability.setSupportReadonly(true);
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeIfNeeded(spec.getDestRootVolume(), host, cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeIfNeeded(vol, host, to));
                    break;
                }
            }
        }

        cmd.setDataVolumes(dtos);



        for (KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
            if (cdRomTO.isEmpty()) {
                continue;
            }
            convertIsoIfNeeded(cdRomTO, host);
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }


    private PrimaryStorageNodeSvc getNodeService(VolumeInventory volumeInventory) {
        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return null;
        }

        return extPsFactory.getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, HostInventory h, VolumeTO volumeTO) {
        if (!VolumeProtocol.iSCSI.name().equals(volumeInventory.getProtocol())) {
            return volumeTO;
        }

        PrimaryStorageNodeSvc nodeSvc = getNodeService(volumeInventory);
        if (nodeSvc == null) {
            return volumeTO;
        }

        String path = nodeSvc.getActivePath(BaseVolumeInfo.valueOf(volumeInventory), h, false);
        volumeTO.setInstallPath(path);
        return volumeTO;
    }

    private KVMAgentCommands.IsoTO convertIsoIfNeeded(KVMAgentCommands.IsoTO isoTO, HostInventory h) {
        if (!VolumeProtocol.iSCSI.name().equals(isoTO.getProtocol())) {
            return isoTO;
        }

        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(isoTO.getPrimaryStorageUuid());
        if (nodeSvc == null) {
            return isoTO;
        }

        BaseVolumeInfo iso = new BaseVolumeInfo();
        iso.setInstallPath(isoTO.getPath());
        iso.setUuid(isoTO.getImageUuid());
        iso.setProtocol(isoTO.getProtocol());
        iso.setShareable(true);

        String path = nodeSvc.getActivePath(iso,  h, true);
        isoTO.setPath(path);
        return isoTO;
    }


    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeIfNeeded(inventory, host, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {}
    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {}

    @Override
    public void preAttachIsoExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachIsoCmd cmd) {
        cmd.iso = convertIsoIfNeeded(cmd.iso, host);
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return VolumeProtocol.iSCSI.toString();
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, Completion completion) {
        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(param.getPrimaryStorage().getUuid());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("setup-self-fencer-for-external-primary-storage-%s", param.getPrimaryStorage().getUuid()));
        chain.then(new ShareFlow() {
            HostInventory host = HostInventory.valueOf(dbf.findByUuid(param.getHostUuid(), HostVO.class));
            HeartbeatVolumeTO heartbeatVol;
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "activate-iscsi-heartbeat-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        nodeSvc.activateHeartbeatVolume(host, new ReturnValueCompletion<HeartbeatVolumeTO>(trigger) {
                            @Override
                            public void success(HeartbeatVolumeTO vol) {
                                heartbeatVol = vol;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "setup-iscsi-self-fencer-on-kvm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
                        cmd.interval = param.getInterval();
                        cmd.maxAttempts = param.getMaxAttempts();
                        cmd.coveringPaths = heartbeatVol.getCoveringPaths();
                        cmd.heartbeatUrl = heartbeatVol.getInstallPath();
                        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
                        cmd.hostId = heartbeatVol.getHostId();
                        cmd.heartbeatRequiredSpace = heartbeatVol.getHeartbeatRequiredSpace();
                        cmd.hostUuid = param.getHostUuid();
                        cmd.strategy = param.getStrategy();
                        cmd.uuid = param.getPrimaryStorage().getUuid();
                        cmd.fencers = param.getFencers();

                        httpCall(KvmIscsiCommands.ISCSI_SELF_FENCER_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp rsp) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });
            }
        }).start();
    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {
        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(param.getPrimaryStorage().getUuid());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("cancel-self-fencer-for-external-primary-storage-%s", param.getPrimaryStorage().getUuid()));
        chain.then(new ShareFlow() {
            HostInventory host = HostInventory.valueOf(dbf.findByUuid(param.getHostUuid(), HostVO.class));
            HeartbeatVolumeTO heartbeatVol;
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        nodeSvc.activateHeartbeatVolume(host, new ReturnValueCompletion<HeartbeatVolumeTO>(trigger) {
                            @Override
                            public void success(HeartbeatVolumeTO vol) {
                                heartbeatVol = vol;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
                        cmd.installPath = heartbeatVol.getInstallPath();
                        cmd.hostId = heartbeatVol.getHostId();
                        cmd.hostUuid = param.getHostUuid();
                        cmd.uuid = param.getPrimaryStorage().getUuid();

                        httpCall(KvmIscsiCommands.CANCEL_ISCSI_SELF_FENCER_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp rsp) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });
            }
        }).start();
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, KVMAgentCommands.AgentCommand cmd, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, respType, completion);
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, KVMAgentCommands.AgentCommand cmd, boolean noCheckStatus, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        DebugUtils.Assert(hostUuid != null, "Host must be set here");
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply rep = reply.castReply();
                final T rsp = rep.toResponse(respType);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }
                completion.success(rsp);
            }
        });
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        List<BaseVolumeInfo> vols = getIscsiVolume(inv);
        if (vols.isEmpty()) {
            completion.success();
            return;
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(destHostUuid, HostVO.class));
        new While<>(vols).each((vol, compl) -> {
            PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(vol.getPrimaryStorageUuid());
            String path = nodeSvc.getActivePath(vol, host, vol.isShareable());
            if (!path.startsWith("iscsi")) {
                compl.done();
                return;
            }

            KVMAgentCommands.LoginIscsiTargetCmd cmd = new KVMAgentCommands.LoginIscsiTargetCmd();
            cmd.setUrl(path);
            httpCall(KVMConstant.KVM_LOGIN_ISCSI_PATH, destHostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(compl) {
                @Override
                public void success(AgentRsp rsp) {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {

    }

    private List<BaseVolumeInfo> getIscsiVolume(VmInstanceInventory vm) {
        List<BaseVolumeInfo> vols = new ArrayList<>();

        vm.getAllDiskVolumes().forEach(vol -> vols.add(BaseVolumeInfo.valueOf(vol)));

        if (extPsFactory.getNodeSvc(vm.getRootVolume().getPrimaryStorageUuid()) != null) {
            List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class).eq(VmCdRomVO_.vmInstanceUuid, vm.getUuid()).list();
            cdRomVOS.forEach(cdRomVO -> {
                if (cdRomVO.getIsoUuid() != null && VolumeProtocol.iSCSI.name().equals(cdRomVO.getProtocol())) {
                    BaseVolumeInfo info = BaseVolumeInfo.valueOf(cdRomVO);
                    info.setPrimaryStorageUuid(vm.getRootVolume().getPrimaryStorageUuid());
                    vols.add(info);
                }
            });
        }

        vols.removeIf(info -> {
            if (info.getInstallPath() == null || !VolumeProtocol.iSCSI.name().equals(info.getProtocol())) {
                return true;
            }
            String identity = info.getInstallPath().split("://")[0];
            return !extPsFactory.support(identity);
        });

        return vols;
    }
}
