package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageDeletionMsg;
import org.zstack.header.image.ImageDeletionPolicyManager;
import org.zstack.header.message.MessageReply;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;

import java.util.Map;

/**
 * Created by MaJin on 2021/3/16.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteTemplateFlow extends NoRollbackFlow {
    @Autowired
    CloudBus bus;
    @Autowired
    VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        boolean temporaryImage = spec.getRootVolumeImage().isTemporary();

        if (temporaryImage || MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class)) {
            deleteImage(spec.getRootVolumeImage().getUuid(), temporaryImage, trigger);
        } else {
            trigger.next();
        }
    }

    private void deleteImage(String imageUuid, boolean temporaryImage, FlowTrigger trigger) {
        ImageDeletionMsg msg = new ImageDeletionMsg();
        msg.setImageUuid(imageUuid);
        msg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        msg.setForceDelete(true);
        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess() && temporaryImage) {
                    volumeEncryptedResourceKeyBackend.detachKeyProviderFromTemporarySnapshotImage(msg.getImageUuid());
                }
                trigger.next();
            }
        });
    }
}
