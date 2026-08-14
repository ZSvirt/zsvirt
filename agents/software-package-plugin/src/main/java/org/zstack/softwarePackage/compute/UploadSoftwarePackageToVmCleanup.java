package org.zstack.softwarePackage.compute;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.host.CleanupUploadFileToVmMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageToVmCleanup {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageToVmCleanup.class);

    @Autowired
    private CloudBus bus;

    public void cleanup(String hostUuid, String taskUuid, NoErrorCompletion completion) {
        if (hostUuid == null || taskUuid == null) {
            completion.done();
            return;
        }

        CleanupUploadFileToVmMsg msg = new CleanupUploadFileToVmMsg();
        msg.setHostUuid(hostUuid);
        msg.setTaskUuid(taskUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format(
                            "failed to clean staged VM upload files[taskUuid:%s, hostUuid:%s]: %s",
                            taskUuid, hostUuid, reply.getError().getReadableDetails()));
                }
                completion.done();
            }
        });
    }
}
