package org.zstack.storage.primary.block.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/22 09:45
 */
public class CancelSelfFencerOnKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param;

    public KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam getParam() {
        return param;
    }

    public void setParam(KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param) {
        this.param = param;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return param.getPrimaryStorage().getUuid();
    }
}
