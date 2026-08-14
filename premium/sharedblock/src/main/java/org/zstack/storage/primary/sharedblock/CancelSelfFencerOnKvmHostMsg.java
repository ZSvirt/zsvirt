package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;

/**
 * Create by weiwang at 2018/7/24
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
