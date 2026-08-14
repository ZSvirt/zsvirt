package org.zstack.scheduler.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.scheduler.SchedulerCanonicalEvents;
import org.zstack.header.scheduler.SchedulerCanonicalEvents.SchedulerExecutedData;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

/**
 * Created by root on 8/16/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RebootVmInstanceJob  extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(RebootVmInstanceJob.class);
    @Autowired
    private transient AccountManager acntMgr;
    @Autowired
    private transient EventFacade evtf;
    @Autowired
    private transient CloudBus bus;

    public RebootVmInstanceJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public RebootVmInstanceJob() {
        super();
    }

    @Override
    public RebootVmInstanceMsg buildRequest() {
        RebootVmInstanceMsg rmsg = new RebootVmInstanceMsg();
        rmsg.setVmInstanceUuid(getTargetResourceUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, getTargetResourceUuid());
        return rmsg;
    }

    @Override
    public void execute(Object msg, ReturnValueCompletion completion) {
        logger.debug(String.format("run scheduler for job: RebootVmInstanceJob; vm uuid is %s", getTargetResourceUuid()));
        bus.send((RebootVmInstanceMsg) msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                SchedulerExecutedData data = new SchedulerExecutedData();
                data.setTargetResourceUuid(getTargetResourceUuid());
                data.setSchedulerName(getName());
                data.setJobUuid(getUuid());

                if (reply.isSuccess()) {
                    data.setResultMessage(String.format("Reboot vm instance job for vm[uuid:%s] succeed", getTargetResourceUuid()));
                } else {
                    data.setError(reply.getError());
                    data.setResultMessage(String.format("Reboot vm instance job for vm[uuid:%s] failed", getTargetResourceUuid()));
                }
                evtf.fire(SchedulerCanonicalEvents.VM_REBOOT_SCHEDULER_PATH, data);
                completion.success(reply);
            }
        });
    }

    @Override
    public ErrorCode allowStateChange() {
        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, getTargetResourceUuid())
                .eq(VmInstanceVO_.state, VmInstanceState.Destroyed)
                .isExists()) {
            return operr("vm[uuid:%s] is destroyed, state change is not allowed", getTargetResourceUuid());
        }

        return null;
    }

    @Override
    public String getType() {
        return SchedulerType.REBOOT_VM;
    }
}
