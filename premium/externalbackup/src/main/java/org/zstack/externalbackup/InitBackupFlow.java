package org.zstack.externalbackup;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

/**
 * Created by MaJin on 2019/11/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class InitBackupFlow implements Flow {
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

        InitExternalBackupMsg msg = new InitExternalBackupMsg();
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

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        if (spec.isAllowResume() && !trigger.getErrorCode().isError(SysErrors.INTERNAL)) {
            trigger.rollback();
            return;
        }

        CancelExternalBackupMsg msg = new CancelExternalBackupMsg();
        msg.setUuid(spec.getBackupUuid());
        msg.setCancellationApiId(ThreadContext.get(Constants.THREAD_CONTEXT_API));
        bus.makeLocalServiceId(msg, ExternalBackupConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                trigger.rollback();
            }
        });
    }
}
