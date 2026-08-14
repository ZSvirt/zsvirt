package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmCapabilities;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:38 2021/7/14
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCapabilitiesJudger {
    private static final CLogger logger = Utils.getLogger(VmCapabilitiesJudger.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    public VmCapabilities judge(String vmUuid) {
        VmCapabilities capabilities = new VmCapabilities();
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        checkPrimaryStorageCapabilities(capabilities, vm);
        checkImageMediaTypeCapabilities(capabilities, vm);

        extEmitter.getVmCapabilities(VmInstanceInventory.valueOf(vm), capabilities);
        return capabilities;
    }

    private void checkPrimaryStorageCapabilities(VmCapabilities capabilities, VmInstanceVO vm) {
        VolumeVO rootVolume = vm.getRootVolume();

        if (rootVolume == null) {
            capabilities.setSupportLiveMigration(false);
            capabilities.setSupportVolumeMigration(false);
        } else {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.select(PrimaryStorageVO_.type);
            q.add(PrimaryStorageVO_.uuid, SimpleQuery.Op.EQ, rootVolume.getPrimaryStorageUuid());
            String type = q.findValue();

            PrimaryStorageType psType = PrimaryStorageType.valueOf(type);

            if (vm.getState() != VmInstanceState.Running) {
                capabilities.setSupportLiveMigration(false);
            } else {
                capabilities.setSupportLiveMigration(psType.isSupportVmLiveMigration());
            }

            if (vm.getState() != VmInstanceState.Stopped) {
                capabilities.setSupportVolumeMigration(false);
            } else {
                capabilities.setSupportVolumeMigration(psType.isSupportVolumeMigration());
            }
        }
    }

    private void checkImageMediaTypeCapabilities(VmCapabilities capabilities, VmInstanceVO vm) {
        ImageVO vo = null;
        ImageConstant.ImageMediaType imageMediaType;

        if (vm.getImageUuid() != null) {
            vo = dbf.findByUuid(vm.getImageUuid(), ImageVO.class);
        }

        if (vo == null) {
            imageMediaType = null;
        } else {
            imageMediaType = vo.getMediaType();
        }

        if (imageMediaType == ImageConstant.ImageMediaType.ISO || imageMediaType == null) {
            capabilities.setSupportReimage(false);
        } else {
            capabilities.setSupportReimage(true);
        }
    }
}
