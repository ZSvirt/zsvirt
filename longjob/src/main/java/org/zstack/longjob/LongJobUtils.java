package org.zstack.longjob;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.progress.ActionProgressService;
import org.zstack.header.Constants;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.JobResultError;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LongJobUtils {
    private static final CLogger logger = Utils.getLogger(LongJobUtils.class);

    public static String succeeded = "Succeeded";

    private static Interner<String> jobUuids = Interners.newWeakInterner();

    private static List<LongJobState> completedStates = Arrays.asList(LongJobState.Failed, LongJobState.Succeeded, LongJobState.Canceled);
    private static List<LongJobState> canceledStates = Arrays.asList(LongJobState.Canceled, LongJobState.Canceling);

    public static ErrorCode cancelErr(String longJobUuid) {
        return Platform.err(LongJobErrors.CANCELED, "long job[uuid:%s] has been canceled", longJobUuid);
    }

    public static ErrorCode cancelErr(String longJobUuid, ErrorCode cause) {
        return err(LongJobErrors.CANCELED, "long job[uuid:%s] has been canceled", longJobUuid).withCause(cause);
    }

    public static ErrorCode noncancelableErr(String error) {
        return err(LongJobErrors.NONCANCELABLE, error);
    }

    public static ErrorCode noncancelableErr(String error, List<ErrorCode> causes) {
        ErrorCode err = noncancelableErr(error);
        err.setCauses(causes);
        return err;
    }

    public static ErrorCode interruptedErr(String longJobUuid, ErrorCode cause) {
        return err(LongJobErrors.INTERRUPTED,
                "some error interrupt long job[uuid:%s], analysis the cause to fix it and resume long job if you want to continue.",
                longJobUuid)
                .withCause(cause);
    }

    public static ErrorCode buildErrIfCanceled() {
        Tuple t = Q.New(LongJobVO.class).select(LongJobVO_.state, LongJobVO_.uuid)
                .eq(LongJobVO_.apiId, ThreadContext.get(Constants.THREAD_CONTEXT_API))
                .findTuple();

        return t == null || !canceledStates.contains(t.get(0, LongJobState.class)) ? null : cancelErr(t.get(1, String.class));
    }

    public static boolean jobCanceled(String longJobUuid) {
        LongJobState state = Q.New(LongJobVO.class).eq(LongJobVO_.uuid,longJobUuid).select(LongJobVO_.state).findValue();
        return canceledStates.contains(state);
    }

    public static boolean jobCanceled() {
        LongJobState state = Q.New(LongJobVO.class).select(LongJobVO_.state)
                .eq(LongJobVO_.apiId, ThreadContext.get(Constants.THREAD_CONTEXT_API))
                .findValue();
        return canceledStates.contains(state);
    }

    public static boolean jobCanceled(LongJobVO vo) {
        return canceledStates.contains(vo.getState());
    }

    static ErrorCode wrapDefaultError(LongJobVO job, ErrorCode err) {
        if (jobCanceled(job) && !err.isError(LongJobErrors.CANCELED)) {
            return cancelErr(job.getUuid(), err);
        } else {
            return err;
        }
    }

    public static LongJobVO setJobError(String uuid, ErrorCode error) {
        return updateByUuid(uuid, vo -> vo.setJobResult(errorCodeToJobResult(wrapDefaultError(vo, error))));
    }

    public static LongJobVO setJobResult(String uuid, String result) {
        return updateByUuid(uuid, vo -> vo.setJobResult(result));
    }

    public static LongJobVO setJobResult(String uuid, Object result) {
        return updateByUuid(uuid, vo -> vo.setJobResult(JSONObjectUtil.toJsonString(result)));
    }

    public static boolean jobCompleted(LongJobVO vo) {
        return completedStates.contains(vo.getState());
    }

    static LongJobVO updateByUuid(String uuid, Consumer<LongJobVO> consumer) {
        return new SQLBatchWithReturn<LongJobVO>(){

            @Override
            protected LongJobVO scripts() {
                LongJobVO job = findByUuid(uuid, LongJobVO.class);
                LongJobState originState = job.getState();
                consumer.accept(job);
                LongJobState newState = job.getState();

                if (jobCompleted(job)) {
                    setExecuteTimeIfNeed(job);
                    ActionProgressService.markEventPublished(job.getApiId());
                }

                if (originState != newState) {
                    logger.debug(String.format("change longjob [uuid:%s] state from %s to %s", uuid, originState, newState));
                }

                merge(job);
                return job;
            }
        }.execute();
    }

    static LongJobVO changeState(String uuid, LongJobStateEvent stateEvent) {
        synchronized (jobUuids.intern(uuid)) {
            return updateByUuid(uuid, it -> it.setState(it.getState().nextState(stateEvent)));
        }
    }

    static LongJobVO changeState(String uuid, LongJobStateEvent stateEvent, Consumer<LongJobVO> consumer) {
        synchronized (jobUuids.intern(uuid)) {
            return updateByUuid(uuid, it -> {
                it.setState(it.getState().nextState(stateEvent));
                consumer.accept(it);
            });
        }
    }

    static LongJobStateEvent getEventOnError(ErrorCode errorCode) {
        if (errorCode.isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR, LongJobErrors.INTERRUPTED)) {
            return LongJobStateEvent.suspend;
        } else if (errorCode.isError(LongJobErrors.CANCELED)) {
            return LongJobStateEvent.canceled;
        } else {
            return LongJobStateEvent.fail;
        }
    }

    private static void setExecuteTimeIfNeed(LongJobVO job) {
        if (job.getExecuteTime() == null) {
            long time = (System.currentTimeMillis() - job.getCreateDate().getTime()) / 1000;
            job.setExecuteTime(Long.max(time, 1));
            logger.info(String.format("longjob [uuid:%s] set execute time:%d.", job.getUuid(), time));
        }
    }

    public static String errorCodeToJobResult(ErrorCode errorCode) {
        if (errorCode == null) {
            return null;
        }
        JobResultError error = JobResultError.valueOf(errorCode);
        return JSONObjectUtil.toJsonString(error);
    }
}
