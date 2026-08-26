package org.zstack.crypto.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.volume.ResizeVolumeExtensionPoint;
import org.zstack.header.volume.ResizeVolumeStruct;
import org.zstack.header.volume.ResizeVolumeVmOverlayMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeProtocolCapability;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMConstant;

public class ZbsEncryptedCbdResizeExtension implements ResizeVolumeExtensionPoint {
    @Autowired
    private CloudBus bus;

    @Override
    public void beforeResizeVolume(VolumeVO volume, long resize, VolumeType type, ResizeVolumeStruct struct, Completion completion) {
        if (volume == null || struct == null || !struct.isVmRunning()) {
            completion.success();
            return;
        }

        if (!volume.isEncrypted() || !VolumeProtocol.CBD.toString().equals(volume.getProtocol())) {
            completion.success();
            return;
        }

        VolumeProtocolCapability capability = VolumeProtocolCapability.get(volume.getProtocol(), KVMConstant.KVM_HYPERVISOR_TYPE);
        if (capability != null && !capability.isSupportResizeOnHypervisor()) {
            completion.success();
            return;
        }

        ResizeVolumeOnPrimaryStorageMsg msg = new ResizeVolumeOnPrimaryStorageMsg();
        msg.setVolume(VolumeInventory.valueOf(volume));
        msg.setSize(resize);
        msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());

        NeedReplyMessage message = msg;
        if (struct.getVmInstanceUuid() != null) {
            ResizeVolumeVmOverlayMsg overlayMsg = new ResizeVolumeVmOverlayMsg();
            overlayMsg.setVmInstanceUuid(struct.getVmInstanceUuid());
            overlayMsg.setMessage(msg);
            bus.makeTargetServiceIdByResourceUuid(overlayMsg, VmInstanceConstant.SERVICE_ID, struct.getVmInstanceUuid());
            message = overlayMsg;
        }

        bus.send(message, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });
    }
}
