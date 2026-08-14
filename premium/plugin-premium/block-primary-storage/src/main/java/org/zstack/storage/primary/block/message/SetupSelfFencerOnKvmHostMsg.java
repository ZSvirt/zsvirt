package org.zstack.storage.primary.block.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/22 00:53
 */
public class SetupSelfFencerOnKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param;

    public KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam getParam() {
        return param;
    }

    public void setParam(KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param) {
        this.param = param;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return param.getPrimaryStorage().getUuid();
    }
}
