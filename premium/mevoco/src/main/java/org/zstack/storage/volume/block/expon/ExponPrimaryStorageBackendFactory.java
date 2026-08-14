package org.zstack.storage.volume.block.expon;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.expon.ExponConstants;
import org.zstack.expon.ExponStorageController;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmAttachVolumeExtensionPoint;
import org.zstack.header.vm.VmDetachVolumeExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.block.ExponBlockVolumeVO;
import org.zstack.header.volume.block.ExponBlockVolumeVO_;
import org.zstack.storage.addon.primary.*;
import org.zstack.storage.volume.block.BlockConstant;

import javax.persistence.Tuple;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExponPrimaryStorageBackendFactory implements BlockExternalPrimaryStorageFactory,
        VmAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint {
    @Autowired
    private ExternalPrimaryStorageFactory factory;
    private ExponStorageController controller;
    @Override
    public String getType() {
        return ExponConstants.EXPON_MANUFACTURER;
    }

    @Override
    public BlockExternalPrimaryStorageBackend getBlockExternalPrimaryStorageBackend(PrimaryStorageVO vo) {
        return new ExponPrimaryStorageBackend((ExternalPrimaryStorage) factory.getPrimaryStorage(vo));
    }

    public void initController(VolumeInventory volume, ReturnValueCompletion<LinkedHashMap> comp) {
        Tuple tuple = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.config, ExternalPrimaryStorageVO_.url)
                .eq(ExternalPrimaryStorageVO_.uuid, volume.getPrimaryStorageUuid()).findTuple();
        controller = new ExponStorageController((String) tuple.get(1));
        controller.connect((String) tuple.get(0), (String) tuple.get(1), comp);
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!Q.New(ExponBlockVolumeVO.class)
                .eq(ExponBlockVolumeVO_.uuid, volume.getUuid()).isExists()) {
            completion.success();
            return;
        }
        initController(volume, new ReturnValueCompletion<LinkedHashMap>(completion) {
            @Override
            public void success(LinkedHashMap addonInfo) {
                controller.activeIscsiVolume(BlockConstant.getInstanceIscsiIqn(vm.getUuid()), BaseVolumeInfo.valueOf(volume), volume.isShareable());
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void preDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        if (!Q.New(ExponBlockVolumeVO.class)
                .eq(ExponBlockVolumeVO_.uuid, volume.getUuid()).isExists()) {
            completion.success();
            return;
        }
        initController(volume, new ReturnValueCompletion<LinkedHashMap>(null) {
            @Override
            public void success(LinkedHashMap addonInfo) {
                controller.deactivateIscsi(volume.getInstallPath(), BlockConstant.getInstanceIscsiIqn(vm.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {

    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {

    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        completion.success();
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }
}
