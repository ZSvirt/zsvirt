package org.zstack.simulator.storage.primary;

import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.ShrinkVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageMsg;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtils;

import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;


public class SimulatorPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(SimulatorPrimaryStorage.class);
    private static final PathUtils putil = Utils.getPathUtil();

    public SimulatorPrimaryStorage(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    public void deleteHook() {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] gets deleted", self.getUuid()));
    }

    @Override
    public void changeStateHook(PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] changes state from %s to %s", self.getUuid(), self.getState(), nextState));
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] attached", self.getUuid()));
        completion.success();
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        logger.debug(String.format("SimulatorPrimaryStorage[uuid:%s] detached from cluster[uuid:%s]", self.getUuid(), clusterUuid));
        completion.success();
    }

    private VolumeInventory instantiateVolume(VolumeInventory vol) {
        String root = self.getUrl();
        String path = putil.join(root, PrimaryStorageConstant.VM_FOLDER, vol.getUuid() + ".qcow2");
        vol.setInstallPath(path);
        return vol;
    }

    private void handle(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        VolumeInventory vol = instantiateVolume(msg.getVolume());
        vol.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        reply.setVolume(vol);
        logger.debug(String.format("Successfully created root volume[uuid:%s] on primary storage[uuid:%s]", msg.getVolume().getUuid(),
                msg.getPrimaryStorageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        logger.debug(String.format("Successfully deleted volume[uuid:%s] from primary storage[uuid:%s]", msg.getVolume().getUuid(), msg.getUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        logger.debug(String.format("Successfully deleted path[uuid:%s] from primary storage[uuid:%s]", msg.getInstallPath(), msg.getPrimaryStorageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg.getClass() == InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg.class) {
            handle((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
            VolumeInventory vol = instantiateVolume(msg.getVolume());
            vol.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
            reply.setVolume(vol);
            logger.debug(String.format("Successfully created data volume[uuid:%s] on primary storage[uuid:%s]", msg.getVolume().getUuid(),
                    msg.getPrimaryStorageUuid()));
            bus.reply(msg, reply);
        }
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        String path = putil.join(self.getUrl(), PrimaryStorageConstant.VM_FOLDER, msg.getVolumeUuid() + ".qcow2");
        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        reply.setInstallPath(path);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        String path = putil.join(self.getUrl(), PrimaryStorageConstant.VM_FOLDER, msg.getVolumeUuid() + ".qcow2");
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        reply.setInstallPath(path);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {
        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        reply.setInstallPath("/xxx.iso");
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
        capability.setSupport(true);
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        reply.setActualSize(0);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        reply.setActualSize(0);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
        Map<String, Long> actualSize = msg.getVolumeUuidInstallPaths()
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> 0L));
        reply.setActualSizes(actualSize);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        completion.success();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = self.getCapacity().getAvailablePhysicalCapacity();
        usage.totalPhysicalSize = self.getCapacity().getTotalPhysicalCapacity();
        completion.success(usage);
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        CheckVolumeSnapshotOperationOnPrimaryStorageReply reply = new CheckVolumeSnapshotOperationOnPrimaryStorageReply();
        if (msg.getVmInstanceUuid() != null) {
            HostStatus hostStatus = SQL.New("select host.status from VmInstanceVO vm, HostVO host" +
                    " where vm.uuid = :vmUuid" +
                    " and vm.hostUuid = host.uuid", HostStatus.class)
                    .param("vmUuid", msg.getVmInstanceUuid())
                    .find();
            if (hostStatus != HostStatus.Connected && hostStatus != null) {
                reply.setError(err(HostErrors.HOST_IS_DISCONNECTED, "host where vm[uuid:%s] locate is not Connected.", msg.getVmInstanceUuid()));
            }
        }

        bus.reply(msg, reply);
    }
}
