package org.zstack.storage.device.localRaid;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerJobFactory;
import org.zstack.scheduler.SchedulerType;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SelfTestLocalRaidJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new SelfTestLocalRaidJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.LOCAL_RAID_SELF_TEST;
    }

    @Override
    public String getJobClassName() {
        return SelfTestLocalRaidJob.class.getName();
    }
}
