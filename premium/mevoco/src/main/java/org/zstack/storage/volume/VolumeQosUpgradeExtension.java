package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.Component;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.mevoco.VolumeQosHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/11/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeQosUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(VolumeQosUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Override
    public boolean start() {
        upgrade();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void upgrade() {
        if (MevocoVolumeGlobalProperty.UPGRADE_VOLUME_QOS) {
            /**
             * upgrade from version lower than 3.2.0
             * 1. if qos system tag on vminstancevo with VmInstanceVO.class and no qos system tag on volumevo with VolumeVO.class, move into volumevo
             * 2. if qos system tag on volumevo with InstanceOfferingVO.class, move into volumevo
             * 3. copy qos system tag on instanceofferingvo to system tag on volumevo
             * 4. copy qos system tag on diskofferingvo to system tag on volumevo
             */
            setVolumeQos();
        }
    }

    // set public for unit test
    public void setVolumeQos() {
        List<VolumeVO> vos = Q.New(VolumeVO.class).list();
        vos.forEach(vo -> {
            if (vo.getType() == VolumeType.Root) {
                if (vo.getVmInstanceUuid() == null) {
                    return;
                }

                // see step 1 in upgrade()
                String mergedQosStr = VolumeQosHelper.mergeVolumeQosStr(vo.getUuid(), VolumeVO.class, vo.getVmInstanceUuid(), VmInstanceVO.class);

                if (!mergedQosStr.isEmpty()) {
                    upgradeVolumeQosSQL(vo.getUuid(), mergedQosStr);
                }
                VolumeQosHelper.removeVolumeQosInOldVersion(vo.getUuid(), VolumeVO.class);
                VolumeQosHelper.removeVolumeQosInOldVersion(vo.getVmInstanceUuid(), VmInstanceVO.class);


                // see step 3 in upgrade()
                VmInstanceVO vm = dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class);
                migrateInstanceOfferingQos(vm, vo);
            } else {
                // see step 2 in upgrade()
                // version 3.1.0 changed system tag resourceType DiskOfferingVO to VolumeVO on volume
                String mergedQosStr = VolumeQosHelper.mergeVolumeQosStr(vo.getUuid(), VolumeVO.class, vo.getUuid(), DiskOfferingVO.class);   // version 3.1.0
                if (!mergedQosStr.isEmpty()) {
                    upgradeVolumeQosSQL(vo.getUuid(), mergedQosStr);
                    VolumeQosHelper.removeVolumeQosInOldVersion(vo.getUuid(), VolumeVO.class);
                }
                VolumeQosHelper.removeVolumeQosInOldVersion(vo.getUuid(), VolumeVO.class);
                VolumeQosHelper.removeVolumeQosInOldVersion(vo.getUuid(), DiskOfferingVO.class);

                // see step 4 in upgrade()
                migrateDiskOfferingQos(vo);
            }
        });
    }

    private void migrateInstanceOfferingQos(VmInstanceVO vm, VolumeVO volume) {
        if (vm != null && volume != null && volume.getType() == VolumeType.Root && vm.getInstanceOfferingUuid() != null) {
            VolumeQosHelper.copyQosFromOffering(vm.getInstanceOfferingUuid(), InstanceOfferingVO.class, volume.getUuid());
        }
    }

    private void migrateDiskOfferingQos(VolumeVO vo) {
        if (vo != null && vo.getType() == VolumeType.Data && vo.getDiskOfferingUuid() != null) {
            VolumeQosHelper.copyQosFromOffering(vo.getDiskOfferingUuid(), DiskOfferingVO.class, vo.getUuid());
        }
    }

    private boolean upgradeVolumeQosSQL(String volumeUuid, String qosStr) {
        VolumeVO vo = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (vo == null) {
            return false;
        }
        if (vo.getVolumeQos() != null) {
            // if volumeQos existed, skip it
            return false;
        }

        UpdateQuery q = UpdateQuery.New(VolumeVO.class);
        q.condAnd(VolumeVO_.uuid, SimpleQuery.Op.EQ, volumeUuid);
        q.set(VolumeVO_.volumeQos, qosStr);
        q.update();

        return true;
    }
}
