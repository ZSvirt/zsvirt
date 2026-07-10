package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

public class ZbsVolumeEncryptionCleanup {
    @Autowired
    private CloudBus bus;

    @SuppressWarnings("deprecation")
    void cleanupVolume(String primaryStorageUuid, String installPath) {
        if (StringUtils.isBlank(primaryStorageUuid) || StringUtils.isBlank(installPath)) {
            return;
        }

        DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setInstallPath(installPath);
        msg.setRecycle(false);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
            }
        });
    }
}
