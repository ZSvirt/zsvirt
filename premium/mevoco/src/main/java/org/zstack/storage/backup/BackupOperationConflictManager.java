package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupOperationConflictChecker;
import org.zstack.header.storage.backup.BackupOperationVmOverlayMsg;
import org.zstack.header.storage.primary.AskVolumeSnapshotCapabilityMsg;
import org.zstack.header.storage.primary.AskVolumeSnapshotCapabilityReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.VolumeSnapshotCapability;
import org.zstack.header.volume.APICreateVolumeSnapshotGroupMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class BackupOperationConflictManager implements GlobalApiMessageInterceptor {
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    public static void sendBackupInVmQueue(CloudBus bus, NeedReplyMessage msg, String vmUuid, CloudBusCallBack callback) {
        if (vmUuid == null) {
            bus.send(msg, callback);
            return;
        }

        BackupOperationVmOverlayMsg overlay = new BackupOperationVmOverlayMsg();
        overlay.setMessage(msg);
        overlay.setVmInstanceUuid(vmUuid);
        overlay.setTimeout(msg.getTimeout());
        overlay.setMessageDeadline(msg.getMessageDeadline());
        bus.makeTargetServiceIdByResourceUuid(overlay, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(overlay, callback);
    }

    public ErrorCode check(String vmUuid, String currentResourceUuid) {
        return check(pluginRgty, vmUuid, currentResourceUuid);
    }

    public static ErrorCode check(PluginRegistry pluginRgty, String vmUuid, String currentResourceUuid) {
        if (vmUuid == null) {
            return null;
        }

        List<BackupOperationConflictChecker> checkers = pluginRgty.getExtensionList(BackupOperationConflictChecker.class);
        for (BackupOperationConflictChecker checker : checkers) {
            ErrorCode error = checker.check(vmUuid, currentResourceUuid);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVolumeSnapshotMsg) {
            checkChainSnapshotConflict(((APICreateVolumeSnapshotMsg) msg).getVolumeUuid());
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            checkCreateVolumeSnapshotGroup((APICreateVolumeSnapshotGroupMsg) msg);
        }

        return msg;
    }

    private void checkCreateVolumeSnapshotGroup(APICreateVolumeSnapshotGroupMsg msg) {
        VolumeVO rootVolume = dbf.findByUuid(msg.getRootVolumeUuid(), VolumeVO.class);
        if (rootVolume == null || rootVolume.getVmInstanceUuid() == null) {
            return;
        }

        List<VolumeVO> volumes = Q.New(VolumeVO.class)
                .in(VolumeVO_.type, Arrays.asList(VolumeType.Root, VolumeType.Data))
                .eq(VolumeVO_.vmInstanceUuid, rootVolume.getVmInstanceUuid())
                .list();
        volumes.forEach(this::checkChainSnapshotConflict);
    }

    public void checkChainSnapshotConflict(String volumeUuid) {
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        checkChainSnapshotConflict(volume);
    }

    private void checkChainSnapshotConflict(VolumeVO volume) {
        if (volume == null || volume.getVmInstanceUuid() == null || volume.getPrimaryStorageUuid() == null) {
            return;
        }

        PrimaryStorageVO primaryStorage = dbf.findByUuid(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (primaryStorage.getState() != PrimaryStorageState.Enabled) {
            return;
        }

        VolumeSnapshotCapability capability = askVolumeSnapshotCapability(volume);
        if (capability == null || !capability.isSupport()
                || capability.getArrangementType() == VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL) {
            return;
        }

        for (BackupOperationConflictChecker checker :
                pluginRgty.getExtensionList(BackupOperationConflictChecker.class)) {
            ErrorCode error = checker.checkChainSnapshot(volume.getVmInstanceUuid());
            if (error != null) {
                throw new ApiMessageInterceptionException(error);
            }
        }
    }

    private VolumeSnapshotCapability askVolumeSnapshotCapability(VolumeVO volume) {
        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
        askMsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
        askMsg.setVolume(VolumeInventory.valueOf(volume));
        bus.makeLocalServiceId(askMsg, PrimaryStorageConstant.SERVICE_ID);
        MessageReply reply = bus.call(askMsg);
        if (!reply.isSuccess()) {
            throw new ApiMessageInterceptionException(operr(
                    "cannot ask primary storage[uuid:%s] for volume snapshot capability, see detail [%s]",
                    volume.getPrimaryStorageUuid(), reply.getError()));
        }

        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
        return areply.getCapability();
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICreateVolumeSnapshotMsg.class, APICreateVolumeSnapshotGroupMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

}
