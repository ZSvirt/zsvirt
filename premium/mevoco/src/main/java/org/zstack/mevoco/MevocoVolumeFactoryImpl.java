package org.zstack.mevoco;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.GetAttachableVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.volume.MevocoVolumeGlobalConfig;
import org.zstack.storage.volume.VolumeBase;
import org.zstack.storage.volume.VolumeFactory;
import org.zstack.storage.volume.VolumeMevocoConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by miao on 12/23/16.
 */
public class MevocoVolumeFactoryImpl implements Component, VolumeFactory, GetAttachableVolumeExtensionPoint, KVMAttachVolumeExtensionPoint, KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoVolumeFactoryImpl.class);

    @Autowired
    protected CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public VolumeBase makeVolumeBase(VolumeVO vo) {
        return new MevocoVolumeBase(vo);
    }

    @Override
    public List<VolumeVO> returnAttachableVolumes(VmInstanceInventory vm, List<VolumeVO> candidates) {
        List<String> attachedShareableVolumes = Q.New(ShareableVolumeVmInstanceRefVO.class).select(ShareableVolumeVmInstanceRefVO_.volumeUuid)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vm.getUuid())
                .listValues();

        logger.debug("Remove attached shareable volume");
        candidates = transformAndRemoveNull(candidates, arg -> attachedShareableVolumes.contains(arg.getUuid()) ? null : arg);

        return candidates;
    }

    @Override
    public boolean start() {
        configureVolumeMultiQueuesConfig();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void configureVolumeMultiQueuesConfig() {
        ResourceConfig volumeMultiQueuesConfig = rcf.getResourceConfig(MevocoVolumeGlobalConfig.VOLUME_MULTI_QUEUES.getIdentity());
        volumeMultiQueuesConfig.installValidatorExtension(this::validateVolumeMultiQueues);
    }

    private void validateVolumeMultiQueues(String resourceUuid, String oldValue, String newValue) {
        VolumeVO vol = dbf.findByUuid(resourceUuid, VolumeVO.class);

        if (vol == null) {
            return;
        }

        if (vol.getType() == VolumeType.Root) {
            throw new OperationFailureException(operr("unsupported operation for setting root volume[%s] multiQueues.", resourceUuid));
        }

        if (KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(resourceUuid)) {
            throw new OperationFailureException(operr("unsupported operation for setting virtio-scsi volume[%s] multiQueues.", resourceUuid));
        }
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        if (KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getUuid())) {
            return;
        }

        Integer multiQueues = rcf.getResourceConfigValue(MevocoVolumeGlobalConfig.VOLUME_MULTI_QUEUES, volume.getUuid(), Integer.class);
        if (multiQueues == null || multiQueues <= VolumeMevocoConstants.VOLUME_MULTI_QUEUES_MIN_NUM) {
            return;
        }

        VolumeTO to = cmd.getVolume();
        to.setMultiQueues(multiQueues.toString());
        cmd.setVolume(to);
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {

    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        to.setAioNative(rcf.getResourceConfigValue(MevocoGlobalConfig.AIO_NATIVE, inventory.getUuid(), Boolean.class));
        return to;
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        for (VolumeTO volume: cmd.getDataVolumes()) {
            if (KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(volume.getVolumeUuid())) {
                continue;
            }

            Integer multiQueues = rcf.getResourceConfigValue(MevocoVolumeGlobalConfig.VOLUME_MULTI_QUEUES, volume.getVolumeUuid(), Integer.class);
            if (multiQueues == null || multiQueues <= VolumeMevocoConstants.VOLUME_MULTI_QUEUES_MIN_NUM) {
                continue;
            }

            volume.setMultiQueues(multiQueues.toString());
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
