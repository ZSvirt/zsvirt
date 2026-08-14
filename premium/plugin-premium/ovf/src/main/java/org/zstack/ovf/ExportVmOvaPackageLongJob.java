package org.zstack.ovf;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.Constants;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.ovf.api.APIExportVmOvaPackageEvent;
import org.zstack.ovf.api.APIExportVmOvaPackageMsg;
import org.zstack.ovf.datatype.ImagePackageVO;
import org.zstack.ovf.message.ExportVmOvaPackageMsg;
import org.zstack.ovf.message.ExportVmOvaPackageReply;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by Qi Le on 2022/5/5
 */
@LongJobFor(APIExportVmOvaPackageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExportVmOvaPackageLongJob implements LongJob {

    @Autowired
    protected CloudBus bus;

    private String auditUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIExportVmOvaPackageMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APIExportVmOvaPackageMsg.class);
        ExportVmOvaPackageMsg msg = new ExportVmOvaPackageMsg(apiMsg);
        auditUuid = msg.getResourceUuid();
        bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getResourceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                ExportVmOvaPackageReply r = reply.castReply();
                APIExportVmOvaPackageEvent event = new APIExportVmOvaPackageEvent(
                        ThreadContext.get(Constants.THREAD_CONTEXT_API));
                event.setInventory(r.getInventory());
                completion.success(event);
            }
        });
    }

    @Override
    public Class<?> getAuditType() {
        return ImagePackageVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditUuid;
    }
}
