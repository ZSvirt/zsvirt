package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDetachVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.IoThreadPinStruct;
import org.zstack.header.volume.MevocoVolumeConstants;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.zstack.core.Platform.operr;

public class VmIoThreadPinExtension implements KVMStartVmExtensionPoint, KVMAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint, KVMConvertVolumeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmIoThreadPinExtension.class);

    @Autowired
    protected CloudBus bus;

    @Autowired
    private VmInstanceResourceMetadataManager vidManager;


    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (cmd.getDataVolumes().isEmpty()) {
            return;
        }

        List<IoThreadPinStruct> ioThreadPins = new ArrayList<>();
        Set<Integer> availableScsiControllerIndexSet= IntStream.rangeClosed(1, cmd.getDataVolumes().size()).boxed().collect(Collectors.toSet());
        List<String> needAllocateControllerIndexVolumeUuids = new ArrayList<>();

        for (VolumeTO volume: cmd.getDataVolumes()) {
            if (!MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(volume.getVolumeUuid())) {
                continue;
            }

            String pinString = MevocoVolumeSystemTags.IO_THREAD_PIN.getTokenByResourceUuid(volume.getVolumeUuid(), MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN);
            String[] pinInfo = pinString.split(MevocoVolumeConstants.IOTHREADPIN_SEPARATOR);

            volume.setIoThreadId(Integer.parseInt(pinInfo[0]));
            volume.setIoThreadPin(pinInfo[1]);

            IoThreadPinStruct pin = new IoThreadPinStruct();
            pin.setVolumeUuid(volume.getVolumeUuid());
            pin.setIoThreadId(Integer.parseInt(pinInfo[0]));
            pin.setPin(pinInfo[1]);
            ioThreadPins.add(pin);

            if (!KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getVolumeUuid())) {
               continue;
            }

            DeviceAddress da = vidManager.getVmDeviceAddress(volume.getVolumeUuid(), cmd.getVmInstanceUuid());
            if (da == null || da.controller == null) {
                needAllocateControllerIndexVolumeUuids.add(volume.getVolumeUuid());
            } else if (MevocoVolumeConstants.VIRTIO_SCSI_VOLUME_DEFAULT_CONTROLLER_INDEX.equals(da.controller)) {
                needAllocateControllerIndexVolumeUuids.add(volume.getVolumeUuid());
                vidManager.deleteVmResourceMetadata(volume.getVolumeUuid(), cmd.getVmInstanceUuid());
                volume.setDeviceAddress(new DeviceAddress());
                logger.info(String.format("delete volume[%s] device address on vm[%s], then allocate new", volume.getVolumeUuid(),cmd.getVmInstanceUuid()));
            } else {
                volume.setControllerIndex(Integer.parseInt(da.controller));
                availableScsiControllerIndexSet.remove(Integer.parseInt(da.controller));
                logger.info(String.format("set volume[%s] controller index with device address[%s]", volume.getVolumeUuid(), da.toString()));
            }
        }

        cmd.getAddons().put("ioThreadNum", ioThreadPins.size());
        cmd.getAddons().put("ioThreadPins", ioThreadPins);

        if (needAllocateControllerIndexVolumeUuids.isEmpty()) {
            return;
        }

        int index = 0;
        List<Integer> availableScsiControllerIndexList = new ArrayList<>(availableScsiControllerIndexSet).stream().sorted().collect(Collectors.toList());
        for (VolumeTO volume: cmd.getDataVolumes()) {
            if (!needAllocateControllerIndexVolumeUuids.contains(volume.getVolumeUuid())) {
                continue;
            }
            volume.setControllerIndex(availableScsiControllerIndexList.get(index));
            logger.info(String.format("set volume[%s] controller index[%d] by allocate.", volume.getVolumeUuid(), availableScsiControllerIndexList.get(index)));
            index += 1;
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        if (!VmInstanceState.Running.toString().equals(vm.getState()) || (!MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(volume.getUuid()))) {
            return;
        }
        String pinInfo = MevocoVolumeSystemTags.IO_THREAD_PIN.getTokenByResourceUuid(volume.getUuid(), MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN);
        String[] temp = pinInfo.split(MevocoVolumeConstants.IOTHREADPIN_SEPARATOR);

        cmd.getVolume().setIoThreadId(Integer.parseInt(temp[0]));
        cmd.getVolume().setIoThreadPin(temp[1]);

        if (!KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getUuid())) {
            return;
        }

        DeviceAddress da = vidManager.getVmDeviceAddress(volume.getUuid(), vm.getUuid());
        if (da == null || da.controller == null) {
           return;
        }
        if (MevocoVolumeConstants.VIRTIO_SCSI_VOLUME_DEFAULT_CONTROLLER_INDEX.equals(da.controller)) {
            vidManager.deleteVmResourceMetadata(volume.getUuid(), vm.getUuid());
            cmd.getVolume().setDeviceAddress(new DeviceAddress());
            logger.info(String.format("reset volume[%s] device address before attach vol, because vol has device address[%s] and iothreadpin.", volume.getUuid(), da.toString()));
        } else {
            cmd.getVolume().setControllerIndex(Integer.parseInt(da.controller));
            logger.info(String.format("set volume[%s] controller index with device address[%s]", volume.getUuid(), da.toString()));
        }
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return to;
    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(volume.getUuid())) {
            completion.success();
            return;
        }

        String pinInfo = MevocoVolumeSystemTags.IO_THREAD_PIN.getTokenByResourceUuid(volume.getUuid(), MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN);
        String[] temp = pinInfo.split(MevocoVolumeConstants.IOTHREADPIN_SEPARATOR);
        MevocoVolumeSystemTags.IO_THREAD_PIN.delete(volume.getUuid());

        if (!VmInstanceState.Running.toString().equals(vm.getState())) {
            completion.success();
            return;
        }

        FlowChain flow = FlowChainBuilder.newSimpleFlowChain();
        flow.setName(String.format("clear-io-thread-pin-on-volume-%s", volume.getUuid()));

        flow.then(new NoRollbackFlow() {
            String __name__ = String.format("delete-controller-on-volume[%s]", volume.getUuid());

            @Override
            public boolean skip(Map data) {
                if (!KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getUuid())) {
                    return true;
                }
                return false;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                MevocoKVMAgentCommands.DelScsiControllerCmd cmd = new MevocoKVMAgentCommands.DelScsiControllerCmd();
                cmd.setVmUuid(vm.getUuid());
                cmd.setIoThreadId(Integer.parseInt(temp[0]));

                KVMHostAsyncHttpCallMsg kMsg = new KVMHostAsyncHttpCallMsg();
                kMsg.setPath(MevocoKVMConstant.DEL_VM_SCSI_CONTROLLER_PATH);
                kMsg.setHostUuid(vm.getHostUuid());
                kMsg.setCommand(cmd);
                kMsg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(kMsg, HostConstant.SERVICE_ID, vm.getHostUuid());
                bus.send(kMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.error(String.format("failed to del scsi controller of iothread[%s] on vm[%s], because %s.", temp[0], vm.getUuid(), reply.getError().getDetails()));
                            trigger.fail(reply.getError());
                        } else {
                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            MevocoKVMAgentCommands.DelScsiControllerRsp rsp = r.toResponse(MevocoKVMAgentCommands.DelScsiControllerRsp.class);
                            if (!rsp.isSuccess()) {
                                logger.error(String.format("failed to del scsi controller of iothread[%s] on vm[%s], because %s.", temp[0], vm.getUuid(), rsp.getError()));
                                trigger.fail(operr(rsp.getError()));
                            } else {
                                logger.info(String.format("del scsi controller of iothread[%s] on vm[%s].", temp[0], vm.getUuid()));
                                trigger.next();
                            }
                        }
                    }
                });
            }
        });

        flow.then(new NoRollbackFlow() {
            String __name__ = String.format("delete-io-thread-on-volume[%s]", volume.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                MevocoKVMAgentCommands.DelVmIoThreadPinCmd cmd = new MevocoKVMAgentCommands.DelVmIoThreadPinCmd();
                cmd.setIoThreadId(Integer.valueOf(temp[0]));
                cmd.setVmUuid(vm.getUuid());

                KVMHostAsyncHttpCallMsg sMsg = new KVMHostAsyncHttpCallMsg();
                sMsg.setPath(MevocoKVMConstant.DEL_VM_IOTHREAD_PIN_PATH);
                sMsg.setHostUuid(vm.getHostUuid());
                sMsg.setCommand(cmd);
                sMsg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(sMsg, HostConstant.SERVICE_ID, vm.getHostUuid());
                bus.send(sMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.error(String.format("failed to del iothread[%s] on vm[%s], because %s.", temp[0], vm.getUuid(), reply.getError().getDetails()));
                            trigger.fail(reply.getError());
                        } else {
                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            MevocoKVMAgentCommands.DelVmIoThreadPinRsp rsp = r.toResponse(MevocoKVMAgentCommands.DelVmIoThreadPinRsp.class);
                            if (!rsp.isSuccess()) {
                                logger.error(String.format("failed to del iothread[%s] on vm[%s], because %s.", temp[0], vm.getUuid(), rsp.getError()));
                                trigger.fail(operr(rsp.getError()));
                            } else {
                                logger.info(String.format("clear iothread[%s] in volume[%s] on VM[%s].", temp[0], volume.getUuid(), vm.getUuid()));
                                trigger.next();
                            }
                        }
                    }
                });
            }
        });

        flow.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                logger.info(String.format("del iothreadpin[%s] of volume[%s] on vm complete.", temp[0], volume.getUuid(), vm.getUuid()));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.info(String.format("failed to del iothread[%s] of volume[%s] on vm[%s], because %s.", temp[0], volume.getUuid(), vm.getUuid(), errCode.getDetails()));
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }

}
