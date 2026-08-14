package org.zstack.storage.primary.sharedblock.migration;

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

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2019/9/7.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SblkToSblkCancelMigrateVolumeFlow extends NoRollbackFlow {

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        boolean withSnapshots = (boolean) data.get(StorageMigrationConstant.WITH_SNAPSHOTS);
        if (!withSnapshots) {
            trigger.fail(operr("migrate volume without snapshot on shared block is not support to cancel."));
            return;
        }

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
