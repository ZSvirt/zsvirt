package org.zstack.externalbackup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.mevoco.MevocoGlobalConfig;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CompleteBackupFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        return spec.isDryRun();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);

        CompleteExternalBackupMsg msg = new CompleteExternalBackupMsg();
        msg.setUuid(spec.getBackupUuid());
        bus.makeLocalServiceId(msg, ExternalBackupConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    trigger.next();
                } else {
                    trigger.fail(reply.getError());
                }
            }
        });
    }
}
