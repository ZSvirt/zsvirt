package org.zstack.tag2;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.Message;
import org.zstack.header.tag.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

public class Tag2ManagerImpl extends AbstractService implements ResourceOwnerAfterChangeExtensionPoint,
        ResourceOwnerPreChangeExtensionPoint, ReportQuotaExtensionPoint, CreateTagFromMsgExtensionPoint {
    private static final CLogger logger = Utils.getLogger(Tag2ManagerImpl.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof TagPatternMessage) {
            passThrough((TagPatternMessage) msg);
        } else if (msg instanceof APICreateTagMsg) {
            handle((APICreateTagMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(TagPatternMessage msg) {
        TagPatternVO vo = dbf.findByUuid(msg.getTagPatternUuid(), TagPatternVO.class);

        if (vo == null) {
            String err = String.format("Cannot find TagPattern[uuid:%s], it may have been deleted", msg.getTagPatternUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        TagPatternBase base = new TagPatternBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateTagMsg msg) {
        APICreateTagEvent evt = new APICreateTagEvent(msg.getId());

        TagPatternVO vo = new TagPatternVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setValue(msg.getValue());
        vo.setDescription(msg.getDescription());
        vo.setColor(msg.getColor());
        vo.setType(TagPatternType.valueOf(msg.getType()));
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        ensureUniquenessMerge(vo, msg.getSession().getAccountUuid(), () -> dbf.persist(vo));

        evt.setInventory(TagPatternInventory.valueOf(dbf.reload(vo)));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(Tag2Constant.SERVICE_ID);
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory origin, String newOwnerUuid) {
        if (origin.getResourceType().equals(TagPatternVO.class.getSimpleName())) {
            handlePatternOwnerChange(origin, newOwnerUuid);
        } else {
            handleResourceOwnerChange(origin, newOwnerUuid);
        }
    }

    @Override
    public void resourceOwnerPreChange(AccountResourceRefInventory origin, String newOwnerUuid) {
        if (origin.getResourceType().equals(TagPatternVO.class.getSimpleName())) {
            TagPatternVO pattern = Q.New(TagPatternVO.class).eq(TagPatternVO_.uuid, origin.getResourceUuid()).find();
            ensureUniquenessMerge(pattern, newOwnerUuid, null);
        }
    }

    @Transactional
    private void handlePatternOwnerChange(AccountResourceRefInventory origin, String newOwnerUuid) {
        if (newOwnerUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
            return;
        }

        String sql = "delete tag from UserTagVO tag, AccountResourceRefVO ref" +
                " where tag.tagPatternUuid = :patternUuid" +
                " and tag.resourceUuid = ref.resourceUuid" +
                " and ref.accountUuid != :newOwnerUuid" +
                " and ref.type = :type";
        dbf.getEntityManager().createNativeQuery(sql)
                .setParameter("patternUuid", origin.getResourceUuid())
                .setParameter("newOwnerUuid", newOwnerUuid)
                .setParameter("type", AccessLevel.Own.toString())
                .executeUpdate();
    }

    @Transactional
    private void handleResourceOwnerChange(AccountResourceRefInventory origin, String newOwnerUuid) {
        if (origin.getAccountUuid().equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
            return;
        }

        String sql = "delete tag from UserTagVO tag, AccountResourceRefVO ref" +
                " where tag.resourceUuid = :uuid" +
                " and tag.tagPatternUuid = ref.resourceUuid" +
                " and ref.accountUuid = :originOwnerUuid" +
                " and ref.type = :type";
        dbf.getEntityManager().createNativeQuery(sql)
                .setParameter("uuid", origin.getResourceUuid())
                .setParameter("originOwnerUuid", origin.getAccountUuid())
                .setParameter("type", AccessLevel.Own.toString())
                .executeUpdate();
    }

    static synchronized void ensureUniquenessMerge(TagPatternVO newTag, String accountUuid, Runnable updater) {
        long count = getDuplicateTagUuids(newTag, accountUuid).size();
        if (count > 0) {
            throw new OperationFailureException(err(TagErrors.DUPLICATED_TAG,
                    "you already has a tag which [name:%s, color:%s]", newTag.getName(), newTag.getColor()));
        }

        Optional.ofNullable(updater).ifPresent(Runnable::run);
    }

    private static List<String> getDuplicateTagUuids(TagPatternVO vo, String accountUuid) {
        List<Tuple> ts = SQL.New("select pattern.uuid, pattern.name from TagPatternVO pattern, AccountResourceRefVO ref" +
                " where pattern.name = :name" +
                " and pattern.color " + (StringUtils.isEmpty(vo.getColor()) ? "is null" : String.format("= '%s'", vo.getColor())) +
                " and pattern.type = :type" +
                " and pattern.uuid != :uuid" +
                " and pattern.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :accountUuid" +
                " and ref.type = :refType", Tuple.class)
                .param("name", vo.getName())
                .param("type", vo.getType())
                .param("uuid", vo.getUuid())
                .param("accountUuid", accountUuid)
                .param("refType", AccessLevel.Own)
                .list();
        return ts.stream().filter(it -> it.get(1, String.class).equals(vo.getName()))
                .map(it -> it.get(0, String.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new TagPatternTotalNumDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateTagMsg.class)
                .addCounterQuota(Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(TagPatternVO.class)
                        .eq(TagPatternVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM));
        return Collections.singletonList(quota);
    }

    @Override
    public void afterCreateTagFromMsg(APICreateMessage msg, String resourceUuid) {
        if (CollectionUtils.isEmpty(msg.getTagUuids())) {
            return;
        }

        List<AttachTagToResourcesMsg> amsgs = msg.getTagUuids().stream().map(it -> {
            AttachTagToResourcesMsg amsg = new AttachTagToResourcesMsg();
            amsg.setTagUuid(it);
            amsg.setResourceUuids(Collections.singletonList(resourceUuid));
            bus.makeTargetServiceIdByResourceUuid(amsg, Tag2Constant.SERVICE_ID, it);
            return amsg;
        }).collect(Collectors.toList());

        new While<>(amsgs).step((amsg, compl) -> {
            bus.send(amsg);
            compl.done();
        }, 3).run(new NopeWhileDoneCompletion());
    }

}
