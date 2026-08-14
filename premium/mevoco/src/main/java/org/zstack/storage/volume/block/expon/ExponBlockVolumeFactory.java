package org.zstack.storage.volume.block.expon;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.expon.ExponConstants;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.APIUpdateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.APICreateBlockVolumeMsg;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.ExponBlockVolumeVO;
import org.zstack.header.volume.block.ExponBlockVolumeVO_;
import org.zstack.storage.volume.block.BlockConstant;
import org.zstack.expon.ExponNameHelper;
import org.zstack.expon.ExponStorageController;
import org.zstack.header.core.Completion;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.snapshot.AfterUpdateVolumeSnapshotExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.*;
import org.zstack.storage.volume.block.BlockVolumeBase;
import org.zstack.storage.volume.block.BlockVolumeFactory;
import org.zstack.storage.volume.block.BlockVolumeMessage;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class ExponBlockVolumeFactory implements BlockVolumeFactory, AfterUpdateVolumeSnapshotExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    private ExponStorageController controller;
    @Override
    public String getType() {
        return ExponConstants.EXPON_MANUFACTURER;
    }

    @Override
    public BlockVolumeVO createBlockVolume(VolumeVO vo, APICreateBlockVolumeMsg msg) {
        ExponBlockVolumeVO exponBlockVolumeVO = new ExponBlockVolumeVO(new BlockVolumeVO(vo));
        exponBlockVolumeVO.setVendor(getType());
        // after activeVolume, the iscsiPath will add iqn
        exponBlockVolumeVO.setIscsiPath(BlockConstant.ISCSI_PATH_PREFIX);
        dbf.persistAndRefresh(exponBlockVolumeVO);
        return exponBlockVolumeVO;
    }

    @Override
    public BlockVolumeBase getBlockVolume(BlockVolumeVO vo) {
        return new ExponBlockVolumeBase(vo);
    }

    @Override
    public void validate(String volumeName, String protocol, String primaryStorageUuid) {
        ExternalPrimaryStorageVO vo = dbf.findByUuid(primaryStorageUuid, ExternalPrimaryStorageVO.class);
        if (StringUtils.isNotEmpty(volumeName) && Q.New(ExponBlockVolumeVO.class).eq(ExponBlockVolumeVO_.name, volumeName).isExists()) {
            throw new ApiMessageInterceptionException(argerr("name: [%s] already exists, " +
                    "block volume name cannot be duplicated on type[%s] primarystorage", volumeName, vo.getType()));
        }

        if (StringUtils.isEmpty(protocol)) {
            throw new ApiMessageInterceptionException(argerr("[protocol] parameter is null, type[%s] primarystorage " +
                    "must set block volume protocol", vo.getType()));
        }

        List<String> protocols = vo.getOutputProtocols().stream().map(PrimaryStorageOutputProtocolRefVO::getOutputProtocol).collect(Collectors.toList());
        if (!protocols.contains(protocol)) {
            throw new ApiMessageInterceptionException(operr("current [%s] primary storage not support [%s] type protocol, " +
                    "please add protocol to storage first", vo.getUuid(), protocol));
        }
    }

    @Override
    public void afterUpdateVolumeSnapshot(VolumeSnapshotVO vo, APIUpdateVolumeSnapshotMsg msg, Completion completion) {
        if (StringUtils.isEmpty(msg.getName())) {
            completion.success();
            return;
        }
        ExponBlockVolumeVO volumeVO = Q.New(ExponBlockVolumeVO.class)
                .eq(ExponBlockVolumeVO_.uuid, vo.getVolumeUuid()).find();
        if (volumeVO == null) {
            completion.success();
            return;
        }

        ExternalPrimaryStorageVO storageVO = Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).find();
        controller = new ExponStorageController(storageVO);
        String snapshotId = ExponNameHelper.getSnapIdFromPath(vo.getPrimaryStorageInstallPath());
        controller.getApiHelper().login();
        controller.getApiHelper().updateVolumeSnapshot(snapshotId, vo.getName(), vo.getDescription());
        completion.success();
    }
}
