package org.zstack.storage.volume.block.expon;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.expon.ExponNameHelper;
import org.zstack.expon.ExponStorageController;
import org.zstack.header.core.Completion;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.ExponBlockVolumeVO;
import org.zstack.header.volume.block.ExponBlockVolumeVO_;
import org.zstack.storage.volume.block.BlockVolumeBase;
import org.zstack.storage.volume.block.BlockVolumeMessage;

import static org.zstack.expon.ExponNameHelper.*;

public class ExponBlockVolumeBase extends BlockVolumeBase {

    private ExponStorageController controller;
    public ExponBlockVolumeBase(BlockVolumeVO vo) {
        super(vo);
        ExternalPrimaryStorageVO storageVO = Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid())
                .find();
        controller = new ExponStorageController(storageVO);
    }

    @Override
    protected ExponBlockVolumeVO getSelf() {
        return (ExponBlockVolumeVO) self;
    }

    @Override
    protected void UpdateHook(BlockVolumeMessage msg, Completion completion) {
        if (StringUtils.isEmpty(msg.getName())) {
            super.UpdateHook(msg, completion);
            return;
        }
        ExponBlockVolumeVO volumeVO = Q.New(ExponBlockVolumeVO.class)
                .eq(ExponBlockVolumeVO_.uuid, msg.getBlockVolumeUuid()).find();
        if (volumeVO == null) {
            super.UpdateHook(msg, completion);
            return;
        }
        String volId = ExponNameHelper.getVolIdFromPath(volumeVO.getInstallPath());
        controller.getApiHelper().login();
        controller.getApiHelper().updateVolume(volId, msg.getName());
        super.UpdateHook(msg, completion);
    }
}
