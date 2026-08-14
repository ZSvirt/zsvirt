package org.zstack.storage.volume.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.APIUpdateVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.AfterUpdateVolumeSnapshotExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.APICreateBlockVolumeMsg;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.XskyBlockVolumeVO;
import org.zstack.header.volume.block.XskyBlockVolumeVO_;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.getGlobalProperties;
import static org.zstack.storage.volume.block.XskyPrimaryStorageBackend.UPDATE_BLOCK_VOLUME_SNAPSHOT;

public class XskyBlockVolumeFactory implements BlockVolumeFactory, AfterUpdateVolumeSnapshotExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getType() {
        return CephConstants.CEPH_MANUFACTURER_XSKY;
    }

    @Override
    public BlockVolumeVO createBlockVolume(VolumeVO vo, APICreateBlockVolumeMsg msg) {
        XskyBlockVolumeVO xskyVo = new XskyBlockVolumeVO(new BlockVolumeVO(vo));
        String iscsiPath = String.format("%s%s", BlockConstant.ISCSI_PATH_PREFIX, msg.getAccessPathIqn());
        xskyVo.setIscsiPath(iscsiPath);
        xskyVo.setVendor(CephConstants.CEPH_MANUFACTURER_XSKY);
        xskyVo.setAccessPathId(msg.getAccessPathId());
        xskyVo.setAccessPathIqn(msg.getAccessPathIqn());
        xskyVo.setBurstTotalIops(msg.getBurstTotalIops());
        xskyVo.setBurstTotalBw(msg.getBurstTotalBw());
        xskyVo.setMaxTotalBw(msg.getMaxTotalBw());
        xskyVo.setMaxTotalIops(msg.getMaxTotalIops());
        xskyVo = dbf.persistAndRefresh(xskyVo);
        return xskyVo;
    }

    public BlockVolumeBase getBlockVolume(BlockVolumeVO vo) {
        return new XskyBlockVolumeBase(vo);
    }

    @Override
    public void validate(String volumeName, String protocol, String primaryStorageUuid) {
        validateToken(primaryStorageUuid);
    }

    private void validateToken(String primaryStorageUuid) {
        if (!CephSystemTags.THIRDPARTY_PLATFORM.hasTag(primaryStorageUuid)) {
            throw new ApiMessageInterceptionException(argerr("the current primaryStorage %s does not " +
                    "have a third-party token set, and the block volume cannot be created temporarily", primaryStorageUuid));
        }
    }

    @Override
    public void afterUpdateVolumeSnapshot(VolumeSnapshotVO vo, APIUpdateVolumeSnapshotMsg msg, Completion completion) {
        if (!Q.New(XskyBlockVolumeVO.class).eq(XskyBlockVolumeVO_.uuid, vo.getVolumeUuid()).isExists()) {
            completion.success();
            return;
        }

        PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).find();
        XskyPrimaryStorageBackend xskyBackend = new XskyPrimaryStorageBackend(primaryStorageVO);
        final CephPrimaryStorageBase.UpdateSnapshotCmd cmd = new CephPrimaryStorageBase.UpdateSnapshotCmd();
        cmd.setSnapshotPath(vo.getPrimaryStorageInstallPath());
        cmd.setName(vo.getName());
        cmd.setDescription(vo.getDescription());
        xskyBackend.httpCall(UPDATE_BLOCK_VOLUME_SNAPSHOT, cmd, CephPrimaryStorageBase.UpdateSnapshotRsp.class, new ReturnValueCompletion<CephPrimaryStorageBase.UpdateSnapshotRsp>(completion) {
            @Override
            public void success(CephPrimaryStorageBase.UpdateSnapshotRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode error) {
                completion.fail(error);
            }
        });
    }
}
