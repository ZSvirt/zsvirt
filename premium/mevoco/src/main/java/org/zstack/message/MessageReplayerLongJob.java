package org.zstack.message;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.*;
import org.zstack.portal.apimediator.PortalSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2020/10/21.
 */
@LongJobFor(APIReplayMessageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MessageReplayerLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(MessageReplayerLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIReplayMessageMsg rmsg = JSONObjectUtil.toObject(job.getJobData(), APIReplayMessageMsg.class);

        List<ReplayMessageVO> replayVOs = Q.New(ReplayMessageVO.class)
                .isNull(ReplayMessageVO_.manageJobUuid)
                .eq(ReplayMessageVO_.locationUuid, rmsg.getLocationUuid()).list();

        replayVOs.forEach(vo -> vo.setManageJobUuid(job.getUuid()));
        dbf.updateCollection(replayVOs);

        new While<>(groupMsg(replayVOs).values()).step((vos, outCompl) -> {
            new While<>(vos).each((vo, compl) -> {
                // TODO: schema
                NeedReplyMessage msg = (NeedReplyMessage) CloudBusGson.fromJson(vo.getMsgDump());
                if (msg.getSystemTags() != null) {
                    msg.getSystemTags().removeIf(t -> PortalSystemTags.FOR_REPLAY.isMatch(t));
                }
                String originMessageUuid = msg.getId();
                msg.setId(Platform.getUuid());

                logger.debug(String.format("replay message[origin id:%s, current id: %s]", originMessageUuid, msg.getId()));

                msg.addSystemTag(PortalSystemTags.FOR_REPLAY.instantiateTag(Collections.singletonMap(PortalSystemTags.REPLAY_ID, vo.getId())));
                String serviceId = bus.getServiceId(msg.getServiceId());
                bus.makeLocalServiceId(msg, serviceId);
                bus.send(msg, new CloudBusCallBack(compl) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess() || reply.getError().rootCause().isError(SysErrors.RESOURCE_NOT_FOUND)) {
                            dbf.remove(vo);
                            compl.done();
                        } else {
                            // TODO error handle
                            compl.addError(reply.getError());
                            compl.allDone();
                        }
                    }
                });
            }).run(new WhileDoneCompletion(outCompl) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errorCodeList.getCauses().isEmpty()) {
                        outCompl.addError(errorCodeList.getCauses().get(0));
                    }
                    outCompl.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                cleanJobUuid(job.getUuid());
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success(new APIRelayMessageEvent(job.getApiId()));
                }
            }
        });
    }

    private Map<String, List<ReplayMessageVO>> groupMsg(List<ReplayMessageVO> replayVOs) {
        // TODO: use groupUuid not resourceUuid
        return replayVOs.stream().collect(Collectors.groupingBy(ReplayMessageVO::getResourceUuid));
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        completion.fail(operr("not supported"));
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        cleanJobUuid(job.getUuid());
        completion.fail(operr("not supported"));
    }

    private void cleanJobUuid(String jobUuid) {
        SQL.New(ReplayMessageVO.class).eq(ReplayMessageVO_.manageJobUuid, jobUuid)
                .set(ReplayMessageVO_.manageJobUuid, null)
                .update();
    }
}
