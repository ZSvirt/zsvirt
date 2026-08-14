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
 * Created by root on 7/30/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartVmInstanceJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(StartVmInstanceJob.class);
    @Autowired
    private transient AccountManager acntMgr;
    @Autowired
    private transient EventFacade evtf;
    @Autowired
    private transient CloudBus bus;

    public StartVmInstanceJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public StartVmInstanceJob() {
        super();
    }

    @Override
    public StartVmInstanceMsg buildRequest() {
        StartVmInstanceMsg smsg = new StartVmInstanceMsg();
        smsg.setVmInstanceUuid(getTargetResourceUuid());
        smsg.setAccountUuid(getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, getTargetResourceUuid());
        return smsg;
    }

    @Override
    public void execute(Object msg, ReturnValueCompletion completion) {
        logger.debug(String.format("run scheduler for job: StartVmInstanceJob; vm uuid is %s", getTargetResourceUuid()));
        bus.send((StartVmInstanceMsg) msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                SchedulerExecutedData data = new SchedulerExecutedData();
                data.setTargetResourceUuid(getTargetResourceUuid());
                data.setSchedulerName(getName());
                data.setJobUuid(getUuid());

                if (reply.isSuccess()) {
                    data.setResultMessage(String.format("Start vm instance job for vm[uuid:%s] succeed", getTargetResourceUuid()));
                } else {
                    data.setError(reply.getError());
                    data.setResultMessage(String.format("Start vm instance job for vm[uuid:%s] failed", getTargetResourceUuid()));
                }
                evtf.fire(SchedulerCanonicalEvents.VM_START_SCHEDULER_PATH, data);
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
        return SchedulerType.START_VM;
    }
}
