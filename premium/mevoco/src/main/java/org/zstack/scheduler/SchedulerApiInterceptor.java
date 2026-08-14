package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.scheduler.*;
import org.quartz.*;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Mei Lei on 7/5/16.
 */
@InterceptorForService("scheduler")
public class SchedulerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private SchedulerFacade schedulerFacade;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof APICreateMessage && msg instanceof CreateSchedulerJobDescMsg) {
            APICreateMessage cmsg = (APICreateMessage) msg;
            if (cmsg.getResourceUuid() == null) {
                cmsg.setResourceUuid(Platform.getUuid());
            }
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, cmsg.getResourceUuid());
        } else if (msg instanceof SchedulerMessage) {
            SchedulerMessage schedmsg = (SchedulerMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, schedmsg.getSchedulerUuid());
        } else if (msg instanceof SchedulerJobGroupMessage) {
            SchedulerJobGroupMessage schedmsg = (SchedulerJobGroupMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, schedmsg.getSchedulerJobGroupUuid());
        }
    }
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        if (msg instanceof APIDeleteSchedulerJobMsg) {
            validate((APIDeleteSchedulerJobMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerJobMsg) {
            validate((APIUpdateSchedulerJobMsg) msg);
        } else if (msg instanceof APICreateSchedulerTriggerMsg) {
            validate((APICreateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobToSchedulerTriggerMsg) {
            validate((APIAddSchedulerJobToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerTriggerMsg) {
            validate((APIUpdateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APICreateSchedulerJobMsg) {
            validate((APICreateSchedulerJobMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobsToSchedulerJobGroupMsg) {
            validate((APIAddSchedulerJobsToSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobGroupToSchedulerTriggerMsg) {
            validate((APIAddSchedulerJobGroupToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIRunSchedulerTriggerMsg) {
            validate((APIRunSchedulerTriggerMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateSchedulerTriggerMsg msg) {
        SchedulerTriggerVO vo = dbf.findByUuid(msg.getUuid(), SchedulerTriggerVO.class);
        String schedulerType = msg.getSchedulerType() == null ? vo.getSchedulerType(): msg.getSchedulerType();

        Long startTime = msg.getStartTime() != null ? msg.getStartTime() : vo.getStartTime().getTime() / 1000L;
        if (schedulerType.equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
            Integer schedulerInterval = msg.getSchedulerInterval() != null ? msg.getSchedulerInterval() : vo.getSchedulerInterval();
            Integer repeatCount = msg.getRepeatCount() != null ? msg.getRepeatCount() : vo.getRepeatCount();
            msg.setSchedulerInterval(schedulerInterval);
            msg.setStartTime(startTime);
            msg.setRepeatCount(repeatCount);

            if (startTime < 0) {
                throw new ApiMessageInterceptionException(argerr("startTime must be positive integer or 0"));
            } else if (startTime > SchedulerConstant.TIMESTAMP_MAX_VALUE){
                //  mysql timestamp range is '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
                //  we accept 0 as startDate means start from current time
                throw new ApiMessageInterceptionException(argerr("startTime out of range"));
            }

            if (schedulerInterval == null && repeatCount != 0 && repeatCount != 1) {
                throw new ApiMessageInterceptionException(argerr("interval must be set when use simple scheduler when repeat more than once"));
            } else if (schedulerInterval != null && repeatCount != 0) {
                long stopTime = (long) schedulerInterval * (long) repeatCount + startTime;
                validateStopAndStartTime(stopTime, startTime);
            }
            if (msg.getRepeatCount() != 0 && (msg.getStopTime() != null)) {
                throw new ApiMessageInterceptionException(argerr("repeatCount and stopTime are mutually exclusive"));
            }
            if (msg.getStopTime() != null) {
                validateStopAndStartTime(msg.getStopTime(), startTime);
            }
        } else if (schedulerType.equals(SchedulerConstant.CRON_TYPE_STRING)) {
            if (msg.getCron() == null || ( msg.getCron() != null && msg.getCron().isEmpty())) {
                throw new ApiMessageInterceptionException(argerr("cron must be set when use cron scheduler"));
            }
            if ( (! msg.getCron().contains("?")) || msg.getCron().split(" ").length != 6) {
                throw new ApiMessageInterceptionException(argerr("cron task must follow format like this : \"0 0/3 17-23 * * ?\" "));
            }
            if (msg.getSchedulerInterval() != null || msg.getRepeatCount() != null) {
                throw new ApiMessageInterceptionException(argerr("cron scheduler only need to specify cron task"));
            }
            if (msg.getStopTime() != null) {
                validateStopAndStartTime(msg.getStopTime(), startTime);
            }
        }
    }

    private void validate(APIAddSchedulerJobToSchedulerTriggerMsg msg) {
        new SQLBatch() {

            @Override
            protected void scripts() {
                SchedulerJobSchedulerTriggerRefVO vo = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, msg.getSchedulerJobUuid())
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getSchedulerTriggerUuid())
                        .find();

                if (vo != null) {
                    throw new ApiMessageInterceptionException(argerr("Can not add job[uuid:%s] twice to the same trigger[uuid:%s]", msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuid()));
                }

                SchedulerTriggerVO triggerVO = findByUuid(msg.getSchedulerTriggerUuid(), SchedulerTriggerVO.class);
                if (triggerVO.getStopTime() != null && triggerVO.getStopTime().getTime() < new Timestamp(System.currentTimeMillis()).getTime()) {
                    throw new ApiMessageInterceptionException(argerr("Can not add job[uuid:%s] to a out of time trigger[uuid:%s]", msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuid()));
                }

                int limit = SchedulerGlobalConfig.ATTACHED_TRIGGERS_LIMIT.value(Integer.class);
                long count = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, msg.getSchedulerJobUuid())
                        .count();
                if (count >= limit) {
                    throw new ApiMessageInterceptionException(argerr(
                            "There are [%d] triggers added to job[uuid:%s], cannot add any more.",
                            count, msg.getSchedulerJobUuid()));

                }
            }
        }.execute();
    }

    private void validate(APIAddSchedulerJobGroupToSchedulerTriggerMsg msg) {
        int limit = SchedulerGlobalConfig.ATTACHED_TRIGGERS_LIMIT.value(Integer.class);
        long count = Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, msg.getSchedulerJobGroupUuid())
                .count();
        if (count >= limit) {
            throw new ApiMessageInterceptionException(argerr(
                    "There are [%d] triggers added to job group[uuid:%s], cannot add any more.",
                    count, msg.getSchedulerJobGroupUuid()));

        }
    }

    private void validate(APICreateSchedulerTriggerMsg msg) {
        if (msg.getSchedulerType().equals("simple")) {
            if (msg.getStartTime() == null) {
                throw new ApiMessageInterceptionException(argerr("startTime must be set for simple scheduler"));
            }

            if (msg.getSchedulerInterval() == null && msg.getRepeatCount() != 1) {
                throw new ApiMessageInterceptionException(argerr("schedulerInterval must be set for simple scheduler"));
            } else if (msg.getSchedulerInterval() != null && msg.getRepeatCount() != 0) {
                long stopTime = (long) msg.getSchedulerInterval() * (long) msg.getRepeatCount() + msg.getStartTime();
                validateStopAndStartTime(stopTime, msg.getStartTime());
            }
            if (msg.getRepeatCount() != 0 && msg.getStopTime() != null) {
                throw new ApiMessageInterceptionException(argerr("repeatCount and stopTime are mutually exclusive"));
            }
            if (msg.getStopTime() != null) {
                validateStopAndStartTime(msg.getStopTime(), msg.getStartTime());
            }
        } else if (msg.getSchedulerType().equals("cron")) {
            if (msg.getCron() == null || ( msg.getCron() != null && msg.getCron().isEmpty())) {
                throw new ApiMessageInterceptionException(argerr("cron must be set when use cron scheduler"));
            }
            if (!CronExpression.isValidExpression(msg.getCron())) {
                throw new ApiMessageInterceptionException(argerr("invalid cron expression"));
            }
            if ((! msg.getCron().contains("?")) || msg.getCron().split(" ").length != 6) {
                throw new ApiMessageInterceptionException(argerr("cron task must follow format like this : \"0 0/3 17-23 * * ?\" "));
            }
            if (msg.getSchedulerInterval() != null || msg.getRepeatCount() != 0) {
                throw new ApiMessageInterceptionException(argerr("cron scheduler only need to specify cron task"));
            }
            if (msg.getStopTime() != null) {
                validateStopAndStartTime(msg.getStopTime(), msg.getStartTime());
            }
        }

        if (msg.getStartTime() != null && msg.getStartTime() < 0) {
            throw new ApiMessageInterceptionException(argerr("startTime must be positive integer or 0"));
        } else if (msg.getStartTime() != null && msg.getStartTime() > SchedulerConstant.TIMESTAMP_MAX_VALUE){
            //  mysql timestamp range is '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
            //  we accept 0 as startDate means start from current time
            throw new ApiMessageInterceptionException(argerr("startTime out of range"));
        }
    }

    private void validateStopAndStartTime(Long stopTime, Long startTime) {
        if (stopTime == 0) {
            return;
        }
        if (stopTime < 0) {
            throw new ApiMessageInterceptionException(argerr("duration time out of range"));
        } else if (stopTime > SchedulerConstant.TIMESTAMP_MAX_VALUE) {
            throw new ApiMessageInterceptionException(argerr("stopTime out of mysql timestamp range"));
        } else if (stopTime < new Timestamp(System.currentTimeMillis()).getTime() / 1000) {
            throw new ApiMessageInterceptionException(argerr("stopTime has been passed"));
        }
        if (startTime != null && startTime > stopTime) {
            throw new ApiMessageInterceptionException(argerr("stop time[%s] cannot be earlier than start time[%s]", stopTime, startTime));
        }
    }

    private void validate(APIDeleteSchedulerJobMsg msg) {
    }

    private void validate(APIUpdateSchedulerJobMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SchedulerJobVO.class)) {
            APIUpdateSchedulerJobEvent evt = new APIUpdateSchedulerJobEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

    }

    private void validate(APICreateSchedulerJobMsg msg) {
        SchedulerJobFactory factory = SchedulerFacadeImpl.getSchedulerJobFactories().get(msg.getType());
        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("No SchedulerJobFactory of type[%s] found", msg.getType()));
        }

        ErrorCode error = factory.validateMsg(msg);
        if (error != null) {
            throw new ApiMessageInterceptionException(error);
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIAddSchedulerJobsToSchedulerJobGroupMsg msg) {
        final Long n = new SQLBatchWithReturn<Long>() {
            @Override
            protected Long scripts() {
                final String requiredClassName = q(SchedulerJobGroupVO.class)
                        .eq(SchedulerJobGroupVO_.uuid, msg.getSchedulerJobGroupUuid())
                        .select(SchedulerJobGroupVO_.jobClassName)
                        .findValue();

                return q(SchedulerJobVO.class)
                        .in(SchedulerJobVO_.uuid, msg.getSchedulerJobUuids())
                        .notEq(SchedulerJobVO_.jobClassName, requiredClassName)
                        .count();
            }
        }.execute();

        if (n > 0) {
            throw new ApiMessageInterceptionException(argerr("%d jobs have different job type with job group", n));
        }

        int limit = SchedulerGlobalConfig.CONTAIN_RESOURCES_LIMIT.value(Integer.class);
        long count = Q.New(SchedulerJobGroupJobRefVO.class)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, msg.getSchedulerJobGroupUuid())
                .count();

        if (count + msg.getSchedulerJobUuids().size() > limit) {
            throw new ApiMessageInterceptionException(argerr("job group has contained %d job, only %d seats left",
                    count, limit - count));
        }
    }

    private void validate(APIRunSchedulerTriggerMsg msg) {
        List<String> jobUuids = schedulerFacade.getJobUuids(msg.getUuid());
        if (msg.getJobUuids() == null) {
            msg.setJobUuids(jobUuids);
        } else {
            msg.getJobUuids().retainAll(jobUuids);
        }

        if (msg.getJobUuids().isEmpty()) {
            return;
        }
        List<String> groupUuids = Q.New(SchedulerJobGroupJobRefVO.class)
                .in(SchedulerJobGroupJobRefVO_.schedulerJobUuid, msg.getJobUuids())
                .select(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid)
                .listValues();
        msg.setJobGroupUuids(new HashSet<>(groupUuids));
    }
}
