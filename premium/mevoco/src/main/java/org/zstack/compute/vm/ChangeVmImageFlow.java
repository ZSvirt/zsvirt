package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.OverwriteVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.image.ImageDefaultBehavior;
import org.zstack.image.ImageSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 11/6/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ChangeVmImageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ChangeVmImageFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VolumeInventory newRootVolume = spec.getDestRootVolume();
        String newImageUuid = spec.getImageSpec().getInventory().getUuid();

        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, spec.getVmInventory().getUuid())
                .find();

        VolumeVO volumeVO = vm.getRootVolume();

        OverwriteVolumeMsg msg = new OverwriteVolumeMsg();
        msg.setTransientVolume(newRootVolume);
        msg.setOriginVolume(VolumeInventory.valueOf(volumeVO));
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                UpdateQuery sql = SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.getUuid())
                        .set(VmInstanceVO_.imageUuid, newImageUuid)
                        .set(VmInstanceVO_.hostUuid, null);
                if (spec.getImageSpec().getInventory().getPlatform() != null) {
                    sql.set(VmInstanceVO_.platform, spec.getImageSpec().getInventory().getPlatform());
                }
                if (spec.getImageSpec().getInventory().getGuestOsType() != null) {
                    sql.set(VmInstanceVO_.guestOsType, spec.getImageSpec().getInventory().getGuestOsType());
                }
                sql.update();

                // create/update bootMode based on new image
                String newBootMode = ImageSystemTags.BOOT_MODE.getTokenByResourceUuid(newImageUuid, ImageSystemTags.BOOT_MODE_TOKEN);
                if (newBootMode == null) {
                    newBootMode = ImageDefaultBehavior.getDefaultBootMode();
                }
                if (newBootMode == null) {
                    trigger.next();
                    return;
                }
                SystemTagCreator creator = VmSystemTags.BOOT_MODE.newSystemTagCreator(vm.getUuid());
                creator.setTagByTokens(map(e(VmSystemTags.BOOT_MODE_TOKEN, newBootMode)));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();

                trigger.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        String oldImageUuid = spec.getVmInventory().getImageUuid();

        VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
        vm.setImageUuid(oldImageUuid);
        vm.setHostUuid(null);
        logger.info(String.format("change vm[uuid:%s] imageUuid back to %s", vm.getUuid(), oldImageUuid));
        dbf.update(vm);
        trigger.rollback();
    }
}
