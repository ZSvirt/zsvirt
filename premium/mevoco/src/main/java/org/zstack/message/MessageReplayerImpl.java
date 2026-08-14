package org.zstack.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.message.*;
import org.zstack.longjob.LongJobManager;
import org.zstack.portal.apimediator.PortalSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by MaJin on 2020/10/21.
 */
public class MessageReplayerImpl implements MessageReplayer, Component {
    private static final CLogger logger = Utils.getLogger(MessageReplayerImpl.class);

    private Map<Class<? extends ReplayableMessage>, Function> groupBuilders = new HashMap<>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private LongJobManager longJobManager;

    @Override
    public long saveMessage(ReplayableMessage msg, String locationUuid, String locationType) {
        ReplayMessageVO vo = new ReplayMessageVO();
        vo.setMsgDump(CloudBusGson.toJson(msg));
        vo.setLocationType(locationType);
        vo.setLocationUuid(locationUuid);
        vo.setResourceUuid(msg.getResourceUuid());
        vo.setGroupUuid(getGroupUuid(msg));
        vo = dbf.persistAndRefresh(vo);

        logger.debug(String.format("saved message[class:%s, id: %d] on %s[uuid:%s]",
                msg.getClass().getSimpleName(), vo.getId(), locationType, locationUuid));
        return vo.getId();
    }

    @Override
    public void removeReplayMessage(long replayId) {
        logger.debug(String.format("remove replay message[id: %d]", replayId));
        SQL.New(ReplayMessageVO.class).eq(ReplayMessageVO_.id, replayId).delete();
    }

    @Override
    public void removeResourceReplayMessage(String resourceUuid, String locationUuid) {
        logger.debug(String.format("remove replay message[resourceUuid: %s, locationUuid:%s]", resourceUuid, locationUuid));
        SQL.New(ReplayMessageVO.class).eq(ReplayMessageVO_.locationUuid, locationUuid)
                .eq(ReplayMessageVO_.resourceUuid, resourceUuid)
                .delete();
    }

    @Override
    public boolean needReplay(String locationUuid, String locationType) {
        return Q.New(ReplayMessageVO.class).eq(ReplayMessageVO_.locationUuid, locationUuid).isExists();
    }

    @Override
    public void replayMessage(String locationUuid, String locationType, Completion completion) {
        String details = String.format("replay message related to [%s:%s]", locationType, locationUuid);
        boolean alreadyRun = Q.New(LongJobVO.class).eq(LongJobVO_.description, details)
                .eq(LongJobVO_.state, LongJobState.Running)
                .isExists();
        if (alreadyRun) {
            logger.debug(String.format("There is already a replay longjob related to[%s:%s] running", locationType, locationUuid));
            completion.success();
        }

        // TODO: ensure only on job running.

        logger.debug(details);
        APIReplayMessageMsg rmsg = new APIReplayMessageMsg();
        rmsg.setLocationType(locationType);
        rmsg.setLocationUuid(locationUuid);

        SubmitLongJobMsg msg = new SubmitLongJobMsg();
        msg.setJobData(JSONObjectUtil.toJsonString(rmsg));
        msg.setName("ReplayMessageJob");
        msg.setDescription(details);
        msg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        msg.setJobName(APIReplayMessageMsg.class.getSimpleName());
        msg.setTargetResourceUuid(locationUuid);
        longJobManager.submitLongJob(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                }
            }
        }, event -> {
            if (event.isSuccess()) {
                completion.success();
            } else {
                completion.fail(event.getError());
            }
        });
    }

    @Override
    public <T extends ReplayableMessage> void registryMessageGroupBuilder(Class<T> msgClz, Function<T, String> groupBuilder) {
        groupBuilders.put(msgClz, groupBuilder);
    }

    @Override
    public String getReplayLocationUuid(NeedReplyMessage msg) {
        if (msg.getSystemTags() != null) {
            for (String tag : msg.getSystemTags()) {
                if (PortalSystemTags.FOR_REPLAY.isMatch(tag)) {
                    long replayId = Long.parseLong(PortalSystemTags.FOR_REPLAY.getTokenByTag(tag, PortalSystemTags.REPLAY_ID));
                    return Q.New(ReplayMessageVO.class).eq(ReplayMessageVO_.id, replayId)
                            .select(ReplayMessageVO_.locationUuid).findValue();
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String getGroupUuid(ReplayableMessage msg) {
        Function builder = groupBuilders.get(msg.getReplayableClass());
        if (builder == null) {
            return null;
        } else {
            return (String) builder.apply(msg);
        }
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
