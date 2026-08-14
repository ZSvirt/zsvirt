package org.zstack.longjob;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.Constants;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.portal.apimediator.ApiMediator;
import org.zstack.tag.TagManager;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static org.zstack.core.Platform.argerr;

/**
 * Created by GuoYi on 12/6/17.
 */
@InterceptorForService("longjob")
public class LongJobApiInterceptor implements ApiMessageInterceptor, Component {
    private static final CLogger logger = Utils.getLogger(LongJobApiInterceptor.class);

    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiMediator apiMediator;

    /**
     * Key:LongJobName
     */
    private TreeMap<String, Class<APIMessage>> apiMsgOfLongJob = new TreeMap<>();

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof LongJobMessage) {
            routeMsg((LongJobMessage) msg);
        }

        if (msg instanceof APISubmitLongJobMsg) {
            validate((APISubmitLongJobMsg) msg);
        } else if (msg instanceof APICancelLongJobMsg) {
            validate((APICancelLongJobMsg) msg);
        } else if (msg instanceof APIDeleteLongJobMsg) {
            validate((APIDeleteLongJobMsg) msg);
        } else if (msg instanceof APIRerunLongJobMsg) {
            validate((APIRerunLongJobMsg) msg);
        } else if (msg instanceof APIResumeLongJobMsg) {
            validate((APIResumeLongJobMsg) msg);
        }

        return msg;
    }

    private void routeMsg(LongJobMessage msg) {
        String mnId = Q.New(LongJobVO.class).select(LongJobVO_.managementNodeUuid)
                .eq(LongJobVO_.uuid, msg.getLongJobUuid())
                .findValue();

        if (mnId == null) {
            bus.makeTargetServiceIdByResourceUuid((Message) msg, LongJobConstants.SERVICE_ID, msg.getLongJobUuid());
        } else {
            bus.makeServiceIdByManagementNodeId((Message) msg, LongJobConstants.SERVICE_ID, mnId);
        }
    }

    private void validate(APISubmitLongJobMsg msg) {
        Class<APIMessage> apiClass = apiMsgOfLongJob.get(msg.getJobName());
        if (null == apiClass) {
            throw new ApiMessageInterceptionException(argerr("%s is not an API", msg.getJobName()));
        }
        APIMessage jobMsg = JSONObjectUtil.toObject(msg.getJobData(), apiClass);
        jobMsg.setSession(msg.getSession());

        try {
            jobMsg = apiMediator.getProcesser().process(jobMsg);                     // may throw ApiMessageInterceptionException
        } catch (StopRoutingException e) {
            // if got stop routing exception persist a success long job and return event success
            LongJobVO vo = createSuccessLongJob(msg);

            APISubmitLongJobEvent evt = new APISubmitLongJobEvent(msg.getId());
            evt.setInventory(LongJobInventory.valueOf(vo));
            evt.setNeedAudit(false);
            bus.publish(evt);

            throw e;
        }

        msg.setJobData(JSONObjectUtil.toJsonString(jobMsg));    // msg may be changed during validation

        if (msg.getResourceUuid() != null) {
            bus.makeTargetServiceIdByResourceUuid(msg, LongJobConstants.SERVICE_ID, msg.getResourceUuid());
        }
    }

    private LongJobVO createSuccessLongJob(APISubmitLongJobMsg msg) {
        // create LongJobVO
        LongJobVO vo = new LongJobVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        if (msg.getName() != null) {
            vo.setName(msg.getName());
        } else {
            vo.setName(msg.getJobName());
        }
        vo.setDescription(msg.getDescription());
        vo.setApiId(ThreadContext.getImmutableContext().get(Constants.THREAD_CONTEXT_API));
        vo.setJobName(msg.getJobName());
        vo.setJobData(msg.getJobData());
        vo.setState(LongJobState.Succeeded);
        vo.setJobResult(LongJobUtils.succeeded);
        vo.setTargetResourceUuid(msg.getTargetResourceUuid());
        vo.setManagementNodeUuid(Platform.getManagementServerId());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);
        msg.setJobUuid(vo.getUuid());
        tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vo.getUuid(), LongJobVO.class.getSimpleName());

        return vo;
    }

    private void validate(APICancelLongJobMsg msg) {
        LongJobState state = Q.New(LongJobVO.class)
                .select(LongJobVO_.state)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .findValue();

        if (state == LongJobState.Succeeded) {
            throw new ApiMessageInterceptionException(argerr("cannot cancel longjob that is succeeded"));
        }
        if (state == LongJobState.Failed) {
            throw new ApiMessageInterceptionException(argerr("cannot cancel longjob that is failed"));
        }
    }

    private void validate(APIDeleteLongJobMsg msg) {
        LongJobState state = Q.New(LongJobVO.class)
                .select(LongJobVO_.state)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .findValue();

        if (state != LongJobState.Succeeded && state != LongJobState.Canceled && state != LongJobState.Failed) {
            throw new ApiMessageInterceptionException(argerr("delete longjob only when it's succeeded, canceled, or failed"));
        }
    }

    private void validate(APIRerunLongJobMsg msg) {
        LongJobVO vo = Q.New(LongJobVO.class)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .find();

        LongJobState state = vo.getState();
        if (state != LongJobState.Succeeded && state != LongJobState.Canceled && state != LongJobState.Failed) {
            throw new ApiMessageInterceptionException(argerr("rerun longjob only when it's succeeded, canceled, or failed"));
        }

        Class<APIMessage> apiClass = apiMsgOfLongJob.get(vo.getJobName());

        APIMessage jobMsg = JSONObjectUtil.toObject(vo.getJobData(), apiClass);
        jobMsg.setSession(msg.getSession());

        try {
            apiMediator.getProcesser().process(jobMsg);                     // may throw ApiMessageInterceptionException
        } catch (StopRoutingException e) {
            APISubmitLongJobEvent evt = new APISubmitLongJobEvent(msg.getId());
            evt.setInventory(LongJobInventory.valueOf(vo));
            evt.setNeedAudit(false);
            bus.publish(evt);

            throw e;
        }

        dbf.updateAndRefresh(vo);
    }

    private void validate(APIResumeLongJobMsg msg) {
        Tuple t = Q.New(LongJobVO.class)
                .select(LongJobVO_.state, LongJobVO_.managementNodeUuid)
                .eq(LongJobVO_.uuid, msg.getUuid())
                .findTuple();

        if (t.get(0, LongJobState.class) != LongJobState.Suspended) {
            throw new ApiMessageInterceptionException(argerr("can only resume longjob that is Suspended"));
        }

        Optional.ofNullable(t.get(1, String.class)).ifPresent(mnId ->
                bus.makeServiceIdByManagementNodeId(msg, LongJobConstants.SERVICE_ID, mnId));
    }

    @Override
    public boolean start() {
        Class<APIMessage> apiClass;
        Set<Class<?>> longJobClasses = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            try {
                apiClass = (Class<APIMessage>) Class.forName(at.value().getName());
            } catch (ClassNotFoundException | ClassCastException e) {
                //ApiMessage and LongJob are not one by one corresponding ,so we skip it
                e.printStackTrace();
                continue;
            }
            logger.debug(String.format("[LongJob] collect api class [%s]", apiClass.getSimpleName()));
            apiMsgOfLongJob.put(at.value().getSimpleName(), apiClass);
        }
        return true;
    }

    @Override
    public boolean stop() {
        apiMsgOfLongJob.clear();
        return true;
    }
}
