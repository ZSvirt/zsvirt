package org.zstack.storage.volume.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.XskyBlockVolumeVO;
import org.zstack.header.volume.block.XskyBlockVolumeVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;

import static org.zstack.storage.volume.block.XskyPrimaryStorageBackend.UPDATE_BLOCK_VOLUME;

public class XskyBlockVolumeBase extends BlockVolumeBase {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private XskyPrimaryStorageBackend xskyBackend;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
    }

    public XskyBlockVolumeBase(VolumeVO vo) {
        super(vo);
    }

    protected XskyBlockVolumeVO getSelf() {
        return (XskyBlockVolumeVO) self;
    }

    @Override
    protected void UpdateHook(BlockVolumeMessage msg, Completion completion) {
        if (msg instanceof XskyBlockVolumeMessage) {
            XskyBlockVolumeMessage xskyBlockVolumeMessage = (XskyBlockVolumeMessage) msg;
            PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, getSelf().getPrimaryStorageUuid()).find();
            xskyBackend = new XskyPrimaryStorageBackend(primaryStorageVO);
            XskyPrimaryStorageBackend.UpdateBlockVolumeCmd cmd = new XskyPrimaryStorageBackend.UpdateBlockVolumeCmd();
            cmd.setName(msg.getName());
            cmd.setDescription(msg.getDescription());
            cmd.setBurstTotalBw(xskyBlockVolumeMessage.getBurstTotalBw());
            cmd.setBurstTotalIops(xskyBlockVolumeMessage.getBurstTotalIops());
            cmd.setMaxTotalBw(xskyBlockVolumeMessage.getMaxTotalBw());
            cmd.setMaxTotalIops(xskyBlockVolumeMessage.getMaxTotalIops());
            cmd.setXskyBlockVolumeId(getSelf().getXskyBlockVolumeId());
            xskyBackend.httpCall(UPDATE_BLOCK_VOLUME, cmd, CephPrimaryStorageBase.AgentResponse.class, new ReturnValueCompletion<CephPrimaryStorageBase.AgentResponse>(completion) {
                @Override
                public void success(CephPrimaryStorageBase.AgentResponse ret) {
                    completion.success();
                }

                @Override
                public void fail(ErrorCode error) {
                    completion.fail(error);
                }
            });
        } else {
            super.UpdateHook(msg, completion);
        }
    }

    @Override
    protected void UpdateVO(BlockVolumeMessage msg, Completion completion) {
        if (msg instanceof XskyBlockVolumeMessage) {
            XskyBlockVolumeMessage message = (XskyBlockVolumeMessage) msg;
            XskyBlockVolumeVO xskyBlockVolumeVO = Q.New(XskyBlockVolumeVO.class)
                    .eq(XskyBlockVolumeVO_.uuid, self.getUuid())
                    .find();
            xskyBlockVolumeVO.setMaxTotalBw(message.getMaxTotalBw());
            xskyBlockVolumeVO.setMaxTotalIops(message.getMaxTotalIops());
            xskyBlockVolumeVO.setBurstTotalBw(message.getBurstTotalBw());
            xskyBlockVolumeVO.setBurstTotalIops(message.getBurstTotalIops());
            dbf.updateAndRefresh(xskyBlockVolumeVO);
        }
        super.UpdateVO(msg, completion);
    }
}
