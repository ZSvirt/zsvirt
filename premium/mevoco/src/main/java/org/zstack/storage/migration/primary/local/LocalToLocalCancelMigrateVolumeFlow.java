package org.zstack.storage.migration.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.Constants;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CancelJobOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.storage.migration.StorageMigrationConstant;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalToLocalCancelMigrateVolumeFlow extends NoRollbackFlow {

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String apiId = (String) data.get(Constants.THREAD_CONTEXT_API);
        String srcPsUuid = (String) data.get(StorageMigrationConstant.SRC_PS_UUID);
        CancelJobOnPrimaryStorageMsg cmsg = new CancelJobOnPrimaryStorageMsg();
        cmsg.setCancellationApiId(apiId);
        cmsg.setPrimaryStorageUuid(srcPsUuid);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                trigger.next();
            }
        });
    }
}
