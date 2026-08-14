package org.zstack.storage.device.localRaid;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.storageDevice.StorageDeviceConstants;
import org.zstack.header.vo.ResourceVO;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerType;
import org.zstack.storage.device.StorageDeviceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SelfTestLocalRaidJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(SelfTestLocalRaidJob.class);

    @Autowired
    private transient CloudBus bus;

    public SelfTestLocalRaidJob() {
        super();
    }

    @Override
    public SelfTestLocalRaidForRecordMsg buildRequest() {
        List<RaidPhysicalDriveVO> physicalDriveVOS = Q.New(RaidPhysicalDriveVO.class).list();
        if (physicalDriveVOS.isEmpty()) {
            logger.debug("no raid physical drive found, skip to execute self test");
            return null;
        }

        if (StorageDeviceGlobalConfig.ENABLE_LOCALRAID.value(Boolean.class).equals(false)) {
            logger.debug(String.format("%s disabled, skip to run self test", StorageDeviceGlobalConfig.ENABLE_LOCALRAID.getCanonicalName()));
            return null;
        }

        SelfTestLocalRaidForRecordMsg recordMsg = new SelfTestLocalRaidForRecordMsg();
        recordMsg.setUuids(physicalDriveVOS.stream().map(ResourceVO::getUuid).collect(Collectors.toList()));
        return recordMsg;
    }

    public SelfTestLocalRaidJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    @Override
    public void execute(Object request, ReturnValueCompletion completion) {
        SelfTestLocalRaidForRecordMsg recordMsg = (SelfTestLocalRaidForRecordMsg) request;
        Map<String, String> results = Collections.synchronizedMap(new HashMap<>());
        Collections.shuffle(recordMsg.getUuids());
        new While<>(recordMsg.getUuids()).step((uuid, whileCompletion) -> {
            SelfTestLocalRaidMsg msg = new SelfTestLocalRaidMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, StorageDeviceConstants.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        results.put(uuid, reply.getError().toString());
                    } else {
                        SelfTestLocalRaidReply reply1 = reply.castReply();
                        results.put(uuid, reply1.getResult());
                    }
                    whileCompletion.done();
                }
            });
        }, 2).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                logger.debug(String.format("do self test on raid physical drives[%s] done, result: %s",
                        recordMsg.getUuids(), results));
                SelfTestLocalRaidForRecordReply recordReply = new SelfTestLocalRaidForRecordReply();
                recordReply.setResults(results);
                completion.success(recordReply);
            }
        });

    }

    @Override
    public String getType() {
        return SchedulerType.LOCAL_RAID_SELF_TEST;
    }
}
